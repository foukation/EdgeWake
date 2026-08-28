package com.cmdc.ai.assist.constraint

import com.google.gson.annotations.SerializedName

/**
 * 表示大模型闲聊的非流式响应
 */
data class ChatbotCompletionResponse(
    /**
     * 响应的唯一ID
     * */
    val id: String,
    /**
     * 对象类型标识
     * */
    @SerializedName("object")
    val objType: String,
    /**
     * 响应创建时间的时间戳
     * */
    val created: Long,
    /**
     * 使用的模型名称
     * */
    val model: String,
    /**
     * 回复
     * */
    val choices: List<ChatbotChoice>,
    /**
     * 令牌使用统计
     * */
    val usage: TokensUsage
)

/**
 * 表示响应中的一个选择项
 */
data class ChatbotChoice(
    /**
     * 索引，从 0 开始
     * */
    val index: Int,
    /**
     * 流式返回的消息
     * */
    val delta: ChatbotMessageForResponse,
    /**
     * 非流式返回的完整消息
     * */
    val message: ChatbotMessageForResponse,
    /**
     * 完成原因
     * */
    val finish_reason: String
)

/**
 * 表示消息内容
 */
data class ChatbotMessageForResponse(
    /**
     * 角色，如 "assistant"
     * */
    val role: String,
    /**
     * 消息内容
     * */
    val content: String,
    val index: Int,
    val type: String,
    val status: String
)

/**
 * token 使用量统计
 */
data class TokensUsage(
    /**
     * 输入提示消耗的 token 数
     * */
    val prompt_tokens: Int,
    /**
     * 生成回复消耗的 token 数
     * */
    val completion_tokens: Int,
    /**
     * 总共消耗的 token 数
     * */
    val total_tokens: Int
)