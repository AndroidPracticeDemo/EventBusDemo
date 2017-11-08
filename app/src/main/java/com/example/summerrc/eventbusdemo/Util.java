package com.example.summerrc.eventbusdemo;

import android.text.TextUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by SummerRC on 17/11/8.
 * description: 工具类
 */

public class Util {

    public static boolean isChinese(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        String regex = "^[\\u4e00-\\u9fa5]*$";
        Matcher m = Pattern.compile(regex).matcher(str);
        return m.find();
    }

}
