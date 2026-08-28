package com.cmdc.ai.assist.wakeup.config

import java.io.Serializable

/**
 * 唤醒行为配置。
 *
 * 控制唤醒检测的灵敏度、时间窗口、调试选项等行为参数。
 * 本类为对底层唤醒引擎 `WakeConfig` 的 1:1 复刻，字段名与默认值均与引擎保持一致，
 * 仅将命名空间替换为 `com.cmdc.ai.assist.wakeup.config`。
 */
class WakeConfig : Serializable {

    /**
     * 最大唤醒时间窗口（毫秒）。窗口内最多触发一次唤醒，用于防止连续误触。默认 `1900`。
     */
    @JvmField
    var wakeInterval: Int = 1900

    /**
     * 唤醒阈值，取值范围 [0.0, 1.0]。数值越大越不易触发（越严格）。默认 `0.9`。
     */
    @JvmField
    var wakeThreshold: Float = 0.9f

    /**
     * 是否保存唤醒时刻的音频（用于问题排查）。默认 `false`。
     */
    @JvmField
    var saveWakeupAudio: Boolean = false

    /**
     * 唤醒音频保存路径。仅在 [saveWakeupAudio] 为 `true` 时有效。默认 `null`。
     */
    @JvmField
    var wakeupAudioPath: String? = null

    /**
     * 延迟触发时长（毫秒）。默认 `1000`。
     */
    @JvmField
    var delayTriggerDuration: Int = 1000

    /**
     * 检测窗口帧数。默认 `40`。
     */
    @JvmField
    var detectionWindowsFrames: Int = 40

    /**
     * 达到阈值的连续帧数。默认 `1`。
     */
    @JvmField
    var thresholdFramesCount: Int = 1

    /**
     * 自定义唤醒词阈值，取值范围 [0.0, 1.0]。仅在自定义唤醒词模式下使用。默认 `0.1`。
     */
    @JvmField
    var customWakeThreshold: Float = 0.1f

    override fun toString(): String =
        "WakeConfig{" +
            "wakeInterval=$wakeInterval" +
            ", wakeThreshold=$wakeThreshold" +
            ", saveWakeupAudio=$saveWakeupAudio" +
            ", wakeupAudioPath='$wakeupAudioPath'" +
            ", delayTriggerDuration=$delayTriggerDuration" +
            ", detectionWindowsFrames=$detectionWindowsFrames" +
            ", thresholdFramesCount=$thresholdFramesCount" +
            ", customWakeThreshold=$customWakeThreshold" +
            "}"
}
