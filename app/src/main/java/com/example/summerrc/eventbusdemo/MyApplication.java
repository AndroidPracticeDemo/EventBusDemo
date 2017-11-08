package com.example.summerrc.eventbusdemo;

import android.app.Application;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by SummerRC on 17/11/8.
 */

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        initEventBusConfig();
    }

    private void initEventBusConfig() {
        //DEBUG模式下抛出异常、发布不抛出
        EventBus.builder().throwSubscriberException(BuildConfig.DEBUG).installDefaultEventBus();
    }
}
