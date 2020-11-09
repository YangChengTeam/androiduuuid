package com.yc.androidunicodeexample;

import android.app.Application;
import android.content.Context;

import com.bun.miitmdid.core.JLibrary;
import com.yc.uuid.UUID;
import com.yc.uuid.UUIDInfo;


public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        UUID.getInstance(this).init();

    }
}
