package com.fxzs.lingxiagent.helper

import java.security.Key
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec

object AESHelper {

    private val AES_KEY = "v9jsd8aJ2rS5x2P9v3yRZl7DqzG3e2fX".toByteArray()

    /*
    * AES-CTR 加密
    * */
    private fun encrypt(data: ByteArray): String {
        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        val keySpec: Key = javax.crypto.spec.SecretKeySpec(AES_KEY, "AES")

        val iv = ByteArray(16)
        val ivSpec = IvParameterSpec(iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
        return String(cipher.doFinal(data))
    }

    /*
    * AES-CTR 解密
    * */
    fun decrypt(data: String): String {
        val encryptedData = Base64.getDecoder().decode(data)
        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        val keySpec: Key = javax.crypto.spec.SecretKeySpec(AES_KEY, "AES")

        val iv = ByteArray(16)
        val ivSpec = IvParameterSpec(iv)

        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
        return String(cipher.doFinal(encryptedData))
    }

}