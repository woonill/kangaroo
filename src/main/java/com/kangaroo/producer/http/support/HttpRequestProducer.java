package com.kangaroo.producer.http.support;

import com.kangaroo.Global;
import com.kangaroo.Proc;
import com.kangaroo.Producer;
import com.kangaroo.Request;
import com.kangaroo.internal.Wrappers;
import com.kangaroo.producer.http.*;
import com.kangaroo.util.Validate;
import com.kangaroo.util.concurrent.DefaultFuture;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by woonill on 07/11/2016.
 */
public abstract class HttpRequestProducer implements Producer {

    private static final Logger logger = LoggerFactory.getLogger(HttpRequestProducer.class);

    private final HttpRequestProducerInfo info;
    private AtomicBoolean _started = new AtomicBoolean(false);

    private HttpRequestProducer(HttpRequestProducerInfo info2) {
        this.info = info2;
    }


    public HttpRequestProducerInfo info() {
        return info;
    }

    public static HttpRequestProducer newHttpProducer(HttpRequestProducerInfo info, HttpRequestHandlerInit handlerInit) {

        return new HttpRequestProducer(info) {
            protected ChannelInitializer<Channel> getChannelInit() {
                HttpRequestHandler shandler = handlerInit.get();
                final Consumer<Channel> httpBase = initBaseHttpHandler(info().maxPostLength(), getHttpChannelInit(shandler));
                return new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel channel) throws Exception {
                        httpBase.accept(channel);
                    }
                };
            }

