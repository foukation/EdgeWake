package com.cmdc.ai.assist.api

import com.cmdc.ai.assist.AIAssistantManager
import com.cmdc.ai.assist.constraint.*
import com.cmdc.ai.assist.http.AIFoundationKitRequestApi
import com.cmdc.ai.assist.http.ApiConfig
import com.google.gson.JsonArray
import com.google.gson.JsonObject

/**
 * AI 功能的基础工具包。
 * 提供了一系列的基础方法和工具，用于支持 AI 相关操作和功能的实现。
 * 本身没有提供具体的 AI 功能，而是作为其他 AI 特性类的基础。
 */
class AIFoundationKit {

    /**
     * 获取流式文本处理器实例
     *
     * 用于创建一个新的流式文本处理器实例，该实例可以处理流式到达的文本数据，
     * 将文本按句子分割并提供实时处理能力。
     *
     * @return StreamTextProcessor 返回新的流式文本处理器实例
     */
    fun streamTextProcessorHelp(): StreamTextProcessor {
        return StreamTextProcessor()
    }

    /**
     * 获取当前语音识别实例。每次调用，返回不同实例。
     * 用于单次语音识别。
     *
     * 该函数返回当前已初始化的 `SpeechRecognition` 实例，用于进行语音识别操作。
     *
     * @return SpeechRecognition 返回当前语音识别实例。
     */
    fun speechRecognitionHelp(): SpeechRecognition {
        return SpeechRecognition()
    }

    /**
     * 获取当前语音识别实例。每次调用，返回不同实例。
     * 用于持续语音识别。
     *
     * 该函数返回当前已初始化的 `SpeechRecognitionPersistent` 实例，用于进行语音识别操作。
     *
     * @return SpeechRecognitionPersistent 返回当前语音识别实例。
     */
    fun speechRecognitionPersistentHelp(): SpeechRecognitionPersistent {
        return SpeechRecognitionPersistent()
    }

    fun speechRecognitionPersistentAliYunHelp(): SpeechRecognitionPersistentAliYun {
        return SpeechRecognitionPersistentAliYun()
    }

    /**
     * 此接口用于大模型闲聊 九天闲聊
     *
     * @param chatCompletionRequest 闲聊请求对象，包含闲聊所需的参数。
     * @param onSuccess 成功回调，返回闲聊响应结果。
     * @param onError 错误回调，返回错误信息。
     * @return 请求ID，可用于取消流式请求
     */
    fun largeModelCasualChat(
        chatCompletionRequest: ChatCompletionRequest,
        onSuccess: (ChatCompletionResponse) -> Unit,
        onError: (Exception) -> Unit
    ): String {
        return AIFoundationKitRequestApi.largeModelCasualChat(
            chatCompletionRequest,
            onSuccess,
            onError
        )
    }

    /**
     * 此接口用于大模型闲聊 chatbot 闲聊
     *
     * @param chatbotCompletionRequest 闲聊请求对象，包含闲聊所需的参数。
     * @param onSuccess 成功回调，返回闲聊响应结果。
     * @param onError 错误回调，返回错误信息。
     * @return 请求ID，可用于取消流式请求
     */
    fun largeModelChatbot(
        chatbotCompletionRequest: ChatbotCompletionRequest,
        onSuccess: (ChatbotCompletionResponse) -> Unit,
        onError: (Exception) -> Unit
    ): String {
        return AIFoundationKitRequestApi.largeModelChatbot(
            chatbotCompletionRequest,
            onSuccess,
            onError
        )
    }

    /**
     * 文本转音频。
     *
     * @param textToAudioRequest 文本转音频请求对象，包含转换所需的参数。
     * @param onSuccess 成功回调，返回音频转换响应结果。
     * @param onError 错误回调，返回错误信息。
     * @return 请求ID，可用于取消流式请求
     */
    fun textToAudio(
        textToAudioRequest: TextToAudioRequest,
        onSuccess: (TextToAudioResponse) -> Unit,
        onError: (Exception) -> Unit
    ): String {
        return AIFoundationKitRequestApi.textToAudio(textToAudioRequest, onSuccess, onError)
    }

