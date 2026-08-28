package com.cmdc.ai.assist.wakeup.config

import java.io.Serializable

/**
 * 唤醒引擎初始化配置。
 *
 * 在调用初始化接口时传入，用于设置唤醒词模式、模型以及唤醒行为参数。
 * 本类为对底层唤醒引擎 `WakeupInitConfig` 的 1:1 复刻，字段名与默认值均与引擎保持一致，
 * 仅将命名空间替换为 `com.cmdc.ai.assist.wakeup.config`。
 */
class WakeupInitConfig : Serializable {

    /**
     * 是否使用自定义唤醒词。
     *
     * 默认 `false`，即使用模型内置的默认唤醒词“灵犀灵犀”。
     * 若设为 `true`，需在唤醒意图中提供自定义唤醒词列表。
     */
    internal var isCustomWords: Boolean = false

    /**
     * 唤醒词模型配置。默认使用 SDK 内置模型。
     */
    internal var wakeWordModel: WakeWordModel = WakeWordModel()

    /**
     * 唤醒行为配置（阈值、时间窗口等）。
     */
    @JvmField
    var wakeConfig: WakeConfig = WakeConfig()

    override fun toString(): String =
        "WakeupInitConfig{" +
                "isCustomWords=$isCustomWords" +
                ", wakeWordModel=$wakeWordModel" +
                ", wakeConfig=$wakeConfig" +
                "}"
}
