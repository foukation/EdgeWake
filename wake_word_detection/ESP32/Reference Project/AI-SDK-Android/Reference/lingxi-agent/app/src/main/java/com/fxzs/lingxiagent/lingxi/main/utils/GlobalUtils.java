package com.fxzs.lingxiagent.lingxi.main.utils;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class GlobalUtils {
    private static final String TAG = "GlobalUtils";

    // 将Base64编码转换为Bitmap
    public static String readExternalJson(String filename) {
        File file = new File(Environment.getExternalStorageDirectory(), filename);
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (IOException e) {
            return null;
        }
    }
}
