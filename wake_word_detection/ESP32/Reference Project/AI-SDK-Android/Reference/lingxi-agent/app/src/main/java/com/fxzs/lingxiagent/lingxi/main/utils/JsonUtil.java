package com.fxzs.lingxiagent.lingxi.main.utils;

import android.content.Context;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

public class JsonUtil {

    // 读取 assets 下的 JSON 文件为字符串
    public static String loadJSONFromAsset(Context context, String fileName) {
        String json = null;
        try {
            InputStream is = context.getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return json;
    }

    // 反序列化 JSON 字符串为实体对象
    public static <T> T parseJson(Context context, String fileName, Type typeOfT) {
        String json = loadJSONFromAsset(context, fileName);
        return new Gson().fromJson(json, typeOfT);
    }
}

