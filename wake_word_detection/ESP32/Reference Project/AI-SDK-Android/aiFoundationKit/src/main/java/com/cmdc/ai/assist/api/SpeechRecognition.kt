package com.cmdc.ai.assist.api

import com.cmdc.ai.assist.aiModel.AsrClient
import com.cmdc.ai.assist.aiModel.MicroRecordStream
import com.cmdc.ai.assist.constraint.SpeechRecognitionData
import com.cmdc.ai.assist.http.ApiConfig
import com.cmdc.ai.assist.utils.AssistUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.lang.Exception
import java.net.URI
import java.nio.ByteBuffer

/**
 * 用于处理语音识别功能。（单次识别）
 * 实时处理音频数据的发送和接收。
 */
class SpeechRecognition {

    private var listener: ASRListener? = null
    private var client: AsrClient? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val microphoneSemaphore = Semaphore(1)
    private var microRecordStream: IRecordStream

    /**
     * 无参构造函数
     * 使用默认的MicroRecordStream实例
     */
    internal constructor() {
        this.microRecordStream = MicroRecordStream.getInstance()
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
         * 当接收到音频识别后的数据消息时调用
         *
         * @param message 可能为空的识别数据对象
         */
        fun onMessageReceived(message: SpeechRecognitionData?)

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
     * 设置 ASRListener 以监听消息和连接状态。
     * @param listener 实现了 ASRListener 接口的监听器。
     */
    fun setListener(listener: ASRListener) {
        this.listener = listener
    }

    /**
     * 开始语音识别过程。
     */
    fun startRecognition() {

        coroutineScope.launch {
            microphoneSemaphore.withPermit {
                try {
                    microRecordStream.startRecording()
                    initAndConnectClient()
                    try {
                        client?.sendStartFrame()
                        /*client?.sendAudioDataByStream(MicroRecordStream.getInstance())*/
                        client?.sendAudioDataByStreamCompress(microRecordStream)
                    } catch (e: Exception) {
                        listener?.onError(e)
                        release()
                    }
                } catch (e: Exception) {
                    listener?.onError(e)
                }
            }
        }

    }

    /**
     * 初始化 WebSocket 客户端并建立连接
     */
    private fun initAndConnectClient() {
        try {
            val wsUrl = AssistUtils.wssParameter(ApiConfig.AUTOMATIC_SPEECH_RECOGNITION_API)
            /*val wsUrl = "wss://duer-kids.baidu.com/sota/realtime_asr?sn=a1b2c3d4-e5f6-7890-abcd-ef1234567890"*/

            client = AsrClient(URI(wsUrl))
            client?.setMessageListener(object : AsrClient.WebSocketListener {
                override fun onMessageReceived(message: SpeechRecognitionData?) {
                    coroutineScope.launch(Dispatchers.Main) {
                        listener?.onMessageReceived(message)
                    }
                }

                override fun onMessageReceived(bytes: ByteBuffer?) {
                    coroutineScope.launch(Dispatchers.Main) {
                        listener?.onMessageReceived(bytes)
                    }
                }

                override fun onClose(code: Int, reason: String?, remote: Boolean) {
                    coroutineScope.launch(Dispatchers.Main) {
                        listener?.onClose(code, reason, remote)
                    }
                    release()
                }

                override fun onError(ex: Exception?) {
                    coroutineScope.launch(Dispatchers.Main) {
                        listener?.onError(ex)
                    }
                    release()
                }
            })
            try {
                client?.connectBlocking()
            } catch (e: Exception) {
                listener?.onError(e)
                release()
            }
        } catch (e: Exception) {
            listener?.onError(e)
        }
    }

    /**
     * 释放资源，包括关闭音频流和断开与服务器的连接。
     */
    fun release() {

        coroutineScope.launch {
            try {
                microRecordStream.close()
                try {
                    client?.sendFinishFrame()
                } catch (e: Exception) {
                    listener?.onError(e)
                }
                client?.disconnect()
                client = null
            } catch (e: Exception) {
                listener?.onError(e)
            } finally {
                coroutineScope.cancel()
            }
        }

    }

}