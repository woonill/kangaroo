package com.kangaroo.internal.fastjson;

@SuppressWarnings("serial")
public class JSONPathException extends com.kangaroo.internal.fastjson.JSONException {

    public JSONPathException(String message){
        super(message);
    }
    
    public JSONPathException(String message, Throwable cause){
        super(message, cause);
    }
}
