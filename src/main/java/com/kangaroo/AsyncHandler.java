package com.kangaroo;

import com.kangaroo.internal.observable.SObserver;

import java.util.concurrent.CompletableFuture;

public interface AsyncHandler<T,R> extends SObserver<T,R> {

    CompletableFuture<R> exec(R req);
}
