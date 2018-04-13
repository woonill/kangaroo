package com.kangaroo.producer.http;

/*
 * Created by woonill on 6/16/16.
 * */


public interface HttpSocketMessageObserver {

    default void onError(Throwable error) {
        error.printStackTrace();
    }

    void observe(HttpSocketMessage msg);
}
