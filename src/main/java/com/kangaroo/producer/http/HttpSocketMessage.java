package com.kangaroo.producer.http;

/**
 * Created by woonill on 6/16/16.
 */
public class HttpSocketMessage {

    private String sid;
    private byte[] payload;

    public HttpSocketMessage(String id, byte[] payload) {
        this.sid = id;
        this.payload = payload;
    }

    private HttpSocketMessage(String id) {
        this.sid = id;
    }

    public byte[] payload() {
        return payload;
    }

    public String sid() {
        return this.sid;
    }

}
