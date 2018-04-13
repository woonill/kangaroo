package com.kangaroo.internal.observable;

import io.reactivex.Observable;

public interface SObserver<T,R> {

    Observable<R> observe(T request);
}
