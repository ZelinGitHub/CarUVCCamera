package com.wt.carcamera;

import android.app.Application;
import android.util.Log;

import com.wt.carcamera.util.TtsManagerProxy;

import org.litepal.LitePal;

import io.reactivex.functions.Consumer;
import io.reactivex.plugins.RxJavaPlugins;

public class AICameraApplication extends Application {

    private static final String TAG = "AICameraApplication";

    private static Application appCtx;



    @Override
    public void onCreate() {
        super.onCreate();
        appCtx = this;
        LitePal.initialize(this);
        initRxJava();
        TtsManagerProxy.init(this);
    }


    public static Application appCtx() {
        return appCtx;
    }


    private void initRxJava() {
        RxJavaPlugins.setErrorHandler(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                Log.e(TAG, Log.getStackTraceString(throwable));
            }
        });
    }



}

