package com.kangaroo.producer.http.support;

import com.kangaroo.Header;
import com.kangaroo.Message;
import com.kangaroo.Request;
import com.kangaroo.internal.observable.DefaultObserver2;
import com.kangaroo.producer.http.HttpRequestHandler;
import com.kangaroo.producer.http.HttpSocketMessage;
import com.kangaroo.producer.http.HttpSocketMessageObserver;
import com.kangaroo.producer.http.SHttpResponse;
import com.kangaroo.util.ObjectUtil;
import com.kangaroo.util.Validate;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.function.Function;

/**
 * Created by woonill on 10/11/2016.
 */
@ChannelHandler.Sharable
public class DefaultNettyHttpChannelHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final static Logger logger = LoggerFactory.getLogger(DefaultNettyHttpChannelHandler.class);

    private final NettyHttpHandler handler;

    DefaultNettyHttpChannelHandler(HttpRequestHandler handler) {
        this(new NettyHttpHandlerAdapter(handler));
    }

    DefaultNettyHttpChannelHandler(NettyHttpHandler handler1) {
        Validate.notNull(handler1, "NettyHandler null");
        this.handler = handler1;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest reqs) throws Exception {
        logger.debug("Received request[" + reqs.uri() + "] http version:" + reqs.protocolVersion().text() + "  The Upgrade Header:" + reqs.headers().get("Upgrade"));

        if (!reqs.decoderResult().isSuccess()) {
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.BAD_REQUEST);

            DefaultNettyHttpChannelHandler.sendHttpResponse(ctx, reqs, response);
            return;
        }


        if (HttpUtil.is100ContinueExpected(reqs)) {
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE);
            ctx.writeAndFlush(response);
            return;
        }

        logger.debug("Start observer request now");

        this.handler.observe(reqs).subscribe(new DefaultObserver2<FullHttpResponse>() {
            @Override
            public void onNext(FullHttpResponse response) {
                logger.debug("Write HttpResponse");
                if (HttpUtil.isKeepAlive(reqs)) {
                    response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                }
                if (!HttpUtil.isKeepAlive(reqs)) {
                    logger.debug("Is not KeepAlive the Request");
                } else {
                    logger.debug("Is the Keepalive the Request ");
                }
                DefaultNettyHttpChannelHandler.sendHttpResponse(ctx, reqs, response).addListener(ChannelFutureListener.CLOSE);
            }

            @Override
            public void onError(Throwable error) {
                logger.error("Request handler error:" + ObjectUtil.errorToString(error));
                error.printStackTrace();
                FullHttpResponse response = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1,
                        HttpResponseStatus.BAD_REQUEST);
                DefaultNettyHttpChannelHandler.sendHttpResponse(ctx, reqs, response).addListener(ChannelFutureListener.CLOSE);
            }
        });
    }


    static final Function<SHttpResponse, FullHttpResponse> httpRespMapper(FullHttpRequest request) {

        return new Function<SHttpResponse, FullHttpResponse>() {
            @Override
            public FullHttpResponse apply(SHttpResponse in) {
                return buildFullHttpResponse(request, in);
            }
        };
    }


    static FullHttpResponse buildFullHttpResponse(FullHttpRequest reqs, SHttpResponse res) {

        if (res.status() != Message.SUCCESS_STATUS_CODE) {

            if (Message.REDIRECT_STATUS == res.status()) {

                DefaultFullHttpResponse dfh = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1,
                        HttpResponseStatus.FOUND);
                dfh.headers().set(Message.REDIRECT_LOCATION_FIELD, new String(res.payload(), Charset.defaultCharset()));
//                    dfh.headers().set(HttpHeaders.Names.CONTENT_LENGTH, response.content().readableBytes());
                return dfh;

            }

            return new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.BAD_REQUEST);
        }


        ByteBuf contents = Unpooled.wrappedBuffer(res.payload());
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.valueOf(res.status()),
                contents);


        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());

        if (HttpUtil.isKeepAlive(reqs)) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }


        Observable<Header> headers = res.headers();
        headers.forEach((Header header) -> {
            String name = header.name();
            if (SHttpResponse.HttpResponseBuilder.isValid(name)
                    && !HttpHeaderNames.CONTENT_LENGTH.contentEqualsIgnoreCase(name)) {
                logger.debug("Header name:" + name + " value:" + header.value());
                response.headers().set(name, header.value());
            }
        });


        if (res.cookie() != null) {
            logger.debug("Start set cookie");
            SHttpResponse.HttpCookie cookie = res.cookie();
            DefaultCookie defaultCookie = new DefaultCookie(cookie.getName(), cookie.getValue());
            defaultCookie.setDomain(cookie.getDomain());
            defaultCookie.setPath(cookie.getPath());
            defaultCookie.setVersion(cookie.getVersion());
            defaultCookie.setMaxAge(cookie.getMaxAge());


            String cookieStr = ServerCookieEncoder.encode(defaultCookie);
            response.headers().set(HttpHeaderNames.SET_COOKIE, cookieStr);
            logger.debug("Set cookie success");
        }
        return response;
    }

    public static boolean isUpgrade(FullHttpRequest req) {
        return (WEB_SOCKET_FIELD.equalsIgnoreCase(req.headers().get("Upgrade")));
    }


    @Sharable
    public static final class HttpBadRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest reqs) throws Exception {

            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.BAD_REQUEST);

            DefaultNettyHttpChannelHandler.sendHttpResponse(ctx, reqs, response);
            return;
        }
    }

    public static final String WEB_SOCKET_FIELD = "websocket";

    @Sharable
    static abstract class HttpServerSocketChannelHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

        private Logger logger = LoggerFactory.getLogger(this.getClass());
        private Function<Request, Boolean> filter;
