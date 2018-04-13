package com.kangaroo.producer.http;


import io.reactivex.Observable;

/**
 * Created by woonill on 9/19/16.
 */
public interface HttpSocketSessions extends HttpSocketSession {


    String id = "0";

    HttpSocketSession getSession(String sid);

    Observable<HttpSocketSession> sessions();

    HttpSocketSessions addSession(HttpSocketSession session);

}

