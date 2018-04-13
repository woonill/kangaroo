package com.kangaroo.producer.http;

import com.kangaroo.Request;

/**
 * Created by woonill on 11/01/2017.
 */
public interface HttpSocketSessionListener {


    void onOpen(Request request, HttpSocketSession session);

    void onClosed(HttpSocketSession session);
}
