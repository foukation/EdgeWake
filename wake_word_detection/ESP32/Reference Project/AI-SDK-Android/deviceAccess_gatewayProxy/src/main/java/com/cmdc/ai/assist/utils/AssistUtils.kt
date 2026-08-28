package com.cmdc.ai.assist.utils

import android.net.Uri
import android.os.Build
import androidx.annotation.RestrictTo
import com.cmdc.ai.assist.AIAssistantManager
import java.util.UUID

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object AssistUtils {

    fun wssParameter(uri: String): String {

        val sn = UUID.randomUUID().toString()
        val deviceNo = AIAssistantManager.getInstance().aiAssistConfig.deviceNo
        val productKey =
            AIAssistantManager.getInstance().aiAssistConfig.productKey
        val productId =
            AIAssistantManager.getInstance().aiAssistConfig.productId
        val timestamp = System.currentTimeMillis()
        val sign = signMd5(timestamp)
        val deviceId =
            AIAssistantManager.getInstance().aiAssistConfig.deviceId
        val clientID = AIAssistantManager.getInstance().aiAssistConfig.clientID
        val modelName = Build.MODEL
        /*val modelName = URLEncoder.encode(Build.MODEL, "UTF-8") // URL编码*/
        val packageName = AIAssistantManager.getInstance().aiAssistConfig.packageName

        val uriBuilder = Uri.parse(uri).buildUpon()
            .appendQueryParameter("sn", sn)
            .appendQueryParameter("deviceNo", deviceNo)
            .appendQueryParameter("productKey", productKey)
            .appendQueryParameter("productId", productId)
            .appendQueryParameter("ts", timestamp.toString())
            .appendQueryParameter("sign", sign)
            .appendQueryParameter("deviceId", deviceId)
            .appendQueryParameter("client-id", clientID)
            .appendQueryParameter("modelName", modelName)
            .appendQueryParameter("packageName", packageName)
            .appendQueryParameter(
                "deviceIdentifier",
                AIAssistantManager.getInstance().aiAssistConfig.deviceIdentifier
            )

        AIAssistantManager.getInstance().aiAssistConfig.sn = sn

        val ws_url = uriBuilder.build().toString()

        return ws_url

    }

    fun signMd5(timestamp: Long): String {
        val sign = StringUtils.calculateSign(
            AIAssistantManager.getInstance().aiAssistConfig.deviceSecret,
            timestamp.toString()
        )
        return sign
    }

    fun timestamp(): Long {
        return System.currentTimeMillis()
    }

}