    /**
     * 此接口用于设备端 NLU 及聊天等
     *
     * @param insideRcChatRequest 内部 RC 聊天请求对象，包含聊天所需的参数。
     * @param onSuccess 成功回调，返回聊天响应结果。
     * @param onError 错误回调，返回错误信息。
     * @return 请求ID，可用于取消流式请求
     */
    fun insideRcChat(
        insideRcChatRequest: InsideRcChatRequest,
        onSuccess: (DialogueResult) -> Unit,
        onError: (Exception) -> Unit
    ): String {

        val speechStateObject = JsonObject().apply {
            add("header", JsonObject().apply {
                addProperty("namespace", "ai.dueros.device_interface.voice_output")
                addProperty("name", "SpeechState")
            })
            add("payload", JsonObject().apply {
                addProperty(
                    "voiceId",
                    AIAssistantManager.getInstance().aiAssistConfig.dialogueTtsConfig.voiceId
                )
                addProperty("source", "baidu_tsn")
                add("baidu_tsn", JsonObject().apply {
                    addProperty(
                        "speed",
                        AIAssistantManager.getInstance().aiAssistConfig.dialogueTtsConfig.speed
                    )
                    addProperty(
                        "pitch",
                        AIAssistantManager.getInstance().aiAssistConfig.dialogueTtsConfig.pitch
                    )
                    addProperty(
                        "volume",
                        AIAssistantManager.getInstance().aiAssistConfig.dialogueTtsConfig.volume
                    )
                })
            })
        }
        insideRcChatRequest.client_context?.add(speechStateObject)
        insideRcChatRequest.rc_version =
            AIAssistantManager.getInstance().aiAssistConfig.centralConfigVersion.toInt()
        insideRcChatRequest.version =
            AIAssistantManager.getInstance().aiAssistConfig.centralConfigVersion

        return AIFoundationKitRequestApi.insideRcChat(insideRcChatRequest, onSuccess, onError)
    }

    /**
     * 此接口用于设备端请求进行 AI 画图
     *
     * @param textToAudioRequest 文本转图像请求对象，包含转换所需的参数。
     * @param onSuccess 成功回调，返回图像转换响应结果。
     * @param onError 错误回调，返回异常信息。
     */
    fun textToImage(
        textToAudioRequest: ImageGenerationRequest,
        onSuccess: (ImageGenerationResponse) -> Unit,
        onError: (Exception) -> Unit
    ) {
        return AIFoundationKitRequestApi.textToImage(textToAudioRequest, onSuccess, onError)
    }

    /**
     * 此接口用于通用物体及场景识别，即对于输入的一张图片（可正常解码，且长宽比适宜），输出图片中的多个物体及场景标签。
     *
     * @param imageRecognitionRequest 图像分类请求对象，包含分类所需的参数。
     * @param onSuccess 成功回调，返回图像分类响应结果。
     * @param onError 错误回调，返回错误信息。
     */
    fun imageClassify(
        imageRecognitionRequest: ImageRecognitionRequest,
        onSuccess: (ImageRecognitionResponse) -> Unit,
        onError: (Exception) -> Unit
    ) {
        return AIFoundationKitRequestApi.imageClassify(imageRecognitionRequest, onSuccess, onError)
    }

    /**
     * 语音文件识别。
     * 根据音频 url、音频格式、语言 id 以及采样率等参数创建音频转写任务。
     *
     * @param speechRecognitionRequest 语音识别请求对象，包含识别所需的参数。
     * @param onSuccess 成功回调，返回语音识别响应结果。
     * @param onError 错误回调，返回错误信息。
     */
    fun speechFileTransfer(
        speechRecognitionRequest: SpeechRecognitionRequest,
        onSuccess: (SpeechRecognitionResponse) -> Unit,
        onError: (Exception) -> Unit
    ) {
        return AIFoundationKitRequestApi.speechFileTransfer(
            speechRecognitionRequest,
            onSuccess,
            onError
        )
    }

    /**
     * 语音文件识别结果查询。
     * 根据 speechFileTransfer 接口返回的 task_id 的数组批量查询音频转写任务结果。
     *
     * @param speechRecognitionRequest 语音识别查询请求对象，包含查询所需的参数。
     * @param onSuccess 成功回调，返回语音识别查询响应结果。
     * @param onError 错误回调，返回错误信息。
     */
    fun speechFileTransferQuery(
        speechRecognitionRequest: SpeechRecognitionQueryRequest,
        onSuccess: (SpeechRecognitionQueryResponse) -> Unit,
        onError: (Exception) -> Unit
    ) {
        return AIFoundationKitRequestApi.speechFileTransferQuery(
            speechRecognitionRequest,
            onSuccess,
            onError
        )
    }

    /**
     * 获取音频文件的上传 URL、访问 URL。此接口默认使用中国移动，移动云服务，如果厂商有自己的云文件存储，可不使用此接口。
     *
     * @param uploadAudioUrlRequest 上传音频 URL 请求对象，包含请求所需的参数。
     * @param onSuccess 成功回调，返回上传音频 URL 响应结果。
     * @param onError 错误回调，返回错误信息。
     */
    fun getUploadAudioUrl(
        uploadAudioUrlRequest: UploadAudioUrlRequest,
        onSuccess: (UploadAudioUrlResponse) -> Unit,
        onError: (Exception) -> Unit
    ) {
        return AIFoundationKitRequestApi.getUploadAudioUrl(
            uploadAudioUrlRequest,
            onSuccess,
            onError
        )
    }

