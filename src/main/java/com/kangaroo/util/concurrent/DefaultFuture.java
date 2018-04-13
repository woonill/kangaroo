package com.kangaroo.util.concurrent;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public final class DefaultFuture<T> implements Future<T>{

	
    private final CountDownLatch finished = new CountDownLatch(1);
    private final AtomicReference<T> value = new AtomicReference<T>();
    private final AtomicReference<Throwable> error = new AtomicReference<Throwable>();

    private volatile boolean cancelled = false;

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (finished.getCount() > 0) {
            cancelled = true;
            // release the latch (a race condition may have already released it by now)
            finished.countDown();
            return true;
        } else {
            // can't cancel
            return false;
        }
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public boolean isDone() {
        return finished.getCount() == 0;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        finished.await();
        return getValue();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (finished.await(timeout, unit)) {
            return getValue();
        } else {
            throw new TimeoutException("Timed out after " + unit.toMillis(timeout) + "ms waiting for underlying Observable.");
        }
    }

    private T getValue() throws ExecutionException {
        if (error.get() != null) {
            throw new ExecutionException("Observable onError", error.get());
        } else if (cancelled) {
            // Contract of Future.get() requires us to throw this:
            throw new CancellationException("Subscription unsubscribed");
        } else {
            return value.get();
        }
    }

	public void success(T v) {
		if(!this.isDone()){
            value.set(v);
            finished.countDown();
		}
	}


    public static Future<?> successFuture(Object obs){

        DefaultFuture df = new DefaultFuture();
        df.success(obs);
        return df;
    }

    public static Future<?> errorFuture(Throwable te){

        DefaultFuture df = new DefaultFuture();
        df.error.set(te);
        df.finished.countDown();
        return df;
    }
}
