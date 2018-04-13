package com.kangaroo;

import java.util.Optional;

public interface Response {

    byte[] body();

    boolean isNone();

    int status();

    boolean isError();

    int getTypeCode();

    default Throwable getError() {
        return null;
    }

    Optional<String> getHeaderVal(String key);

    String[] getHeaderKeys();

    public static interface ResponseBuilder {

        Response errorResonse(Throwable e);

        Response errorResonse(int i, Throwable te);

        Response response(byte[] contents, Message.Type type);

        Response response(int statusCode, byte[] contents, Message.Type type);

        Response noneResponse();

        Response errorResonse(int status, String errorMsg, Throwable te);
    }


    public interface ResponserFactory {


        Responser get(Request request);
    }


    public interface Responser extends ResponseBuilder {
        Response objectToResponse(Object obj);

    }
}
