package com.kangaroo.producer.http.support;

import com.alibaba.fastjson.JSON;
import com.kangaroo.Attachment;
import com.kangaroo.DefaultRequest;
import com.kangaroo.Header;
import com.kangaroo.Request;
import com.kangaroo.internal.observable.DefaultSafeObserver;
import com.kangaroo.internal.observable.Observers;
import com.kangaroo.producer.http.HttpRequestHandler;
import com.kangaroo.producer.http.SHttpResponse;
import com.kangaroo.util.StrUtils;
import com.kangaroo.util.Validate;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.*;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableOperator;
import io.reactivex.observers.DefaultObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Created by woonill on 21/11/2016.
 */

public final class NettyHttpHandlerAdapter implements NettyHttpHandler {

    static final String JSON_REQUEST_CONTENTS_TYPE = "application/json";
    static final String FULL_JSON_REQUEST_CONTENTS_TYPE = "application/json;charset=UTF-8";
    static final String JAVA_OBJECT = "application/x-java-serialized-object";

    private static final Logger logger = LoggerFactory.getLogger(NettyHttpHandlerAdapter.class);

    private final HttpRequestHandler handler;

    public NettyHttpHandlerAdapter(HttpRequestHandler handler1) {
        Validate.notNull(handler1);
        this.handler = handler1;
    }


    @Override
    public Observable<FullHttpResponse> observe(FullHttpRequest frequest) {


        return Observable.create(new ObservableOnSubscribe<FullHttpResponse>() {
            @Override
            public void subscribe(ObservableEmitter<FullHttpResponse> observableEmitter) throws Exception {
                logger.debug("Request URI:" + getUri(frequest) + "  and Method:" + frequest.method().name());
                if (HttpMethod.POST.equals(frequest.method())) {
                    postObserve(frequest)
                            .map((SHttpResponse sres) -> {
                                return DefaultNettyHttpChannelHandler.buildFullHttpResponse(frequest,sres);
                            })
                            .subscribe(Observers.toObserver(observableEmitter));

                } else if (HttpMethod.GET.equals(frequest.method())) {
                    handler.observe(getRequest(frequest))
                            .map((SHttpResponse sres) -> {
                                return DefaultNettyHttpChannelHandler.buildFullHttpResponse(frequest,sres);
                            })
                            .subscribe(Observers.toObserver(observableEmitter));
                } else {
//                    logger.debug("Is Non Support method:" + request.method());
                    final Header[] headers = initHeader(frequest);
                    final Request request1 = new DefaultRequest(getUri(frequest), "".getBytes(), headers);
                    handler.observe(getRequest(frequest))
                            .map((SHttpResponse sres) -> {
                                return DefaultNettyHttpChannelHandler.buildFullHttpResponse(frequest,sres);
                            })
                            .subscribe(Observers.toObserver(observableEmitter));
                }
            }
        });
    }


    public Observable<SHttpResponse> postObserve(FullHttpRequest request) {


        //HttpHeaders.Names

        Header[] header = initHeader(request);
        String requestUri = getUri(request);
        HttpPostRequestDecoder decoder = null;
        final String rcType = request.headers().get(HttpHeaderNames.CONTENT_TYPE);
        logger.debug("Request Contents type:" + rcType);
        logger.debug("The last HttpContent");
        logger.debug("Is failed:" + request.decoderResult().isFailure());
        logger.debug("Is finished:" + request.decoderResult().isFinished());
        logger.debug("Is successed:" + request.decoderResult().isSuccess());

        Request resRequest = null;
        if (JAVA_OBJECT.equalsIgnoreCase(rcType)
                || JSON_REQUEST_CONTENTS_TYPE.equalsIgnoreCase(rcType)
                || FULL_JSON_REQUEST_CONTENTS_TYPE.equalsIgnoreCase(rcType)) {
            logger.debug("The Java Object RPC request");
            final ByteBuf content = request.content();
            byte[] req = new byte[content.readableBytes()];
            content.readBytes(req);
            resRequest = new DefaultRequest(requestUri, req, header);
        } else {
            if (HttpPostRequestDecoder.isMultipart(request)) {
                decoder = new HttpPostRequestDecoder(new DefaultHttpDataFactory(true), request);
                resRequest = postRequest(request, decoder);
            } else {
                logger.info("Start handle :" + rcType + "  Request");
                decoder = new HttpPostRequestDecoder(request); //注意此部分，不应该设置 HttpDataFactory不然以要创建虚拟的文件夹等带来异常发生
                resRequest = postRequest(request, decoder);
            }
        }
        return handler.observe(resRequest).lift(postResponsOperator(decoder));
    }


