package com.cmdc.ai.assist.api

import android.os.SystemClock
import com.cmdc.ai.assist.aiModel.AsrClientPersistentAliYun
import com.cmdc.ai.assist.aiModel.MicroRecordStreamModify
import com.cmdc.ai.assist.api.SpeechRecognitionPersistentAliYun.Companion.TAIL_DRAIN_MS
import com.cmdc.ai.assist.constraint.SpeechRecognitionPersistentAliYunData
import com.cmdc.ai.assist.http.ApiConfig
import com.cmdc.ai.assist.utils.AssistUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import timber.log.Timber
import java.net.URI
import java.nio.ByteBuffer

/**
 * 用于处理语音识别功能。（持续识别）
 * 实时处理音频数据的发送和接收。
 */
class SpeechRecognitionPersistentAliYun {

    private var vad: Long? = null

    private var listener: ASRListener? = null
    private var client: AsrClientPersistentAliYun? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val TAG = SpeechRecognitionPersistentAliYun::class.simpleName.toString()

    private var microRecordStream: IRecordStream
    private val stateLock = Any()

    // 标记当前实例是否已有启动中的识别流程，防止重复 start 覆盖 client 和录音流状态。
    @Volatile
    private var isStarted = false

    // 标记当前识别是否已进入结束流程，防止 finish/cancel 重复触发或互相打架。
    @Volatile
    private var isEnding = false

    private object AudioResourceManager {
        val microphoneSemaphore = Semaphore(1) // 全局共享
    }

    companion object {
        /**
         * 尾包收音保留时长（毫秒）。
         *
         * 业务调用 [finish] 后，并不立即停止采集与释放麦克风，而是先继续录制并上传 [TAIL_DRAIN_MS] 毫秒，
         * 让用户说话末尾（停顿前最后一段语音）以及 AudioRecord 内部缓冲区中已采集但尚未读出的 PCM 数据，
         * 能够通过 WebSocket 上传循环正常发送到服务端，之后再发送 FINISH 帧并关闭录音流。
         *
         * 取值原则：
         * - 过短：尾部音频仍可能被截断，体现为句尾识别丢字；
         * - 过长：拖慢最终识别结果返回，影响交互响应。
         * 经验值 300ms 可在大多数场景下覆盖典型的句尾停顿与系统调度抖动。
         */
        private const val TAIL_DRAIN_MS: Long = 600L
    }

    /**
     * 无参构造函数
     * 使用默认的MicroRecordStream实例
     */
    internal constructor() {
        this.microRecordStream = MicroRecordStreamModify.getInstance()
    }

    /**
     * 带参数的构造函数
     *
     * @param recordStream 音频录制流实例，用于获取音频数据
     */
    internal constructor(recordStream: IRecordStream) {
        this.microRecordStream = recordStream
    }

    /**
     * 定义处理消息接收、错误处理和连接状态变化的方法。
     */
    interface ASRListener {
        /**
         * 当 WebSocket 连接建立成功时调用。
         * 仅表示客户端与服务端建联成功，不代表服务端已经完成识别初始化。
         */
        fun onConnected() {}

        /**
         * 当 SDK 从当前录音流读取到一帧 PCM 数据并计算出语音能量时调用。
         *
         * @param energy 归一化后的能量值，范围为 0.0f ~ 1.0f，可直接用于驱动录音波形动画。
         */
        fun onAudioEnergy(energy: Float) {}

        /**
         * 当接收到音频识别后的数据消息时调用
         *
         * @param message 可能为空的识别数据对象
         */
        fun onMessageReceived(message: SpeechRecognitionPersistentAliYunData?)

        /**
         * 当接收到 tts 输出流时调用。
         * 返回 pcm 格式的 tts 音频播报数据。
         *
         * @param bytes 可能为空的字节缓冲区
         */
        fun onMessageReceived(bytes: ByteBuffer?)

        /**
         * 当连接关闭时调用
         *
         * @param code 关闭连接的状态码
         * @param reason 可能为空的关闭原因描述
         * @param remote 是否由远程主机发起的关闭
         */
        fun onClose(code: Int, reason: String?, remote: Boolean)

        /**
         * 当发生错误时调用
         *
         * @param ex 可能为空的异常对象
         */
        fun onError(ex: Exception?)
    }

    /**
     * 设置语音活动检测（VAD）的静音判断时长。单位 ms，默认 800 ms。
     *
     * 该时长用于服务端判断用户是否已停止说话：当检测到连续静音达到此时长后，
     * 服务端将自动触发断句并返回识别结果。值越小断句越灵敏，值越大允许更长的说话停顿。
     *
     * @param vad 静音判断时长，单位为毫秒。传入后会覆盖默认值，在下次发起识别时生效。
     */
    fun setVad(vad: Long): SpeechRecognitionPersistentAliYun {
        this.vad = vad
        return this
    }

