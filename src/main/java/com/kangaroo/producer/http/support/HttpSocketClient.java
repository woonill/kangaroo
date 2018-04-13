package com.kangaroo.producer.http.support;

import com.kangaroo.producer.http.HttpSocketMessage;
import com.kangaroo.producer.http.HttpSocketSession;
import com.kangaroo.util.Validate;
import com.kangaroo.util.concurrent.DefaultFuture;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.util.CharsetUtil;
import io.reactivex.Observer;
import io.reactivex.observers.DefaultObserver;
import org.reactivestreams.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

/*
 *Created by woonill on 7/10/16.
 */

public abstract class HttpSocketClient {

    private int reactCore;
    private int maxPayloadLenth = Integer.MAX_VALUE;
    private Supplier<String> serverUri;


//    private int maxConnectTimeout = 1000;
//    private AtomicReference<State> stateRef = new AtomicReference<>(State.CLOSED);

    private State state = State.CLOSED;

    private static final Logger logger = LoggerFactory.getLogger(HttpSocketClient.class);

    private HttpSocketClient(String serverUri) {
        this(new Supplier<String>() {
            @Override
            public String get() {
                return serverUri;
            }
        }, null);
    }


    private HttpSocketClient(Supplier<String> uriProvider, Integer reactor) {
        this.serverUri = uriProvider;
        this.reactCore = reactor;
    }

    private final AtomicInteger _runState = new AtomicInteger(0);  //0 normal 1 running 2 down

//    private HttpSocketClientProducer producer;

    public static Supplier<String> singleUri(String s) {
        return new Supplier<String>() {
            @Override
            public String get() {
                return s;
            }
        };
    }

    private HttpSocketSession session;
    private EventLoopGroup eventLoopGroup;

    private final HttpSocketClient connect() {

        if (!_runState.compareAndSet(0, 1)) {
            throw new IllegalStateException("is runnning now");
        }

        this.state = State.CONNECTING;

        DefaultFuture<HttpSocketSession> future = new DefaultFuture<>();

        try {

            final StateObserver stateObserver = new StateObserver() {

                public Observer<HttpSocketMessage> connected(HttpSocketSession session) {
//                    stateRef.set(State.CONNECTED);

                    HttpSocketClient.this.state = State.CONNECTED;

                    future.success(session);

                    Subscriber<HttpSocketMessage> subscriber = messageSubscriber(session);

                    return new DefaultObserver<HttpSocketMessage>() {

                        @Override
                        public void onNext(HttpSocketMessage event) {
                            subscriber.onNext(event);
                        }

                        @Override
                        public void onError(Throwable error) {
                            subscriber.onError(error);
                        }

                        @Override
                        public void onComplete() {
                            HttpSocketClient.this.state = State.CLOSED;
                            subscriber.onComplete();
                        }
                    };
                }
            };

            this.eventLoopGroup = new NioEventLoopGroup(reactCore);
            URI uri = new URI(serverUri.get());
            final HostAndPort hostAddress = HostAndPort.get(uri);

            ChannelHandler socketHandler = newHttpSocketClientHandler(getWebSocketClientHandshaker(uri), stateObserver);
            final ChannelFuture connect = new Bootstrap()
                    .group(eventLoopGroup)
                    .channel(NioSocketChannel.class)
                    .handler(getChannelInitFunc(socketHandler))
                    .connect(uri.getHost(), hostAddress.port);

            logger.info("Connect to HttpSocketServer port:" + hostAddress.port + "  success");
            this.session = future.get();


            return this;

        } catch (Throwable tex) {
            future.success(null);
            tex.printStackTrace();
            throw new IllegalStateException("Connect error:" + tex.getMessage());
        }
    }

    public State getState() {
        return state;
    }


    public void produce(String msg) {
        this.session.producer().produce(msg.getBytes());
    }


    interface StateObserver {

        Observer<HttpSocketMessage> connected(HttpSocketSession session);
    }


