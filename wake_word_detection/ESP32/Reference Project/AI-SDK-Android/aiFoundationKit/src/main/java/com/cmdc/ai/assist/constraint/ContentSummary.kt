package com.cmdc.ai.assist.constraint

/**
 * 内容摘要 API 请求数据类
 *
 * 该数据类用于封装发送给服务器的内容摘要请求
 * 可用于会议纪要、文档摘要等内容的处理请求
 */
data class ContentSummaryRequest(
    /**
     * 需要进行摘要处理的原始内容文本
     *
     * 可以包含会议纪要、文档正文等需要处理的文本内容
     * 服务器将基于此内容生成摘要或进行其他处理
     */
    val content: String,
    /**
     * 是否启用流式响应模式
     *
     * true - 启用流式传输，服务器将逐步返回处理结果，适用于实时显示处理进度
     * false - 使用标准响应模式，等待完整处理结果后一次性返回
     * 默认值为 true，推荐用于提升用户体验
     */
    val stream: Boolean = true,
    /**
     * 指定返回摘要的语言，默认auto，指定时使用英文，例：English、Chinese、French
     * */
    val language: String = "auto"
)

/**
 * 内容摘要 API 响应的顶层数据结构
 *
 * @property msg 消息信息，通常用于表示请求状态的描述
 * @property data 包含实际内容数据的对象
 * @property logId 日志标识符
 * @property status 状态码，表示请求的处理状态
 */
data class ContentSummaryResponse(
    val msg: String,
    val data: ContentData,
    val logId: String,
    val status: Int
)

/**
 * 内容数据结构
 *
 * @property content 实际的内容文本，包含会议纪要等信息
 */
data class ContentData(
    val content: String
)