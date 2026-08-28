package com.cmdc.ai.assist.http

import android.os.Build
import com.cmdc.ai.assist.AIAssistantManager
import com.cmdc.ai.assist.api.AISessionManager
import com.cmdc.ai.assist.constraint.*
import com.cmdc.ai.assist.http.error.BizErrorParser
import com.cmdc.ai.assist.utils.AssistUtils
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Headers
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

object AIFoundationKitRequestApi {

    private const val CHATBOT_IMAGE_MODEL = "jiutian-image"

    private val TAG = AIFoundationKitRequestApi::class.simpleName.toString()
    private val gson: Gson = GsonBuilder().create()
    private val mOkHttpManager = OkHttpManager()

    // 存储活跃的EventSource
    private val activeEventSources = ConcurrentHashMap<String, EventSource>()

    /**
     * 取消流式请求
     */
    fun cancelStreamRequest(requestId: String): Boolean {
        val eventSource = activeEventSources.remove(requestId)
        return if (eventSource != null) {
            eventSource.cancel()
            true
        } else {
            false
        }
    }

    /**
     * 生成唯一请求ID
     */
    private fun generateRequestId(apiType: String): String {
        return "${apiType}_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }

    private fun createAIServerHeaders(): Headers {
        val timestamp = AssistUtils.timestamp()
        return Headers.Builder()
            .add("Content-Type", "application/json; charset=utf-8")
            .apply {
                add("sign", AssistUtils.signMd5(timestamp))
                add("deviceNo", AIAssistantManager.getInstance().aiAssistConfig.deviceNo)
                add("productId", AIAssistantManager.getInstance().aiAssistConfig.productId)
                add("productKey", AIAssistantManager.getInstance().aiAssistConfig.productKey)
                add("ts", timestamp.toString())
                add("deviceId", AIAssistantManager.getInstance().aiAssistConfig.deviceId)
                add("client-id", AIAssistantManager.getInstance().aiAssistConfig.clientID)
                add("modelName", Build.MODEL)
                /*add("ip", "192.168.1.1")*/
                add("packageName", AIAssistantManager.getInstance().aiAssistConfig.packageName)
                    .add("deviceIdentifier", AIAssistantManager.getInstance().aiAssistConfig.deviceIdentifier)
            }
            .build()
    }

    private fun createChatbotServerHeaders(authToken: String): Headers {
        val headersBuilder = createAIServerHeaders().newBuilder()
        headersBuilder.add(
            "authorization",
            "Bearer $authToken"
        )
        return headersBuilder.build()
    }

    /*
     * 大模型闲聊 九天闲聊
     */
    internal fun largeModelCasualChat(
        chatCompletionRequest: ChatCompletionRequest,
        onSuccess: (ChatCompletionResponse) -> Unit,
        onError: (Exception) -> Unit
    ): String {

        val requestId = generateRequestId("chat_completion")
        val jsonResult = gson.toJson(chatCompletionRequest)

        if (chatCompletionRequest.stream) {

            val eventSource = mOkHttpManager.postStream(
                ApiConfig.CHAT_COMPLETIONS_API,
                header = createAIServerHeaders(),
                params = jsonResult,
                callback = object : EventSourceListener() {
                    override fun onEvent(
                        eventSource: EventSource,
                        id: String?,
                        type: String?,
                        data: String
                    ) {
                        // 这里直接获取数据，不需要处理"data:"前缀
                        if (data == "[DONE]") {
                            activeEventSources.remove(requestId)
                            return
                        }

                        try {

                            val result = data
                            Timber.tag(TAG).d("response : $result")

                            val jsonString = result.let {
                                if (it.startsWith("data:")) {
                                    result.substring(5).trim()
                                } else {
                                    result.trim()
                                }
                            }

                            val resp =
                                gson.fromJson(jsonString, ChatCompletionResponse::class.java)

                            onSuccess(resp)
                        } catch (e: Exception) {
                            activeEventSources.remove(requestId)
                            onError(e)
                            Timber.tag(TAG).e(e, "大模型闲聊流式请求失败")
                        }

                    }

                    override fun onFailure(
                        eventSource: EventSource,
                        t: Throwable?,
                        response: Response?
                    ) {
                        activeEventSources.remove(requestId)

                        val bizException = BizErrorParser.parse(response, t)
                        if (bizException != null) {
                            onError(bizException)
                            Timber.tag(TAG).e("请求业务错误: code=${bizException.code}, msg=${bizException.msg}")
                        } else {
                            onError(Exception(t))
                            Timber.tag(TAG).e(t, "流式网络请求失败")
                        }

                        /*onError(Exception(t))
                        Timber.tag(TAG).e(t, "流式网络请求失败")*/
                    }
                }
            )

            activeEventSources[requestId] = eventSource
            return requestId
        }

        mOkHttpManager.post(
            ApiConfig.CHAT_COMPLETIONS_API,
            header = createAIServerHeaders(),
            params = jsonResult,
            callback = object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    try {

                        val result = response.body?.string()
                        Timber.tag(TAG).d("response : $result")

                        BizErrorParser.parseText(result)?.let { biz ->
                            onError(biz)
                            Timber.tag(TAG).e("请求业务错误: code=${biz.code}, msg=${biz.msg}")
                            return
                        }

                        val jsonString = result?.let {
                            if (it.startsWith("data:")) {
                                result.substring(5).trim()
                            } else {
                                result.trim()
                            }
                        }

                        val resp =
                            gson.fromJson(jsonString, ChatCompletionResponse::class.java)

                        onSuccess(resp)
                    } catch (e: Exception) {
                        onError(e)
                        Timber.tag(TAG).e(e, "请求处理失败")
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    onError(e)
                    Timber.tag(TAG).e(e, "网络请求失败")
                }
            })

        return requestId // 非流式请求也返回ID，虽然不能取消
    }

