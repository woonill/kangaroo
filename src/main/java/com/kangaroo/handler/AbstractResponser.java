package com.kangaroo.handler;

import com.kangaroo.DefaultResponse;
import com.kangaroo.Message;
import com.kangaroo.Request;
import com.kangaroo.Response;
import com.kangaroo.util.Validate;

public abstract class AbstractResponser implements Request.Responser {


    private Request request;


    public AbstractResponser(Request requst) {

        Validate.notNull(requst, "Request null");

        this.request = requst;
    }


    protected Request request() {
        return request;
    }

    @Override
    public Response errorResonse(Throwable e) {

        return new DefaultResponse.Builder(request)
                .statusCode(Message.BAD_REQUEST)
                .error(e)
                .theType(Message.Type.error)
                .build();
    }

    @Override
    public Response errorResonse(int i, Throwable e) {


        return new DefaultResponse.Builder(request)
                .statusCode(i)
                .error(e)
                .theType(Message.Type.error)
                .build();
    }

    @Override
    public Response response(byte[] contents, Message.Type type) {
        return new DefaultResponse.Builder(request)
                .payload(contents)
                .statusCode(Message.SUCCESS_STATUS_CODE)
                .theType(type)
                .build();
    }

    @Override
    public Response response(int statusCode, byte[] contents, Message.Type type) {
        return new DefaultResponse.Builder(request)
                .payload(contents)
                .statusCode(statusCode)
                .theType(type)
                .build();
    }

    @Override
    public Response noneResponse() {

        return new DefaultResponse.Builder(request)
                .payload("".getBytes())
                .statusCode(200)
                .theType(Message.Type.none)
                .build();
    }

    @Override
    public Response errorResonse(int status, String errorMsg, Throwable e) {
        return new DefaultResponse.Builder(request)
                .statusCode(status)
                .error(e)
                .theType(Message.Type.error)
                .build();
    }
}

