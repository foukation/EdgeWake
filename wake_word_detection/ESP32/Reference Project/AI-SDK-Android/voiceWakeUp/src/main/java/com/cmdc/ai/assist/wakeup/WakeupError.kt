package com.cmdc.ai.assist.wakeup

import java.io.Serializable

/**
 * 唤醒错误信息。
 *
 * 封装唤醒过程中的错误码与错误消息。本类为对底层唤醒引擎 `DuWakeupError`
 * 的 1:1 复刻（去除第三方品牌前缀 `Du`），字段、错误码与预设常量均与引擎保持一致，
 * 仅将命名空间替换为 `com.cmdc.ai.assist.wakeup`。
 *
 * @property code    错误码
 * @property message 错误消息
 */
class WakeupError(
    @JvmField val code: Int,
    @JvmField val message: String
) : Serializable {

    override fun toString(): String = "WakeupError{code=$code, message='$message'}"

    companion object {

        /** 模型初始化失败（模型文件不存在），错误码 `-100`。 */
        @JvmField
        val ERROR_INIT_MODEL = WakeupError(-100, "model is not exists, init error")

        /** 未初始化就调用开始监听，错误码 `-200`。 */
        @JvmField
        val ERROR_START_UN_INIT = WakeupError(-200, "state is not init")

        /** 启动录音失败，错误码 `-201`。 */
        @JvmField
        val ERROR_START_RECORD = WakeupError(-201, "start record error")

        /** 启动唤醒失败，错误码 `-202`。 */
        @JvmField
        val ERROR_START_WAKEUP = WakeupError(-202, "start wakeup error")

        /** 录音数据错误，错误码 `-201`。 */
        @JvmField
        val ERROR_RECORD_DATA = WakeupError(-201, "record data error")

        /** 未初始化就调用停止监听，错误码 `-300`。 */
        @JvmField
        val ERROR_STOP_UN_INIT = WakeupError(-300, "state is not init")
    }
}
