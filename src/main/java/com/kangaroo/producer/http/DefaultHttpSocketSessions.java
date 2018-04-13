package kangaroo.producer.http;/*
package com.kangaroo.producer.http;

import com.smog.observable.Observable;
import com.kangaroo.util.Validate;
import com.kangaroo.util.logging.Logger;
import com.kangaroo.util.logging.SLoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

*/
/**
 * Created by woonill on 9/19/16.
 *//*

public final class DefaultHttpSocketSessions implements HttpSocketSessions {

    private Map<String, HttpSocketSession> sessionMap = new ConcurrentHashMap<String, HttpSocketSession>();

    private Logger logger = SLoggerFactory.getInstance(this.getClass());

    private final HttpSocketMessageProducer producer = httpSocketMessageProducer();


    @Override
    public HttpSocketSession getSession(String sid) {
        return sessionMap.get(sid);
    }

    @Override
    public Observable<HttpSocketSession> sessions() {
        return Observable.collect(this.sessionMap.values());
    }

    public HttpSocketSessions addSession(HttpSocketSession session) {
        Validate.notNull(session);
        sessionMap.put(session.id(),session);
        return this;
    }

    @Override
    public String id() {
        return HttpSocketSessions.id;
    }

    @Override
    public State state() {
        return OPEN;
    }


*/
/*    private final HttpSocketSessionState OPEN = new HttpSocketSessionState(){

        @Override
        public boolean isOpen() {
            return true;
        }

        @Override
        public boolean isClosed() {
            return false;
        }
    };*//*


    @Override
    public State close() {
        return OPEN;
    }

    @Override
    public HttpSocketMessage payload(byte[] msg) {
        return null;
    }

    @Override
    public HttpSocketMessageProducer producer() {
        return producer;
    }

    public void remove(String id) {
        this.sessionMap.remove(id);
    }


    private HttpSocketMessageProducer httpSocketMessageProducer(){


        return new HttpSocketMessageProducer() {
            @Override
            public boolean run(byte[] message) {
                Observable.collect(sessionMap.values()).each((HttpSocketSession session) -> {
                    session.producer().run(message);
                });
                return true;
            }
        } ;
    }
}





*/
