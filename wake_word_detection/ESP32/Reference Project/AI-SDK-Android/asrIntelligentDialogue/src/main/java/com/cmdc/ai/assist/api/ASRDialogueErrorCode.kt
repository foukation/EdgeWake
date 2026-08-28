package com.cmdc.ai.assist.api

/**
 * 语音助手错误码定义
 *
 * 定义了语音助手对话过程中可能出现的各种错误，
 * 用于统一错误处理和状态反馈。
 */
object ASRDialogueErrorCode {
    /** 无录音权限 */
    const val ERROR_PERMISSION = 100

    /** 音频设备异常（被占用/初始化失败/读取异常） */
    const val ERROR_AUDIO_DEVICE = 101

    /** WebSocket 网络连接失败 */
    const val ERROR_NETWORK = 200

    /** WebSocket 连接超时 */
    const val ERROR_TIMEOUT = 201

    /** 数据发送失败（start/audio/finish 信号） */
    const val ERROR_SEND_FAILED = 202

    /** 服务端消息解析失败 */
    const val ERROR_PROTOCOL = 301

    /** SDK 内部未预期异常 */
    const val ERROR_INTERNAL = 500

    /** 配置文件加载失败 */
    private const val ERROR_CONFIG = 501

    /** 识别已在进行中，勿重复调用 */
    const val ERROR_ALREADY_RUNNING = 600

    /** 实例已释放，请重新创建 */
    const val ERROR_RELEASED = 601

    // ==================== 计费校验错误 (6xxx) ====================
    // 服务端 WebSocket 返回计费校验失败时使用，结构为：
    // {"code": 6403, "msg": "提示文案", "data": {"type": "billing_check_error", ...}}
    // SDK 将 code 作为错误码，完整 JSON 作为错误信息透传给调用方。

    /** 服务未订阅 */
    const val ERROR_BILLING_UNSUBSCRIBED = 6403

    /** 计费服务异常 */
    const val ERROR_BILLING_SERVICE_ERROR = 6404

    /** 服务已过期 */
    const val ERROR_BILLING_EXPIRED = 6405

    /** 服务未生效 */
    const val ERROR_BILLING_NOT_EFFECTIVE = 6406

    /** 用量已耗尽 */
    const val ERROR_BILLING_QUOTA_EXHAUSTED = 6407

    /** 计费系统内部错误 */
    const val ERROR_BILLING_INTERNAL = 6500
}
