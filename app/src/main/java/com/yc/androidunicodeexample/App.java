package com.yc.androidunicodeexample;

import android.app.Application;
import android.content.Context;

import com.yc.uuid.UDID;


public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        UDID.getInstance(this).init();

    }
}
