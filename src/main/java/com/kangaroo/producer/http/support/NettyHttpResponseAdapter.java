/*
package com.kangaroo.producer.http.support;

import com.kangaroo.Header;
import com.kangaroo.Message;
import com.kangaroo.Request;
import com.kangaroo.Response;
import com.kangaroo.producer.http.SHttpResponse;
import com.kangaroo.util.ObjectUtil;
import io.netty.handler.codec.http.FullHttpResponse;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class NettyHttpResponseAdapter implements BiFunction<Request, Response, FullHttpResponse> {


    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public final FullHttpResponse apply(Request request, Response res) {

        logger.debug("Start mapping Response to HttpResponse");

        if (res == null || res.body() == null) {
            return noneResponse();
        }

        if (res.isError()) {
            return handleException(res);
        }

        logger.debug("Body:" + res.body());
        logger.debug("Type:" + res.getContentType() + "   ---------------------------------------------->>");
        String contentsType = getContentsType(Message.Type.get(res.getTypeCode()));
        if (Message.Type.none.equals(Message.Type.get(res.getTypeCode()))) {
            return this.noneResponse();
        }
        logger.debug("ContentsType:" + contentsType);
        byte[] contents = this.toBytePayload(res);
        SHttpResponse.HttpResponseBuilder hrb = SHttpResponse.HttpResponseBuilder.newIns();
        hrb.setContentType(contentsType);
        hrb.status(res.status());


        Header[] headers = this.httpHeaders(res);

        if (!ObjectUtil.isEmpty(headers)) {
            Observable.fromArray(headers).forEach((Header header) -> {
                hrb.header(header.name(), (String) header.value());
            });
        }

        SHttpResponse.HttpCookie httpCookie = this.cookie(request, res);
        if (httpCookie != null) {
            hrb.setCookie(httpCookie);
        }

        SHttpResponse sr = hrb.build(contents);

        logger.debug("HttpResponse status:" + sr.status() + " \n");


        return sr;

    }

    protected byte[] toBytePayload(Response res) {
        return (byte[]) res.body();
    }

    protected SHttpResponse handleException(Response res) {
        return SHttpResponse.HttpResponseBuilder.newIns()
                .setContentType("text/plain,text/html")
                .status(Message.BAD_REQUEST)
                .build("BadRequest".getBytes());
    }

    protected Header[] httpHeaders(Response res) {


        List<Header> headerList = new LinkedList<Header>();
        Observable.fromArray(res.getHeaderKeys())
                .forEach((String event) ->{
                    headerList.add(new Header(event, res.getHeaderVal(event).get()));
                });
        if (!headerList.isEmpty()) {
            return headerList.toArray(new Header[headerList.size()]);
        }
        return null;
    }

    protected SHttpResponse.HttpCookie cookie(Request request, Response res) {
        return null;
    }

    protected SHttpResponse noneResponse() {
        return SHttpResponse.HttpResponseBuilder.newIns()
                .setContentType("text/plain,text/html")
                .status(Message.NOT_FOUND)
                .build("Not found Resource".getBytes());
    }
}

*/
