package com.fxzs.lingxiagent.util

/**
 * AMapKeyProvider
 *
 * 通过 JNI 从 libamapkey.so 中获取高德地图 API Key
 * 避免 Key 以明文形式出现在 AndroidManifest.xml 或 Kotlin/Java 源码中
 *
 * 使用方式：
 *   val key = AMapKeyProvider.getApiKey()
 */
object AMapKeyProvider {

    init {
        // 加载 .so 库（对应 libamapkey.so）
        System.loadLibrary("amapkey")
    }

    /**
     * 从 Native 层获取高德地图 API Key
     * @return API Key 字符串
     */
    external fun getApiKey(): String
}