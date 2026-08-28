package com.cmdc.ai.assist.utils

import android.os.Environment
import com.cmdc.ai.assist.AIAssistantManager
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AudioFilePcmUtils {
    private const val TAG = "AudioFilePcmUtils"
    private const val RECORDER_SAMPLERATE = 16000
    private const val RECORDER_CHANNELS = 1
    private const val RECORDER_BPP = 16

    // 指定保存路径
    private var SAVE_PATH =
        Environment.getExternalStorageDirectory().absolutePath + "/audio_records/"

    /**
     * 将音频数据保存为PCM文件
     *
     * @param audioData 音频数据的字节数组
     * @return 保存的文件路径，如果保存失败则返回null
     */
    @JvmStatic
    fun saveToPcmFile(audioData: ByteArray, modulePath: String): String? {
        try {

            val cacheDir: File = AIAssistantManager.getInstance().aiAssistConfig.context!!.cacheDir
            val cachePath: String = cacheDir.absolutePath + "/" + modulePath
            SAVE_PATH = cachePath
            // 创建保存音频的目录
            val directory = File(SAVE_PATH)
            if (!directory.exists()) {
                directory.mkdirs()
            }

            // 使用时间戳创建文件名
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val audioFile = File(
                directory,
                "audio_${timestamp}_${AIAssistantManager.getInstance().aiAssistConfig.sn}.pcm"
            )

            FileOutputStream(audioFile).use { out ->
                // PCM格式直接写入原始音频数据，不需要文件头
                out.write(audioData)
            }

            Timber.tag(TAG).d("PCM audio file saved: ${audioFile.absolutePath}")
            return audioFile.absolutePath

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error saving PCM audio file")
            return null
        }
    }

    /**
     * 获取PCM文件的音频格式信息
     * 这个方法用于记录PCM文件的格式信息，因为PCM文件本身不包含格式信息
     */
    fun getPcmFormat(): String {
        return "Sample Rate: $RECORDER_SAMPLERATE Hz, " +
                "Channels: $RECORDER_CHANNELS, " +
                "Bits Per Sample: $RECORDER_BPP"
    }
}