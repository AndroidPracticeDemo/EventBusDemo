package com.example.summerrc.eventbusdemo;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class MainActivity extends AppCompatActivity {

    private TextView tv_content;
    private NetThread netThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv_content = (TextView) findViewById(R.id.tv_content);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        initFab();
    }

    private void initFab() {
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "不许点我", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // register EventBus
        EventBus.getDefault().register(this);
        netThread = new NetThread();
        netThread.start();
    }

    @Override
    public void onStop() {
        // Unregister EventBus
        EventBus.getDefault().unregister(this);
        netThread.stopThreadByFlag();
        super.onStop();
    }

    /**
     * 接收ThreadEvent事件
     * @param threadEvent 事件类型
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onThreadEvent(ThreadEvent threadEvent) {
        switch (threadEvent.event) {        //消息类型
            case EVENT_CANCEL_THREAD:
                break;
            case EVENT_GET_CONTENT:
                StringBuffer text = new StringBuffer(tv_content.getText().toString());
                String content = (String) threadEvent.data;
                text.append(content);
                tv_content.setText(text);
        }
    }
}
