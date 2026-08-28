package com.cmdc.ai.assist.api

import android.content.Context
import com.cmdc.ai.assist.AIAssistantManager
import com.cmdc.ai.assist.api.AISessionManager.buildMessages
import com.cmdc.ai.assist.aiModel.DealSotaOne
import com.cmdc.ai.assist.aiModel.MicroRecordStream
import com.cmdc.ai.assist.constraint.AsrResult
import com.cmdc.ai.assist.constraint.ChatData
import com.cmdc.ai.assist.constraint.DialogueResult
import com.cmdc.ai.assist.http.ApiConfig
import com.cmdc.ai.assist.utils.AssistUtils
import com.cmdc.ai.assist.utils.TimeStampTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.InputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


/**
 * 实时麦克风语音识别处理类
 * 负责处理实时语音输入的识别流程
 */
class ASRIntelligentDialogue {

    companion object {

        private const val CONFIG_FILE_NAME = "hht_ctx4.conf"

        /*private const val WS_URL = "wss://duer-kids.baidu.com/sandbox/sota/realtime_asr?sn=%s"*/

        /*private const val WS_URL = "wss://36.133.228.58:36981/app-ws/v1/asr?sign=7f9e4b2a1d8c5f3e6m9n7k4l2p8q5r3t&sn=%s"*/
        /*private const val WS_URL =
            "wss://c124df1z.cxzfdm.com:36981/app-ws/v1/asr?sign=7f9e4b2a1d8c5f3e6m9n7k4l2p8q5r3t&sn=%s"*/
        /*private const val WS_URL =
            "wss://z5f3vhk2.cxzfdm.com:30101/apgp/ws/sandbox/sota/realtime_asr?sn=xxxxx"*/
        private var WS_URL = ""

    }

    @Volatile
    private var isReleased = false

    @Volatile
    private var isRecognizing = false
    private val stateLock = Object()
    private var currentDealSotaOne: DealSotaOne? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var microRecordStream: IRecordStream
    private val TAG = ASRIntelligentDialogue::class.simpleName.toString()

    /**
     * 无参构造函数
     * 使用默认的 MicroRecordStream 实例
     */
    internal constructor() {
        this.microRecordStream = MicroRecordStream.getInstance()
    }

    /**
     * 带参数的构造函数
     *
     * @param recordStream 音频录制流实例，用于获取音频数据
     */
    constructor(recordStream: IRecordStream) {
        this.microRecordStream = recordStream
    }

    /**
     * 实时语音识别监听器接口
     * 该接口定义了实时语音识别过程中不同阶段的回调方法，包括中间结果、最终结果、对话结果、错误处理和完成通知
     */
    interface RealtimeAsrListener {

        /**
         * 建联成功回调
         *
         */
        fun onConnected() {}

        /**
         * 语音识别中间结果回调
         *
         * @param text 识别到的文本信息
         */
        fun onAsrMidResult(asrResult: AsrResult) {}

        /**
         * 语音识别最终结果回调
         *
         * @param text 最终识别的文本信息
         */
        fun onAsrFinalResult(asrResult: AsrResult) {}

        /**
         * 对话结果回调
         *
         * @param result 包含对话相关信息的JSON对象
         */
        private fun onDialogueResult(result: JSONObject) {}

        /**
         * 对话结果回调
         *
         * @param result 包含对话相关信息的JSON对象
         */
        fun onDialogueResult(result: DialogueResult) {}

        /**
         * 错误回调
         *
         * @param code 错误代码
         * @param message 错误信息
         */
        fun onError(code: Int, message: String) {}

        /**
         * 完成回调
         * 表示 ASR 智能对话过程已完成
         */
        fun onComplete() {}
    }

    private var listener: RealtimeAsrListener? = null

    /**
     * 设置实时语音识别监听器
     *
     * 该方法用于将一个RealtimeAsrListener接口的实现类对象注册为监听器，以便在语音识别过程中接收实时的识别结果
     * 通过此监听器，开发者可以获取到语音识别的中间结果和最终结果，进而进行相应的业务处理
     *
     * @param listener 实现了RealtimeAsrListener接口的监听器对象，用于接收语音识别事件
     */
    fun setListener(listener: RealtimeAsrListener) {
        this.listener = listener
    }

