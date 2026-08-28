package com.cmdc.ai.assist.constraint

/**
 * @brief ASR 识别结果结构体
 *
 * 包含语音识别的中间结果或最终结果。
 * 在语音识别过程中，服务器会多次返回识别结果：
 * - 中间结果（isFinal = false）：实时显示识别进度
 * - 最终结果（isFinal = true）：确认最终识别内容
 */
data class AsrResult(

    /**
     * 识别文本内容
     * 包含当前识别出的语音文本
     *
     * 示例：
     * - 中间结果："你好"、"你好小智"
     * - 最终结果："你好小智今天天气怎么样"
     */
    val text: String,

    /**
     * 是否为最终结果
     * - false：中间识别结果，会持续更新
     * - true：最终确认结果，识别结束
     *
     * 建议：
     * - 中间结果用于实时显示（提升用户体验）
     * - 最终结果用于执行指令
     */
    val isFinal: Boolean,

    /**
     * 情绪标签（仅最终结果包含）
     * 由服务器根据语音内容和语义分析返回的情绪标签。
     * 仅在 fin_result（is_final = true）中存在，mid_result 不返回此字段。
     * 为空字符串表示服务器未返回情绪标签。
     *
     * 可能的值：
     * "happy", "angry", "dejected", "wronged", "thingking",
     * "terrified", "smirk", "confused", "bored", "dizzy", "chaos", "wink"
     */
    val emotion: String

)