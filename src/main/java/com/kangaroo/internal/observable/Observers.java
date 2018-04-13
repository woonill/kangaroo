package com.kangaroo.internal.observable;

import io.reactivex.ObservableEmitter;
import io.reactivex.Observer;
import io.reactivex.observers.DefaultObserver;

public class Observers {

    private Observers() {
        throw new IllegalStateException("No instances!");
    }

    private static final Observer<Object> EMPTY = new DefaultObserver<Object>() {

        @Override
        public final void onError(Throwable e) {
        }

        @Override
        public void onComplete() {
        }

        @Override
        public final void onNext(Object args) {
        }
    };

    public static <T> Observer<T> empty() {
        return (Observer<T>) EMPTY;
    }



    public static <T> Observer<T> toObserver(ObservableEmitter<T> emitter){

        return new DefaultSafeObserver<T>(){

            @Override
            protected Observer<T> getObserver() {
                return new DefaultObserver<T>() {

                    @Override
                    public void onNext(T t) {
                        emitter.onNext(t);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        emitter.onError(throwable);
                    }

                    @Override
                    public void onComplete() {
                        emitter.onComplete();
                    }
                };
            }
        };
    }
}
