package com.ai.multimodal.domain

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import com.ai.multimodal.http.MultimodalKit
import com.ai.multimodal.model.request.ImageAssistantRequest
import com.ai.multimodal.model.request.ImageData
import com.ai.multimodal.model.request.MedicineRequest
import com.ai.multimodal.model.request.PromptRequest
import com.ai.multimodal.model.response.ImageAssistantResponse
import com.ai.multimodal.model.response.MedicineResponse
import com.ai.multimodal.model.response.PromptResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

/**
 * 药品信息服务类
 *
 * 封装了药品信息提取的业务逻辑，处理药品图像识别和信息解析等功能
 */
class DrugInformationService {

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val multimodalKit = MultimodalKit()

    /**
     * 从本地图像中提取药品信息
     *
     * @param imagePath 本地图片文件路径
     * @param query 用户查询/指令内容，可选参数，默认为"请识别图片中的药品信息"
     * @param enableStream 是否启用流式响应，默认为false
     * @param onSuccess 成功回调函数
     * @param onError 错误回调函数
     */
    fun extractDrugInformation(
        context: Context,
        imageUri: Uri,
        query: String = "",
        enableStream: Boolean = false,
        onSuccess: (ImageAssistantResponse) -> Unit,
        onError: (String) -> Unit
    ) {

        imageInformationExtraction(
            context,
            imageUri,
            query,
            enableStream,
            "med_extract",
            onSuccess,
            onError
        )

    }

    /**
     * 菜单翻译
     *
     * @param imagePath 本地图片文件路径
     * @param query 用户查询/指令内容，可选参数，默认为"请识别图片中的药品信息"
     * @param enableStream 是否启用流式响应，默认为false
     * @param onSuccess 成功回调函数
     * @param onError 错误回调函数
     */
    fun menuTranslation(
        context: Context,
        imageUri: Uri,
        query: String = "",
        enableStream: Boolean = false,
        onSuccess: (ImageAssistantResponse) -> Unit,
        onError: (String) -> Unit
    ) {

        imageInformationExtraction(
            context,
            imageUri,
            query,
            enableStream,
            "menu_translate",
            onSuccess,
            onError
        )

    }

    /**
     * 从本地图像中提取药品信息
     *
     * @param imagePath 本地图片文件路径
     * @param query 用户查询/指令内容，可选参数，默认为"请识别图片中的药品信息"
     * @param enableStream 是否启用流式响应，默认为false
     * @param onSuccess 成功回调函数
     * @param onError 错误回调函数
     */
    private fun imageInformationExtraction(
        context: Context,
        imageUri: Uri,
        query: String = "",
        enableStream: Boolean = false,
        taskType: String,
        onSuccess: (ImageAssistantResponse) -> Unit,
        onError: (String) -> Unit
    ) {

        coroutineScope.launch {

            try {

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

                // 创建图像数据对象
                val imageData = ImageData(
                    type = imageType,
                    width = imageWidth,
                    height = imageHeight,
                    img_encoded = imageBase64
                )

                // 创建请求对象
                val request = ImageAssistantRequest(
                    task_type = taskType,
                    stream = enableStream,
                    query = query,
                    image = imageData
                )

                // 调用MultimodalKit进行药品信息提取
                multimodalKit.imageInformationExtraction(request, onSuccess, onError)

            } catch (e: Exception) {
                onError("处理图片时发生错误: ${e.message}")
            }

        }

    }
    /**
     * 该函数用于处理问答请求，根据输入的文本和系统提示生成回答，并通过回调函数返回结果或错误信息。
     *
     * @param text 用户输入的文本，作为问答的查询内容。
     * @param systemPrompt 系统提示信息，通常用于提供上下文或额外的信息。
     * @param onSuccess 成功回调函数，当问答请求成功时调用，返回[PromptResponse]对象。
     * @param onError 错误回调函数，当问答请求失败时调用，返回错误信息字符串。
     */
    fun questionAnswer(
        text: String,
        systemPrompt: String,
        onSuccess: (PromptResponse) -> Unit,
        onError: (String) -> Unit
    ) {

        coroutineScope.launch {

            try {

                // 创建请求对象
                val request = PromptRequest(
                    query = text,
                    medicine_info = systemPrompt,
                )

                multimodalKit.questionAnswer(request, onSuccess, onError)

            } catch (e: Exception) {
                onError("问答发生错误: ${e.message}")
            }

        }

    }

    /**
     * 用药提醒函数，用于根据输入的文本生成用药提醒，并通过回调函数返回结果或错误信息。
     *
     * @param text 用户输入的文本，用于生成用药提醒。
     * @param onSuccess 成功回调函数，接收一个 [MedicineResponse] 对象作为参数，表示用药提醒的成功结果。
     * @param onError 错误回调函数，接收一个字符串作为参数，表示用药提醒过程中发生的错误信息。
     */
    fun medicationReminder(
        text: String,
        onSuccess: (MedicineResponse) -> Unit,
        onError: (String) -> Unit
    ) {

        coroutineScope.launch {

            try {

                // 创建请求对象
                val request = MedicineRequest(
                    medicine_info = text,
                )

                multimodalKit.medicationReminder(request, onSuccess, onError)

            } catch (e: Exception) {
                onError("用药提醒发生错误: ${e.message}")
            }

        }

    }

}