package com.kangaroo;

import com.kangaroo.component.DefaultComponentFactory;
import com.kangaroo.handler.RequestHandler;
import com.kangaroo.handler.RequestHandlerContext;
import com.kangaroo.handler.RequestHandlerInitializer;
import com.kangaroo.handler.context.RequestHandlerHolderContext;
import com.kangaroo.producer.http.HttpRequestHandler;
import com.kangaroo.producer.http.HttpRequestHandlerAdapter;
import com.kangaroo.producer.http.support.HttpRequestProducer;

import java.util.concurrent.Future;

public class HttpProducerTest {


    public static void main(String...args){


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
    }
}
