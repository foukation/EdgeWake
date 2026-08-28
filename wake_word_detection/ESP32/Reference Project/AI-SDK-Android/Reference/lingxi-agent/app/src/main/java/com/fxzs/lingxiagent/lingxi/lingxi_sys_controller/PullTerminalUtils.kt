package com.example.device_control

import android.os.Build
import android.text.TextUtils
import android.util.Log
import java.io.UnsupportedEncodingException
import java.net.URLEncoder

/**
 *创建者：ZyOng
 *描述：拉起移动app sellerid
 *创建时间：2025/6/18 9:23 AM
 */
object PullTerminalUtils {

    fun getDeviceSellerId(): String {
        var sellerId = ""
        if (Build.MANUFACTURER.contains("HUAWEI")) {
            sellerId = "2136971FW1230300001"
        }else if (Build.MANUFACTURER.contains("OPPO") || Build.MANUFACTURER.contains("oppo")) {
            sellerId = "2136973FW1230500001"
        }
        else if (Build.MANUFACTURER.contains("VIVO") || Build.MANUFACTURER.contains("vivo")) {
            sellerId = "2136972FW1230400001"
        }

        else if (Build.MANUFACTURER.contains("Xiaomi") || Build.MANUFACTURER.contains("xiaomi")) {
            sellerId = "2136975FW1231000001"
        }
        else if (Build.MANUFACTURER.contains("ZTE") ) {
            sellerId = "2136975FW1301300001"
        }
        else if (Build.MANUFACTURER.contains("samsung")  || Build.MANUFACTURER.contains("SAMSUNG")) {
            sellerId = "2136975FW1231000002"
        }
        if (!TextUtils.isEmpty(sellerId)){
            sellerId += "_"
        }
        return sellerId
    }
    fun urlEncode(url:String):String {
        var encoded = ""
        try {
            encoded = URLEncoder.encode(url, "UTF-8")
            Log.i("UrlEncode", "encoded = $encoded")
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }
        return encoded
    }
}