package com.kangaroo.producer.http.support;

import com.alibaba.fastjson.JSON;
import com.kangaroo.*;
import com.kangaroo.internal.observable.DefaultSafeObserver;
import com.kangaroo.producer.http.HttpResponseAdapter;
import com.kangaroo.producer.http.SHttpResponse;
import com.kangaroo.util.StrUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.*;
import io.reactivex.Observable;
import io.reactivex.ObservableOperator;
import io.reactivex.Observer;
import io.reactivex.functions.Function;
import io.reactivex.observers.DefaultObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.BiFunction;

public class SimpleNettyHttpHandler implements NettyHttpHandler {

    static final String JSON_REQUEST_CONTENTS_TYPE = "application/json";
    static final String JAVA_OBJECT = "application/x-java-serialized-object";

    private static final Logger logger = LoggerFactory.getLogger(SimpleNettyHttpHandler.class);

    private RequestObserver requestSubscriber;
    private BiFunction<Request, Response, SHttpResponse> responseFunc2;

    public SimpleNettyHttpHandler(RequestObserver handler) {
        this(handler, new HttpResponseAdapter());
    }

    public SimpleNettyHttpHandler(RequestObserver handler, BiFunction<Request, Response, SHttpResponse> resHandler) {
        this.requestSubscriber = handler;
        this.responseFunc2 = resHandler;
    }


    @Override
    public Observable<FullHttpResponse> observe(FullHttpRequest fullHttpRequest) {

        logger.debug("Start handler request");
        final NettyHttpData nhd = new NettyHttpData(fullHttpRequest);
        Request request = nhd.initParameter();
        logger.debug("Start observer request now");
        return this.requestSubscriber
                .observe(request)
                .map(new Function<Response, SHttpResponse>() {
                    @Override
                    public SHttpResponse apply(Response response) {
                        return responseFunc2.apply(request, response);
                    }})
                .lift(new ObservableOperator<FullHttpResponse, SHttpResponse>() {
                    @Override
                    public Observer<? super SHttpResponse> apply(Observer<? super FullHttpResponse> observer) throws Exception {

                        return new DefaultSafeObserver<SHttpResponse>(){
                            @Override
                            protected Observer<SHttpResponse> getObserver() {
                                return new DefaultObserver<SHttpResponse>() {
                                    @Override
                                    public void onNext(SHttpResponse sHttpResponse) {
                                        final FullHttpResponse fresponse = buildFullHttpResponse(fullHttpRequest, sHttpResponse);
                                        observer.onNext(fresponse);
                                        observer.onComplete();
                                    }

                                    @Override
                                    public void onError(Throwable throwable) {
                                        observer.onError(throwable);
                                    }
                                    @Override
                                    public void onComplete() {
                                        nhd.destory();
                                    }
                                };
                            }
                        };
                    }
                });
    }


    final static class NettyHttpData {

        //            private HttpDataFactory factory;
        private FullHttpRequest request = null;
        private Logger logger = LoggerFactory.getLogger(this.getClass());


        private Logger logger() {
            return this.logger;
        }

        public NettyHttpData(FullHttpRequest req) {
//                this.factory = factory;
            this.request = req;
        }

