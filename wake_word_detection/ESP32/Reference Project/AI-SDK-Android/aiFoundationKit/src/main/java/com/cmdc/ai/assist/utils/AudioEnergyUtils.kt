package com.cmdc.ai.assist.utils

/**
 * PCM 音频能量计算工具。
 */
internal object AudioEnergyUtils {

    private const val PCM_16BIT_FULL_SCALE = 32768.0 // 16-bit PCM 满幅值，用于换算 dBFS。
    private const val DBFS_EPSILON = 1e-9 // 避免静音帧计算 log10(0)。
    private const val MIN_DBFS = -60.0 // 低于该能量按静音处理。
    private const val MAX_DBFS = -20.0 // 高于该能量按满格处理。

    /**
     * 基于 16-bit little-endian PCM 计算当前音频帧的归一化能量。
     *
     * 计算流程：PCM 采样值 -> RMS -> dBFS -> 映射到 0.0f ~ 1.0f。
     * [length] 必须使用 AudioRecord.read 的实际返回值，避免把缓冲区尾部的无效数据计入能量。
     *
     * @param pcm PCM 音频缓冲区。
     * @param length 本次实际读取到的有效字节数。
     * @return 归一化后的能量值，范围为 0.0f ~ 1.0f。
     */
    fun calculateEnergy(pcm: ByteArray, length: Int): Float {
        if (length < 2) {
            return 0f
        }

        val sampleCount = length / 2
        var sumSq = 0.0

        for (i in 0 until sampleCount) {
            val index = i * 2
            val sample = ((pcm[index].toInt() and 0xFF) or (pcm[index + 1].toInt() shl 8)).toShort()
            sumSq += sample.toDouble() * sample.toDouble()
        }

        val rms = kotlin.math.sqrt(sumSq / sampleCount)
        val dbfs = 20.0 * kotlin.math.log10(rms / PCM_16BIT_FULL_SCALE + DBFS_EPSILON)
        val normalized = (dbfs - MIN_DBFS) / (MAX_DBFS - MIN_DBFS)

        return normalized.coerceIn(0.0, 1.0).toFloat()
    }
}
