package com.example.summerrc.eventbusdemo;

import org.greenrobot.eventbus.EventBus;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class NetThread extends Thread {
    private HttpURLConnection mUrlConnection;
    private URL url;
    private boolean flag = true;    //控制线程的标志位

    public NetThread() {
    }

    public void stopThreadByFlag() {
        flag = false;
    }

    public void run() {
        try {
            url = new URL("http://xiayu.me");
            mUrlConnection = (HttpURLConnection) url.openConnection();
            mUrlConnection.setConnectTimeout(20000);     //请求超时时间为2秒
            mUrlConnection.setReadTimeout(30000);        //读取超时时间为3秒

            InputStream in = new BufferedInputStream(mUrlConnection.getInputStream());
            BufferedReader bin = new BufferedReader(new InputStreamReader(in));

            ThreadEvent threadEvent = new ThreadEvent();
            int content = bin.read();
            // 一次读取一个字符，每读取一个字符就发送一个事件
            while ((content != -1) && flag) {
                threadEvent.data = String.valueOf((char) content);
                threadEvent.event = ThreadEvent.Event.EVENT_GET_CONTENT;
                // EventBus发送ThreadEvent事件， 由注册了该ThreadEvent事件的对象接收
                EventBus.getDefault().post(threadEvent);
                // 线程休眠0.05秒钟
                Thread.sleep(50);
                content = bin.read();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (mUrlConnection != null) {
                mUrlConnection.disconnect();
            }
        }
    }
}