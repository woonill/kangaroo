package com.kangaroo.handler.context;

import com.kangaroo.*;
import com.kangaroo.handler.RequestHandler;
import com.kangaroo.handler.RequestHandlerContext;
import com.kangaroo.handler.RequestHandlerInitializer;
import com.kangaroo.util.PathBuilder;
import io.reactivex.Observer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.kangaroo.handler.context.RequestHandlerHolder.RequestHandlerHolderPipeline;
import static com.kangaroo.handler.context.RequestHandlerHolder.RequestHandlerHolderPipelineSComposite;

public abstract class RequestHandlerHolderContext implements Consumer.Context,Handler<Request,Response> {

    private Consumer.Context parentContext;
    private RequestHandlerHolder[] holders;
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public RequestHandlerHolderContext(Consumer.Context csContext, RequestHandlerHolder[] holders2) {
        this.parentContext = csContext;
        this.holders = holders2;
    }


    public RequestHandlerHolder[] getRequestHandler() {
        return holders;
    }

    @Override
    public ExecutorService getService() {
        return parentContext.getService();
    }

    @Override
    public Map<String, Object> props() {
        return parentContext.props();
    }

    @Override
    public Response handle(Request request) {
        return handle2(request).handle();
    }

    @Override
    public ComponentFactory getComponentFactory() {
        return this.parentContext.getComponentFactory();
    }

    public RequestHandlerHolderPipeline toHolderPipeline() {
        return new RequestHandlerHolderPipelineSComposite(this.holders);
    }

    public RequestHandlerContext handle2(Request request) {

        RequestHandlerHolderPipeline pipeline = toHolderPipeline();
        final AtomicBoolean _isRespnosed = new AtomicBoolean(false);
        final Map<String, Object> globalProps = new ConcurrentHashMap<>();
//        final List<EventDispacher.Event> tempEventList = new LinkedList<>();

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
                return Collections.unmodifiableMap(globalProps);
            }

            @Override
            public Response.Factory getResponseFactory(Request request) {
                return null;
            }


            @Override
            public Request request() {
                return request;
            }

            @Override
            public ComponentContext getComponents() {
                throw new UnsupportedOperationException("error");
            }

            @Override
            public ComponentContext getComponents(String name) {
               throw new UnsupportedOperationException("error");
            }

            public Future<?> runTask(Runnable runnable) {
                if (!_isRespnosed.get()) {
                    return parentContext.getService().submit(runnable);
                }
                throw new IllegalArgumentException("error RequestHandleContext is out");

            }
        };
    }

    public static abstract class DefaultHandlerContextFactory implements HandlerContextFactory<RequestHandlerHolderContext>{

        private Logger logger = LoggerFactory.getLogger(this.getClass());
        private String handleRoot;

        @Override
        public RequestHandlerHolderContext build(Consumer.Context csContext) {

            RequestHandlerInitializer[] initializers = this.getHandlerInitializer();
            final RequestHandlerHolder[] holders = createHolders(csContext,initializers);
            return new RequestHandlerHolderContext(csContext, holders){

            };
        }

        protected RequestHandlerHolder[] createHolders(
                Consumer.Context csContext,
                RequestHandlerInitializer[] initializers){

//            String thePath = StrUtils.isNull(handleRoot) ? "/" : handleRoot;
//            RequestHandlerPathBuilder appPathBuilder = getDefaultPathBuilder(thePath);
            List<RequestHandlerHolder> holderList = new ArrayList<RequestHandlerHolder>();

            for (RequestHandlerInitializer init : initializers) {
                RequestHandlerInitializer.Context initContext = newInitContext(csContext,init);
                RequestHandler handler = init.getHandler(initContext);

                RequestHandlerHolder[] chiHolder = null;
                final RequestHandlerInitializer[] children = init.getChildren();
                if(children != null && children.length>0){
                    chiHolder= createHolders(csContext, children);
                }

                RequestHandlerHolder holder = new RequestHandlerHolder(init.getName(),handler,chiHolder);
                logger.debug("Regist holder:" + holder.name());
                holderList.add(holder);
            }
            return holderList.toArray(new RequestHandlerHolder[holderList.size()]);
        }

        protected RequestHandlerInitializer.Context newInitContext(
                Consumer.Context csContext,
                RequestHandlerInitializer init){


            return new RequestHandlerInitializer.Context() {

                @Override
                public ComponentFactory getComponentFactory() {
                    return csContext.getComponentFactory();
                }

                @Override
                public List<Object> annoObjects(Class<? extends Annotation> annotation) {
                    return null;
                }
            };
        }

        protected abstract RequestHandlerInitializer[] getHandlerInitializer();
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
}
