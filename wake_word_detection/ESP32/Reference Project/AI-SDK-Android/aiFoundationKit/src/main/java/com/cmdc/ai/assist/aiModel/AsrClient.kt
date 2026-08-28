package com.cmdc.ai.assist.aiModel

import com.cmdc.ai.assist.AIAssistantManager
import com.cmdc.ai.assist.api.IRecordStream
import com.cmdc.ai.assist.constraint.SpeechRecognitionData
import com.google.gson.Gson
import com.google.gson.GsonBuilder
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

internal class AsrClient(serverUri: URI) : WebSocketClient(serverUri) {

    private val TAG = AsrClient::class.simpleName.toString()
    private val receiveBinaryData: MutableList<Byte?>
    private var mWebSocketListener: WebSocketListener? = null
    private val gson: Gson = GsonBuilder().create()

    // 音频采样率
    private val AUDIO_RATE: Int = 16000
    private val TIME_CUT = 160 // 每个音频段的时间间隔(毫秒)
    private val CHUNK_SIZE = 5120 // 音频分段大小(字节)

    // 每帧音频数据时长: 毫秒
    private val AUDIO_SLICE_MS: Int = 40
    private val START_FRAME: MutableMap<String?, Any?> = object : HashMap<String?, Any?>() {
        init {
            /*put("type", "start")*/
            /*put("format", "pcm")*/
            put("cuid", AIAssistantManager.getInstance().aiAssistConfig.deviceNo)
            put("format", "opus")
            put("sample", 16000)

            /*
             * VAD 为可选配置，单位 ms。仅在调用方显式配置时下发；
             * 未配置则省略该字段，继续使用服务端默认值。
             */
            AIAssistantManager.getInstance().aiAssistConfig.vad?.let { vad ->
                put("vad", vad)
            }

            /*put("appid", "gNCcuuUkSB63XXEK8Q1ljjPSBrxAqyPY")
            put("appkey", "GDZlYoLrLZ1jQCBq8W7jKS6aD0rxdB74")
            put("pid", "ne9U0")*/
        }
    }
    private val FINISH_FRAME: MutableMap<String?, Any?> = object : HashMap<String?, Any?>() {
        init {
            put("type", "finish")
        }
    }

    init {
        Timber.tag(TAG).d("AsrClient serverUri=%s", serverUri)
        receiveBinaryData = ArrayList<Byte?>()
        Timber.tag(TAG).d("AsrClient init end")
    }

    fun setMessageListener(listener: WebSocketListener?) {
        this.mWebSocketListener = listener
    }

    interface WebSocketListener {
        fun onMessageReceived(message: SpeechRecognitionData?)

        fun onMessageReceived(bytes: ByteBuffer?)

        fun onClose(code: Int, reason: String?, remote: Boolean)

        fun onError(ex: Exception?)
    }

    internal fun sendStartFrame() {

        val requestMap = mutableMapOf(
            "type" to "start",
            "data" to START_FRAME
        )
        val jsonString = Gson().toJson(requestMap)

        send(jsonString)
        Timber.tag(TAG).d("%s%s", "sendStartFrame  ", jsonString)
    }

    internal fun sendFinishFrame() {
        if (this.isOpen) {
            send(JSONObject.wrap(FINISH_FRAME)?.toString())
        } else {
            Timber.tag(TAG).d("WebSocket is not connected.")
        }
    }

    internal fun sendAudioDataByStream(microStream: MicroRecordStream?) {
        // 麦克风模式：实时读取并发送数据
        val buffer = ByteArray(CHUNK_SIZE)

        while (this.isOpen) {
            if (microStream != null) {
                try {
                    val bytesRead = microStream.read(buffer, 0, buffer.size)
                    Timber.tag(TAG).d("sendAudioDateByStream bytesRead= $bytesRead")
                    if (bytesRead > 0) {
                        Timber.tag(TAG).d("sendAudioDateByStream send buffer to client")
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

    internal fun sendAudioDataByStreamCompress(microStream: IRecordStream?) {

        val realTimeUploader = CompressUploader(microStream)
        realTimeUploader.sendAudioFrames(this)

    }

    override fun onOpen(handshakedata: ServerHandshake?) {
        Timber.tag(TAG).d("AsrClient Connected")
    }

    override fun onMessage(message: String) {

        Timber.tag(TAG).d("AsrClient onMessage: %s", message)

        try {

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

            val resp =
                gson.fromJson(message, SpeechRecognitionData::class.java)

            if (mWebSocketListener != null) {
                mWebSocketListener!!.onMessageReceived(resp)
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
        //Log.d(TAG, "AsrClient onMessage: " + receiveBinaryData);
        //Log.d(TAG, "AsrClient onMessage: " + receiveBinaryData.toString());
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        Timber.tag(TAG).d("AsrClient onClose")
        if (mWebSocketListener != null) {
            mWebSocketListener!!.onClose(code, reason, remote)
        }
    }

    fun disconnect() {
        if (this.isOpen) {
            this.close() // 关闭连接
            Timber.tag(TAG).d("AsrClient disconnect")
        }
    }

    override fun onError(ex: Exception) {
        Timber.tag(TAG).e("AsrClient Error: %s", ex.message)
        if (mWebSocketListener != null) {
            mWebSocketListener!!.onError(ex)
        }
    }

    fun getAudioBinaryData(): MutableList<Byte?> {
        return receiveBinaryData
    }

}
