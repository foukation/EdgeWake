package com.cmdc.ai.assist.aiModel

import com.cmdc.ai.assist.AIAssistantManager
import com.cmdc.ai.assist.api.IRecordStream
import com.cmdc.ai.assist.constraint.SpeechRecognitionPersistentAliYunData
import com.cmdc.ai.assist.utils.AudioEnergyUtils
import com.cmdc.ai.assist.utils.AudioFilePcmUtils
import com.cmdc.ai.assist.utils.AudioFileUtils
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.net.URI
import java.nio.ByteBuffer
import java.util.UUID
import kotlin.jvm.java

internal class AsrClientPersistentAliYun(serverUri: URI) : WebSocketClient(serverUri) {

    private val TAG = AsrClientPersistentAliYun::class.simpleName.toString()
    private val receiveBinaryData: MutableList<Byte?>
    private var mWebSocketListener: WebSocketListener? = null
    private val gson: Gson = GsonBuilder().create()

    // 音频采样率
    private val AUDIO_RATE: Int = 16000
    private val TIME_CUT = 160 // 每个音频段的时间间隔(毫秒)
    private val CHUNK_SIZE = 5120 // 音频分段大小(字节)

    // default 语言模型 ， 可以修改为其它语言模型测试，如远场普通话19362
    private val DEV_PID = 15372

    // 每帧音频数据时长: 毫秒
    private val AUDIO_SLICE_MS: Int = 40

    // vad 时间，默认 800 ms
    private val vad: Long = 800
    private val START_FRAME: MutableMap<String?, Any?> = object : HashMap<String?, Any?>() {
        init {
            put("vendor", "aliyun")
            put("format", "pcm")
            put("sample", AUDIO_RATE)
            put("cuid", AIAssistantManager.getInstance().aiAssistConfig.deviceId)
            put("dialog_request_id", UUID.randomUUID().toString())
            put("language", "zh-CN")
            put("vad", vad)
        }
    }
    private val FINISH_FRAME: MutableMap<String?, Any?> = object : HashMap<String?, Any?>() {
        init {
            put("type", "finish")
        }
    }

    private val CANCEL_FRAME: MutableMap<String?, Any?> = object : HashMap<String?, Any?>() {
        init {
            put("type", "cancel")
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
        /**
         * WebSocket 连接建立成功时回调。
         */
        fun onOpen()

        fun onMessageReceived(message: SpeechRecognitionPersistentAliYunData?)

        fun onMessageReceived(bytes: ByteBuffer?)

        fun onClose(code: Int, reason: String?, remote: Boolean)

        fun onError(ex: Exception?)
    }

    internal fun sendStartFrame(pid: Int?, vad: Long?) {

        if (pid != null) {
            START_FRAME["dev_pid"] = pid
        }

        if (vad != null) {
            START_FRAME["vad"] = vad
        }

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
            val jsonString = JSONObject.wrap(FINISH_FRAME)?.toString()
            send(jsonString)
            Timber.tag(TAG).d("%s%s", "sendFinishFrame  ", jsonString)
        } else {
            Timber.tag(TAG).d("WebSocket is not connected.")
        }
    }

    internal fun sendCancelFrame() {
        if (this.isOpen) {
            val jsonString = JSONObject.wrap(CANCEL_FRAME)?.toString()
            send(jsonString)
            Timber.tag(TAG).d("%s%s", "sendCancelFrame  ", jsonString)
        } else {
            Timber.tag(TAG).d("WebSocket is not connected.")
        }
    }

