package com.ai.multimodal.model.request

/**
 * 表示银发助手任务请求的数据结构
 */
data class ImageAssistantRequest(
    /**
     * 任务类型，例如："click_coordinate", "text_input" 等
     */
    val task_type: String,

    /**
     * 是否启用流式响应
     */
    val stream: Boolean,

    /**
     * 用户查询/指令内容
     */
    val query: String,

    /**
     * 可选的图像数据
     */
    val image: ImageData?
)

/**
 * 图像数据结构
 */
data class ImageData(
    /**
     * 图像类型，例如："png", "jpeg" 等
     */
    val type: String,

    /**
     * 图像宽度(像素)
     */
    val width: Int,

    /**
     * 图像高度(像素)
     */
    val height: Int,

    /**
     * Base64编码的图像数据
     */
    val img_encoded: String
)