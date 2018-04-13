package com.kangaroo;

import io.netty.util.concurrent.DefaultEventExecutorGroup;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public abstract class AbstractConsumer<C extends Consumer.Context> implements Consumer{


    private String name;
    private DefaultEventExecutorGroup eventExecutor;
    private AtomicBoolean _start = new AtomicBoolean(false);
    private ComponentFactory componentFactory;

    public AbstractConsumer(String name){
        this.name= name;
    }

    abstract protected C getContext();

    @Override
    public Proc<C> run(Producer... producers) {


        if(!_start.compareAndSet(false,true)){
            throw new IllegalStateException("already running");
        }


        final C wContext = getContext();
        return new Proc<C>(){

            @Override
            public C getContext() {
                return wContext;
            }

            @Override
            public Future<?> halt() {
                return eventExecutor.shutdownGracefully();
            }

            @Override
            public CompletableFuture<Future<?>> haltFuture() {
                return CompletableFuture.supplyAsync(new Supplier<Future<?>>() {
                    @Override
                    public Future<?> get() {
                        return eventExecutor.shutdownGracefully();
                    }
                });
            }
        };
    }


    static final class InternalContext implements Context{

//        private Map<String,Object> propsMap = new ConcurrentHashMap<>();
        private ComponentFactory componentFactory;
        private Map<String,Object> props;

        public InternalContext(Map<String, Object> props, ComponentFactory componentFactory2) {
            this.props = Collections.unmodifiableMap(new HashMap<>(props));
            this.componentFactory = componentFactory2;
        }

        @Override
        public ExecutorService getService() {
            return null;
        }

        @Override
        public ComponentFactory getComponentFactory() {
            return componentFactory;
        }

        @Override
        public Map<String, Object> props() {
            return props;
        }
    }



    public static final class Builder{


        private String name;
        private ComponentFactory componentFactory;
        private Map<String,Object> props = new HashMap<>();
        private int threadPool;

        public Builder(String name2) {
            this.name = name2;
        }


        public Builder putProp(String key,Object val){
            this.props.put(key,val);
            return this;
        }

        public Builder threadCore(int size){
            this.threadPool = size;
            return this;
        }

        public <T extends Consumer.Context> Consumer<T> build(HandlerContextFactory<T> configure){
            Consumer.Context context = new InternalContext(this.props,this.componentFactory);
            final T contextWrapper = configure.build(context);
            return new AbstractConsumer(this.name) {
                @Override
                protected Context getContext() {
                    return contextWrapper;
                }
            };
        }

        public Builder componentFactory(ComponentFactory cf) {
            this.componentFactory = cf;
            return this;
        }
    }


    static final class InternalExecutorService implements ExecutorService{


        private ExecutorService service;

        InternalExecutorService(ExecutorService es){
            Objects.requireNonNull(es);
            this.service = es;
        }

        @Override
        public void shutdown() {
            throw new IllegalStateException("not stop state");
        }

        @Override
        public List<Runnable> shutdownNow() {
            throw new UnsupportedOperationException("error shutdownNow");
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return false;
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            return service.submit(task);
        }

        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            return service.submit(task,result);
        }

        @Override
        public Future<?> submit(Runnable task) {
            return service.submit(task);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
            return service.invokeAll(tasks);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
            return service.invokeAll(tasks,timeout,unit);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
            return service.invokeAny(tasks);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return service.invokeAny(tasks,timeout,unit);
        }

        @Override
        public void execute(Runnable command) {
            service.execute(command);
        }
    }
}
