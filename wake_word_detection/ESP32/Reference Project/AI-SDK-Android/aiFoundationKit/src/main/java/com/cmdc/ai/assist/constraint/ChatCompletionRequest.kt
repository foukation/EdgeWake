package com.cmdc.ai.assist.constraint

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject

/**
 * 表示聊天完成请求的数据类
 */
data class ChatCompletionRequest(

    /**
     * 必选参数
     * 示例 ：[{"role": "user", "content": "Hello!"}]
     * 对话消息列表，包含角色和内容
     *
     * */
    val messages: List<Message>,
    /**
     * 必选参数
     * 使用的模型，例如 "ikutan_7.5b"
     * */
    private val model: String = "jiutian_75b",
    /**
     * 可选参数
     * 是否启用流式响应
     * */
    val stream: Boolean = false,
    /**
     * 可选参数
     * 终端用户的唯一标识符
     * */
    val user: String? = null,
    /**
     * 可选参数
     * 采样温度，范围0-2，较高的值会使输出更随机，较低的值会使输出更确定
     * */
    val temperature: Double? = null,
    /**
     * 可选参数
     * 生成的最大标记数
     * */
    val max_tokens: Int? = null,
    /**
     * 可选参数
     * 核采样，范围0-1，替代temperature的另一种采样方法
     * */
    val top_p: Double? = null,
    /**
     * 可选参数
     * -2.0到2.0之间，正值会惩罚新标记基于文本中的现有频率出现
     * */
    val frequency_penalty: Double? = null,
    /**
     * 可选参数
     * -2.0到2.0之间，正值会惩罚新标记基于是否出现在文本中
     * */
    val presence_penalty: Double? = null,
    /**
     * 可选参数
     * 生成停止标记，最多4个
     * */
    val stop: Any? = null,
    /**
     * 可选参数
     * 为每个提示生成的完成数量
     * */
    val n: Int? = null,
    /**
     * 可选参数
     * 可调用的函数列表
     * */
    val functions: JsonArray? = null,
    /**
     * 可选参数
     * 控制函数调用行为
     * */
    val function_call: JsonElement? = null,
    /**
     * 可选参数
     * 指定响应格式类型
     * */
    val response_format: JsonObject? = null,
    /**
     * 可选参数
     * 用于确定性采样的随机数种子
     * */
    val seed: Int? = null,
    /**
     * 可选参数
     * 可用工具列表，包括函数等
     * */
    val tools: JsonArray? = null,
    /**
     * 可选参数
     * 控制工具选择行为
     * */
    val tool_choice: JsonElement? = null
)

/**
 * 表示对话消息的数据类
 */
data class Message(
    /**
     * 消息角色，如"user", "assistant", "system"等
     * */
    val role: String,
    /**
     * 消息内容
     * */
    val content: String
)

/**
 * 表示可调用函数的数据类
 */
internal data class Function(
    val name: String, // 函数名称
    val description: String? = null, // 函数描述
    val parameters: Map<String, Any>? = null // 函数参数定义
)

/**
 * 表示响应格式的数据类
 */
internal data class ResponseFormat(
    val type: String // 响应类型，如"json_object"
)

/**
 * 表示工具的数据类
 */
internal data class Tool(
    val type: String, // 工具类型，如"function"
    val function: Function // 工具关联的函数
)