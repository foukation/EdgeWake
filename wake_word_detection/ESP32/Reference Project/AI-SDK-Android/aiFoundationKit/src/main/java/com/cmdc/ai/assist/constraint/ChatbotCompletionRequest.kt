package com.cmdc.ai.assist.constraint

/**
 * 表示聊天完成请求的数据类
 */
data class ChatbotCompletionRequest(

    /**
     * 必选参数
     * 示例 ：[{"role": "user", "content": "Hello!"}]
     * 对话消息列表，包含角色和内容
     *
     * */
    val messages: List<ChatbotMessage>,
    /**
     * 必选参数
     * 使用的模型，例如 "jiutian-lan"
     * */
    val model: String = "jiutian-lan",
    /**
     * 可选参数
     * 是否启用流式响应
     * */
    val stream: Boolean = false,
    /**
     * 可选参数
     * 采样温度，范围0-2，较高的值会使输出更随机，较低的值会使输出更确定
     * */
    val temperature: Double? = null,
    /**
     * 可选参数
     * 核采样，范围0-1，替代temperature的另一种采样方法
     * */
    val top_p: Double? = null,
)

/**
 * 表示对话消息的数据类
 */
data class ChatbotMessage(
    /**
     * 消息角色，如"user", "assistant", "system"等
     * */
    val role: String,
    /**
     * 消息内容。文本对话时传 String，例如："今天天气怎么样？"。图片识别时传 List<ChatbotContent>，用于按接口文档生成 text + image_url 的图文数组。List 中的元素必须为 ChatbotContent
     *
     * */
    val content: Any
)

/**
 * 图文消息内容基类。
 *
 * 仅用于 ChatbotMessage.content 的图片识别场景，文本对话仍然直接传 String。
 */
sealed interface ChatbotContent

/**
 * 图文消息中的文本提示内容。
 */
data class ChatbotContentText @JvmOverloads constructor(
    val text: String,
    val type: String = "text"
) : ChatbotContent

/**
 * 图文消息中的图片内容。
 */
data class ChatbotContentImageUrl @JvmOverloads constructor(
    val image_url: ChatbotImageUrl,
    val type: String = "image_url"
) : ChatbotContent

/**
 * 图片地址。
 *
 * 支持接口文档中的 base64 data URL，例如："data:image/jpeg;base64,..."
 * 也支持 URL 地址，例如："https://example.com/image.jpg"
 */
data class ChatbotImageUrl(
    val url: String
)
