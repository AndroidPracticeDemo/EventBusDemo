package com.example.summerrc.eventbusdemo;

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
