package com.cmdc.ai.assist.utils

import android.os.Environment
import com.cmdc.ai.assist.AIAssistantManager
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AudioFileUtils {
    private const val TAG = "AudioFileUtils"
    private const val RIFF_HEADER_SIZE = 44
    private const val RECORDER_SAMPLERATE = 16000
    private const val RECORDER_CHANNELS = 1
    private const val RECORDER_BPP = 16

    // 指定保存路径
    /*private const val SAVE_PATH = "/sdcard/Android/data/com.cmdc.ai.assist.demo/files/audio_records"*/
    /*private const val SAVE_PATH = "/sdcard/Android/data/audio_records/"*/
    private var SAVE_PATH =
        Environment.getExternalStorageDirectory().absolutePath + "/audio_records/"

    /**
     * 将音频数据保存为WAV文件
     *
     * @param audioData 音频数据的字节数组
     * @return 保存的文件路径，如果保存失败则返回null
     */
    @JvmStatic
    fun saveToWavFile(audioData: ByteArray, modulePath: String): String? {
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
                "audio_${timestamp}_${AIAssistantManager.getInstance().aiAssistConfig.sn}.wav"
            )

            FileOutputStream(audioFile).use { out ->
                // 写入WAV文件头
                writeWavHeader(out, audioData.size.toLong())
                // 写入音频数据
                out.write(audioData)
                // 回到文件开始处更新文件大小
                updateWavHeader(audioFile, audioData.size.toLong())
            }

            Timber.tag(TAG).d("Audio file saved: ${audioFile.absolutePath}")
            return audioFile.absolutePath

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error saving audio file")
            return null
        }
    }

    /**
     * 写入WAV文件头
     */
    private fun writeWavHeader(out: FileOutputStream, audioLength: Long) {
        val totalDataLen = audioLength + 36
        val byteRate = (RECORDER_BPP * RECORDER_SAMPLERATE * RECORDER_CHANNELS / 8).toLong()

        val header = ByteArray(RIFF_HEADER_SIZE)
        header[0] = 'R'.code.toByte() // RIFF
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte() // file length
        header[5] = (totalDataLen shr 8 and 0xff).toByte()
        header[6] = (totalDataLen shr 16 and 0xff).toByte()
        header[7] = (totalDataLen shr 24 and 0xff).toByte()
        header[8] = 'W'.code.toByte() // WAVE
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte() // fmt
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // 4 bytes: size of 'fmt ' chunk
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // format = 1 (PCM)
        header[21] = 0
        header[22] = RECORDER_CHANNELS.toByte() // channels
        header[23] = 0
        header[24] = (RECORDER_SAMPLERATE and 0xff).toByte() // sample rate
        header[25] = (RECORDER_SAMPLERATE shr 8 and 0xff).toByte()
        header[26] = (RECORDER_SAMPLERATE shr 16 and 0xff).toByte()
        header[27] = (RECORDER_SAMPLERATE shr 24 and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte() // byte rate
        header[29] = (byteRate shr 8 and 0xff).toByte()
        header[30] = (byteRate shr 16 and 0xff).toByte()
        header[31] = (byteRate shr 24 and 0xff).toByte()
        header[32] = (RECORDER_CHANNELS * RECORDER_BPP / 8).toByte() // block align
        header[33] = 0
        header[34] = RECORDER_BPP.toByte() // bits per sample
        header[35] = 0
        header[36] = 'd'.code.toByte() // data
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (audioLength and 0xff).toByte() // data length
        header[41] = (audioLength shr 8 and 0xff).toByte()
        header[42] = (audioLength shr 16 and 0xff).toByte()
        header[43] = (audioLength shr 24 and 0xff).toByte()

        out.write(header, 0, RIFF_HEADER_SIZE)
    }

    /**
     * 更新WAV文件头中的文件大小信息
     */
    private fun updateWavHeader(file: File, audioLength: Long) {
        val totalDataLen = audioLength + 36

        FileOutputStream(file, true).use { out ->
            out.channel.position(4)
            out.write((totalDataLen and 0xff).toByte().toInt())
            out.write((totalDataLen shr 8 and 0xff).toByte().toInt())
            out.write((totalDataLen shr 16 and 0xff).toByte().toInt())
            out.write((totalDataLen shr 24 and 0xff).toByte().toInt())

            out.channel.position(40)
            out.write((audioLength and 0xff).toByte().toInt())
            out.write((audioLength shr 8 and 0xff).toByte().toInt())
            out.write((audioLength shr 16 and 0xff).toByte().toInt())
            out.write((audioLength shr 24 and 0xff).toByte().toInt())
        }
    }
} 