package com.kangaroo.producer.http;

/**
 * Created by woonill on 03/01/2017.
 */
public interface HttpSocketSessionContext {


    HttpSocketSession[] getSessions();

    HttpSocketSession getSession(String sid);
}
