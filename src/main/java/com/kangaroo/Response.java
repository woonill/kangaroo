package com.kangaroo;

import java.util.Collections;
import java.util.Map;

public interface Response extends Global.SObject{

    byte[] body();

    String requestId();
    int status();
    Message.Type getContentType();
    default Map<String,String> props(){
        return Collections.EMPTY_MAP;
    };

/*
    Optional<String> getHeaderVal(String key);
    String[] getHeaderKeys();
*/


    public interface Factory {

        Response error(int status, Throwable te,String message);
        Response response(int status,Object object);
        Response response(int status,byte[] contents, Message.Type type);

        Response response(Object object);
        Response response(byte[] contents, Message.Type type);

    }


/*    public static interface ResponseBuilder {

        Response errorResonse(Throwable e);
        Response errorResonse(int i, Throwable te);
        Response response(byte[] contents, Message.Type type);
        Response response(int statusCode, byte[] contents, Message.Type type);
        Response noneResponse();
        Response errorResonse(int status, String errorMsg, Throwable te);
    }*/
}
