package com.cmdc.ai.assist.utils

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

        val parameter =
            "?sn=$sn&deviceNo=$deviceNo&productKey=$productKey&productId=$productId&ts=$timestamp&sign=$sign&deviceId=$deviceId"

        AIAssistantManager.getInstance().aiAssistConfig.sn = sn

        val ws_url = uri + parameter

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