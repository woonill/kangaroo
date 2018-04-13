package com.kangaroo;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public interface Proc<T extends Global.Context> {

    T getContext();

    Future<?> halt();
    CompletableFuture<Future<?>> haltFuture();
}
