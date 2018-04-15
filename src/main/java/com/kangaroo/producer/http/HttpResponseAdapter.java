/*
package com.kangaroo.producer.http;

import com.kangaroo.Header;
import com.kangaroo.Message;
import com.kangaroo.Request;
import com.kangaroo.Response;
import com.kangaroo.util.ObjectUtil;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

*/
/**
 * Created by woonill on 10/9/15.
 *//*

public class HttpResponseAdapter implements BiFunction<Request, Response, SHttpResponse> {


    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public final SHttpResponse apply(Request request, Response res) {

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


    static final String getContentsType(Message.Type type) {
        return HTTP_RESPONSE_MAPPER.get(type.code());
    }


    private static final Map<Integer, String> HTTP_RESPONSE_MAPPER = new HashMap<Integer, String>();

    static {

        HTTP_RESPONSE_MAPPER.put(Message.Type.htm.code(), "text/html; charset=utf-8");
        HTTP_RESPONSE_MAPPER.put(Message.Type.html.code(), "text/html; charset=utf-8");
        HTTP_RESPONSE_MAPPER.put(Message.Type.xml.code(), "text/xml; charset=utf-8");
        HTTP_RESPONSE_MAPPER.put(Message.Type.json.code(), "text/json; charset=utf-8");
        HTTP_RESPONSE_MAPPER.put(Message.Type.text.code(), "text/html; charset=utf-8");
        HTTP_RESPONSE_MAPPER.put(Message.Type.css.code(), "text/css; charset=utf-8");
        HTTP_RESPONSE_MAPPER.put(Message.Type.scss.code(), "text/css; charset=utf-8");
        HTTP_RESPONSE_MAPPER.put(Message.Type.svg.code(), "text/xml; charset=utf-8");
        HTTP_RESPONSE_MAPPER.put(Message.Type.woff.code(), "font/x-font-woff");
        HTTP_RESPONSE_MAPPER.put(Message.Type.eot.code(), "font/x-font-eot");


        HTTP_RESPONSE_MAPPER.put(Message.Type.png.code(), "image/png");
        HTTP_RESPONSE_MAPPER.put(Message.Type.jpg.code(), "image/jpg");
        HTTP_RESPONSE_MAPPER.put(Message.Type.tif.code(), "image/tiff");
        HTTP_RESPONSE_MAPPER.put(Message.Type.tiff.code(), "image/tiff");
        HTTP_RESPONSE_MAPPER.put(Message.Type.ico.code(), "image/x-icon");


        HTTP_RESPONSE_MAPPER.put(Message.Type.flash.code(), "application/x-shockwave-flash");
        HTTP_RESPONSE_MAPPER.put(Message.Type.js.code(), "application/x-javascript");
        HTTP_RESPONSE_MAPPER.put(Message.Type.map.code(), "application/x-javascript");

        HTTP_RESPONSE_MAPPER.put(Message.Type.gif.code(), "image/gif");
        HTTP_RESPONSE_MAPPER.put(Message.Type.bmp.code(), "image/bmp");
        HTTP_RESPONSE_MAPPER.put(Message.Type.jpeg.code(), "image/jpeg");
        HTTP_RESPONSE_MAPPER.put(Message.Type.ppt.code(), "application/vnd.ms-powerpoint");

        HTTP_RESPONSE_MAPPER.put(Message.Type.pdf.code(), "application/pdf");
        HTTP_RESPONSE_MAPPER.put(Message.Type.word.code(), "application/msword");
        HTTP_RESPONSE_MAPPER.put(Message.Type.word.code(), "application/msword");
        HTTP_RESPONSE_MAPPER.put(Message.Type.excel.code(), "application/vnd.ms-excel");


        HTTP_RESPONSE_MAPPER.put(Message.Type.zip.code(), "application/zip");
        HTTP_RESPONSE_MAPPER.put(Message.Type.gz.code(), "application/x-gzip");
        HTTP_RESPONSE_MAPPER.put(Message.Type.tar.code(), "application/x-tar");

    }


    public interface HeaderCreator {

        public Header[] call(Request request, Response response);
    }

}
*/