//        private HttpSocketMessageReader socketMessageReader = new HttpSocketMessageReader();


        protected HttpServerSocketChannelHandler() {
            this(new Function<Request, Boolean>() {
                @Override
                public Boolean apply(Request in) {
                    return Boolean.TRUE;
                }
            });
        }

        protected HttpServerSocketChannelHandler(Function<Request, Boolean> filter) {
            Validate.notNull(filter);
            this.filter = filter;
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
            ctx.close();
        }

        @Override
        protected final void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {

            if (req.decoderResult().isSuccess()
                    && (WEB_SOCKET_FIELD.equalsIgnoreCase(req.headers().get("Upgrade")))) {


                Request request = NettyHttpHandlerAdapter.getRequest(req);
                if (!filter.apply(request)) {
                    logger.warn("Not allowed request for Filter on:" + req.uri());
                    FullHttpResponse response = new DefaultFullHttpResponse(
                            HttpVersion.HTTP_1_1,
                            HttpResponseStatus.NOT_ACCEPTABLE);
                    DefaultNettyHttpChannelHandler.sendHttpResponse(ctx, req, response);
                    return;
                }

                logger.debug("Start handler Init WebSocket ");
                WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(req.uri(), null, true);
                WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(req);
                if (handshaker == null) {
                    WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
                } else {
                    final ChannelFuture sfuture = handshaker.handshake(ctx.channel(), req);
                    sfuture.get();

                    logger.debug("Start WebSocketChannel init");
//                    HttpSocketMessageObserver handler = initHttpSocketHandler(request,handshaker,sfuture.channel());
//                    socketMessageReader.addObserver(ctx.channel(),handler);
//                    socketMessageReader.observerMap.put(ctx.channel().id(),handler);
                    HttpSocketMessageReader handler = messageReader(request, handshaker, sfuture.channel());
                    ctx.pipeline().replace(this, "httpsocket-handler", handler);
                }
            } else {
                logger.warn("Can not handle Upgrade Protocole");
            }
        }

        //        abstract protected HttpSocketMessageObserver initHttpSocketHandler(Request request, WebSocketServerHandshaker wsh, Channel channel);
        abstract protected HttpSocketMessageReader messageReader(Request request, WebSocketServerHandshaker wsh, Channel channel);

    }

    ;

    abstract static class HttpSocketMessageReader extends SimpleChannelInboundHandler<WebSocketFrame> {

        private Logger logger = LoggerFactory.getLogger(this.getClass());
        private HttpSocketMessageObserver observer;

        public HttpSocketMessageReader(HttpSocketMessageObserver observer2) {
            this.observer = observer2;
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
//            observerMap.get(ctx.channel().id()).onError(cause);
            ctx.fireExceptionCaught(cause);
            ctx.close();
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {

            logger.info("Start handler Reader message:" + frame);


            if (frame instanceof PongWebSocketFrame) {
                logger.debug("WebSocket Client received pong");
            } else if (frame instanceof PingWebSocketFrame) {
                logger.debug("WebSocket Client received ping");
                ctx.channel().writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
                return;
            } else if (frame instanceof CloseWebSocketFrame) {
                logger.info("WebSocket Client received closing so close this channel");
//                final HttpSocketMessageObserver httpSocketMessageObserver = observerMap.remove(ctx.channel().id());
//                httpSocketMessageObserver.onCompleted();
//                getObserver().onCompleted();
                onClosed(ctx.channel(), ((CloseWebSocketFrame) frame));
            } else {
                if (!(frame instanceof TextWebSocketFrame)) {
                    throw new IllegalStateException(
                            "Unexpected FullHttpResponse (getStatus=500, content= not support WebSocketFrame");
                }
                TextWebSocketFrame tws = (TextWebSocketFrame) frame;
                logger.debug("WebSocket Client received message: " + tws.text());
                HttpSocketMessage hsm = toMessage(ctx.channel(), tws.text());
                observer.observe(hsm);
            }
        }


        protected void onClosed(Channel channel, CloseWebSocketFrame closeFrame) {
            channel.close();
        }

        abstract protected HttpSocketMessage toMessage(Channel channel, String str);
    }


    public static final ChannelFuture sendHttpResponse(
            ChannelHandlerContext ctx,
            FullHttpRequest req,
            FullHttpResponse res) {
        // Generate an error page if response getStatus code is not OK (200).

        if (!ctx.channel().isOpen()) {
            logger.warn("channel is closed can not write HttpResponse");
            throw new IllegalArgumentException("Channel was closed");
        }

/*        if (res.status().code() != 200) {
//            System.err.println("error response not 200");
            ByteBuf buf = Unpooled.copiedBuffer(res.status().toString(), CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
        }*/
        ChannelFuture f = ctx.channel().writeAndFlush(res);
        if (!HttpUtil.isKeepAlive(req) || res.status().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
        return f;
    }
}
