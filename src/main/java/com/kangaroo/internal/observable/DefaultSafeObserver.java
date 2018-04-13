package com.kangaroo.internal.observable;

import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.SafeObserver;

public abstract class DefaultSafeObserver<T> implements Observer<T>{


    private SafeObserver<T> safeObserver = new SafeObserver<>(getObserver());

    public final void onSubscribe(@NonNull Disposable var1){
        safeObserver.onSubscribe(var1);
    }

    public final void onNext(@NonNull T var1){
        safeObserver.onNext(var1);
    }

    public final void onError(@NonNull Throwable var1){
        safeObserver.onError(var1);
    }

    public final void onComplete(){
        safeObserver.onComplete();
    }

    protected abstract Observer<T> getObserver();
}
