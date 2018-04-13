/*
package com.kangaroo.handler.configure;

import com.kangaroo.*;
import com.kangaroo.component.AutoComponentConfigurable;
import com.kangaroo.component.ComponentConfigurable;
import com.kangaroo.component.ComponentDefinition;
import com.kangaroo.component.DefaultComponentContext;
import com.kangaroo.component.scan.AnnotationScannerBuilder;
import com.kangaroo.component.scan.ConfigurableComponentContext;
import com.kangaroo.component.scan.DefaultClassFilter;
import com.kangaroo.handler.*;
import com.kangaroo.internal.observable.Observers;
import com.kangaroo.util.CUtils;
import com.kangaroo.util.ObjectUtil;
import com.kangaroo.util.PathBuilder;
import com.kangaroo.util.StrUtils;
import com.kangaroo.util.concurrent.DefaultFuture;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.observers.DefaultObserver;
import io.reactivex.observers.SafeObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.function.Function;

import static com.kangaroo.handler.configure.RequestHandlerHolder.*;

*/
/**
 * Created by woonill on 29/11/2016.
 *//*

public abstract class AbstractRequestHandlerHolderContext {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final int DEFAULT_EXEC_THREAD_NUM = Runtime.getRuntime().availableProcessors() * 4;

    private final RequestHandlerHolder[] rootHolders;
    private final EventExecutorGroup threadExecutor;
    private AtomicBoolean _started = new AtomicBoolean(true);

    private final Observer<EventDispacher.Event> requestEventNotifier;

    private AbstractRequestHandlerHolderContext(int exeCore, RequestHandlerHolder[] holders) {
        //       this.rootHolders = this.init();
        this.threadExecutor = new DefaultEventExecutorGroup(exeCore);
        this.rootHolders = holders;
        this.requestEventNotifier = initRequestEventNotifier();
    }


    public RequestObserver requestSubscriber() {

        return new RequestObserver() {

            @Override
            public Observable<Response> observe(Request request) {

                return Observable.create(new ObservableOnSubscribe<Response>() {
                    @Override
                    public void subscribe(ObservableEmitter<Response> observableEmitter) throws Exception {
                        threadExecutor.submit(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    logger.debug("Start run Handle Request now");
                                    Response res = handle(request).handle();
                                    logger.debug("Start handle response now:" + res);
                                    observableEmitter.onNext(res);
                                } catch (Throwable te) {
                                    observableEmitter.onError(te);
                                } finally {
                                    observableEmitter.onComplete();
                                }
                            }
                        });
                    }
                });
            }
        };
    }


    public ExecutorService getExecutorService() {
        return this.threadExecutor;
    }


    private Map<String, Observer<EventDispacher.Event>> globalEventConsumers = new ConcurrentHashMap<>();

    public RequestHandlerContext handle(Request request) {

        RequestHandlerHolder.RequestHandlerHolderPipeline pipeline = toHolderPipeline();
        final AtomicBoolean _isRespnosed = new AtomicBoolean(false);
        final Map<String, Object> globalProps = new ConcurrentHashMap<>();
        final List<EventDispacher.Event> tempEventList = new LinkedList<>();
        final EventHandlerCount eventHandlerCount = getEventHandlerCount(tempEventList);

        return new RequestHandlerContext() {

            RequestHandlerHolder current;

            @Override
            public Response handle() {
                return handle(Collections.EMPTY_MAP);
            }


            @Override
            public Response handle(Map<String, Object> props) {

                if (!pipeline.isLast()) {

                    current = pipeline.next();

                    if (props != null && !props.isEmpty()) {
                        globalProps.putAll(props);
                    }

                    logger.debug("Start handler:" + current.targetHandler().getClass().getName());

                    final Response response = current.handler().handle(this);

//                    logger.debug("Status:"+response.status()+" Type:"+ Message.Type.get(response.getTypeCode())+" body:"+new String(response.body()));

                    if (response != null && !response.isNone()) {
                        _isRespnosed.compareAndSet(false, true);
                        return response;
                    }
                    return handle();
                }

                _isRespnosed.compareAndSet(false, true);

                globalProps.clear();

                return new DefaultResponse.Builder(request)
                        .payload("no handler".getBytes())
                        .theType(Message.Type.text)
                        .statusCode(Message.BAD_REQUEST)
                        .build();
            }

            @Override
            public Map<String, Object> props() {
                return Collections.synchronizedMap(globalProps);
            }


            public EventDispacher getDispacher(String name) {

                Observer<EventDispacher.Event> targetConsumer = globalEventConsumers.get(name);
                if (targetConsumer == null) {
                    return null;
                }
                return new EventDispacher() {

                    @Override
                    public Future<?> submit(EventDispacher.Event event) {

                        if (_isRespnosed.get()) {
                            return DefaultFuture.errorFuture(new OutOfStateException("Request:" + request.id() + " is responsed"));
                        }

                        eventHandlerCount.add();
                        return threadExecutor.submit(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    targetConsumer.onNext(event);
                                    tempEventList.add(event);
                                    eventHandlerCount.down();
                                } catch (Throwable te) {
                                    tempEventList.add(new EventDispacher.ErrorEvent(event, te));
                                    eventHandlerCount.down();
                                    throw te;
                                }
                            }
                        });
                    }
                };
            }

            @Override
            public Request request() {
                return request;
            }

            @Override
            public ComponentContext getComponents() {
                return current.componentContext();
            }

            @Override
            public ComponentContext getComponents(String name) {

                final RequestHandlerHolder holder = getHolder(name);
                if (holder == null) {
                    return ComponentContext.NONE;
                }
                return holder.componentContext();
            }

            public Future<?> runTask(Runnable runnable) {
                if (!_isRespnosed.get()) {
                    return threadExecutor.submit(runnable);
                }
                throw new IllegalArgumentException("error RequestHandleContext is out");

            }
        };
    }


    public EventHandlerCount getEventHandlerCount(List<EventDispacher.Event> events) {

        return new EventHandlerCount() {

            @Override
            void update() {
                if (!events.isEmpty()) {
                    threadExecutor.submit(new Runnable() {
                        @Override
                        public void run() {
                            for (EventDispacher.Event event : events) {
                                notifyEvent(event);
                            }
                        }
                    });
                }
            }
        };
    }


    public void notifyEvent(EventDispacher.Event event) {
        requestEventNotifier.onNext(event);
*/
/*
        requestEventObservers.forEach(new BiConsumer<String, Observer<EventProducer.Event>>() {
            @Override
            public void accept(String s, Observer<EventProducer.Event> eventObserver) {
                eventObserver.onNext(event);
            }
        });
*//*


    }

    private static final AtomicIntegerFieldUpdater<EventHandlerCount> counterUpdater = AtomicIntegerFieldUpdater.newUpdater(EventHandlerCount.class, "count");

    abstract static class EventHandlerCount {

        volatile int count = 0;

        private int add() {
            return counterUpdater.addAndGet(this, 1);
        }

        private int down() {
            int res = counterUpdater.decrementAndGet(this);
            if (res == 0) {
                update();
            }
            return res;
        }

        public int getCount() {
            return count;
        }

        abstract void update();
    }


    private Observer<EventDispacher.Event> initRequestEventNotifier() {


        logger.debug("Start handler EventObserver ------- \n \n");

        final RequestHandlerHolderPipeline pipeline = this.toHolderPipeline();
        final Map<String, Observer<EventDispacher.Event>> observerMap = new ConcurrentHashMap<>();
        while (!pipeline.isLast()) {

            final RequestHandlerHolder next = pipeline.next();

            logger.debug("RequestHandlerHolder:" + next.name());

            final Observer<EventDispacher.Event> eventObserver = next.targetHandler().getEventObserver();
            if (eventObserver != null && !Observers.empty().equals(eventObserver)) {
                logger.info("Add RequestEventObserver:" + next.name() + " and class:" + eventObserver.getClass().getName());
                observerMap.put(next.name(), toSaveEventObserver(eventObserver));
            }
        }
        return toObserver(observerMap);
    }

    Observer<EventDispacher.Event> toObserver(Map<String, Observer<EventDispacher.Event>> observerMap) {

        if (observerMap == null || observerMap.isEmpty()) {
            return Observers.empty();
        }

        Collection<Observer<EventDispacher.Event>> observers = observerMap.values();
        return new DefaultObserver<EventDispacher.Event>() {
            @Override
            public void onNext(EventDispacher.Event event) {
                for (Observer<EventDispacher.Event> observer : observers) {
                    observer.onNext(event);
                }
            }

            @Override
            public void onError(Throwable throwable) {
            }

            @Override
            public void onComplete() {
            }
        };
    }

    Observer<EventDispacher.Event> toSaveEventObserver(Observer eventObserver) {

        if (eventObserver instanceof SafeObserver) {
            return eventObserver;
        }

        return new SafeObserver<EventDispacher.Event>(new DefaultObserver<EventDispacher.Event>() {
            @Override
            public void onNext(EventDispacher.Event event) {
                eventObserver.onNext(event);
            }

            @Override
            public void onError(Throwable error) {
                eventObserver.onError(error);
            }

            @Override
            public void onComplete() {
                eventObserver.onComplete();
            }
        });
    }


    public RequestHandlerHolder.RequestHandlerHolderPipeline toHolderPipeline() {
        return new RequestHandlerHolderPipelineSComposite(this.rootHolders);
    }


    public RequestHandlerHolder getHolder(String name) {


        final RequestHandlerHolderPipeline pipeline = toHolderPipeline();
        while (pipeline.isLast()) {

            final RequestHandlerHolder next = pipeline.next();
            if (next.name().equalsIgnoreCase(name)) {
                return next;
            }
        }

        return null;
    }

    public int holderSize() {
        return this.rootHolders.length;
    }

    public final Future<?> abort() {


        if (!_started.compareAndSet(true, false)) {
            throw new IllegalArgumentException("Stoped");
        }
        return threadExecutor.shutdownGracefully();
    }


    static List<Class<?>> findComponentsForAutoComponent(Class<?>[] comsp, URL from) {
        if (comsp != null && comsp.length > 0) {
            List<Class<?>> cls = AnnotationScannerBuilder.ins().scan(new DefaultClassFilter(comsp), from);
            if (cls != null) {
                for (Class<?> component : cls) {
//                    logger.info("Find AutoComponent:" + component.getName());
                }
                return cls;
            }
        }
        return Collections.EMPTY_LIST;
    }

    public final static class Builder {

        private ComponentConfigurable componentConfigurable;
        private Set<Class<? extends Annotation>> scanComponents = new HashSet<>();
        private List<HolderConfiguration> initializers = new LinkedList<>();
        private URL scanRoot;
        private String handleRoot;
        private int executorThread = AbstractRequestHandlerHolderContext.DEFAULT_EXEC_THREAD_NUM;


        private Class<? extends ComponentContextFactory> componentContextClass = null;
        private Map<String, Function<EventConsumerConfigurable, Observer<EventDispacher.Event>>> eventConsumerMap = new HashMap<>();

        private Logger logger = LoggerFactory.getLogger(this.getClass());


        public Builder() {
        }

        public Builder componentConfigurable(ComponentConfigurable cc) {
            this.componentConfigurable = cc;
            return this;
        }

        public Builder addHandler(String name, Class<? extends RequestHandlerInitializer> init, String... mappings) {
            RequestHandlerInitializer handlerInitializer = (RequestHandlerInitializer) CUtils.getInstance(init);
            return this.addHandler(name, handlerInitializer, mappings);
        }

        public Builder addHandler(String name, RequestHandlerInitializer rInitializer, String... mappings) {
            this.initializers.add(new HolderConfiguration.Builder(name).addMappingUri(mappings).build(rInitializer));
            return this;
        }

        public Builder addHandler(String name, RequestHandler handler, String... mappings) {
            this.initializers.add(new HolderConfiguration.Builder(name).addMappingUri(mappings).build(handler));
            return this;
        }

        public Builder addHandler(HolderConfiguration initializer) {
            this.initializers.add(initializer);
            return this;
        }

        public Builder withScanComponent(URL url, Class<? extends Annotation>... compAnno) {

            if (!ObjectUtil.isEmpty(compAnno)) {
                this.scanComponents.addAll(Arrays.asList(compAnno));
                this.scanRoot = url;
            }
            return this;
        }


        public Builder addEventConsumer(
                String name,
                Observer<EventDispacher.Event> eventObserver) {
            this.eventConsumerMap.put(name, new DefaultEventConsumerInit(eventObserver));
            return this;
        }

        public Builder addEventConsumer(
                String name,
                Function<EventConsumerConfigurable, Observer<EventDispacher.Event>> initFunc) {
            this.eventConsumerMap.put(name, initFunc);
            return this;
        }


        public Builder executorThread(int size) {
            if (size > 0) {
                this.executorThread = size;
            }
            return this;
        }


        public Builder rootPath(String handleRoot) {
            this.handleRoot = handleRoot;
            return this;
        }


        public AbstractRequestHandlerHolderContext build() {
            return this.build(new Function<RequestHandlerHolders, RequestHandlerHolder[]>() {
                @Override
                public RequestHandlerHolder[] apply(RequestHandlerHolders in) {
                    return in.buildToArray();
                }
            });
        }

        public AbstractRequestHandlerHolderContext build(Function<RequestHandlerHolders, RequestHandlerHolder[]> initBuilder) {


            this.componentContextClass = componentContextClass == null ? DefaultComponentContext.DefaultComponentContextFactory.class : componentContextClass;
            final ComponentContextFactory cc = (ComponentContextFactory) CUtils.getInstance(componentContextClass);

            final int executorThreadCore = this.executorThread;


            List<Class<?>> scanComps = new LinkedList<>();
            if (!scanComponents.isEmpty()) {
                logger.info("Scan class root url:" + scanRoot.getPath());
                Class<?>[] comsp = scanComponents.toArray(new Class<?>[scanComponents.size()]);
                scanComps.addAll(findComponentsForAutoComponent(comsp, Builder.this.scanRoot));
            }
            logger.debug("Find :" + scanComps.size() + " Annotation Object");
            if (componentConfigurable == null) {
                componentConfigurable = ComponentConfigurable.NONE;
            }
            ConfigurableComponentContext ccc = cc.get(componentConfigurable.getDefinitions());
            final RequestHandlerHolder[] holders = createHolders(scanComps, ccc, initBuilder);


            Map<String, Observer<EventDispacher.Event>> globalEventConsumers = initEventConsumer(this.eventConsumerMap, ccc, scanComps);

            AbstractRequestHandlerHolderContext context = new AbstractRequestHandlerHolderContext(executorThreadCore, holders) {
            };

            context.globalEventConsumers.putAll(globalEventConsumers);
            return context;


        }

        private Map<String, Observer<EventDispacher.Event>> initEventConsumer(
                Map<String, Function<EventConsumerConfigurable, Observer<EventDispacher.Event>>> eventConsumerMap,
                ConfigurableComponentContext configurableComponentContext,
                List<Class<?>> scanComps) {

            EventConsumerConfigurable ecc = new EventConsumerConfigurable(configurableComponentContext, scanComps);

            Map<String, Observer<EventDispacher.Event>> consumerMap = new HashMap<>();
            final Set<Map.Entry<String, Function<EventConsumerConfigurable, Observer<EventDispacher.Event>>>> entries = eventConsumerMap.entrySet();
            for (Map.Entry<String, Function<EventConsumerConfigurable, Observer<EventDispacher.Event>>> entry : entries) {

                final Observer<EventDispacher.Event> apply = entry.getValue().apply(ecc);
                consumerMap.put(entry.getKey(), apply);
            }
            return consumerMap;
        }


        private RequestHandlerHolder[] createHolders(
                List<Class<?>> scanComps,
                ConfigurableComponentContext ccc,
                Function<RequestHandlerHolders, RequestHandlerHolder[]> initBuilder) {


            String thePath = StrUtils.isNull(handleRoot) ? "/" : handleRoot;
            RequestHandlerPathBuilder appPathBuilder = getDefaultPathBuilder(thePath);

            SRequestHandlerHolderConfigurables acc = new SRequestHandlerHolderConfigurables(appPathBuilder);
            final Observable<Class<?>> comps = Observable.fromIterable(scanComps);
            HandlerConfigurable ec = new HandlerConfigurable.DefaultHandlerConfigurable(comps, ccc.injector());
            RequestHandlerHolderConfigurable rhhc = acc.newConfigurable(ccc, comps);

            List<RequestHandlerHolder> holderList = new ArrayList<RequestHandlerHolder>();
            for (HolderConfiguration init : initializers) {
                RequestHandlerHolder holder = init.build(rhhc);
                logger.debug("Regist holder:" + holder.name());
                holderList.add(holder);
            }
            RequestHandlerHolder[] holders = holderList.toArray(new RequestHandlerHolder[holderList.size()]);
            RequestHandlerHolders holdersArray = acc.newHolders(ccc, holders);
            return initBuilder.apply(holdersArray);

        }

    }

    static final RequestHandlerPathBuilder getDefaultPathBuilder(String rootPath) {

        return new RequestHandlerPathBuilder() {
            @Override
            public String[] apply(String[] in) {
                String[] strs = new String[in.length];
                for (int i = 0; i < in.length; i++) {
                    String uriPath = in[i];
                    strs[i] = new PathBuilder(rootPath).append(uriPath).toString();
                }
                return strs;
            }
        };
    }


    static final class SRequestHandlerHolderConfigurables {

        private Logger logger = LoggerFactory.getLogger(this.getClass());

        private RequestHandlerPathBuilder appPathBuilder;
        private HolderFactory holderFactory = new HolderFactory();

        private SRequestHandlerHolderConfigurables(
                RequestHandlerPathBuilder rhp) {
            this.appPathBuilder = rhp;


        }

        public RequestHandlerHolders newHolders(
                ConfigurableComponentContext ccc,
                RequestHandlerHolder... holders) {
            return RequestHandlerHolder.newHolders(ccc, new HolderFactory() {

                @Override
                protected String[] mappingBuild(String... mappings) {
                    return appPathBuilder.apply(mappings);
                }
            }, holders);
        }

        public RequestHandlerHolderConfigurable newConfigurable(
                ConfigurableComponentContext ccc,
                Observable<Class<?>> comps) {

            return new RequestHandlerHolderConfigurable() {
                @Override
                public RequestHandlerHolder newHolder(
                        String name,
                        RequestHandlerInitializer initializer,
                        ComponentConfigurable cc,
                        String[] uriMaps,
                        RequestHandlerHolder... children) {

                    ComponentConfigurable scc = cc == null ? ComponentConfigurable.NONE : cc;
                    ComponentDefinition[] cds = scc.getDefinitions();
                    ComponentContext cc2 = ccc.creatSub(name, cds);

                    RequestHandler handler = initializer.init(new RequestHandlerConfigurable(name, comps, cc2));
                    return holderFactory.newHolder(name, handler, appPathBuilder.apply(uriMaps), cc2, children);
                }

                @Override
                public ComponentConfigurable newComponentConfigurable(AutoComponentConfigurable acc) {
                    return acc.init(comps);
                }
            };
        }
    }


    public interface RequestHandlerHolderConfigurable {

        RequestHandlerHolder newHolder(
                String name,
                RequestHandlerInitializer initializer,
                ComponentConfigurable cc,
                String[] uriMaps,
                RequestHandlerHolder... children);

        ComponentConfigurable newComponentConfigurable(AutoComponentConfigurable acc);
    }


    public static final class EventConsumerConfigurable {

        private GroupComponentContext contextGroup;
        private List<Class<?>> scanComponents;

        EventConsumerConfigurable(GroupComponentContext contextGroup) {
            this.contextGroup = contextGroup;
        }

        public EventConsumerConfigurable(GroupComponentContext ccc, List<Class<?>> scanComps) {
            this.contextGroup = ccc;
            this.scanComponents = scanComps;
        }


        public ComponentContext getComponentContext() {
            return this.contextGroup;
        }

        public ComponentContext getComponents(String name2) {
            return contextGroup.getComponents(name2);
        }


        public List<Object> annoObjects(List<Class<?>> classes) {
            if (classes != null && !classes.isEmpty()) {
                List<Object> obsList = new LinkedList();
                Iterator var4 = classes.iterator();
                while (var4.hasNext()) {
                    Class<?> obsClass = (Class) var4.next();
                    obsList.add(this.getComponentContext().injector().toComponent(obsClass));
                }

                return obsList;
            }
            return Collections.emptyList();
        }

        public List<Class<?>> getScanComponents() {
            return Collections.synchronizedList(scanComponents);
        }

        public List<Class<?>> getClassOfAnno(Class<? extends Annotation> ano) {
            return Observable.fromIterable(this.scanComponents)
                    .filter((Class<?> in) -> {
                                    return in.getClass().getAnnotation(ano) != null;
                                })
                    .toList()
                    .blockingGet();

        }
    }

}
*/
