package com.ai.multimodal.model.response

/**
 * 银发助手API响应的基础数据结构
 */
data class ImageAssistantResponse(
    /**
     * 响应状态："success" 或 "error"
     */
    val state: String,

    /**
     * 响应内容，是一个JSON字符串，需要根据具体业务场景自行解析
     */
    val content: String,

    /**
     * 处理时间(秒)
     */
    val time: Double
)