    /**
     * 设置 ASRListener 以监听消息和连接状态。
     * @param listener 实现了 ASRListener 接口的监听器。
     */
    fun setListener(listener: ASRListener): SpeechRecognitionPersistentAliYun {
        this.listener = listener
        return this
    }

    /**
     * 开始语音识别（无业务标识）。通用语音识别。
     *
     */
    fun startRecognition() {
        startRecognitionDefault(null)
    }

    private fun startRecognition(pid: Int) {
        startRecognitionDefault(pid)
    }

    /**
     * 开始语音识别过程。
     */
    private fun startRecognitionDefault(pid: Int?) {
        synchronized(stateLock) {
            if (isStarted) {
                Timber.tag(TAG).d("Recognition already started, skipping this recognition request")
                return
            }
            isStarted = true
            isEnding = false
        }

        // 检查是否有可用的许可，如果没有说明正在被使用
        if (AudioResourceManager.microphoneSemaphore.availablePermits == 0) {
            Timber.tag(TAG).d("Microphone is busy, skipping this recognition request")
            listener?.onError(Exception("The microphone is in use, please try again later."))
            // 麦克风忙只拒绝本次识别请求，不取消当前实例的协程作用域，避免实例后续无法再次启动。
            /*coroutineScope.cancel()*/
            resetRecognitionState()
            return
        }

        coroutineScope.launch {
            AudioResourceManager.microphoneSemaphore.withPermit {
                try {
                    Timber.tag(TAG).d("startRecognition")
                    microRecordStream.startRecording()
                    initAndConnectClient()
                    if (isEnding) {
                        Timber.tag(TAG).d("recognition ended before start frame, aborting start")
                        try {
                            client?.sendCancelFrame()
                        } catch (e: Exception) {
                            listener?.onError(e)
                        }
                        microRecordStream.close()
                        client?.disconnect()
                        client = null
                        resetRecognitionState()
                        return@withPermit
                    }
                    try {
                        client?.sendStartFrame(pid, vad)
                        client?.sendAudioDataByStream(microRecordStream) { energy ->
                            coroutineScope.launch(Dispatchers.Main) {
                                listener?.onAudioEnergy(energy)
                            }
                        }
                        /*client?.sendAudioDataByStreamCompress(MicroRecordStream.getInstance())*/
                    } catch (e: Exception) {
                        listener?.onError(e)
                        Timber.tag(TAG).d(e)
                        resetRecognitionState()
                        release()
                        coroutineScope.cancel()
                    }
                    Timber.tag(TAG).d("sendAudioDataByStream end")
                    microRecordStream.close()
                } catch (e: Exception) {
                    Timber.tag(TAG).d("Exception %s", e.message)
                    microRecordStream.close()
                    resetRecognitionState()
                    listener?.onError(e)
                }
            }
        }

    }

    /**
     * 初始化 WebSocket 客户端并建立连接
     */
    private fun initAndConnectClient() {
        val wsUrl =
            AssistUtils.wssParameter(ApiConfig.AUTOMATIC_SPEECH_RECOGNITION_PERSISTENT_ALIYUN_API)

        client = AsrClientPersistentAliYun(URI(wsUrl))
        client?.setMessageListener(object : AsrClientPersistentAliYun.WebSocketListener {
            override fun onOpen() {
                coroutineScope.launch(Dispatchers.Main) {
                    listener?.onConnected()
                }
            }

            override fun onMessageReceived(message: SpeechRecognitionPersistentAliYunData?) {
                coroutineScope.launch(Dispatchers.Main) {
                    listener?.onMessageReceived(message)
                    /*Timber.tag(TAG).d("SpeechRecognitionPersistentAliYun onMessage: %s", message)*/
                }
            }

            override fun onMessageReceived(bytes: ByteBuffer?) {
                coroutineScope.launch(Dispatchers.Main) {
                    listener?.onMessageReceived(bytes)
                }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                coroutineScope.launch(Dispatchers.Main) {
                    try {
                        listener?.onClose(code, reason, remote)
                        Timber.tag(TAG).d("onClose")
                    } finally {
                        microRecordStream.close()
                        client = null
                        resetRecognitionState()
                        coroutineScope.cancel()
                    }
                }
            }

            override fun onError(ex: Exception?) {
                coroutineScope.launch(Dispatchers.Main) {
                    try {
                        listener?.onError(ex)
                        Timber.tag(TAG).d("onError")
                    } finally {
                        microRecordStream.close()
                        client = null
                        resetRecognitionState()
                        coroutineScope.cancel()
                    }
                }
            }
        })
        val connectStartMs = SystemClock.elapsedRealtime()
        try {
            client?.connectBlocking()
            val connectCostMs = SystemClock.elapsedRealtime() - connectStartMs
            Timber.tag(TAG).d("SpeechRecognitionPersistentAliYun WebSocket connect success, cost=%dms", connectCostMs)
        } catch (e: Exception) {
            val connectCostMs = SystemClock.elapsedRealtime() - connectStartMs
            Timber.tag(TAG).e(e, "SpeechRecognitionPersistentAliYun WebSocket connect failed, cost=%dms", connectCostMs)
            listener?.onError(e)
            Timber.tag(TAG).d(e)
            release()
            // 建联失败不会触发 WebSocket onClose/onError，这里兜底清理客户端引用。
            client = null
            resetRecognitionState()
            coroutineScope.cancel()
        }
    }

