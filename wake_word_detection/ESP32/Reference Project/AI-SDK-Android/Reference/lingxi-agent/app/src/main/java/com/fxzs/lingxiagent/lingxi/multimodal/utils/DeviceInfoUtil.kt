package com.fxzs.lingxiagent.lingxi.multimodal.utils

import android.content.Context
import android.provider.Settings
import com.fxzs.lingxiagent.BuildConfig
import com.fxzs.lingxiagent.util.DeviceUUIDGenerator

/**
 * 设备信息工具类
 */
object DeviceInfoUtil {

    /**
     * 获取设备号
     *
     * @param context 应用上下文
     * @return 设备号
     */
    fun getDeviceNo(context: Context): String {
//        return if (BuildConfig.FLAVOR == "tabletLaiKu") {
//            getLaiKuDeviceNo(context)
//        } else {
//            DeviceUUIDGenerator.getDeviceUUID(context)
//        }
        val deviceNo = Settings.Secure.getString(context.contentResolver, "DeviceNo")
        return deviceNo ?: DeviceUUIDGenerator.getDeviceUUID(context)
    }

    /**
     * 获取设备号类型
     *
     * @return 设备号类型
     */
    fun getNoType(context: Context): String {
//        return if (BuildConfig.FLAVOR == "tabletLaiKu") {
//            getLaiKuNoType()
//        } else {
//            "SN"
//        }
        val deviceNoType = Settings.Secure.getString(context.contentResolver, "DeviceNoType")
        return deviceNoType ?: "SN"
    }

    /**
     * 获取来酷设备号（IMEI）
     *
     * @param context 应用上下文
     * @return 设备号
     */
    private fun getLaiKuDeviceNo(context: Context): String {
        val deviceNo = Settings.Secure.getString(context.contentResolver, "IMEI")
        return deviceNo ?: ""
    }

    /**
     * 获取来酷设备号类型
     *
     * @return 设备号类型
     */
    private fun getLaiKuNoType(): String {
        return "IMEI"
    }


    /**
     * 获取产品 ID
     */
     fun getProductId(context: Context):String{
        val productId = Settings.Secure.getString(context.contentResolver, "ProductId")
        return productId ?: ""
    }

    /**
     * 产品密钥
     */
     fun getProductKey(context: Context):String{
        val productId = Settings.Secure.getString(context.contentResolver, "ProductKey")
        return productId ?: ""
    }

    fun getDeviceFlavorType(): Boolean {
        return if (!BuildConfig.FLAVOR.contains("Beta")) {
            true
        } else {
            false
        }
    }

    fun getNoTypeTest(): String {
        return if (!BuildConfig.FLAVOR.contains("Beta")) {
            getLaiKuNoType()
        } else {
            "SN"
        }
    }

}
