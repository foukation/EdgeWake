package com.cmdc.ai.assist.utils

import timber.log.Timber
import kotlin.math.abs
import kotlin.math.log10
import kotlin.text.format

/**
 * 音频增益处理器（修复版 - 使用全局固定增益）
 *
 * 修复内容：
 * - 使用全局固定增益，避免逐块增益变化
 * - 消除"呼吸效应"和"咔嚓声"杂音
 *
 * 核心改进：
 * 1. 收集前3秒音频数据
 * 2. 计算全局安全增益（只计算一次）
 * 3. 所有后续音频块使用相同的固定增益
 *
 * @property targetGainCoefficient 目标增益系数（默认 4.41 倍）
 * @property safetyMargin 安全余量（0.95 = 留5%防止削波）
 */
class AudioGainProcessor(
    private val targetGainCoefficient: Float = 4.41f,
    private val safetyMargin: Float = 0.95f
) {

    private val TAG = "AudioGainProcessor"

    // 全局固定增益（关键：所有块使用同一个增益）
    private var globalGain: Float = targetGainCoefficient
    private var isGainCalculated = false

    // 初始缓冲区（用于计算全局安全增益）
    private val initialBuffer = mutableListOf<Short>()
    private val initialBufferDuration = 3.0f  // 收集3秒音频
    private val sampleRate = 16000  // 假设16kHz采样率
    private val initialBufferSize = (sampleRate * initialBufferDuration).toInt()

    // 统计信息
    private var processedBlockCount = 0

    /**
     * 处理单个音频块（实时流处理 - 就地修改）
     *
     * @param buffer 字节数组（16位PCM小端序）
     * @param length 有效数据长度
     * @return 是否处理成功
     */
    fun processBlockInPlace(buffer: ByteArray, length: Int): Boolean {
        try {
            // 1. ByteArray → ShortArray
            val sampleCount = length / 2
            val audioData = ShortArray(sampleCount)

            for (i in 0 until sampleCount) {
                val byteIndex = i * 2
                if (byteIndex + 1 < length) {
                    // 小端序：低字节在前
                    val low = buffer[byteIndex].toInt() and 0xFF
                    val high = buffer[byteIndex + 1].toInt()
                    audioData[i] = ((high shl 8) or low).toShort()
                }
            }

            // 2. 如果还在收集初始数据，缓存音频
            if (!isGainCalculated && initialBuffer.size < initialBufferSize) {
                initialBuffer.addAll(audioData.toList())

                // 收集够3秒后，计算全局安全增益
                if (initialBuffer.size >= initialBufferSize) {
                    calculateGlobalGain()
                }
            }

            // 3. 应用全局固定增益（关键：所有块使用同一个增益）
            val processed = ShortArray(sampleCount) { i ->
                val gained = audioData[i] * globalGain
                gained.toInt().coerceIn(-32768, 32767).toShort()
            }

            // 4. ShortArray → ByteArray（就地修改）
            for (i in 0 until sampleCount) {
                val sample = processed[i].toInt()
                val byteIndex = i * 2
                if (byteIndex + 1 < length) {
                    // 小端序写回
                    buffer[byteIndex] = (sample and 0xFF).toByte()
                    buffer[byteIndex + 1] = (sample shr 8).toByte()
                }
            }

            processedBlockCount++
            return true

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "音频增益处理失败")
            return false
        }
    }

    /**
     * 计算全局安全增益（只计算一次）
     */
    private fun calculateGlobalGain() {
        // 计算初始音频的峰值
        val peak = initialBuffer.maxOf { abs(it.toInt()) }
        val maxAllowedPeak = 32767 * safetyMargin

        // 检查目标增益是否会导致削波
        globalGain = if (peak * targetGainCoefficient > maxAllowedPeak) {
            // 会削波 - 计算安全增益
            (maxAllowedPeak / peak).toFloat()
        } else {
            // 不会削波 - 使用目标增益
            targetGainCoefficient
        }

        isGainCalculated = true

        Timber.tag(TAG).i("========== 全局增益已计算 ==========")
        Timber.tag(TAG).i("  初始音频峰值: $peak")
        Timber.tag(TAG).i("  目标增益系数: ${targetGainCoefficient}x")
        Timber.tag(TAG).i("  实际增益系数: ${globalGain}x")
        Timber.tag(TAG).i("  增益dB: ${"%.1f".format(20 * log10(globalGain))} dB")
        if (globalGain < targetGainCoefficient) {
            Timber.tag(TAG).w("  ⚠️ 增益已调整以防止削波")
        } else {
            Timber.tag(TAG).i("  ✓ 增益安全，无削波风险")
        }
        Timber.tag(TAG).i("====================================")
    }

    /**
     * 获取处理统计信息
     */
    fun getStatistics(): ProcessingStatistics {
        return ProcessingStatistics(
            processedBlockCount = processedBlockCount,
            globalGain = globalGain,
            isGainCalculated = isGainCalculated,
            targetGain = targetGainCoefficient,
            safetyMargin = safetyMargin
        )
    }

    /**
     * 重置统计信息
     */
    fun resetStatistics() {
        processedBlockCount = 0
        globalGain = targetGainCoefficient
        isGainCalculated = false
        initialBuffer.clear()
    }

    /**
     * 处理统计信息数据类
     */
    data class ProcessingStatistics(
        val processedBlockCount: Int,
        val globalGain: Float,
        val isGainCalculated: Boolean,
        val targetGain: Float,
        val safetyMargin: Float
    ) {
        override fun toString(): String {
            return """
                |音频增益处理统计:
                |  已处理块数: $processedBlockCount
                |  全局增益: ${globalGain}x (${"%.1f".format(20 * log10(globalGain))} dB)
                |  目标增益: ${targetGain}x (${"%.1f".format(20 * log10(targetGain))} dB)
                |  增益状态: ${if (isGainCalculated) "已计算" else "计算中..."}
                |  安全余量: ${safetyMargin}
            """.trimMargin()
        }
    }
}