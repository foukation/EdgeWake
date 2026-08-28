package com.cmdc.ai.assist.constraint

/**
 * TTS（文本转语音）配置类
 * 统一管理语音合成的参数配置
 *
 * @property voiceId 音色 ID，不同的 ID 对应不同的发音人
 * @property speed 语速，取值范围 0-15，默认为 5（中速）
 * @property pitch 音调，取值范围 0-15，默认为 5（中音调）
 * @property volume 音量，取值范围 0-15，默认为 10（中音量）
 */
data class TtsConfig(
    val voiceId: Int = 4100,
    val speed: Int = 5,
    val pitch: Int = 5,
    val volume: Int = 15
) {
    companion object {
        /**
         * 默认配置 - 活力女主播
         */
        val DEFAULT = TtsConfig()
    }

    /**
     * 验证配置参数是否在有效范围内
     * @return true 如果所有参数都有效，false 否则
     */
    private fun isValid(): Boolean {
        return speed in 0..15 && pitch in 0..15 && volume in 0..15
    }
}

