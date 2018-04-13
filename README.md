Kangaroo Project

基于Netty的响应式风格的 Http处理器 。可基于基础代码实现 restful风格的请求处理和CQRS风格的或简单的Command模式的处理风格。



Links
Web Site
Downloads
Documentation
@netty_project
How to build
For the detailed information about building and developing Netty, please visit the developer guide. This page only gives very basic information.

You require the following to build Netty:

Latest stable Oracle JDK 8
Latest stable Apache Maven




Example


        Proc<?> proc = new AbstractConsumer.Builder("DefaultConsumer")
                .componentFactory(new DefaultComponentFactory())
                .build(new RequestHandlerHolderContext.DefaultHandlerContextFactory() {
                    @Override
                    protected RequestHandlerInitializer[] getHandlerInitializer() {
                        final RequestHandlerInitializer rootInitializer = new RequestHandlerInitializer("SecurityHandler") {
                            public RequestHandler getHandler(Context context) {
                                return new RequestHandler() {
                                    @Override
                                    public Response handle(RequestHandlerContext rhc) {
                                        return null;
                                    }
                                };
                            }

                            @Override
                            public RequestHandlerInitializer[] getChildren() {
                                return new RequestHandlerInitializer[]{
                                        new RequestHandlerInitializer("CommandHandler") {
                                            public RequestHandler getHandler(Context context) {
                                                return null;
                                            }
                                        },
                                        new RequestHandlerInitializer("QueryHandler") {
                                            public RequestHandler getHandler(Context context) {
                                                return null;
                                            }
                                        }
                                };
                            }
                        };

                        return new RequestHandlerInitializer[]{
                                rootInitializer
                        };
                    }
                })
                .run(
                    new HttpRequestProducer.HttpRequestHandlerInitBuilder()
                            .setWorkerPool(8)
                            .setPort(8090)
                            .build(new HttpRequestProducer.HttpRequestHandlerInit() {
                                @Override
                                public HttpRequestHandler get() {
                                    return new HttpRequestHandlerAdapter
                                            .DefaultHttpRequestSubscriberAdapter(null);
                                }
                            })
                );

                proc.haltFuture()
                    .thenAccept(new java.util.function.Consumer<Future<?>>() {
                        @Override
                        public void accept(Future<?> future) {
                            System.out.println("system out now");
                        }
                    })
                    .join();
