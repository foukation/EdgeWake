package com.cmdc.ai.assist.wakeup.config

import java.io.Serializable

/**
 * 唤醒词模型配置。
 *
 * 用于指定唤醒词模型文件的位置。默认情况下引擎使用 SDK 内置的模型资源，
 * 也可以通过 [modelPath] 指定外部模型文件的绝对路径。
 *
 * 本类为对底层唤醒引擎 `WakeWordModel` 的 1:1 复刻，字段名保持一致，
 * 仅将命名空间替换为 `com.cmdc.ai.assist.wakeup.config`。
 */
class WakeWordModel : Serializable {

    /**
     * 模型文件绝对路径。
     *
     * 默认值为 `null`，表示使用 SDK 内置模型资源；
     * 如需使用外部模型文件，请设置为该文件的绝对路径。
     */
    @JvmField
    var modelPath: String? = null

    override fun toString(): String = "WakeWordModel{modelPath='$modelPath'}"
}
