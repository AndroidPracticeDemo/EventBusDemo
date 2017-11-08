# EventBusDemo

[![Gitter](https://badges.gitter.im/SummerRC/EventBusForThread.svg)](https://gitter.im/SummerRC/EventBusForThread?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

## 一、简介
在公司项目[语戏](http://yuxip.com)(一款二次元社交软件）的开发过程中，由于工程很大，需要互相通讯的模块有很多，一般的通讯方式如接口回调、Handler、广播等都有其局限性，因此我们在项目中广泛使用开源库[EventBus](https://github.com/greenrobot/EventBus)进行模块 之间的通讯。不仅使用灵活，而且在很大程度上降低了模块之间的耦合性。

EvnetBus的github地址：https://github.com/greenrobot/EventBus

## 二、EventBus的基本使用

### (1)通过配置Gradle文件下载EventBus类库 
```java 
dependencies {
    compile 'de.greenrobot:eventbus:2.4.0'
}
```

当然也可以通过添加jar包的方式使用EventBus，[EventBus jar包下载地址](https://github.com/SummerRC/EventBusDemo/blob/master/libs/eventbus.jar)

### (2)自定义一个事件类，比如：
```java 
public class ThreadEvent {
    public Object object;           //用于Event中传递信息
    public Event event;             //用于区别Event的类型:获取到内容和取消线程两个类型
    
    public enum Event {
        EVENT_GET_CONTENT, EVENT_CANCLE_THREAD
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
public void onEventMainThread(ThreadEvent threadEvent) {
    switch (threadEvent.event) {        //消息类型
        case EVENT_CANCLE_THREAD:
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

### (7)EventBus的4个接收事件的回调函数：  

- **onEvent**：   它和ThreadModel中的PostThread对应，这个也是默认的类型，当使用这种类型时，回调函数和发起事件的函数会在同一个线程中执行   
- **onEventMainThread**：当使用这种类型时，回调函数会在主线程中执行，这个在Android中非常有用，因为在Android中禁止在子线程中修改UI   
- **onEventBackgroundThread**：当使用这种类型时，如果事件发起函数在主线程中执行，那么回调函数另启动一个子线程，如果事件发起函数在子线程执行，那么回调函数就在这个子线程执行。   
- **onEventBusAsync**：当使用这种类型时，不管事件发起函数在哪里执行，都会另起一个线程去执行回调。

## 三、例子的详细代码
用EventBus写了一个简单的小例子：MainActivity启动之后开启一个线程去访问http://baidu.com, 线程将访问到的网页内容以流的形式读出之后，每读取一个字符就将该字符封装到Event里面通过EventBus发送给MainActivity,MainActivity每得到一个字符就将其显示到TextView上面，从而达到一个逐字书写的效果。代码如下：

### (1)主界面MainActivity类:

```java 
public class MainActivity extends Activity {

    private TextView tv_content;
    private NetThread netThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);     //去掉标题栏
        setContentView(R.layout.activity_main);
        tv_content = (TextView) findViewById(R.id.tv_content);

        /** register EventBus*/
        EventBus.getDefault().register(this);
        netThread = new NetThread();
        netThread.start();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        /** Unregister EventBus*/
        EventBus.getDefault().unregister(this);
        netThread.stopThreadByFlag();
    }


    /**
     * 接收ThreadEvent事件
     * @param threadEvent 事件类型
     */
    public void onEventMainThread(ThreadEvent threadEvent) {
        switch (threadEvent.event) {        //消息类型
            case EVENT_CANCLE_THREAD:
                break;
            case EVENT_GET_CONTENT:
                StringBuffer text = new StringBuffer(tv_content.getText().toString());
                String content = (String) threadEvent.object;
                text.append(content);
                tv_content.setText(text);
        }

    }

}
```

### (2)访问网络的自定义线程NetThread类：

```java
public class NetThread extends Thread {
    private DefaultHttpClient client;
    private String url;
    private boolean flag = true;    //控制线程的标志位

    public NetThread() {
        url = "http://www.baidu.com/";
    }

    public void stopThreadByFlag() {
        flag = false;
    }

    public void run() {
        client = new DefaultHttpClient();
        client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 2000);     //请求超时时间为2秒
        client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 30000);            //读取超时时间为3秒

        try {
            HttpGet get = new HttpGet(url);
            get.getParams().setParameter(CoreProtocolPNames.HTTP_CONTENT_CHARSET, "UTF-8");
            HttpResponse response = client.execute(get);
            ThreadEvent threadEvent = new ThreadEvent();


            if (response.getStatusLine().getStatusCode() == 200) {
                BufferedReader bin = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                char content;

                /** 一次读取一个字符，每读取一个字符就发送一个事件 */
                while ((bin.read()!=-1) && flag) {
                    content = (char) bin.read();
                    threadEvent.object = String.valueOf(content);
                    threadEvent.event = ThreadEvent.Event.EVENT_GET_CONTENT;
                    /** EventBus发送ThreadEvent事件， 由注册了该ThreadEvent事件的对象接收 */
                    EventBus.getDefault().post(threadEvent);
                    /** 线程休眠0.05秒钟 */
                    Thread.sleep(50);
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
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
    public Object object;           //用于Event中传递信息
    public Event event;             //用于区别Event的类型:获取到内容和取消线程两个类型

    public enum Event {
        EVENT_GET_CONTENT, EVENT_CANCLE_THREAD
    }
}
```

## 四、两个Demo
EventBus使用挺简单的，自己写了一个Deme,修改了一个网上别人写的Demo,Demo地址：

- [EventBusForThread](http://github.com/SummerRC/EventBusForThread)  
- [EventBusDemo](http://github.com/SummerRC/EventBusDemo)
- [吐槽点啥呢]()
