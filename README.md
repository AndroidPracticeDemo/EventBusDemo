# EventBusDemo

[![Gitter](https://badges.gitter.im/SummerRC/EventBusForThread.svg)](https://gitter.im/SummerRC/EventBusForThread?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

## 一、简介
在公司项目[语戏](http://yuxip.com)(一款二次元社交软件）的开发过程中，由于工程很大，需要互相通讯的模块有很多，一般的通讯方式如接口回调、Handler、广播等都有其局限性，因此我们在项目中广泛使用开源库[EventBus](https://github.com/greenrobot/EventBus)进行模块 之间的通讯。不仅使用灵活，而且在很大程度上降低了模块之间的耦合性。

EvnetBus的github地址：https://github.com/greenrobot/EventBus

## 二、EventBus的基本使用

### (1)通过配置Gradle文件下载EventBus类库 
```java 
dependencies {
    compile 'org.greenrobot:eventbus:3.1.1'
}
```

当然也可以通过添加jar包的方式使用EventBus，[EventBus jar包下载地址(非最新版)](https://github.com/SummerRC/EventBusDemo/blob/master/libs/eventbus.jar)

### (2)自定义一个事件类，比如：
```java 
public class ThreadEvent {
    public Object data;           //用于Event中传递信息
    public Event event;             //用于区别Event的类型:获取到内容和取消线程两个类型
    
    public enum Event {
        EVENT_GET_CONTENT, EVENT_CANCEL_THREAD
    }
}
```
### (3)在要订阅事件的类注册EventBus：
```java
EventBus.getDefault().register(this);
```
        
### (4)发送事件：
```java
EventBus.getDefault().post(new ThreadEvent());
```

### (5)定义回调函数接收事件：
```java
/**
 * 接收ThreadEvent事件
 * @param threadEvent 事件类型
 */
@Subscribe(threadMode = ThreadMode.MAIN)
public void onEventMainThread(ThreadEvent threadEvent) {
    switch (threadEvent.event) {        //消息类型
        case EVENT_CANCEL_THREAD:
            break;
        case EVENT_GET_CONTENT:
            StringBuffer text = new StringBuffer(tv_content.getText().toString());
            String content = (String) threadEvent.object;
            text.append(content);
            tv_content.setText(text);
    }
}
```
### (6)取消EventBus的注册：
```java
EventBus.getDefault().unregister(this);
```

### (7)EventBus接收事件的回调函数的4个调用模式：  

- **ThreadMode.POSTING（~~onEvent~~）**：默认类型，回调函数在发起事件的线程中执行   
- **ThreadMode.MAIN（~~onEventMainThread~~）**：回调函数在UI线程中执行  
- **ThreadMode.BACKGROUND（~~onEventBackgroundThread~~）**：如果事件发起函数在UI线程中执行，那么回调函数另启动一个子线程; 如果事件发起函数在子线程执行，那么回调函数就在这个子线程执行
- **ThreadMode.ASYNC（~~onEventBusAsync~~）**：回调在新开辟的线程中执行(不管发起函数在哪个线程), 主要用于耗时操作

### (8)EventBus的简单配置

```java 
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
```

## 三、例子的详细代码
用EventBus写了一个简单的小例子：MainActivity启动之后开启一个线程去访问http://baidu.com, 线程将访问到的网页内容以流的形式读出之后，每读取一个字符就将该字符封装到Event里面通过EventBus发送给MainActivity,MainActivity每得到一个字符就将其显示到TextView上面，从而达到一个逐字书写的效果。代码如下：

### (1)主界面MainActivity类:

```java 
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

```

### (2)访问网络的自定义线程NetThread类：

```java
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
```
### (3)自定义事件ThreadEvent类：

```java 
/**
 * @author : SummerRC on 2015/9/27 0027
 * @version :  V1.0 <当前版本>
 *          description : <在此填写描述信息>
 */
public class ThreadEvent {
    public Object data;           //用于Event中传递信息
    public Event event;             //用于区别Event的类型:获取到内容和取消线程两个类型

    public enum Event {
        EVENT_GET_CONTENT, EVENT_CANCEL_THREAD
    }
}
```

## 四、两个Demo
EventBus使用挺简单的，自己写了一个Deme,修改了一个网上别人写的Demo,Demo地址：

- [EventBusDemo One](https://github.com/AndroidPracticeDemo/EventBusDemo)  
- [EventBusDemo Two](http://github.com/SummerRC/EventBusDemo)
- [吐槽点啥呢]()