    private fun release() {
        cancel()
    }

    /**
     * 正常结束当前语音识别。
     *
     * 用于用户说完话并需要服务端返回最终识别结果的场景。
     *
     * 执行顺序（解决尾包丢失问题，详见 [TAIL_DRAIN_MS]）：
     * 1. 先进入尾包收音阶段：保持麦克风继续录制 [TAIL_DRAIN_MS] 毫秒，期间上传协程
     *    （[com.cmdc.ai.assist.aiModel.AsrClientPersistentAliYun.sendAudioDataByStream]）持续读取并上传 PCM；
     * 2. 通过 WebSocket 发送 FINISH 帧，通知服务端音频输入已结束、请求最终识别结果；
     * 3. 关闭本地录音流，释放 AudioRecord 资源。
     *
     * WebSocket 客户端引用在 onClose/onError 中统一清理，避免提前清空连接状态。
     */
    fun finish() {
        synchronized(stateLock) {
            if (!isStarted || isEnding) {
                Timber.tag(TAG).d("finish ignored, recognition is not active")
                return
            }
            isEnding = true
        }

        coroutineScope.launch {
            try {
                Timber.tag(TAG).d("finish")

                // 步骤 1：尾包收音。
                // 维持麦克风录制，等待上传协程把用户说话末尾以及 AudioRecord 内部缓冲区中
                // 尚未读出的 PCM 数据全部上传完成，避免句尾被截断。
                try {
                    delay(TAIL_DRAIN_MS)
                } catch (e: Exception) {
                    // delay 被取消属于正常的协程取消路径，仅记录日志即可。
                    Timber.tag(TAG).d("finish tail-drain interrupted: %s", e.message)
                }

                // 步骤 2：发送 FINISH 帧，通知服务端音频输入结束。
                try {
                    if (client?.isOpen == true) {
                        client?.sendFinishFrame()
                    } else {
                        Timber.tag(TAG).d("finish requested before WebSocket connected")
                    }
                } catch (e: Exception) {
                    listener?.onError(e)
                }

                // 步骤 3：关闭本地录音流，释放麦克风资源。
                microRecordStream.close()
            } catch (e: Exception) {
                listener?.onError(e)
            } finally {

            }
        }

    }

    /**
     * 取消当前语音识别。
     * 用于放弃本次识别结果的场景。
     * 先发送 cancel 帧通知服务端取消本次识别，再关闭本地录音流。
     * WebSocket 客户端引用在 onClose/onError 中统一清理，避免提前清空连接状态。
     */
    fun cancel() {
        synchronized(stateLock) {
            if (!isStarted || isEnding) {
                Timber.tag(TAG).d("cancel ignored, recognition is not active")
                return
            }
            isEnding = true
        }

        coroutineScope.launch {
            try {
                Timber.tag(TAG).d("cancel")
                try {
                    if (client?.isOpen == true) {
                        client?.sendCancelFrame()
                    } else {
                        Timber.tag(TAG).d("cancel requested before WebSocket connected")
                    }
                } catch (e: Exception) {
                    listener?.onError(e)
                }
                microRecordStream.close()
            } catch (e: Exception) {
                listener?.onError(e)
            } finally {
            }
        }

    }

    /**
     * 重置本次识别的操作状态。
     * 在连接关闭、连接失败或启动异常时调用，允许后续重新发起识别。
     */
    private fun resetRecognitionState() {
        synchronized(stateLock) {
            isStarted = false
            isEnding = false
        }
    }

}
