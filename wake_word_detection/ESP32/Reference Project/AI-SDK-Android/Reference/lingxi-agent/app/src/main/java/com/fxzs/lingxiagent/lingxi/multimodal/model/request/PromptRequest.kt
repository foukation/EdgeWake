package com.ai.multimodal.model.request

/**
 * 表示发送给AI模型的提示请求
 */
data class PromptRequest(
    /**
     * 用户输入的文本
     */
    val query: String,

    /**
     * 系统提示，用于指导AI模型的行为
     * 可以包含变量占位符如 ${variable}，后续会被实际值替换
     */
    val medicine_info: String
) {}