    public Future<?> close() {

        if (!_runState.compareAndSet(1, 2)) {
            throw new IllegalStateException("Is not running");
        }
        return null;

    }

    static final class HostAndPort {

        private String scheme;
        private String host;
        private int port;

        private URI uri;

        public HostAndPort(String scheme, String host, int port) {
            this.scheme = scheme;
            this.host = host;
            this.port = port;
        }

        static final HostAndPort get(URI uri) {


            String scheme = uri.getScheme() == null ? "ws" : uri.getScheme();
            final String host = uri.getHost() == null ? "127.0.0.1" : uri.getHost();
            final int port;
            if (uri.getPort() == -1) {
                if ("ws".equalsIgnoreCase(scheme)) {
                    port = 80;
                } else if ("wss".equalsIgnoreCase(scheme)) {
                    port = 443;
                } else {
                    port = -1;
                }
            } else {
                port = uri.getPort();
            }
            return new HostAndPort(scheme, host, port);
        }
    }

    public ChannelInitializer<SocketChannel> getChannelInitFunc(ChannelHandler socketHandler) {

        return new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ChannelPipeline p = ch.pipeline();
                p.addLast(new HttpClientCodec());
                p.addLast(new HttpObjectAggregator(maxPayloadLenth));
                p.addLast(WebSocketClientCompressionHandler.INSTANCE);
                p.addLast(socketHandler);
            }
        };
    }


    public WebSocketClientHandshaker getWebSocketClientHandshaker(URI uri) {


        final WebSocketClientHandshaker wscHandler = WebSocketClientHandshakerFactory.newHandshaker(
                uri,
                WebSocketVersion.V13,
                null,
                false,
                new DefaultHttpHeaders());

        return wscHandler;

    }

    final ChannelHandler newHttpSocketClientHandler(WebSocketClientHandshaker wscHandler, StateObserver stateObserver) {

        return new NettyHttpSocketClientHandler(wscHandler) {
            @Override
            protected SimpleChannelInboundHandler<WebSocketFrame> handler() {
                return HttpSocketClient.this.streamHandler(stateObserver);
            }
        };
    }


    abstract protected Subscriber<HttpSocketMessage> messageSubscriber(HttpSocketSession session);


    public static final class Builder {


        private int reactCore = 1;
        private int maxPayloadLenth = Integer.MAX_VALUE;
        private Supplier<String> serverUri;
        private String socketVersion;

        public Builder reactThread(int pool) {
            this.reactCore = pool;
            return this;
        }

        public Builder maxPayloadLenth(int maxSize) {
            this.maxPayloadLenth = maxSize;
            return this;
        }

        public Builder serverUris(Supplier<String> uris) {
            this.serverUri = uris;
            return this;
        }


        public HttpSocketClient build(Function<HttpSocketSession, Subscriber<HttpSocketMessage>> initFactory) {

            if (serverUri == null) {
                throw new IllegalArgumentException("ServerUri provider is required");
            }

            HttpSocketClient hscp = new HttpSocketClient(this.serverUri, this.reactCore) {

                @Override
                protected Subscriber<HttpSocketMessage> messageSubscriber(HttpSocketSession session) {
                    return initFactory.apply(session);
                }
            };
            hscp.maxPayloadLenth = this.maxPayloadLenth;
            hscp.reactCore = this.reactCore;
            return hscp.connect();
        }

        public HttpSocketClient build(Subscriber<HttpSocketMessage> messageSubscriber) {
            return build(new Function<HttpSocketSession, Subscriber<HttpSocketMessage>>() {
                @Override
                public Subscriber<HttpSocketMessage> apply(HttpSocketSession session) {
                    return messageSubscriber;
                }
            });
        }
    }


    public static final class State {

        public static final State CONNECTING = new State("01");
        public static final State CONNECTED = new State("02");
        public static final State CLOSING = new State("03");
        public static final State CLOSED = new State("00");


        private String code;

        public State(String s) {
            this.code = s;
        }

        public String getCode() {
            return code;
        }


        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((code == null) ? 0 : code.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            State other = (State) obj;

            if (code == null) {
                if (other.code != null)
                    return false;
            } else if (!code.equalsIgnoreCase(other.code))
                return false;
            return true;
        }
    }

    static abstract class NettyHttpSocketClientHandler extends SimpleChannelInboundHandler<FullHttpMessage> {

        private final WebSocketClientHandshaker handshaker;
        private ChannelPromise handshakeFuture;
        private Logger logger = LoggerFactory.getLogger(this.getClass());

        public NettyHttpSocketClientHandler(WebSocketClientHandshaker handshaker) {
            Validate.notNull(handshaker, "WebSocketClientHandshaker is required");
            this.handshaker = handshaker;
        }

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) {
            handshakeFuture = ctx.newPromise();
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            handshaker.handshake(ctx.channel());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            logger.warn("WebSocket Client disconnected!");
            handshaker.close(ctx.channel(), new CloseWebSocketFrame());
            ctx.close();
        }

        @Override
        public void channelRead0(ChannelHandlerContext ctx, FullHttpMessage msg) throws Exception {

            if (!handshaker.isHandshakeComplete()) {

                Channel ch = ctx.channel();

                handshaker.finishHandshake(ch, (FullHttpResponse) msg);
                logger.info("WebSocket Client connected!");
                handshakeFuture.setSuccess();

                ctx.channel().pipeline().replace(this.getClass(), "wsRequestHandler", handler());
                return;
            }


            if (msg instanceof FullHttpResponse) {
                FullHttpResponse response = (FullHttpResponse) msg;
                throw new IllegalStateException(
                        "Unexpected FullHttpResponse (getStatus=" + response.status() +
                                ", content=" + response.content().toString(CharsetUtil.UTF_8) + ')');
            }
        }


        protected abstract SimpleChannelInboundHandler<WebSocketFrame> handler();

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            if (!handshakeFuture.isDone()) {
                handshakeFuture.setFailure(cause);
            }
            ctx.close();
        }
    }


    public interface HttpSocketClientSessionInit {

        Subscriber<HttpSocketMessage> messageSubscriber(HttpSocketSession session);
    }

    protected SimpleChannelInboundHandler<WebSocketFrame> streamHandler(StateObserver sessionInit) {


        return new SimpleChannelInboundHandler<WebSocketFrame>() {

            private Observer<HttpSocketMessage> msgObserver;

            @Override
            public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
                this.msgObserver = sessionInit.connected(initSession(ctx.channel()));
                super.handlerAdded(ctx);
            }

            @Override
            public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
                super.handlerRemoved(ctx);
                msgObserver.onComplete();
            }

            @Override
            protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
                if (frame instanceof PongWebSocketFrame) {
                    logger.debug("WebSocket Client received pong");
                } else if (frame instanceof PingWebSocketFrame) {
                    logger.debug("WebSocket Client received ping");
                    ctx.channel().writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
                } else if (frame instanceof CloseWebSocketFrame) {
                    logger.info("WebSocket Client received closing so close this channel");
                    ctx.channel().close();

                } else {
                    if (!(frame instanceof TextWebSocketFrame)) {
                        throw new IllegalStateException(
                                "Unexpected FullHttpResponse (getStatus=500, content= not support WebSocketFrame");
                    }
                    TextWebSocketFrame tws = (TextWebSocketFrame) frame;
                    logger.debug("WebSocket Client received message: " + tws.text());
                    HttpSocketMessage hsm = toMessage(ctx.channel(), tws.text());
                    msgObserver.onNext(hsm);
                }
            }

            protected HttpSocketMessage toMessage(Channel channel, String payload) {
                return new HttpSocketMessage(channel.id().asLongText(), payload.getBytes());
            }
        };
    }


    protected HttpSocketSession initSession(Channel channel) {
        final HttpSocketSession session = new HttpRequestProducer.AbstractHttpSocketSession(channel) {
            @Override
            protected void doClose() {
                channel.close();
            }
        };
        return session;
    }

}
