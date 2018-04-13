package com.kangaroo.internal.observable;

import io.reactivex.observers.DefaultObserver;

public abstract class DefaultObserver2<T> extends DefaultObserver<T>{

    @Override
    public void onError(Throwable throwable) {
        throwable.printStackTrace();
    }

    @Override
    public void onComplete() {
    }
}