    /*
     * 大模型闲聊 Chatbot
     */
    internal fun largeModelChatbot(
        chatbotCompletionRequest: ChatbotCompletionRequest,
        onSuccess: (ChatbotCompletionResponse) -> Unit,
        onError: (Exception) -> Unit
    ): String {

        val requestId = generateRequestId("chatbot")
        val config = AIAssistantManager.getInstance().aiAssistConfig

        /*
         * 图片模型必须使用独立的 imageToken。配置缺失时通过现有错误回调返回，
         * 不抛出异常、不发起网络请求，也不回退使用文本模型 token。
         */
        val authToken = if (chatbotCompletionRequest.model == CHATBOT_IMAGE_MODEL) {
            if (config.imageToken.isBlank()) {
                onError(IllegalStateException("Chatbot imageToken is not configured"))
                return requestId
            }
            config.imageToken
        } else {
            config.token
        }

        val chatbotHeaders = createChatbotServerHeaders(authToken)
        val jsonResult = gson.toJson(chatbotCompletionRequest)

        if (chatbotCompletionRequest.stream) {

            val eventSource = mOkHttpManager.postStream(
                ApiConfig.CHAT_BOT_COMPLETIONS_API,
                header = chatbotHeaders,
                params = jsonResult,
                callback = object : EventSourceListener() {
                    override fun onEvent(
                        eventSource: EventSource,
                        id: String?,
                        type: String?,
                        data: String
                    ) {
                        // 这里直接获取数据，不需要处理"data:"前缀
                        if (data == "[DONE]") {
                            activeEventSources.remove(requestId)
                            return
                        }

                        try {

                            val result = data
                            Timber.tag(TAG).d("response : $result")

                            val jsonString = result.let {
                                if (it.startsWith("data:")) {
                                    result.substring(5).trim()
                                } else {
                                    result.trim()
                                }
                            }

                            val resp =
                                gson.fromJson(jsonString, ChatbotCompletionResponse::class.java)

                            onSuccess(resp)
                        } catch (e: Exception) {
                            activeEventSources.remove(requestId)
                            onError(e)
                            Timber.tag(TAG).e(e.message.toString())
                        }

                    }

                    override fun onFailure(
                        eventSource: EventSource,
                        t: Throwable?,
                        response: Response?
                    ) {
                        activeEventSources.remove(requestId)

                        // 优先解析服务端返回的业务错误（如权益/计费失败）。
                        // SSE 在握手阶段如果收到 Content-Type: application/json，
                        // 会抛 IllegalStateException("Invalid content-type: ...") 触发此回调，
                        // 业务错误体（{"code":6xxx,"msg":...}）就在 response.body 中。
                        // 命中业务码段位时上抛 BizException，否则回退到原网络错误。
                        val bizException = BizErrorParser.parse(response, t)
                        if (bizException != null) {
                            onError(bizException)
                            Timber.tag(TAG).e("请求业务错误: code=${bizException.code}, msg=${bizException.msg}")
                        } else {
                            onError(Exception(t))
                            Timber.tag(TAG).e(t, "流式网络请求失败")
                        }

                        /*onError(Exception(t))
                        Timber.tag(TAG).e(t, "流式网络请求失败")*/
                    }
                }
            )

            activeEventSources[requestId] = eventSource
            return requestId
        }

        mOkHttpManager.post(
            ApiConfig.CHAT_BOT_COMPLETIONS_API,
            header = chatbotHeaders,
            params = jsonResult,
            callback = object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    try {

                        val result = response.body?.string()
                        Timber.tag(TAG).d("response : $result")

                        // 权益模块业务错误兜底（HTTP 200 + body 内 code != 0 的场景）。
                        // 背景：服务端约定 code==0 为成功，其他非零（如 6409 NO_BIND_PHONE 未绑定手机、
                        //      套餐用尽等）均为业务错误，且这类错误通常仍以 HTTP 200 下发，
                        //      其响应体里的 `data` 字段为对象结构（与成功体里的 List<Directive> 不兼容），
                        //      若直接交给 Gson 反序列化 InsideRcChatResponse 会抛
                        //      JsonSyntaxException: Expected BEGIN_ARRAY but was BEGIN_OBJECT at $.data，
                        //      导致上层只能识别为"网络/解析异常"，丢失业务码无法分流。
                        // 处理：在进入 Gson 解析前，先用 BizErrorParser 识别一刀；
                        //      命中即上抛 BizException（携带 code/msg/rawJson），由上层根据 code 走业务弹窗等流程。
                        BizErrorParser.parseText(result)?.let { biz ->
                            onError(biz)
                            Timber.tag(TAG).e("请求业务错误: code=${biz.code}, msg=${biz.msg}")
                            return
                        }

                        val jsonString = result?.let {
                            if (it.startsWith("data:")) {
                                result.substring(5).trim()
                            } else {
                                result.trim()
                            }
                        }

                        val resp =
                            gson.fromJson(jsonString, ChatbotCompletionResponse::class.java)

                        onSuccess(resp)
                    } catch (e: Exception) {
                        onError(e)
                        Timber.tag(TAG).e(e, "请求处理失败")
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    onError(e)
                    Timber.tag(TAG).e(e, "网络请求失败")
                }
            })

