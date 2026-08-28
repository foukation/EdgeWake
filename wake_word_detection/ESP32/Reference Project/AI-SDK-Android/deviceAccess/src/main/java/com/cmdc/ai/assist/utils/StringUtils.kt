package com.cmdc.ai.assist.utils

import androidx.annotation.RestrictTo
import java.security.MessageDigest

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal object StringUtils {

    /**
     * 字符串的 MD5 扩展函数
     */
    fun String.md5(): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(this.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * 计算签名
     */
    fun calculateSign(deviceSecret: String, ts: String): String {
        return (deviceSecret + ts).md5()
    }

}