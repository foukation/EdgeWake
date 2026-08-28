package com.cmdc.ai.assist.api

import com.cmdc.ai.assist.aiModel.MicroRecordStream
import com.cmdc.ai.assist.aiModel.RtAsrClient
import com.cmdc.ai.assist.aiModel.RtAsrClient.WebSocketListener
import com.cmdc.ai.assist.constraint.Language
import com.cmdc.ai.assist.constraint.TranslationData
import com.cmdc.ai.assist.constraint.TranslationTypeCode
import com.cmdc.ai.assist.http.ApiConfig
import com.cmdc.ai.assist.utils.AssistUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import timber.log.Timber
import java.net.URI
import java.nio.ByteBuffer

/**
 * 用于处理语音识别和翻译功能。
 * 实时处理音频数据的发送和接收。
 */
class ASRTranslation {

    private var listener: ASRTranslationListener? = null
    private var client: RtAsrClient? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val TAG = ASRTranslation::class.simpleName.toString()
    private var microRecordStream: IRecordStream

    private object AudioResourceManager {
        val microphoneSemaphore = Semaphore(1) // 全局共享
    }

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
    interface ASRTranslationListener {
        /**
         * 当接收到翻译数据消息时调用
         *
         * @param message 可能为空的翻译数据对象
         */
        fun onMessageReceived(message: TranslationData?)

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
     * 设置 ASRTranslationListener 以监听消息和连接状态。
     * @param listener 实现了 ASRTranslationListener 接口的监听器。
     */
    fun setListener(listener: ASRTranslationListener) {
        this.listener = listener
    }

    /**
     * 开始语音识别和翻译过程，此接口默认支持中英互译。
     *
     * @param translationType 指定翻译类型，见 TranslationTypeCode 枚举。
     */
    fun startRecognition(translationType: TranslationTypeCode) {

        // 检查是否有可用的许可，如果没有说明正在被使用
        if (AudioResourceManager.microphoneSemaphore.availablePermits == 0) {
            Timber.tag(TAG).d("Microphone is busy, skipping this recognition request")
            listener?.onError(Exception("The microphone is in use, please try again later."))
            return
        }

        coroutineScope.launch {
            AudioResourceManager.microphoneSemaphore.withPermit {
                try {
                    microRecordStream.startRecording()
                    initAndConnectClient()
                    try {
                        client?.sendStartFrame(translationType)
                        client?.sendAudioDateByStream(microRecordStream)
                    } catch (e: Exception) {
                        listener?.onError(e)
                        release()
                        coroutineScope.cancel()
                    }
                } catch (e: Exception) {
                    listener?.onError(e)
                }
            }
        }

    }

    /**
     * 开始语音识别和翻译过程，此接口支持多种语言互译。
     *
     * 此函数负责初始化并与服务端建立连接，然后开始语音帧的发送过程
     * 它接受两种语言参数，用于指定语音识别的源语言和目标语言
     *
     * @param languageFrom 源语言，表示期望识别的语音语言，见 Language 枚举。
     * @param languageTo 目标语言，表示期望将识别结果转换成的语言，见 Language 枚举。
     */
    fun startRecognition(languageFrom: Language, languageTo: Language) {

        // 检查是否有可用的许可，如果没有说明正在被使用
        if (AudioResourceManager.microphoneSemaphore.availablePermits == 0) {
            Timber.tag(TAG).d("Microphone is busy, skipping this recognition request")
            listener?.onError(Exception("The microphone is in use, please try again later."))
            coroutineScope.cancel()
            return
        }

        coroutineScope.launch {
            AudioResourceManager.microphoneSemaphore.withPermit {
                try {
                    microRecordStream.startRecording()
                    initAndConnectClient()
                    try {
                        client?.sendStartFrame(languageFrom, languageTo)
                        client?.sendAudioDateByStream(microRecordStream)
                    } catch (e: Exception) {
                        listener?.onError(e)
                        release()
                        coroutineScope.cancel()
                    }
                } catch (e: Exception) {
                    listener?.onError(e)
                }
            }
        }

    }

    /**
     * 初始化WebSocket客户端并建立连接
     */
    private fun initAndConnectClient() {
        val wsUrl = AssistUtils.wssParameter(ApiConfig.ASR_TRANSLATION_API)
        client = RtAsrClient(URI(wsUrl))
        client?.setMessageListener(object : WebSocketListener {
            override fun onMessageReceived(message: TranslationData?) {
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
                    try {
                        listener?.onClose(code, reason, remote)
                    } finally {
                        microRecordStream.close()
                        coroutineScope.cancel()
                    }
                }
            }

            override fun onError(ex: Exception?) {
                coroutineScope.launch(Dispatchers.Main) {
                    try {
                        listener?.onError(ex)
                    } finally {
                        microRecordStream.close()
                        coroutineScope.cancel()
                    }
                }
            }
        })
        try {
            client?.connectBlocking()
        } catch (e: Exception) {
            listener?.onError(e)
            release()
            coroutineScope.cancel()
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
                client = null
            } catch (e: Exception) {
                listener?.onError(e)
            } finally {
            }
        }

    }

}