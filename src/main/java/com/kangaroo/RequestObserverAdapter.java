package com.kangaroo;

import io.reactivex.Observable;

import java.util.Objects;

public class RequestObserverAdapter implements RequestObserver{

    private Handler handler;

    public RequestObserverAdapter(Handler handler) {
        Objects.requireNonNull(handler);
        this.handler = handler;
    }

    @Override
    public Observable<Response> observe(Request request) {
        return handler.observe(request);
    }
}
