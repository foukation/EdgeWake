package com.ai.multimodal.model.response

/**
 * 表示从AI模型接收到的响应
 */
data class PromptResponse(
    /**
     * 响应状态，如"success"或"error"
     */
    val state: String,

    /**
     * 响应的实际内容文本
     */
    val content: String,

    /**
     * 处理时间（以秒为单位）
     */
    val time: Double
) {
    /**
     * 判断响应是否成功
     */
    fun isSuccess(): Boolean = state == "success"
}