package com.cmdc.ai.assist.constraint

/*
 * 聊天模型
 */

internal data class ChatMessage(
    val role: String,	  // 消息角色(system/user/assistant)
    val content: String,  // 消息内容
)

internal data class UsageContent(
    val prompt_tokens: Int,     // 提示词token数量
    val completion_tokens: Int, // 完成token数量
    val total_tokens: Int       // 总token数量
)

internal data class  ChoicesContent(
    val index: Int,	           // 响应选项的索引
    val message: ChatMessage,  // 消息内容（仅非流式）
    val delta: ChatMessage,    // 增量消息内容（仅流式）
    val finish_reason: String, // 响应完成原因
)

internal data class ErrorContent(
    val message: String,        // 错误信息
    val type:  String           // 错误类型
)

internal data class ChatModelReq(
    val model: String = "jiutian-lan", // 指定调用的语言模型
    val messages: Array<ChatMessage>,  // 对话消息列表
    val stream: Boolean = false, 	   // 是否使用流式响应
    val top_p: Double = 0.75,          // 核采样参数
    val top_k: Int = 10, 	           // 保留最高概率的token数量
)



internal data class ChatModelRes(
    val id:	String, 	       // 响应唯一标识
    val `object`: String,      // 响应对象类型
    val created: Int, 	       // 响应创建时间戳
    val model: String, 	       // 使用的模型名称
    val choices: Array<ChoicesContent>,  // 响应选项数组
    val usage: UsageContent,   // Token使用情况
    val code: Int,             // 状态码
    val error: ErrorContent?   // 错误信息内容
)