    public static final ObservableOperator<SHttpResponse, SHttpResponse> postResponsOperator(HttpPostRequestDecoder hpod) {

        return new ObservableOperator<SHttpResponse, SHttpResponse>() {
            @Override
            public io.reactivex.Observer<? super SHttpResponse> apply(io.reactivex.Observer<? super SHttpResponse> observer) throws Exception {

                return new DefaultSafeObserver<SHttpResponse>(){
                    @Override
                    protected io.reactivex.Observer<SHttpResponse> getObserver() {
                        return new DefaultObserver<SHttpResponse>() {
                            @Override
                            public void onNext(SHttpResponse event) {
                                observer.onNext(event);
                            }

                            @Override
                            public void onError(Throwable error) {
                                observer.onError(error);
                            }

                            @Override
                            public void onComplete() {
                                try {
                                    if (hpod != null) {
                                        hpod.cleanFiles();
                                        hpod.destroy();
                                    }
                                } finally {
                                    observer.onComplete();
                                }
                            }
                        };
                    }
                };
            }
        };
    }


    public static Request getRequest(FullHttpRequest request) {
        logger.debug("Handler Get method");

        Header[] header = initHeader(request);
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
        return new DefaultRequest(getUri(request), JSON.toJSONBytes(values), header);
    }


    public static final Header[] initHeader(FullHttpRequest request) {

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
        return header;
    }


    public static final String getUri(FullHttpRequest request) {

        String requestUri = request.uri();
        logger.debug("Full http uri:" + requestUri);
        if (requestUri.indexOf("?") > 0) {
            requestUri = request.uri().substring(0, request.uri().indexOf("?"));
        }
        logger.debug("Request URI:" + requestUri + "  and Method:" + request.method().name());
        return requestUri;
    }

    public static final Request postRequest(FullHttpRequest request, HttpPostRequestDecoder decoder) {

//            this.decoder= new HttpPostRequestDecoder(new DefaultHttpDataFactory(true),request);

        String path = getUri(request);
        Header[] header = initHeader(request);
        final InterfaceHttpPostRequestDecoder offer = decoder.offer(request);

        byte[] payload = null;
        Map<String, String> textValues = new HashMap<String, String>();
        List<Attachment> attachments = new LinkedList<>();

        try {

            List<InterfaceHttpData> datas = offer.getBodyHttpDatas();
            logger.debug("Request size:" + datas.size());
            for (InterfaceHttpData data : datas) {
                logger.debug("Start handle Request now");
                if (data != null) {
                    try {
                        if (InterfaceHttpData.HttpDataType.FileUpload.equals(data.getHttpDataType())) {
                            logger.debug("Start handle upload file");
                            FileUpload fu = (FileUpload) data;
                            if (fu.isCompleted()) {
                                File file = fu.getFile();
                                attachments.add(new Attachment(fu.getName(), fu.getFilename(), fu.get(), file));
                            }
                        } else {
                            Attribute ma = (Attribute) data;
                            ma.setCharset(Charset.forName("UTF-8"));
                            logger.debug("Post Attribute name:" + ma.getName() + " Post Value:" + ma.getValue());
                            textValues.put(ma.getName(), ma.getValue());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new IllegalArgumentException(e);
                    }
                }
            }
            payload = JSON.toJSONBytes(textValues);
        } catch (HttpPostRequestDecoder.EndOfDataDecoderException e1) {
            e1.printStackTrace();
            throw new IllegalArgumentException(e1);
        }

        if (attachments.isEmpty()) {
            return new DefaultRequest(path, payload, header);
        }

        Attachment[] attaArray = attachments.toArray(new Attachment[attachments.size()]);
        return new DefaultRequest(path, payload, header, attaArray);

    }
}