    /**
     * 图像翻译。
     *
     * @param uploadAudioUrlRequest 图像翻译请求对象，包含翻译所需的参数。
     * @param onSuccess 成功回调，返回图像翻译响应结果。
     * @param onError 错误回调，返回错误信息。
     */
    fun imageTranslate(
        uploadAudioUrlRequest: ImageTranslationRequest,
        onSuccess: (ImageTranslationResponse) -> Unit,
        onError: (Exception) -> Unit
    ) {
        return AIFoundationKitRequestApi.imageTranslate(uploadAudioUrlRequest, onSuccess, onError)
    }

    /**
     * 光学字符识别（OCR）功能。
     *
     * @param ocrRequest OCR 请求对象，包含 OCR 识别所需的参数，例如图片数据、语言类型等。
     * @param onSuccess 成功回调函数，当 OCR 识别成功时执行，返回 OCR 响应结果。
     * @param onError 错误回调函数，当 OCR 识别失败时执行，返回错误信息字符串。
     */
    fun ocr(
        ocrRequest: OcrRequest,
        onSuccess: (OcrResponse) -> Unit,
        onError: (Exception) -> Unit
    ) {
        return AIFoundationKitRequestApi.ocr(ocrRequest, onSuccess, onError)
    }

    /**
     * 查询单词或汉字的相关信息
     *
     * @param chineseCharacterRequest 包含查询信息的请求对象，用于指定查询的汉字或单词
     * @param onSuccess 成功回调函数，当查询成功时调用，传递一个包含查询结果的响应对象
     * @param onError 失败回调函数，当查询失败时调用，传递一个包含错误信息的字符串
     */
    fun wordQuery(
        chineseCharacterRequest: ChineseCharacterRequest,
        onSuccess: (ChineseCharacterResponse) -> Unit,
        onError: (Exception) -> Unit
    ) {
        return AIFoundationKitRequestApi.wordQuery(chineseCharacterRequest, onSuccess, onError)
    }

    /**
     * 内容摘要功能
     *
     * 该函数用于处理内容摘要请求
     * 它接收一个 ContentSummaryRequest 对象作为请求参数，通过 onSuccess 回调函数返回处理结果，
     * 如果发生错误，则通过 onError 回调函数返回错误信息
     *
     * @param contentSummaryRequest 包含了请求内容摘要所需的所有信息的对象
     * @param onSuccess 成功回调函数，当内容摘要请求成功时调用，传递一个ContentSummaryResponse对象给调用者
     * @param onError 错误回调函数，当内容摘要请求失败时调用，传递一个包含错误信息的字符串给调用者
     * @return 请求ID，可用于取消流式请求
     */
    fun contentSummary(
        contentSummaryRequest: ContentSummaryRequest,
        onSuccess: (ContentSummaryResponse) -> Unit,
        onError: (Exception) -> Unit
    ): String {
        return AIFoundationKitRequestApi.contentSummary(contentSummaryRequest, onSuccess, onError)
    }

    /**
     * 文本翻译功能(机器翻译)
     *
     * 调用翻译API将指定文本从源语言翻译为目标语言
     *
     * @param contentSummaryRequest 翻译请求参数，包含目标语言、源文本和源语言代码
     * @param onSuccess 翻译成功回调函数，参数为翻译响应结果[TranslateResponse]
     * @param onError 翻译失败回调函数，参数为错误信息字符串
     */
    fun textTranslate(
        contentSummaryRequest: TranslationRequest,
        onSuccess: (TranslateResponse) -> Unit,
        onError: (Exception) -> Unit
    ) {
        return AIFoundationKitRequestApi.textTranslate(contentSummaryRequest, onSuccess, onError)
    }

    /**
     * 文本翻译功能(模型翻译)
     *
     * 调用翻译API将指定文本从源语言翻译为目标语言
     *
     * @param contentSummaryRequest 翻译请求参数，包含目标语言、源文本和源语言代码
     * @param onSuccess 翻译成功回调函数，参数为翻译响应结果[TranslateResponse]
     * @param onError 翻译失败回调函数，参数为错误信息字符串
     */
    fun textTranslateWithModel(
        contentSummaryRequest: TranslationRequest,
        onSuccess: (TranslateResponse) -> Unit,
        onError: (Exception) -> Unit
    ) {
        return AIFoundationKitRequestApi.textTranslate(
            contentSummaryRequest, onSuccess, onError,
            ApiConfig.TEXT_TRANSLATE_ALI_YUN_API
        )
    }

    /**
     * 取消流式请求
     *
     * @param requestId 流式请求ID，由流式接口返回
     * @return 是否成功取消
     */
    fun cancelStreamRequest(requestId: String): Boolean {
        return AIFoundationKitRequestApi.cancelStreamRequest(requestId)
    }

}