    /**
     * 启动语音识别功能
     *
     * 此函数通过协程在IO调度器上启动语音识别任务，以便不阻塞主线程
     * 它尝试使用麦克风输入进行语音识别如果识别过程中遇到错误，
     * 将通过监听器回调错误信息，并记录错误日志
     *
     * @param context 上下文，用于访问应用环境信息
     */
    fun startRecognition(context: Context) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                microRecordStream.startRecording()
                startMicrophoneRecognition(context, microRecordStream)
            } catch (e: CancellationException) {
                // 协程取消（用户主动 release/stop），不通知错误
                Timber.tag(TAG).d("协程取消: ${e.message}")
            } catch (e: SecurityException) {
                // SecurityException → 无录音权限
                listener?.onError(ASRDialogueErrorCode.ERROR_PERMISSION, "无录音权限: ${e.message}")
                Timber.tag(TAG)
                    .e("错误: code=${ASRDialogueErrorCode.ERROR_PERMISSION}, message=${e.message}")
            } catch (e: IllegalStateException) {
                // IllegalStateException → AudioRecord 设备异常
                listener?.onError(
                    ASRDialogueErrorCode.ERROR_AUDIO_DEVICE,
                    "音频设备异常: ${e.message}"
                )
                Timber.tag(TAG)
                    .e("错误: code=${ASRDialogueErrorCode.ERROR_AUDIO_DEVICE}, message=${e.message}")
            } catch (e: Exception) {
                // 其他未预期异常
                listener?.onError(ASRDialogueErrorCode.ERROR_INTERNAL, "启动失败: ${e.message}")
                Timber.tag(TAG)
                    .e("错误: code=${ASRDialogueErrorCode.ERROR_INTERNAL}, message=${e.message}")
            }
        }
    }

    /**
     * 开始麦克风录音识别
     */
    private suspend fun startMicrophoneRecognition(
        context: Context,
        microStream: IRecordStream
    ): String =
        withContext(Dispatchers.IO) {

            synchronized(stateLock) {
                if (isRecognizing) {
                    listener?.onError(ASRDialogueErrorCode.ERROR_ALREADY_RUNNING, "识别已在进行中")
                    Timber.tag(TAG).e("Recognition already in progress")
                    return@withContext ""
                }
                if (isReleased) {
                    listener?.onError(ASRDialogueErrorCode.ERROR_RELEASED, "实例已释放，请重新创建")
                    Timber.tag(TAG).e("Instance has been released")
                    return@withContext ""
                }
                isRecognizing = true
            }

            try {
                return@withContext suspendCancellableCoroutine { continuation ->

                    //  hht_ctx4.conf 这个文件在Android assets 目录下
                    val configFile = File(context.cacheDir, CONFIG_FILE_NAME).apply {
                        context.assets.open("hht_ctx4.conf").use { input ->
                            outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }

                    val dealSotaOne = DealSotaOne(
                        microStream,
                        configFile
                    )
                    currentDealSotaOne = dealSotaOne

                    dealSotaOne.setListener(object : DealSotaOne.DealSotaOneListener {
                        override fun onConnected() {
                            if (!isReleased) {
                                listener?.onConnected()
                                Timber.tag(TAG).d("wss connected:")
                            }
                        }

                        override fun onMidResult(asrResult: AsrResult) {
                            if (!isReleased) {
                                listener?.onAsrMidResult(asrResult)
                                Timber.tag(TAG).d("%s%s", "实时识别结果: ", asrResult)
                            }
                        }

                        override fun onFinalResult(asrResult: AsrResult) {
                            if (!isReleased) {
                                listener?.onAsrFinalResult(asrResult)
                                val chatData = ChatData(true, asrResult.text)
                                AISessionManager.getChatDataList().add(chatData)
                                Timber.tag(TAG).d("%s%s", "最终识别结果: ", asrResult)
                                TimeStampTracker.start()
                            }
                        }

                        override fun onDialogueResult(result: JSONObject) {
                            if (!isReleased) {

                                /*log(result)*/
                                /*val params = dialogueResultParse(result)
                                listener?.onDialogueResult(dialogueResult)
                                Timber.tag(TAG).d("onDialogueResult params: $params")*/

                                dialogueResultParseDialogueResult(listener, result)

                                Timber.tag(TAG).d("%s%s", "智能对话结果: ", result)
                            }
                        }

                        override fun onError(code: Int, message: String) {
                            if (!isReleased) {
                                Timber.tag(TAG).e("错误: code=$code, message=$message")
                                listener?.onError(code, message)
                                /*continuation.resumeWithException(Exception(message))*/
                            }
                        }

                        override fun onComplete() {
                            if (!isReleased) {
                                listener?.onComplete()
                                Timber.tag(TAG).d("识别完成")
                            }
                            synchronized(stateLock) {
                                isRecognizing = false
                            }
                            continuation.resume(dealSotaOne.getAsrResult())
                        }
                    })

                    // Launch a coroutine to call dealOne
                    launch(Dispatchers.IO) {
                        try {

                            // 地址：wss://ivs.chinamobiledevice.com:11443/app-ws/v1/asr?sn=xxxx-xxxx-xxx&deviceNo=xxxxxx&productKey=xxxx&productId=xxxx&ts=1740017297000&sign=xxxxx

                            WS_URL =
                                AssistUtils.wssParameter(ApiConfig.ASR_INTELLIGENT_DIALOGUE_API)
                            Timber.tag(TAG).d("WS_URL ：%s", WS_URL)
                            val message = AISessionManager.getChatDataList().buildMessages()
                            dealSotaOne.dealOne(WS_URL, message)
                        } catch (e: CancellationException) {
                            // 协程取消（用户主动 release/stop），重新抛出以支持协程取消机制
                            throw e
                        } catch (e: Exception) {
                            // isReleased 时不通知错误（用户主动停止导致的资源清理异常）
                            if (!isReleased) {
                                Timber.tag(TAG)
                                    .e("错误: code=${ASRDialogueErrorCode.ERROR_INTERNAL}, message=${e.message}")
                                listener?.onError(
                                    ASRDialogueErrorCode.ERROR_INTERNAL,
                                    "识别失败: ${e.message}"
                                )
                            }
                            // 用 resume 代替 resumeWithException，避免异常传播到外层再次触发 onError
                            if (continuation.isActive) {
                                continuation.resume("")
                            }
                        }
                    }

                    continuation.invokeOnCancellation {
                        dealSotaOne.stopRecognition()
                    }
                }
            } finally {
                synchronized(stateLock) {
                    isRecognizing = false
                }
            }

        }

    private fun dialogueResultParseDialogueResult(
        listener: RealtimeAsrListener?,
        result: JSONObject
    ) {

        val isEnd = result.optInt("is_end")
        var assistant_answer_content: String? = null
        val qid = result.optString("qid")

        try {
            val directives = result.optJSONArray("data")
            if (directives == null) {

                listener?.onDialogueResult(
                    DialogueResult(
                        qid = qid,
                        assistant_answer_content = assistant_answer_content
                    )
                )
                return  // 阻止往下执行，避免 directives!! 抛 NPE

            }
            for (i in 0 until directives.length()) {
                val directive = directives.optJSONObject(i)
                val header = directive.optJSONObject("header")
                val payload = directive.optJSONObject("payload")
                val name = header?.optString("name")
                when (name) {
                    "RenderProcessing" -> {
                        // progress
                        val percent = payload?.optInt("percent")
                        Timber.tag(TAG).d("isGenerating = true 进度 ：%s", percent)

                    }

                    "Nlu" -> {
                        // nlu
                        val nlu = payload?.optJSONArray("nlu")
                        Timber.tag(TAG).d("意图 ：%s", nlu?.toString())

                    }

                    "NluTag" -> {
                        // NluTag
                        val domain = payload?.optString("domian")
                        val intent = payload?.optString("intent")
                        val nluTag = JSONObject().apply {
                            put("domain", domain)
                            put("intent", intent)
                        }
                        Timber.tag(TAG).d("%s%s", "NluTag  = domain: $domain  ", "intent: $intent")

                    }

                    "RenderMultiImageCard" -> {
                        // 图片
                        val images = payload?.optJSONArray("images")
                        if (images == null || images.length() == 0) {
                            continue
                        }
                        val pic = images.optJSONObject(0).optString("url")
                        Timber.tag(TAG).d("percent = 100 图片 ：%s", pic)

                    }

                    "Play" -> {
                        // 音乐
                        val mediaUrl = payload?.optJSONObject("audioItem")?.optJSONObject("stream")
                            ?.optString("url")
                        val albumName =
                            payload?.optJSONObject("audioItem")?.optString("extension")
                        val play = JSONObject().apply {
                            put("mediaUrl", mediaUrl)
                            put("albumName", albumName)
                        }
                        Timber.tag(TAG)
                            .d("%s%s", "Play  = mediaUrl: $mediaUrl  ", "albumName: $albumName")

                    }

                    "RenderStreamCard" -> {
                        // 流式
                        val text = payload?.optString("answer")
                        Timber.tag(TAG).d("answer content ：%s", text)

                    }

                    "Speak" -> {
                        // tts
                        /*asrChatAdapter.stopMedia()*/
                        val ttlUrl = payload?.optString("url")
                        Timber.tag(TAG).d("%s%s", "Speak  = qid: $qid  ", "ttlUrl: $ttlUrl")

                    }

                    else -> {
                        // 处理其他未明确指定的情况
                        Timber.tag(TAG).d("未处理的消息类型: %s, payload: %s", name, payload)
                    }
                }

                listener?.onDialogueResult(
                    DialogueResult(
                        qid = qid,
                        assistant_answer_content = assistant_answer_content,
                        header = header,
                        payload = payload
                    )
                )

            }

            if (isEnd == 1) {
                val assistantAnswer = result.optString("assistant_answer")
                val chatData = ChatData(false, "")
                chatData.rawAssistAnswer = assistantAnswer
                AISessionManager.getChatDataList().add(chatData)
                val content = getAnswer(assistantAnswer)
                assistant_answer_content = content
                Timber.tag(TAG).d(" isEnd == 1 answer content ：%s", content)

                listener?.onDialogueResult(
                    DialogueResult(
                        qid = qid,
                        is_end = isEnd,
                        assistant_answer_content = assistant_answer_content,
                    )
                )

            }

        } catch (e: Exception) {
            Timber.tag(TAG).e("analyzePayload err : $e")
        }

    }

    private fun dialogueResultParse(result: JSONObject): JSONObject {

        val params = JSONObject()

        val isEnd = result.optInt("is_end")
        params.put("is_end", isEnd)

        if (isEnd == 1) {
            val assistantAnswer = result.optString("assistant_answer")
            val content = getAnswer(assistantAnswer)
            params.put("assistant_answer_content", content)
            Timber.tag(TAG).d(" isEnd == 1 answer content ：%s", content)
        }
        analyzeDirectives(params, result)
        return params
    }

    private fun getAnswer(assistantAnswer: String): String {
        return try {
            val obj = JSONObject(assistantAnswer)
            if (obj.has("content")) {
                obj.optString("content")
            } else {
                assistantAnswer
            }
        } catch (e: Exception) {
            Timber.tag(TAG)
                .e(("assistantAnswer parse error: " + "${e.message}"))
            assistantAnswer
        }
    }

    private fun analyzeDirectives(params: JSONObject, result: JSONObject) {
        try {
            val directives = result.optJSONArray("data")
            if (directives == null) {
                return
            }
            val qid = result.optString("qid")
            params.put("qid", qid)
            for (i in 0 until directives.length()) {
                val directive = directives.optJSONObject(i)
                val header = directive.optJSONObject("header")
                val payload = directive.optJSONObject("payload")
                val name = header?.optString("name")
                when (name) {
                    "RenderProcessing" -> {
                        // progress
                        val percent = payload?.optInt("percent")
                        params.put("renderProcessing_percent", percent)
                        Timber.tag(TAG).d("isGenerating = true 进度 ：%s", percent)
                    }

                    "Nlu" -> {
                        // nlu
                        val nlu = payload?.optJSONArray("nlu")
                        params.put("nlu", nlu)
                        Timber.tag(TAG).d("意图 ：%s", nlu?.toString())
                    }

                    "NluTag" -> {
                        // NluTag
                        val domain = payload?.optString("domian")
                        val intent = payload?.optString("intent")
                        val nluTag = JSONObject().apply {
                            put("domain", domain)
                            put("intent", intent)
                        }
                        params.put("nluTag", nluTag)
                        Timber.tag(TAG).d("%s%s", "NluTag  = domain: $domain  ", "intent: $intent")
                    }

                    "RenderMultiImageCard" -> {
                        // 图片
                        val images = payload?.optJSONArray("images")
                        if (images == null || images.length() == 0) {
                            continue
                        }
                        val pic = images.optJSONObject(0).optString("url")
                        params.put("renderMultiImageCard_url", pic)
                        Timber.tag(TAG).d("percent = 100 图片 ：%s", pic)
                    }

                    "Play" -> {
                        // 音乐
                        val mediaUrl = payload?.optJSONObject("audioItem")?.optJSONObject("stream")
                            ?.optString("url")
                        val albumName =
                            payload?.optJSONObject("audioItem")?.optString("extension")
                        val play = JSONObject().apply {
                            put("mediaUrl", mediaUrl)
                            put("albumName", albumName)
                        }
                        params.put("play", play)
                        Timber.tag(TAG)
                            .d("%s%s", "Play  = mediaUrl: $mediaUrl  ", "albumName: $albumName")
                    }

                    "RenderStreamCard" -> {
                        // 流式
                        val text = payload?.optString("answer")
                        params.put("renderStreamCard_answer_content", text)
                        Timber.tag(TAG).d("answer content ：%s", text)
                    }

                    "Speak" -> {
                        // tts
                        /*asrChatAdapter.stopMedia()*/
                        val ttlUrl = payload?.optString("url")
                        params.put("speak_ttsUrl", ttlUrl)
                        Timber.tag(TAG).d("%s%s", "Speak  = qid: $qid  ", "ttlUrl: $ttlUrl")
                    }
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e("analyzePayload err : $e")
        }
    }

    private fun log(result: JSONObject) {
        val directives = result.optJSONArray("data") ?: return
        Timber.tag(TAG).d("test___data ：%s", directives)
        Timber.tag(TAG)
            .d("test___data_directives_size ：%s", directives.length())
        for (i in 0 until directives.length()) {
            val directive = directives.optJSONObject(i)
            val header = directive.optJSONObject("header")
            val payload = directive.optJSONObject("payload")
            Timber.tag(TAG).d("test___directive ：%s", directive)
            Timber.tag(TAG).d("test___header ：%s", header)
            Timber.tag(TAG).d("test___payload ：%s", payload)
            Timber.tag(TAG).d("test_________")
        }
    }

    /**
     * 释放所有资源并停止语音识别
     * 在不再需要语音识别时调用此方法
     */
    fun release() {

        synchronized(stateLock) {
            if (isReleased) {
                return
            }
            isReleased = true
        }

        try {
            currentDealSotaOne?.let { dealSota ->
                try {
                    // 停止语音识别
                    dealSota.stopRecognition()
                } catch (e: Exception) {
                    Timber.tag(TAG).e("Error stopping recognition: ${e.message}")
                } finally {
                    currentDealSotaOne = null
                }
            }
            microRecordStream.close()
        } catch (e: Exception) {
            Timber.tag(TAG).e("Error during release: ${e.message}")
        }

        coroutineScope.cancel()

    }

} 