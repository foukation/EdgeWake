package com.ai.multimodal.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import java.io.ByteArrayOutputStream

object ImageUtils {
    /**
     * 将图片URI转换为Base64编码字符串
     *
     * @param context 上下文
     * @param imageUri 图片URI
     * @return 包含图片信息的Map，包括base64字符串、宽度、高度和图片类型
     */
    @JvmStatic
    fun convertImageUriToBase64(context: Context, imageUri: Uri): Map<String, Any> {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(imageUri) ?: "image/jpeg"

        // 从MIME类型提取图片格式
        val imageType = when {
            mimeType.endsWith("/png") -> "png"
            mimeType.endsWith("/webp") -> "webp"
            mimeType.endsWith("/gif") -> "gif" // 注意：如果API支持GIF
            else -> "jpeg"
        }

        // 读取图片并转为Base64
        val bitmap = MediaStore.Images.Media.getBitmap(
            context.applicationContext.contentResolver,
            imageUri
        )
        val imageWidth = bitmap.width
        val imageHeight = bitmap.height

        // 将Bitmap转换为Base64字符串
        val byteArrayOutputStream = ByteArrayOutputStream()
        val compressFormat = when (imageType) {
            "png" -> Bitmap.CompressFormat.PNG
            "webp" -> Bitmap.CompressFormat.WEBP
            else -> Bitmap.CompressFormat.JPEG
        }
        bitmap.compress(compressFormat, 90, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        val imageBase64 = Base64.encodeToString(byteArray, Base64.NO_WRAP)

        return mapOf(
            "base64" to imageBase64,
            "width" to imageWidth,
            "height" to imageHeight,
            "format" to imageType
        )
    }

    /**
     * 简化版本，只返回Base64字符串
     */
    @JvmStatic
    fun getBase64FromImageUri(context: Context, imageUri: Uri): String {
        return convertImageUriToBase64(context, imageUri)["base64"] as String
    }
}