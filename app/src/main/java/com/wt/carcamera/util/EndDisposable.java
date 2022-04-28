package com.wt.carcamera.util;

import io.reactivex.annotations.NonNull;
import io.reactivex.observers.DisposableObserver;

public abstract class EndDisposable<T> extends DisposableObserver<T> {

    @Override
    public void onNext(@NonNull T t) {
        onResponse(t);
    }

    @Override
    public void onError(@NonNull Throwable e) {

    }

    @Override
    public void onComplete() {

    }

    public abstract void onResponse(T response);
}
