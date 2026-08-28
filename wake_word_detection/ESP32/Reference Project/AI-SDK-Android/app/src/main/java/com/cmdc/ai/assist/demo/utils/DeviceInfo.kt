package com.cmdc.ai.assist.demo.utils

import android.content.Context
import android.provider.Settings
import com.cmdc.ai.assist.AIAssistantManager
import com.cmdc.ai.assist.api.GateWay
import com.cmdc.ai.assist.constraint.AIAssistConfig
import com.cmdc.ai.assist.constraint.DeviceInfoResponse
import timber.log.Timber

class DeviceInfo(
    val deviceNoType: String,
    val deviceNo: String,
    val productId: String,
    val productKey: String
) {

    private val TAG = "DeviceInfo"

    companion object {
        @JvmStatic
        fun getDeviceNoType(context: Context): String? {
            return Settings.Secure.getString(context.contentResolver, "DeviceNoType")
        }

        @JvmStatic
        fun getDeviceNo(context: Context): String? {
            return Settings.Secure.getString(context.contentResolver, "DeviceNo")
        }

        @JvmStatic
        fun getProductId(context: Context): String? {
            return Settings.Secure.getString(context.contentResolver, "ProductId")
        }

        @JvmStatic
        fun getProductKey(context: Context): String? {
            return Settings.Secure.getString(context.contentResolver, "ProductKey")
        }
    }

    private val gateWay by lazy {
        GateWay()
    }

    fun obtainDeviceInformation(context: Context, deviceInfoCallback: DeviceInfoCallback) {

        val config = AIAssistConfig.Builder()
            .setProductId(productId)
            .setProductKey(productKey)
            .setDeviceNo(deviceNo)
            .setDeviceNoType(deviceNoType) // 传 SN 或 IMEI
            .build()

        /*if (config.isValid()) {
            // 使用配置初始化
            AIAssistantManager.initialize(context, config)
        }*/

        AIAssistantManager.initialize(context, config)

        gateWay.obtainDeviceInformation({ response ->
            Timber.tag(TAG).d("%s%s", "response: ", response)
            deviceInfoCallback.onSuccess(response)
        }, { error ->
            Timber.tag(TAG).e("%s%s", "error: ", error)
            deviceInfoCallback.onError(error)
        })
    }

    interface DeviceInfoCallback {
        fun onSuccess(deviceInfo: DeviceInfoResponse)
        fun onError(error: String)
    }
}