        return requestId // 非流式请求也返回ID，虽然不能取消
    }

    /*
     * 设备端文字转语音
     */
    internal fun textToAudio(
        textToAudioRequest: TextToAudioRequest,
        onSuccess: (TextToAudioResponse) -> Unit,
        onError: (Exception) -> Unit
    ): String {

        val requestId = generateRequestId("tts")
        val jsonResult = gson.toJson(textToAudioRequest)
        // 3为mp3格式(默认)；* 4为pcm-16k；* 5为pcm-8k；* 6为wav（内容同pcm-16k）
        val aue = textToAudioRequest.aue

        if (textToAudioRequest.streaming) {
            // 不支持
            val eventSource = mOkHttpManager.postStream(
                ApiConfig.TEXT_TO_AUDIO_API,
                header = createAIServerHeaders(),
                params = jsonResult,
                callback = object : EventSourceListener() {
                    override fun onEvent(
                        eventSource: EventSource,
                        id: String?,
                        type: String?,
                        data: String
                    ) {

                        try {

                            val result = data
                            Timber.tag(TAG).d("response : $result")

                            onSuccess(TextToAudioResponse())
                        } catch (e: Exception) {
                            activeEventSources.remove(requestId)
                            onError(e)
                            Timber.tag(TAG).e(e.message.toString())
                        }

                    }

                    override fun onFailure(
                        eventSource: EventSource,
                        t: Throwable?,
                        response: Response?
                    ) {
                        activeEventSources.remove(requestId)

                        val bizException = BizErrorParser.parse(response, t)
                        if (bizException != null) {
                            onError(bizException)
                            Timber.tag(TAG).e("请求业务错误: code=${bizException.code}, msg=${bizException.msg}")
                        } else {
                            onError(Exception(t))
                            Timber.tag(TAG).e(t, "流式网络请求失败")
                        }

                        /*onError(Exception(t))
                        Timber.tag(TAG).e(t, "流式网络请求失败")*/
                    }
                }
            )

            activeEventSources[requestId] = eventSource
            return requestId
        }
        mOkHttpManager.post(
            ApiConfig.TEXT_TO_AUDIO_API,
            header = createAIServerHeaders(),
            params = jsonResult,
            callback = object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    try {

                        val responseBody = response.body
                        // audio/mpeg、audio/pcm、audio/wav
                        val contentType = responseBody?.contentType().toString()
                        var result: String? = null

                        if (contentType == "application/json") {
                            result = responseBody?.string()

                            BizErrorParser.parseText(result)?.let { biz ->
                                onError(biz)
                                Timber.tag(TAG).e("请求业务错误: code=${biz.code}, msg=${biz.msg}")
                                return
                            }

                            val resp = gson.fromJson(result, TextToAudioResponse::class.java)
                            onSuccess(resp)
                            return
                        }

                        var tempFile: File? = null
                        if (contentType == "audio/mpeg" == true && aue == 3) {
                            tempFile = File.createTempFile("audio", ".mp3")
                        }
                        if (contentType == "audio/pcm" == true && (aue == 4 || aue == 5)) {
                            tempFile = File.createTempFile("audio", ".pcm")
                        }
                        if (contentType == "audio/wav" == true && aue == 6) {
                            tempFile = File.createTempFile("audio", ".wav")
                        }

                        val audioBytes = responseBody?.bytes()
                        audioBytes?.let { tempFile?.writeBytes(it) }

                        Timber.tag(TAG).d("response : ${tempFile?.absolutePath}")


                        val resp = TextToAudioResponse(
                            code = ResCode.SUC.alias,
                            data = tempFile,
                            msg = "成功"
                        )

                        onSuccess(resp)
                    } catch (e: Exception) {
                        onError(e)
                        Timber.tag(TAG).e(e, "请求处理失败")
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    onError(e)
                    Timber.tag(TAG).e(e, "网络请求失败")
                }
            })

        return requestId // 非流式请求也返回ID，虽然不能取消
    }

    /*
     * Inside-Rc中控接口
     * 此接口用于设备端NLU及聊天等
     */
    internal fun insideRcChat(
        insideRcChatRequest: InsideRcChatRequest,
        onSuccess: (DialogueResult) -> Unit,
        onError: (Exception) -> Unit
    ): String {

        val requestId = generateRequestId("rc_chat")
        val jsonResult = gson.toJson(insideRcChatRequest)

        if (insideRcChatRequest.stream) {

            val eventSource = mOkHttpManager.postStream(
                ApiConfig.INSIDE_RC_CHAT_API,
                header = createAIServerHeaders(),
                params = jsonResult,
                callback = object : EventSourceListener() {
                    override fun onEvent(
                        eventSource: EventSource,
                        id: String?,
                        type: String?,
                        data: String
                    ) {
                        // 这里直接获取数据，不需要处理"data:"前缀
                        if (data == "[DONE]") {
                            activeEventSources.remove(requestId)
                            return
                        }

                        try {

                            val result = data
                            Timber.tag(TAG).d("response : $result")

                            val jsonString = result.let {
                                if (it.startsWith("data:")) {
                                    result.substring(5).trim()
                                } else {
                                    result.trim()
                                }
                            }

                            val resp =
                                gson.fromJson(jsonString, InsideRcChatResponse::class.java)

                            var content = ""
                            if (resp.is_end == 1) {
                                val assistant_answer = JSONObject(resp.assistant_answer)
                                content = assistant_answer.optString("content")
                                updateChatData(
                                    insideRcChatRequest.messages,
                                    assistant_answer.toString()
                                )
                            }
                            val _data = resp.data

                            _data?.forEach { directive ->
                                var header: JSONObject? = null
                                var payload: JSONObject? = null
                                if (data.isNotEmpty())
                                    header = JSONObject(Gson().toJson(directive.header))
                                /*header = JSONObject(data[0].header)*/
                                if (data.isNotEmpty())
                                    payload = JSONObject(Gson().toJson(directive.payload))
                                /*payload = JSONObject(data[0].payload.toString())*/

                                onSuccess(
                                    DialogueResult(
                                        qid = resp.qid,
                                        assistant_answer_content = "",
                                        header = header,
                                        payload = payload
                                    )
                                )

                            }

                            if (resp.is_end == 1) {
                                onSuccess(
                                    DialogueResult(
                                        qid = resp.qid,
                                        is_end = resp.is_end,
                                        assistant_answer_content = content,
                                        completion_tokens = resp.usage?.completion_tokens ?: 0,
                                        debug = resp.debug
                                    )
                                )
                            }

                        } catch (e: Exception) {
                            activeEventSources.remove(requestId)
                            onError(e)
                            Timber.tag(TAG).e(e.message.toString())
                        }

                    }

                    override fun onFailure(
                        eventSource: EventSource,
                        t: Throwable?,
                        response: Response?
                    ) {
                        activeEventSources.remove(requestId)
                        // 优先解析服务端返回的业务错误（如权益/计费失败）。
                        // SSE 在握手阶段如果收到 Content-Type: application/json，
                        // 会抛 IllegalStateException("Invalid content-type: ...") 触发此回调，
                        // 业务错误体（{"code":6xxx,"msg":...}）就在 response.body 中。
                        // 命中业务码段位时上抛 BizException，否则回退到原网络错误。
                        val bizException = BizErrorParser.parse(response, t)
                        if (bizException != null) {
                            onError(bizException)
                            Timber.tag(TAG).e("请求业务错误: code=${bizException.code}, msg=${bizException.msg}")
                        } else {
                            onError(Exception(t))
                            Timber.tag(TAG).e(t, "流式网络请求失败")
                        }
                    }
                }
            )

            activeEventSources[requestId] = eventSource
            return requestId
        }
        mOkHttpManager.post(
            ApiConfig.INSIDE_RC_CHAT_API,
            header = createAIServerHeaders(),
            params = jsonResult,
            callback = object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    try {

                        val result = response.body?.string()
                        Timber.tag(TAG).d("response : $result")

                        // 权益模块业务错误兜底（HTTP 200 + body 内 code != 0 的场景）。
                        // 背景：服务端约定 code==0 为成功，其他非零（如 6409 NO_BIND_PHONE 未绑定手机、
                        //      套餐用尽等）均为业务错误，且这类错误通常仍以 HTTP 200 下发，
                        //      其响应体里的 `data` 字段为对象结构（与成功体里的 List<Directive> 不兼容），
                        //      若直接交给 Gson 反序列化 InsideRcChatResponse 会抛
                        //      JsonSyntaxException: Expected BEGIN_ARRAY but was BEGIN_OBJECT at $.data，
                        //      导致上层只能识别为"网络/解析异常"，丢失业务码无法分流。
                        // 处理：在进入 Gson 解析前，先用 BizErrorParser 识别一刀；
                        //      命中即上抛 BizException（携带 code/msg/rawJson），由上层根据 code 走业务弹窗等流程。
                        BizErrorParser.parseText(result)?.let { biz ->
                            onError(biz)
                            Timber.tag(TAG).e("请求业务错误: code=${biz.code}, msg=${biz.msg}")
                            return
                        }

                        val jsonString = result?.let {
                            if (it.startsWith("data:")) {
                                result.substring(5).trim()
                            } else {
                                result.trim()
                            }
                        }

                        val resp =
                            gson.fromJson(jsonString, InsideRcChatResponse::class.java)

                        val assistant_answer = JSONObject(resp.assistant_answer)
                        var content = ""
                        /*if (resp.is_end == 1)
                            content = assistant_answer.get("content").toString()*/

                        val data = resp.data

                        var isEnd = 0
                        data?.forEachIndexed { index, directive ->
                            var header: JSONObject? = null
                            var payload: JSONObject? = null
                            if (data.isNotEmpty())
                                header = JSONObject(Gson().toJson(directive.header))
                            if (data.isNotEmpty())
                                payload = JSONObject(Gson().toJson(directive.payload))

                            content = ""
                            if (index == data.size - 1)
                                content = assistant_answer.optString("content")

                            if (index == data.size - 1) {
                                isEnd = 1
                                updateChatData(
                                    insideRcChatRequest.messages,
                                    assistant_answer.toString()
                                )
                            }

                            onSuccess(
                                DialogueResult(
                                    qid = resp.qid,
                                    assistant_answer_content = "",
                                    header = header,
                                    payload = payload
                                )
                            )
                        }

                        onSuccess(
                            DialogueResult(
                                qid = resp.qid,
                                is_end = isEnd,
                                assistant_answer_content = content,
                                completion_tokens = resp.usage?.completion_tokens ?: 0,
                                debug = resp.debug
                            )
                        )

                    } catch (e: Exception) {
                        onError(e)
                        Timber.tag(TAG).e(e, "请求处理失败")
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    onError(e)
                    Timber.tag(TAG).e(e, "网络请求失败")
                }
            })

        return requestId // 非流式请求也返回ID，虽然不能取消
    }

    /*
     * 文生图
     */
    internal fun textToImage(
        imageGenerationRequest: ImageGenerationRequest,
        onSuccess: (ImageGenerationResponse) -> Unit,
        onError: (Exception) -> Unit
    ) {

        val jsonResult = gson.toJson(imageGenerationRequest)

        mOkHttpManager.post(
            ApiConfig.TEXT_TO_IMAGE_API,
            header = createAIServerHeaders(),
            params = jsonResult,
            callback = object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    try {

                        val result = response.body?.string()
                        Timber.tag(TAG).d("response : $result")

                        BizErrorParser.parseText(result)?.let { biz ->
                            onError(biz)
                            Timber.tag(TAG).e("请求业务错误: code=${biz.code}, msg=${biz.msg}")
                            return
                        }

                        val resp =
                            gson.fromJson(result, ImageGenerationResponse::class.java)

                        onSuccess(resp)
                    } catch (e: Exception) {
                        onError(e)
                        Timber.tag(TAG).e(e, "文生图请求处理失败")
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    onError(e)
                    Timber.tag(TAG).e(e, "文生图网络请求失败")
                }
            })
    }

    /*
     * 通用物体和场景识别
     */
    internal fun imageClassify(
        imageRecognitionRequest: ImageRecognitionRequest,
        onSuccess: (ImageRecognitionResponse) -> Unit,
        onError: (Exception) -> Unit
    ) {

        val jsonResult = gson.toJson(imageRecognitionRequest)

        mOkHttpManager.post(
            ApiConfig.IMAGE_CLASSIFY_API,
            header = createAIServerHeaders(),
            params = jsonResult,
            callback = object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    try {

                        val result = response.body?.string()
                        Timber.tag(TAG).d("response : $result")

                        BizErrorParser.parseText(result)?.let { biz ->
                            onError(biz)
                            Timber.tag(TAG).e("请求业务错误: code=${biz.code}, msg=${biz.msg}")
                            return
                        }

                        val resp =
                            gson.fromJson(result, ImageRecognitionResponse::class.java)

                        onSuccess(resp)
                    } catch (e: Exception) {
                        onError(e)
                        Timber.tag(TAG).e(e, "请求处理失败")
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    onError(e)
                    Timber.tag(TAG).e(e, "网络请求失败")
                }
            })
    }

    /*
     * 音频文件转写任务
     */
    internal fun speechFileTransfer(
        speechRecognitionRequest: SpeechRecognitionRequest,
        onSuccess: (SpeechRecognitionResponse) -> Unit,
        onError: (Exception) -> Unit
    ) {

        val jsonResult = gson.toJson(speechRecognitionRequest)

        mOkHttpManager.post(
            ApiConfig.SPEECH_FILE_TRANSFER_API,
            header = createAIServerHeaders(),
            params = jsonResult,
            callback = object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    try {

                        val result = response.body?.string()
                        Timber.tag(TAG).d("response : $result")

                        BizErrorParser.parseText(result)?.let { biz ->
                            onError(biz)
                            Timber.tag(TAG).e("请求业务错误: code=${biz.code}, msg=${biz.msg}")
                            return
                        }

                        val resp =
                            gson.fromJson(result, SpeechRecognitionResponse::class.java)

                        onSuccess(resp)
                    } catch (e: Exception) {
                        onError(e)
                        Timber.tag(TAG).e(e, "请求处理失败")
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    onError(e)
                    Timber.tag(TAG).e(e, "网络请求失败")
                }
            })
    }

    /*
     * 查询音频转写任务结果
     */
    internal fun speechFileTransferQuery(
        speechRecognitionQueryRequest: SpeechRecognitionQueryRequest,
        onSuccess: (SpeechRecognitionQueryResponse) -> Unit,
        onError: (Exception) -> Unit
    ) {

        val jsonResult = gson.toJson(speechRecognitionQueryRequest)

        mOkHttpManager.post(
            ApiConfig.SPEECH_FILE_TRANSFER_QUERY_API,
            header = createAIServerHeaders(),
            params = jsonResult,
            callback = object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    try {

                        val result = response.body?.string()
                        Timber.tag(TAG).d("response : $result")

                        BizErrorParser.parseText(result)?.let { biz ->
                            onError(biz)
                            Timber.tag(TAG).e("请求业务错误: code=${biz.code}, msg=${biz.msg}")
                            return
                        }

                        val resp =
                            gson.fromJson(result, SpeechRecognitionQueryResponse::class.java)

                        onSuccess(resp)
                    } catch (e: Exception) {
                        onError(e)
                        Timber.tag(TAG).e(e, "请求处理失败")
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    onError(e)
                    Timber.tag(TAG).e(e, "网络请求失败")
                }
            })
    }

    /*
     * 获取音频文件上传URL
     */
    internal fun getUploadAudioUrl(
        uploadAudioUrlRequest: UploadAudioUrlRequest,
        onSuccess: (UploadAudioUrlResponse) -> Unit,
        onError: (Exception) -> Unit
    ) {

        val jsonResult = gson.toJson(uploadAudioUrlRequest)

        mOkHttpManager.post(
            ApiConfig.GET_UPLOAD_AUDIO_URL_API,
            header = createAIServerHeaders(),
            params = jsonResult,
            callback = object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    try {

                        val result = response.body?.string()
                        Timber.tag(TAG).d("response : $result")

                        BizErrorParser.parseText(result)?.let { biz ->
                            onError(biz)
                            Timber.tag(TAG).e("请求业务错误: code=${biz.code}, msg=${biz.msg}")
                            return
                        }

                        val resp =
                            gson.fromJson(result, UploadAudioUrlResponse::class.java)

                        onSuccess(resp)
                    } catch (e: Exception) {
                        onError(e)
                        Timber.tag(TAG).e(e, "请求处理失败")
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    onError(e)
                    Timber.tag(TAG).e(e, "网络请求失败")
                }
            })
    }

    /*
     * 图片翻译
     */
    internal fun imageTranslate(
        uploadAudioUrlRequest: ImageTranslationRequest,
        onSuccess: (ImageTranslationResponse) -> Unit,
        onError: (Exception) -> Unit
    ) {

        val jsonResult = gson.toJson(uploadAudioUrlRequest)

        mOkHttpManager.post(
            ApiConfig.IMAGE_TRANSLATE_API,
            header = createAIServerHeaders(),
            params = jsonResult,
            callback = object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    try {

                        val result = response.body?.string()
                        Timber.tag(TAG).d("response : $result")

                        BizErrorParser.parseText(result)?.let { biz ->
                            onError(biz)
                            Timber.tag(TAG).e("请求业务错误: code=${biz.code}, msg=${biz.msg}")
                            return
                        }

                        val resp =
                            gson.fromJson(result, ImageTranslationResponse::class.java)

                        onSuccess(resp)
                    } catch (e: Exception) {
                        onError(e)
                        Timber.tag(TAG).e(e, "请求处理失败")
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    onError(e)
                    Timber.tag(TAG).e(e, "网络请求失败")
                }
            })
    }

    /*
     *  OCR
     */
    internal fun ocr(
        ocrRequest: OcrRequest,
        onSuccess: (OcrResponse) -> Unit,
        onError: (Exception) -> Unit
    ) {

        val jsonResult = gson.toJson(ocrRequest)

        mOkHttpManager.post(
            ApiConfig.OCR_API,
            header = createAIServerHeaders(),
            params = jsonResult,
            callback = object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    try {

                        val result = response.body?.string()
                        Timber.tag(TAG).d("response : $result")

                        BizErrorParser.parseText(result)?.let { biz ->
                            onError(biz)
                            Timber.tag(TAG).e("请求业务错误: code=${biz.code}, msg=${biz.msg}")
                            return
                        }

                        val resp =
                            gson.fromJson(result, OcrResponse::class.java)

                        onSuccess(resp)
                    } catch (e: Exception) {
                        onError(e)
                        Timber.tag(TAG).e(e, "请求处理失败")
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    onError(e)
                    Timber.tag(TAG).e(e, "网络请求失败")
                }
            })
    }

    /*
     *  word query
     */
    internal fun wordQuery(
        chineseCharacterRequest: ChineseCharacterRequest,
        onSuccess: (ChineseCharacterResponse) -> Unit,
        onError: (Exception) -> Unit
    ) {

        val jsonResult = gson.toJson(chineseCharacterRequest)

        mOkHttpManager.post(
            ApiConfig.WORD_QUERY_API,
            header = createAIServerHeaders(),
            params = jsonResult,
            callback = object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    try {

                        val result = response.body?.string()
                        Timber.tag(TAG).d("response : $result")

                        BizErrorParser.parseText(result)?.let { biz ->
                            onError(biz)
                            Timber.tag(TAG).e("请求业务错误: code=${biz.code}, msg=${biz.msg}")
                            return
                        }

                        val resp =
                            gson.fromJson(result, ChineseCharacterResponse::class.java)

                        onSuccess(resp)
                    } catch (e: Exception) {
                        onError(e)
                        Timber.tag(TAG).e(e, "请求处理失败")
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    onError(e)
                    Timber.tag(TAG).e(e, "网络请求失败")
                }
            })
    }

    /*
     *  content summary
     */
    internal fun contentSummary(
        contentSummaryRequest: ContentSummaryRequest,
        onSuccess: (ContentSummaryResponse) -> Unit,
        onError: (Exception) -> Unit
    ): String {

        val requestId = generateRequestId("summary")
        val jsonResult = gson.toJson(contentSummaryRequest)

        if (contentSummaryRequest.stream) {

            val eventSource = mOkHttpManager.postStream(
                ApiConfig.NOTE_SUMMARY_API,
                header = createAIServerHeaders(),
                params = jsonResult,
                callback = object : EventSourceListener() {
                    override fun onEvent(
                        eventSource: EventSource,
                        id: String?,
                        type: String?,
                        data: String
                    ) {
                        // 这里直接获取数据，不需要处理"data:"前缀
                        if (data == "[DONE]") {
                            activeEventSources.remove(requestId)
                            return
                        }

                        try {

                            val result = data
                            Timber.tag(TAG).d("response : $result")

                            val jsonString = result.let {
                                if (it.startsWith("data:")) {
                                    result.substring(5).trim()
                                } else {
                                    result.trim()
                                }
                            }

                            val resp =
                                gson.fromJson(jsonString, ContentSummaryResponse::class.java)

                            onSuccess(resp)
                        } catch (e: Exception) {
                            activeEventSources.remove(requestId)
                            onError(e)
                            Timber.tag(TAG).e(e.message.toString())
                        }

                    }

                    override fun onFailure(
                        eventSource: EventSource,
                        t: Throwable?,
                        response: Response?
                    ) {
                        activeEventSources.remove(requestId)

                        val bizException = BizErrorParser.parse(response, t)
                        if (bizException != null) {
                            onError(bizException)
                            Timber.tag(TAG).e("请求业务错误: code=${bizException.code}, msg=${bizException.msg}")
                        } else {
                            onError(Exception(t))
                            Timber.tag(TAG).e(t, "流式网络请求失败")
                        }

                        /*onError(Exception(t))
                        Timber.tag(TAG).e(t, "流式网络请求失败")*/
                    }
                }
            )

            activeEventSources[requestId] = eventSource
            return requestId
        }

        mOkHttpManager.post(
            ApiConfig.NOTE_SUMMARY_API,
            header = createAIServerHeaders(),
            params = jsonResult,
            callback = object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    try {

                        val result = response.body?.string()
                        Timber.tag(TAG).d("response : $result")

                        BizErrorParser.parseText(result)?.let { biz ->
                            onError(biz)
                            Timber.tag(TAG).e("请求业务错误: code=${biz.code}, msg=${biz.msg}")
                            return
                        }

                        val resp =
                            gson.fromJson(result, ContentSummaryResponse::class.java)

                        onSuccess(resp)
                    } catch (e: Exception) {
                        onError(e)
                        Timber.tag(TAG).e(e, "请求处理失败")
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    onError(e)
                    Timber.tag(TAG).e(e, "网络请求失败")
                }
            })

        return requestId // 非流式请求也返回ID，虽然不能取消
    }

    private fun updateChatData(messages: List<InsideRcChatRequest.Message>, content: String) {
        val message = messages[messages.size - 1]
        val chatDataSend = ChatData(true, message.content)
        AISessionManager.getChatDataList().add(chatDataSend)
        val chatDataReceived = ChatData(false, "")
        chatDataReceived.rawAssistAnswer = content
        AISessionManager.getChatDataList().add(chatDataReceived)
    }

    /*
     *  text translate
     */
    internal fun textTranslate(
        translationRequest: TranslationRequest,
        onSuccess: (TranslateResponse) -> Unit,
        onError: (Exception) -> Unit,
        requestApi: String = ApiConfig.TEXT_TRANSLATE_API
    ) {

        val jsonResult = gson.toJson(translationRequest)

        mOkHttpManager.post(
            requestApi,
            header = createAIServerHeaders(),
            params = jsonResult,
            callback = object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    try {

                        val result = response.body?.string()
                        Timber.tag(TAG).d("response : $result")

                        // 权益模块业务错误兜底（HTTP 200 + body 内 code != 0 的场景）。
                        // 背景：服务端约定 code==0 为成功，其他非零均为业务错误，且这类错误通常仍以 HTTP 200 下发，
                        //      可能来自：① 权益/计费链路（如 6409 NO_BIND_PHONE 未绑定手机、套餐用尽等）；
                        //               ② 翻译服务自身错误码（401 鉴权 / 20003 敏感词 / 31xxx 翻译内部错误等）。
                        //      Gson 反序列化 TranslateResponse 不会抛 JsonSyntaxException（data 是对象不是数组），
                        //      但会把 TextTranslateResp 的非空 String 字段填成 null，且原 onError 仅传 msg
                        //      会丢失 code 与原始 JSON，上层无法区分错误来源、也拿不到 data.errorCode 等扩展信息。
                        // 处理：在进入 Gson 解析前，先用 BizErrorParser 识别一刀；
                        //      命中即上抛 BizException（携带 code/msg/rawJson），由上层根据 code 走业务弹窗等流程。
                        BizErrorParser.parseText(result)?.let { biz ->
                            onError(biz)
                            Timber.tag(TAG).e("textTranslate 业务错误: code=${biz.code}, msg=${biz.msg}")
                            return
                        }

                        val resp =
                            gson.fromJson(result, TranslateResponse::class.java)
                        if (resp.code == 0) {
                            onSuccess(resp)
                            return
                        }
                        /*onError(Exception(resp.msg))*/

                    } catch (e: Exception) {
                        onError(e)
                        Timber.tag(TAG).e(e, "请求处理失败")
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    onError(e)
                    Timber.tag(TAG).e(e, "网络请求失败")
                }
            })
    }

}
