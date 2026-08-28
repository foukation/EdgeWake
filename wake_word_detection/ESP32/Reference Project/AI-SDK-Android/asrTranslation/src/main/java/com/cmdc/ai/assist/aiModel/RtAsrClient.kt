package com.cmdc.ai.assist.aiModel

import com.cmdc.ai.assist.api.IRecordStream
import com.cmdc.ai.assist.constraint.Language
import com.cmdc.ai.assist.constraint.TranslationData
import com.cmdc.ai.assist.constraint.TranslationTypeCode
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.lang.Exception
import java.net.URI
import java.nio.ByteBuffer
import java.util.ArrayList
import java.util.HashMap

internal class RtAsrClient(serverUri: URI) : WebSocketClient(serverUri) {

    private val TAG = "RtAsrClient"
    private val receiveBinaryData: MutableList<Byte?>
    private var mWebSocketListener: WebSocketListener? = null

    // 音频采样率
    private val AUDIO_RATE: Int = 16000

    // private val TIME_CUT = 160 // 每个音频段的时间间隔(毫秒)
    private val TIME_CUT = 40 // 每个音频段的时间间隔(毫秒)

    // private val CHUNK_SIZE = 5120 // 音频分段大小(字节)
    private val CHUNK_SIZE = 1280  // 音频分段大小(字节)

    // 每帧音频数据时长: 毫秒
    private val AUDIO_SLICE_MS: Int = 40

    private val START_FRAME_EN: MutableMap<String?, Any?> = object : HashMap<String?, Any?>() {
        init {
            put("type", "START")
            put("from", "zh")
            put("to", "en")
            put("sampling_rate", AUDIO_RATE)
            /*put("app_id", APP_ID);
            put("app_key", APP_KEY);*/
            put("return_target_tts", true)
            put("tts_speaker", "man")
        }
    }
    private val START_FRAME_CHINESE: MutableMap<String?, Any?> = object : HashMap<String?, Any?>() {
        init {
            put("type", "START")
            put("from", "en")
            put("to", "zh")
            put("sampling_rate", AUDIO_RATE)
            /*put("app_id", APP_ID);
            put("app_key", APP_KEY);*/
            put("return_target_tts", true)
            put("tts_speaker", "man")
        }
    }

    private val FINISH_FRAME: MutableMap<String?, Any?> = object : HashMap<String?, Any?>() {
        init {
            put("type", "FINISH")
        }
    }

    init {
        Timber.tag(TAG).d("RtAsrClient serverUri=%s", serverUri)
        receiveBinaryData = ArrayList<Byte?>()
        Timber.tag(TAG).d("RtAsrClient init end")
    }

    fun setMessageListener(listener: WebSocketListener?) {
        this.mWebSocketListener = listener
    }

    interface WebSocketListener {
        fun onMessageReceived(message: TranslationData?)

        fun onMessageReceived(bytes: ByteBuffer?)

        fun onClose(code: Int, reason: String?, remote: Boolean)

        fun onError(ex: Exception?)
    }

    internal fun sendStartFrame(translationType: TranslationTypeCode) {
        if (translationType.code == TranslationTypeCode.ZH_TO_EN.code) {
            send(JSONObject.wrap(START_FRAME_EN)?.toString())
            Timber.tag(TAG).d("sendStartFrame translationType ZH_TO_EN")
            return
        }
        if (translationType.code == TranslationTypeCode.EN_TO_ZH.code) {
            send(JSONObject.wrap(START_FRAME_CHINESE)?.toString())
            Timber.tag(TAG).d("sendStartFrame translationType EN_TO_ZH")
        }
    }

    internal fun sendStartFrame(languageFrom: Language, languageTo: Language) {

        val START_FRAME_LANGUAGE: MutableMap<String?, Any?> = object : HashMap<String?, Any?>() {
            init {
                put("type", "START")
                /*put("from", "en")*/
                /*put("to", "zh")*/
                put("sampling_rate", AUDIO_RATE)
                /*put("app_id", APP_ID);
                put("app_key", APP_KEY);*/
                put("return_target_tts", true)
                put("tts_speaker", "man")
            }
        }

        START_FRAME_LANGUAGE.put("from", languageFrom.languageCode)
        START_FRAME_LANGUAGE.put("to", languageTo.languageCode)
        send(JSONObject.wrap(START_FRAME_LANGUAGE)?.toString())
        Timber.tag(TAG)
            .d("sendStartFrame translationType ${languageFrom.languageCode} To ${languageTo.languageCode}")
    }