            ;
        };
    }


    public static HttpRequestProducer newHttpSocketProducer(HttpRequestProducerInfo info, HttpServerSocketHandlerInit socketInit) {

        return new HttpRequestProducer(info) {

            protected ChannelInitializer<Channel> getChannelInit() {

                Consumer<Channel> action = new Consumer<Channel>() {
                    @Override
                    public void accept(Channel ch) {
                        ch.pipeline().addLast(getSocketChannelInit(socketInit));
                    }
                };

                final Consumer<Channel> httpBase = initBaseHttpHandler(info().maxPostLength(), action);
                return new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel channel) throws Exception {
                        httpBase.accept(channel);
                    }
                };
            }

            ;

            @Override
            protected void close() {
                if (socketInit.closeFuture() != null) {
                    socketInit.closeFuture().run();
                }
            }
        };
    }


    public static HttpRequestProducer newFullHttpProducer(HttpRequestProducerInfo info, FullHttpRequestHandlerInit finit) {


        final Runnable closeFuture = finit.newSocketHandler().closeFuture();

        return new HttpRequestProducer(info) {

            protected ChannelInitializer<Channel> getChannelInit() {

                final ChannelHandler httpRouteChannelHandler = getHttpRouteChannelHandler(finit);
                final Consumer<Channel> httpBase = initBaseHttpHandler(info().maxPostLength(), new Consumer<Channel>() {
                    @Override
                    public void accept(Channel event) {
                        event.pipeline().addLast(httpRouteChannelHandler);
                    }
                });
                return new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel channel) throws Exception {
                        httpBase.accept(channel);
                    }
                };
            }

            @Override
            protected void close() {

                try {
                    if (closeFuture != null) {
                        closeFuture.run();
                    }
                } catch (Throwable te) {
                    logger.error("Error on Close HttpSocketInit.close method");
                    te.printStackTrace();

                }
                if (finit.closeFuture() != null) {
                    finit.closeFuture().run();
                }
            }
        };
    }


    public static ChannelHandler getHttpRouteChannelHandler(FullHttpRequestHandlerInit finit) {

        final DefaultNettyHttpChannelHandler channelHandler = new DefaultNettyHttpChannelHandler(finit.newHandler());
        final ChannelHandler handler = getSocketChannelInit(finit.newSocketHandler());
        //因不是Shared的话就不能动态添加，所以做成 Abstract class来处理
        return new HttpRouteChannelHandler() {
            @Override
            protected ChannelHandler httpSocketHandler() {
                return handler;
            }

            @Override
            protected ChannelHandler httpHandler() {
                return channelHandler;
            }
        };
    }


    @ChannelHandler.Sharable
    public abstract static class HttpRouteChannelHandler extends SimpleChannelInboundHandler<FullHttpRequest> {


        @Override
        protected void channelRead0(ChannelHandlerContext channelHandlerContext, FullHttpRequest fullHttpRequest) throws Exception {

            if (DefaultNettyHttpChannelHandler.isUpgrade(fullHttpRequest)) {
                channelHandlerContext.pipeline().addLast(httpSocketHandler());
                channelHandlerContext.fireChannelRead(fullHttpRequest.retain());
            } else {
                channelHandlerContext.pipeline().addLast(httpHandler());
                channelHandlerContext.fireChannelRead(fullHttpRequest.retain());
            }
        }

        abstract protected ChannelHandler httpSocketHandler();

        abstract protected ChannelHandler httpHandler();
    }


    final protected Logger logger() {
        return this.logger;
    }


    public Proc run() {

        if (!this._started.compareAndSet(false, true)) {
            throw new IllegalArgumentException("Is running now");
        }

        int acceptor = info.acceptor(1);
        int workerSize = info.workerPool(4);
        logger.info("Acceptor:" + acceptor + " worker :" + workerSize);

        EventLoopGroup bossGroup = new NioEventLoopGroup(acceptor);
        EventLoopGroup workerGroup = new NioEventLoopGroup(workerSize);
        try {
            final ChannelFuture future = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
//                                .channel(EpollServerSocketChannel.class)
//                                .option(ChannelOption.SO_KEEPALIVE, true)
//                                .option(ChannelOption.SO_BACKLOG, hrpi.socketBacklog())
                    .option(ChannelOption.SO_BACKLOG, info.socketBacklog())
//                                .option(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT)
//                                .childOption(ChannelOption.TCP_NODELAY,true)
//                                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(getChannelInit())
                    .bind(info.port());


            logger.info("HttpProducer listen on:" + info.port());

            return new Proc() {

                public Future<?> blocking() {
                    try {
                        return future.channel().closeFuture().sync();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return DefaultFuture.errorFuture(e);
                    }
                }

                @Override
                public Global.Context getContext() {
                    return null;
                }

                @Override
                public Future<?> halt() {
                    try {
                        future.channel().close();
                        HttpRequestProducer.this.close();
                    } finally {
                        bossGroup.shutdownGracefully();
                        return workerGroup.shutdownGracefully();
                    }
                }

                @Override
                public CompletableFuture<Future<?>> haltFuture() {
                    Future<Void> closeFuture = future.channel().closeFuture();
                    return CompletableFuture.completedFuture(closeFuture);
                }
            };


        } catch (Throwable tex) {
            tex.printStackTrace();
            throw new IllegalStateException("Connect error:" + tex.getMessage());
        }
    }

    protected void close() {
    }


    static Consumer<Channel> initBaseHttpHandler(int maxPayload, Consumer<Channel> channelAdder) {
        return new Consumer<Channel>() {
            @Override
            public void accept(Channel ch) {
                ch.pipeline().addLast("http-codec", new HttpServerCodec());
                ch.pipeline().addLast(new HttpObjectAggregator(maxPayload));
                ch.pipeline().addLast(new ChunkedWriteHandler());
                channelAdder.accept(ch);
            }
        };
    }

    static private final ChannelHandler getSocketChannelInit(HttpServerSocketHandlerInit initFunc) {

        NettyHttpSocketSessionContext nhssc = new NettyHttpSocketSessionContext(initFunc);

        return new DefaultNettyHttpChannelHandler.HttpServerSocketChannelHandler(initFunc.connectFilter()) {
            @Override
            protected DefaultNettyHttpChannelHandler.HttpSocketMessageReader messageReader(
                    Request request,
                    WebSocketServerHandshaker wsh,
                    Channel channel) {
                return nhssc.initObserver(request, wsh, channel);
            }
        };
    }

    private static HttpSocketSession newSession(WebSocketServerHandshaker wsh, Channel channel) {
        return new AbstractHttpSocketSession(channel) {
            @Override
            protected void doClose() {
                wsh.close(channel, new CloseWebSocketFrame());
            }
        };
    }

    final static Consumer<Channel> getHttpChannelInit(HttpRequestHandler handler) {

        DefaultNettyHttpChannelHandler channelHandler = new DefaultNettyHttpChannelHandler(handler);
        return new Consumer<Channel>() {
            @Override
            public void accept(Channel ch) {
                ch.pipeline().addLast(channelHandler);
            }
        };
    }


    abstract protected ChannelInitializer<Channel> getChannelInit();


    public interface HttpRequestSubscriberInit {

        default Function<Request, Boolean> connectFilter() {

            return new Function<Request, Boolean>() {
                @Override
                public Boolean apply(Request in) {
                    return Boolean.TRUE;
                }
            };
        }

        default Runnable closeFuture() {
            return Wrappers.NONE_RUNNABLE;
        }
    }


    public interface HttpRequestHandlerInit extends HttpRequestSubscriberInit {

        HttpRequestHandler get();
    }

    public interface FullHttpRequestHandlerInit extends HttpRequestSubscriberInit {

        public HttpRequestHandler newHandler();

        HttpServerSocketHandlerInit newSocketHandler();
    }

    public interface HttpServerSocketHandlerInit extends HttpRequestSubscriberInit {

        HttpSocketSessionListener listener();

        HttpSocketMessageObserver initObserver(HttpSocketSessionContext context);
    }


    static abstract class AbstractHttpSocketSession implements HttpSocketSession {

        private Channel channel;
        private String sid;
        private State state = State.OPEN;
        private HttpSocketMessageProducer producer;
//        private Logger logger = LoggerFactory.getLogger(this.getClass());

        public AbstractHttpSocketSession(String sid, Channel channel1) {

            Validate.notNull(sid, "Session id is rquired");
            Validate.notNull(channel1, "Channel is required");

            this.channel = channel1;
            this.sid = sid;
            this.producer = initProducer();
        }

        public AbstractHttpSocketSession(Channel channel1) {
            this(channel1.id().asLongText(), channel1);
        }


        private HttpSocketMessageProducer initProducer() {
            return new HttpSocketMessageProducerImpl(this.sid, this.state, channel);
        }

        abstract protected void doClose();

        // 향후 추가 Constructor에서 받아서 처리하는걸로 한다
        //이유는 HttpRequestSubscriber에서 통일적으로 closing에 대한 부분을 처리한다
        public State close() {
            synchronized (this) {
                if (this.state.isOpen()) {
                    this.state = State.CLOSED;
                    this.doClose();
                }
                return state;
            }
        }

        @Override
        public HttpSocketMessage payload(byte[] msg) {

            if (msg == null) {
                return new HttpSocketMessage(this.id(), msg);
            }
            return new HttpSocketMessage(sid, msg);

        }

        @Override
        public String id() {
            return sid;
        }

        @Override
        public State state() {
            return state;
        }

        public HttpSocketMessageProducer producer() {
            return this.producer;
        }


        static final class HttpSocketMessageProducerImpl implements HttpSocketMessageProducer {

            private final String sid;
            private final State state;
            private final Channel channel;

            private HttpSocketMessageProducerImpl(String sid, State state, Channel channel) {
                this.sid = sid;
                this.state = state;
                this.channel = channel;
            }

            @Override
            public boolean produce(byte[] message) {


                if (state.isClosed()) {
                    return false;
                }

                if (message == null || message.length < 1) {
                    return false;
                }
                channel.writeAndFlush(new TextWebSocketFrame(new String(message)));
                return true;
            }
        }
    }


    public abstract static class HttpRequestProducerBuilder<S extends HttpRequestSubscriberInit, T extends HttpRequestProducerBuilder<S, T>> {

        public static final String ENV_PROPERTY = "producer.env";


        private Integer acceptor;
        private Integer workerPool;
        private Integer port = 8080;
        private Integer maxPostLength = new Integer(65536);
        private String inetHost = "127.0.0.1";
        private boolean supportHttp2 = false;
        private int socketBacklog = 50;
        private boolean socketkeepAlive = true;


        private T asDerivedType() {
            return (T) this;
        }

        public T setPort(int port) {
            this.port = port;
            return asDerivedType();
        }

        public T setSupportHttp2() {
            this.supportHttp2 = true;
            return asDerivedType();
        }

        public T setAcceptor(Integer acceptor) {
            this.acceptor = acceptor == null ? 0 : acceptor.intValue();
            return asDerivedType();
        }

        public T setWorkerPool(Integer workerPool) {
            this.workerPool = workerPool == null ? 0 : workerPool.intValue();
            return asDerivedType();
        }

        public T setSocketBacklog(int socketBacklog) {
            this.socketBacklog = socketBacklog;
            return asDerivedType();
        }

        public T disableSocketKeepAlive() {
            this.socketkeepAlive = true;
            return asDerivedType();
        }

        private HttpRequestProducerInfo getInfo() {

            return new HttpRequestProducerInfo.HttpRequestProducerInfoBuilder()
                    .setPort(this.port)
                    .setMaxPostLength(this.maxPostLength)
                    .setAcceptor(this.acceptor)
                    .setWorkerPool(this.workerPool)
                    .setSocketBacklog(this.socketBacklog < 50 ? 50 : this.socketBacklog)
                    .setSupportHttp2(this.supportHttp2)
                    .setSocketKeepAlive(this.socketkeepAlive)
                    .build();
        }


        public T setMaxPostLength(Integer maxPostLength) {
            if (maxPostLength != null) {
                this.maxPostLength = maxPostLength;
            }
            return asDerivedType();
        }


        public HttpRequestProducer build(S reqHandlerInit) {
            Function<S, HttpRequestProducer> initFunc = initProduceFunc(this.getInfo());
            return initFunc.apply(reqHandlerInit);
        }

        abstract protected Function<S, HttpRequestProducer> initProduceFunc(HttpRequestProducerInfo info);

    }


    public static final class HttpRequestHandlerInitBuilder extends HttpRequestProducerBuilder<HttpRequestHandlerInit, HttpRequestHandlerInitBuilder> {

        @Override
        protected Function<HttpRequestHandlerInit, HttpRequestProducer> initProduceFunc(HttpRequestProducerInfo info) {
            return new Function<HttpRequestHandlerInit, HttpRequestProducer>() {
                @Override
                public HttpRequestProducer apply(HttpRequestHandlerInit in) {
                    return HttpRequestProducer.newHttpProducer(info, in);
                }
            };
        }
    }


    public static final class HttpSocketRequestProducerBuilder extends HttpRequestProducerBuilder<HttpServerSocketHandlerInit, HttpSocketRequestProducerBuilder> {

        @Override
        protected Function<HttpServerSocketHandlerInit, HttpRequestProducer> initProduceFunc(HttpRequestProducerInfo info) {
            return new Function<HttpServerSocketHandlerInit, HttpRequestProducer>() {
                @Override
                public HttpRequestProducer apply(HttpServerSocketHandlerInit in) {
                    return HttpRequestProducer.newHttpSocketProducer(info, in);
                }
            };
        }
    }


    public static final class FullHttpRequestProducerBuilder extends HttpRequestProducerBuilder<FullHttpRequestHandlerInit, FullHttpRequestProducerBuilder> {

        @Override
        protected Function<FullHttpRequestHandlerInit, HttpRequestProducer> initProduceFunc(HttpRequestProducerInfo info) {
            return new Function<FullHttpRequestHandlerInit, HttpRequestProducer>() {
                @Override
                public HttpRequestProducer apply(FullHttpRequestHandlerInit in) {
                    return HttpRequestProducer.newFullHttpProducer(info, in);
                }
            };
        }
    }

    public static final class NettyHttpRequestProducerBuilder extends HttpRequestProducerBuilder<NettyBaseHttpRequestHandlerInit, NettyHttpRequestProducerBuilder> {

        @Override
        protected Function<NettyBaseHttpRequestHandlerInit, HttpRequestProducer> initProduceFunc(HttpRequestProducerInfo info) {

            return new Function<NettyBaseHttpRequestHandlerInit, HttpRequestProducer>() {
                @Override
                public HttpRequestProducer apply(NettyBaseHttpRequestHandlerInit in) {

                    return new HttpRequestProducer(info) {

                        protected ChannelInitializer<Channel> getChannelInit() {

                            DefaultNettyHttpChannelHandler dnhch = new DefaultNettyHttpChannelHandler(in.getHandler());
                            final Consumer<Channel> httpBase = initBaseHttpHandler(info().maxPostLength(), new Consumer<Channel>() {
                                @Override
                                public void accept(Channel event) {
                                    event.pipeline().addLast(dnhch);
                                }
                            });
                            return new ChannelInitializer<Channel>() {
                                @Override
                                protected void initChannel(Channel channel) throws Exception {
                                    httpBase.accept(channel);
                                }
                            };
                        };
                    };
                }
            };
        }
    }


    public interface NettyBaseHttpRequestHandlerInit extends HttpRequestSubscriberInit {

        NettyHttpHandler getHandler();
    }


    // Netty HttpSocketReader adapter  code

    static final class NettyHttpSocketSessionContext {

        private HttpSocketMessageObserver observer;
        private CopyOnWriteArraySet<HttpSocketSessionListener> sessionListeners = new CopyOnWriteArraySet<>();
        private Map<String, HttpSocketSession> sessionMap = new ConcurrentHashMap<>();

        public NettyHttpSocketSessionContext(HttpServerSocketHandlerInit channelInit) {
//            this.sessionInit = httpSocketSessionInit;

            this.observer = initObserver(channelInit);


            final HttpSocketSessionListener listener = channelInit.listener();
            if (listener != null) {
                this.sessionListeners.add(listener);
            }
        }

        private HttpSocketMessageObserver initObserver(HttpServerSocketHandlerInit channelInit) {
            return channelInit.initObserver(initHttpSocketMessageContext());
        }

        private HttpSocketSessionContext initHttpSocketMessageContext() {

            return new HttpSocketSessionContext() {

                @Override
                public HttpSocketSession[] getSessions() {
                    return sessionMap.values().toArray(new HttpSocketSession[sessionMap.size()]);
                }

                @Override
                public HttpSocketSession getSession(String sid) {
                    return sessionMap.get(sid);
                }

            };
        }


        public DefaultNettyHttpChannelHandler.HttpSocketMessageReader initObserver(
                Request request,
                WebSocketServerHandshaker wsh,
                Channel channel) {


            HttpSocketSession session = new AbstractHttpSocketSession(getSessionId(channel), channel) {
                @Override
                protected void doClose() {
                    wsh.close(channel, new CloseWebSocketFrame());
                }
            };
            sessionMap.put(session.id(), session);

            return new DefaultNettyHttpChannelHandler.HttpSocketMessageReader(this.observer) {

                @Override
                public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
                    super.handlerAdded(ctx);
                    logger.info("Channel addes");
                    String sid = getSessionId(ctx.channel());
                    updateSessionState(request, sessionMap.get(sid));
                }

                @Override
                public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
                    super.handlerRemoved(ctx);
                    logger.info("Channel closed");
                    String sid = getSessionId(ctx.channel());
                    final HttpSocketSession session1 = sessionMap.remove(sid);
                    if (session1 != null) {
                        session1.close();
                        updateSessionState(request, session1);
                    }
                }

                protected void onClosed(CloseWebSocketFrame closeFrame) {
                    wsh.close(channel, closeFrame);
                }

                protected HttpSocketMessage toMessage(Channel channel, String str) {
                    return readMessage(channel, str);
                }
            };
        }


        public void updateSessionState(Request request, HttpSocketSession session) {

            if (sessionListeners.isEmpty()) {
                return;
            }

            if (session.state().isOpen()) {
                sessionListeners.forEach(handleOpenListener(request, session));
            } else if (session.state().isClosed()) {
                logger.info("Start HttpSocketSessionListener.close ");
                sessionListeners.forEach(handleClosedListener(session));
                logger.info("End handle HttpSocketSession.onClosed method ok");
            } else {
                logger.warn("not supported state");
            }
        }


        final Consumer<HttpSocketSessionListener> handleOpenListener(Request request, HttpSocketSession session) {

            return new Consumer<HttpSocketSessionListener>() {
                @Override
                public void accept(HttpSocketSessionListener listener) {
                    try {
                        listener.onOpen(request, session);
                    } catch (Throwable te) {
                        te.printStackTrace();
                    }
                }
            };
        }

        final Consumer<HttpSocketSessionListener> handleClosedListener(HttpSocketSession session) {

            return new Consumer<HttpSocketSessionListener>() {
                @Override
                public void accept(HttpSocketSessionListener listener) {
                    try {
                        listener.onClosed(session);
                    } catch (Throwable te) {
                        te.printStackTrace();
                    }
                }
            };
        }

        public HttpSocketMessage readMessage(Channel channel, String str) {
            HttpSocketMessage hsm = new HttpSocketMessage(getSessionId(channel), str.getBytes());
            return hsm;
        }


        public HttpSocketSession newSession(WebSocketServerHandshaker wsh, Channel channel) {
//            return HttpRequestProducer.newSession(wsh,channel);
            return new AbstractHttpSocketSession(channel) {
                @Override
                protected void doClose() {
                    wsh.close(channel, new CloseWebSocketFrame());
                }
            };
        }

        public String getSessionId(Channel channel) {
            return channel.id().asLongText();
        }
    }

}
