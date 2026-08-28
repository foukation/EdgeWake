package com.cmdc.ai.assist.wakeup.config

import android.media.MediaRecorder
import java.io.Serializable
import java.util.Arrays
import java.util.Objects

/**
 * 唤醒意图配置。
 *
 * 在开启唤醒监听时传入，用于设置录音源、回声消除 / 降噪开关、是否手动喂入音频，
 * 以及自定义唤醒词列表等。本类为对底层唤醒引擎 `DuWakeupIntent` 的 1:1 复刻
 * （去除第三方品牌前缀 `Du`），字段名与默认值均与引擎保持一致，
 * 仅将命名空间替换为 `com.cmdc.ai.assist.wakeup.config`。
 */
class WakeupIntent : Serializable {

    /**
     * 录音源，取值同 [android.media.MediaRecorder.AudioSource]。
     * 默认 [android.media.MediaRecorder.AudioSource.VOICE_RECOGNITION]（值为 6），使用内部录音。
     */
    @JvmField
    var audioSource: Int = MediaRecorder.AudioSource.VOICE_RECOGNITION

    /**
     * 是否启用 AEC（回声消除）。仅在使用内置录音机时支持。默认 `false`。
     */
    @JvmField
    var isEnableAEC: Boolean = false

    /**
     * 是否启用 NS（降噪）。仅在使用内置录音机时支持。默认 `false`。
     */
    @JvmField
    var isEnableNS: Boolean = false

    /**
     * 是否手动传入音频。
     *
     * 默认 `false`，即由 SDK 内部自动录音；设为 `true` 时需由业务侧
     * 通过喂音接口手动传入 16k / 单声道 / 16bit 的 PCM 数据。
     */
    @JvmField
    var isUseFeedData: Boolean = false

    /**
     * 唤醒词列表。仅在自定义唤醒模式下需要设置；默认使用模型内置唤醒词“灵犀灵犀”。
     */
    @JvmField
    var wakeupWords: Array<String>? = null

    /**
     * 创建一份带默认值的唤醒意图。
     * 默认使用内部录音（`audioSource = VOICE_RECOGNITION`），AEC / NS / 手动喂音均关闭。
     */
    constructor()

    /**
     * 创建一份指定录音源的唤醒意图。
     *
     * @param audioSource 录音源，取值同 [android.media.MediaRecorder.AudioSource]
     */
    constructor(audioSource: Int) {
        this.audioSource = audioSource
    }

    /**
     * 创建一份指定是否手动喂音的唤醒意图。
     *
     * @param isUseFeedData 是否由业务侧手动传入音频
     */
    constructor(isUseFeedData: Boolean) {
        this.isUseFeedData = isUseFeedData
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as WakeupIntent
        return audioSource == that.audioSource &&
            isEnableAEC == that.isEnableAEC &&
            isEnableNS == that.isEnableNS &&
            isUseFeedData == that.isUseFeedData &&
            Arrays.equals(wakeupWords, that.wakeupWords)
    }

    override fun hashCode(): Int {
        var result = Objects.hash(audioSource, isEnableAEC, isEnableNS, isUseFeedData)
        result = 31 * result + Arrays.hashCode(wakeupWords)
        return result
    }

    override fun toString(): String =
        "WakeupIntent{" +
            "audioSource=$audioSource" +
            ", isEnableAEC=$isEnableAEC" +
            ", isEnableNS=$isEnableNS" +
            ", isUseFeedData=$isUseFeedData" +
            ", wakeupWords=${Arrays.toString(wakeupWords)}" +
            "}"
}