    internal fun sendFinishFrame() {
        if (this.isOpen) {
            send(JSONObject.wrap(FINISH_FRAME)?.toString())
        } else {
            Timber.tag(TAG).d("WebSocket is not connected.")
        }
    }

    internal fun sendAudioDateByStream(microStream: IRecordStream?) {
        // 麦克风模式：实时读取并发送数据
        val buffer = ByteArray(CHUNK_SIZE)

        while (this.isOpen) {
            if (microStream != null) {
                try {
                    val bytesRead = microStream.read(buffer, 0, buffer.size)
                    /*Timber.tag(TAG).d("sendAudioDateByStream bytesRead= $bytesRead")*/
                    if (bytesRead > 0) {
                        /*Timber.tag(TAG).d("sendAudioDateByStream send buffer to client")*/
                        send(buffer)
                    }
                    Thread.sleep(TIME_CUT.toLong())
                } catch (e: Exception) {
                    Timber.tag(TAG).e("%s%s", "Error sending audio data", e.message)
                }
            } else {
                Timber.tag(TAG).d("sendAudioDateByStream microStream != null")
            }
        }
    }

    override fun onOpen(handshakedata: ServerHandshake?) {
        Timber.tag(TAG).d("RtAsrClient Connected")
    }

    override fun onMessage(message: String) {
        try {
            Timber.tag(TAG).d("RtAsrClient onMessage: %s", message)

            // 解析 JSON
            val jsonObject = JSONObject(message)
            val serverCode = jsonObject.optInt("code", 0)
            if (serverCode in 4000..4999 ||
                serverCode in 6000..6999 ||
                serverCode in 2000000000..2099999999
            ) {
                Timber.tag(TAG)
                    .e("[WebSocket.onMessage] 计费错误: code=$serverCode")
                if (mWebSocketListener != null) {
                    mWebSocketListener!!.onError(RuntimeException(message))
                }
                return
            }

            // 检查 "msg" 是否为 "success"
            if (jsonObject.getString("msg") == "success") {
                // 获取 "data" 对象
                val data = jsonObject.getJSONObject("data")

                val status = data.getString("status")

                if (status == "TRN") {
                    // 获取 "result" 对象
                    val result = data.getJSONObject("result")

                    val asr = result.getString("asr")
                    val asrTrans = result.getString("asr_trans")
                    val sentence = result.getString("sentence")
                    val sentence_trans = result.getString("sentence_trans")
                    val type = result.getString("type")

                    val translationData = TranslationData()
                    translationData.type = type
                    if (type == "MID") {
                        translationData.asrResult = asr
                        translationData.translationResult = asrTrans
                    }
                    if (type == "FIN") {
                        translationData.asrResult = sentence
                        translationData.translationResult = sentence_trans
                    }

                    // 更新 UI
                    if (mWebSocketListener != null) {
                        mWebSocketListener!!.onMessageReceived(translationData) // 通知 MainActivity
                    }
                }
            }
        } catch (e: JSONException) {
            Timber.tag(TAG).d("Error parsing JSON %s", e.message)
        }
    }

    override fun onMessage(bytes: ByteBuffer) {
        // 返回 pcm 格式的 tts 音频播报数据

        /*val temp = bytes.array()
        for (i in 1 until temp.size) {
            receiveBinaryData.add(temp[i])
        }*/

        if (mWebSocketListener != null) {
            mWebSocketListener!!.onMessageReceived(bytes) // 通知 MainActivity
        }
        //Log.d(TAG, "RtAsrClient onMessage: " + receiveBinaryData);
        //Log.d(TAG, "RtAsrClient onMessage: " + receiveBinaryData.toString());
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        Timber.tag(TAG).d("RtAsrClient onClose")
        if (mWebSocketListener != null) {
            mWebSocketListener!!.onClose(code, reason, remote)
        }
    }

    fun disconnect() {
        if (this.isOpen) {
            this.close() // 关闭连接
            Timber.tag(TAG).d("RtAsrClient disconnect")
        }
    }

    override fun onError(ex: Exception) {
        Timber.tag(TAG).e("RtAsrClient Error: %s", ex.message)
        if (mWebSocketListener != null) {
            mWebSocketListener!!.onError(ex)
        }
    }

    fun getAudioBinaryData(): MutableList<Byte?> {
        return receiveBinaryData
    }

}