        Request initParameter() {

            String requestUri = this.request.uri();
            if (requestUri.indexOf("?") > 0) {
                requestUri = this.request.uri().substring(0, this.request.uri().indexOf("?"));
            }

            logger.debug("Request URI:" + requestUri + "  and Method:" + request.method().name());
            String cookieSet = request.headers().get(HttpHeaderNames.COOKIE);
            logger.debug("Cookie:" + cookieSet);

            List<Header> headers = new ArrayList<>();
            Header[] header = null;
            if (!StrUtils.isNull(cookieSet)) { // find cookies will add messages
                Set<Cookie> cset = CookieDecoder.decode(cookieSet);
                for (Cookie cookie : cset) {
                    headers.add(new Header(cookie.name(), cookie.value()));
                }
            }
            request.headers().entries().forEach((Map.Entry<String, String> entr) -> {
                if (!HttpHeaderNames.COOKIE.contentEqualsIgnoreCase(entr.getKey())) {
                    headers.add(new Header(entr.getKey(), entr.getValue()));
                }
            });

            headers.add(new Header(Header.ACTION_TYPE_HEADER, request.method().name()));

            if (!headers.isEmpty()) {
                header = headers.toArray(new Header[headers.size()]);
            }

            Request resRequest = null;
            if (HttpMethod.POST.equals(request.method())) {

                //HttpHeaders.Names
                final String rcType = request.headers().get(HttpHeaderNames.CONTENT_TYPE);
                logger.debug("Request Contents type:" + rcType);
                logger.debug("The last HttpContent");
                logger.debug("failed:" + request.decoderResult().isFailure());
                logger.debug("finished:" + request.decoderResult().isFinished());
                logger.debug("successed:" + request.decoderResult().isSuccess());

                if (JAVA_OBJECT.equalsIgnoreCase(rcType) || JSON_REQUEST_CONTENTS_TYPE.equalsIgnoreCase(rcType)) {
                    final ByteBuf content = request.content();
                    byte[] req = new byte[content.readableBytes()];
                    content.readBytes(req);
                    resRequest = new DefaultRequest(requestUri, req, header);
                } else {
                    if (HttpPostRequestDecoder.isMultipart(request)) {
                        this.decoder = new HttpPostRequestDecoder(new DefaultHttpDataFactory(true), request);
                        final InterfaceHttpPostRequestDecoder offer = decoder.offer(request);
                        resRequest = readHttpDataChunkByChunk(requestUri, this.request, header, offer);
                    } else {
                        logger.info("Start handle :" + rcType + "  Request");
                        this.decoder = new HttpPostRequestDecoder(request);
                        final InterfaceHttpPostRequestDecoder offer = decoder.offer(request);
                        resRequest = readHttpDataChunkByChunk(requestUri, this.request, header, offer);
                    }
                }
            } else if (HttpMethod.GET.equals(request.method())) {
                logger.debug("Handle GetMethod");
                QueryStringDecoder qsd = new QueryStringDecoder(request.uri());
                Map<String, List<String>> maps = qsd.parameters();
                Map<String, String> values = new HashMap<>();
                Set<String> keys = maps.keySet();
                Iterator<String> ikeys = keys.iterator();
                while (ikeys.hasNext()) {
                    String key = ikeys.next();
                    StringBuilder sb = new StringBuilder();
                    List<String> qvals = maps.get(key);
                    int i = 0;
                    for (i = 0; i < qvals.size(); i++) {
                        if (i > 0) {
                            sb.append(",");
                        }
                        String str = qvals.get(i);
                        sb.append(str);
                    }
                    values.put(key, sb.toString());
                }
                resRequest = new DefaultRequest(requestUri, JSON.toJSONBytes(values), header);
            } else {
                logger.debug("Is Non Support method:" + request.method());
                resRequest = new DefaultRequest(requestUri, "".getBytes(), header);
            }
            return resRequest;
        }

        private HttpPostRequestDecoder decoder;

        private Request readHttpDataChunkByChunk(String path, HttpContent chunk,Header[] header, InterfaceHttpPostRequestDecoder offer) {

            byte[] payload = null;
            Map<String, String> textValues = new HashMap<String, String>();
            List<Attachment> attachments = new LinkedList<>();

            try {
                List<InterfaceHttpData> datas = offer.getBodyHttpDatas();
                logger.debug("Request size:" + datas.size());
                for (InterfaceHttpData data : datas) {

//                    while (offer.hasNext()){
                    logger.debug("Start handle Request now");
//                        InterfaceHttpData data = offer.next();
                    if (data != null) {
                        try {
                            if (InterfaceHttpData.HttpDataType.FileUpload.equals(data.getHttpDataType())) {
                                logger().debug("Start handler upload file");
                                FileUpload fu = (FileUpload) data;
                                if (fu.isCompleted()) {
                                    File file = fu.getFile();
                                    attachments.add(new Attachment(fu.getName(), fu.getFilename(), fu.get(), file));
                                }
                            } else {
                                Attribute ma = (Attribute) data;
                                ma.setCharset(Charset.forName("UTF-8"));
                                logger().debug("Post Attribute name:" + ma.getName() + " Post Value:" + ma.getValue());
                                textValues.put(ma.getName(), ma.getValue());
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            this.destory();
                            ;
                            throw new IllegalArgumentException(e);
                        }
                    }
                }
                payload = JSON.toJSONBytes(textValues);
            } catch (HttpPostRequestDecoder.EndOfDataDecoderException e1) {
                e1.printStackTrace();
                this.destory();
                throw new IllegalArgumentException(e1);
            }

            if (attachments.isEmpty()) {
                return new DefaultRequest(path, payload, header);
            }

            Attachment[] attaArray = attachments.toArray(new Attachment[attachments.size()]);
            return new DefaultRequest(path, payload, header, attaArray);
        }


        final void destory() {

            if (this.decoder != null) {
                decoder.cleanFiles();
                decoder.destroy();
                this.decoder = null;
            }
        }

    }


    static FullHttpResponse buildFullHttpResponse(FullHttpRequest reqs, SHttpResponse res) {

        if (res.status() != Message.SUCCESS_STATUS_CODE) {

            if (Message.REDIRECT_STATUS == res.status()) {

                DefaultFullHttpResponse dfh = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1,
                        HttpResponseStatus.FOUND);

                dfh.headers().set(Message.REDIRECT_LOCATION_FIELD, new String(res.payload(), Charset.defaultCharset()));
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
}