    internal fun sendAudioDataByStream(
        microStream: IRecordStream?,
        onAudioEnergy: ((Float) -> Unit)? = null
    ) {
        // 麦克风模式：实时读取并发送数据。
        //
        // 设计说明（避免"永久延迟"和"关流刷屏"两个问题）：
        //
        // 1) 不使用 Thread.sleep 做节拍。
        //    阻塞 read 本身就自带节拍：
        //      - 硬件缓冲有数据时立刻返回（用于清理建联期间积压的音频）；
        //      - 硬件缓冲空时阻塞 ~160ms 等麦克风采集（自然形成实时节奏）。
        //    使用 sleep 反而会让积压永远追不上 → 出现持续累积的延迟。
        //
        // 2) 当 read 返回 < 0（流被外部 close，audioRecord 已 null）时必须立刻 break，
        //    否则没有 sleep 的循环会以 ns 级速度反复调 read，
        //    底层 JNI 持续打印 "Unable to retrieve AudioRecord object" 刷屏。
        //
        // 3) microStream == null 或读写异常时同样 break，理由同上：避免循环空转。
        val buffer = ByteArray(CHUNK_SIZE)
        val audioBuffer = ByteArrayOutputStream()  // ← 1. 循环前声明

        while (this.isOpen) {
            // microStream 为空属于异常入参，没有继续循环的意义，直接退出避免死循环刷日志。
            if (microStream == null) {
                Timber.tag(TAG).d("sendAudioDateByStream microStream == null, exit loop")
                break
            }
            try {
                val bytesRead = microStream.read(buffer, 0, buffer.size)
                // 负值 = 录音流已被关闭（audioRecord 为 null），立刻退出循环，
                // 避免在 WebSocket 真正关闭前持续 CPU 空转、刷 JNI 报错日志。
                if (bytesRead < 0) {
                    Timber.tag(TAG)
                        .d("sendAudioDateByStream stream closed (read=$bytesRead), exit loop")
                    break
                }
                if (bytesRead > 0) {
                    audioBuffer.write(buffer, 0, bytesRead)  // ← 2. 循环内写入
                    onAudioEnergy?.invoke(AudioEnergyUtils.calculateEnergy(buffer, bytesRead))
                    send(buffer.validAudioFrame(bytesRead))
                }
                // 不再 Thread.sleep —— 阻塞 read 自带 160ms 节拍。
                //Thread.sleep(TIME_CUT.toLong())
            } catch (e: Exception) {
                Timber.tag(TAG).e("%s%s", "Error sending audio data ", e.message)
                // 任一读取/发送异常都视为不可恢复，跳出避免持续抛异常导致 CPU 空转。
                break
            }
        }
        // 仅在开启音频文件写入时保存
        if (AIAssistantManager.getInstance().aiAssistConfig.enableAudioFileSave) {
            saveAudioData(audioBuffer)  // ← 3. 循环结束后保存到磁盘
        }
    }

    /**
     * 返回本次 AudioRecord.read 实际读到的有效 PCM 数据。
     *
     * [ByteArray.size] 是请求读取的最大长度，[length] 才是本次实际有效长度。
     * 读满时直接复用原数组，避免实时音频上传循环中产生不必要的内存分配。
     */
    private fun ByteArray.validAudioFrame(length: Int): ByteArray {
        return if (length == size) this else copyOf(length)
    }

    /**
     * 保存持续识别过程中采集到的麦克风原始音频数据。
     *
     * 数据格式与录音源保持一致：16kHz、单声道、16-bit PCM。
     */
    private fun saveAudioData(audioBuffer: ByteArrayOutputStream) {
        val audioData = audioBuffer.toByteArray()
        val wavFilePath = AudioFileUtils.saveToWavFile(audioData, "SpeechRecognitionPersistentAliYun")
        val pcmFilePath = AudioFilePcmUtils.saveToPcmFile(audioData, "SpeechRecognitionPersistentAliYun")
        AIAssistantManager.getInstance().aiAssistConfig.currentPcmFilePath = pcmFilePath.toString()
    }

    internal fun sendAudioDataByStreamCompress(microStream: MicroRecordStream?) {

        val realTimeUploader = CompressUploader(microStream)
        realTimeUploader.sendAudioFrames(this)

    }

    override fun onOpen(handshakedata: ServerHandshake?) {
        Timber.tag(TAG).d("AsrClientPersistentAliYun Connected")
        mWebSocketListener?.onOpen()
    }

    override fun onMessage(message: String) {

        Timber.tag(TAG).d("AsrClientPersistentAliYun onMessage: %s", message)

        try {

            // 解析 JSON
            val jsonObject = JSONObject(message)
            val serverCode = jsonObject.optInt("err_no", 0)
            if (serverCode < 0
            ) {
                Timber.tag(TAG)
                    .e("[WebSocket.onMessage] 服务异常: code=$serverCode")
                if (mWebSocketListener != null) {
                    mWebSocketListener!!.onError(RuntimeException(message))
                }
                return
            }

            val resp =
                gson.fromJson(message, SpeechRecognitionPersistentAliYunData::class.java)

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
        //Log.d(TAG, "AsrClientPersistentAliYun onMessage: " + receiveBinaryData.toString());
        Timber.tag(TAG).d("AsrClientPersistentAliYun onMessage ByteBuffer %s", "接收到音频数据")
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        Timber.tag(TAG).d("AsrClientPersistentAliYun onClose")
        if (mWebSocketListener != null) {
            mWebSocketListener!!.onClose(code, reason, remote)
        }
    }

    fun disconnect() {
        if (this.isOpen) {
            this.close() // 关闭连接
            Timber.tag(TAG).d("AsrClientPersistentAliYun disconnect")
        }
    }

    override fun onError(ex: Exception) {
        Timber.tag(TAG).e("AsrClientPersistentAliYun Error: %s", ex.message)
        if (mWebSocketListener != null) {
            mWebSocketListener!!.onError(ex)
        }
    }

    fun getAudioBinaryData(): MutableList<Byte?> {
        return receiveBinaryData
    }

}
