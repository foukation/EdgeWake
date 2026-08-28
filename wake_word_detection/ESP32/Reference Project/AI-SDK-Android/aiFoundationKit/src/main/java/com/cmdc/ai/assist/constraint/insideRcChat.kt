package com.cmdc.ai.assist.constraint

import com.google.gson.JsonArray
import com.google.gson.JsonObject

/**
 * 设备端 NLU 及聊天，请求数据类;
 * 封装 API 请求参数;
 */
data class InsideRcChatRequest(
    /**
     * 必选参数；
     * 请求 ID，请求方自定义，推荐 uuid；
     * */
    val qid: String,
    /**
     * 必选参数；
     * 第三方自定义用户 id；
     * */
    val third_user_id: String,
    /**
     * 必选参数;
     * 设备 id;
     * */
    val cuid: String,
    /**
     * 必选参数；
     * 服务配置版本号
     * */
    internal var version: String = "",                  // 服务配置版本号
    /**
     * 必选参数；
     * 聊天上下文信息。
     * 说明：（1）messages 成员不能为空，1个成员表示单轮对话，多个成员表示多轮对话；
     * 例如： - 1个成员示例，"messages": [ {"role": "user","content": "你好"}]
     *       - 3个成员示例，"messages": [ {"role": "user","content": "你好"},{"role":"assistant","content":"需要什么帮助"},{"role":"user","content":"自我介绍下"}]
     * （2）最后一个 message 为当前请求的信息，前面的 message 为历史对话信息
     * （3）成员数目必须为奇数，成员中 message 的 role 值说明如下：奇数位 message 的 role 值必须为 user，偶数位 message 的 role 值为 assistant。
     * */
    val messages: List<Message>,          // 聊天上下文信息，必须为数组
    /**
     * 必选参数；
     * 服务配置版本号
     * */
    internal var rc_version: Int = 0,

    /**
     * 可选参数；
     * 端状态信息；
     * */
    val client_context: JsonArray? = JsonArray(),
    /**
     * 可选参数;
     * event 事件;
     * */
    val event: JsonObject? = null,             // event事件
    /**
     * 可选参数；
     * 可指定 bot 路径, 不做 nlu分发, 会额外带上 params 参数给到下游 bot 格式为: fxzsos://{bot_id}/{path};
     * */
    val url: String? = null,              // 可指定bot路径，不做nlu分发，会额外带上params参数给到下游bot格式为: fxzsos://{bot_id}/{path}
    /**
     * 可选参数;
     * 透传给 bot 的参数;
     * */
    val params: Map<String, Any>? = null, // 透传给bot的参数
    /**
     * 可选参数;
     * 是否流式返回: true/false 流式将采用 text/event-stream 的方式返回内容;非流式返回 application/json;
     * */
    val stream: Boolean = false,          // 是否流式返回: true/false 流式将采用text/event-stream的方式返回内容非流式返回application/json
    /**
     * 可选参数；
     * 客户端 client ip，用于协助定位；
     * */
    val client_ip: String? = null,        // 客户端client ip，用于协助定位
    /**
     * 可选参数；
     * 请求 dcs 时使用的 dialog_request_id，并用于填充指令中的 dialogRequestId 注意，如果传入了有效的 event 参数，则会使用 event 中的 dialog_request_id 来请求 dcs 和构造返回，此时这个参数不生效
     * */
    val dialog_request_id: String? = null, // 请求dcs时使用的dialog_request_id，并用于填充指令中的dialogRequestId

    val is_debug: Int = 0
) {

    /**
     * 必选参数；
     * */
    data class Message(
        /**
         * 必选参数;
         * 当前支持以下：user: 表示用户 assistant: 表示对话助手
         * */
        val role: String,     // 当前支持: user: 表示用户 assistant: 表示对话助手
        /**
         * 必选参数;
         * 对话内容, 不能为空;
         * */
        val content: String   // 对话内容，不能为空
    )

    // EVENT枚举类
    /*enum class EVENT {
        EVENT                 // event事件
    }*/

    // CLIENT_CONTEXT_TEXT枚举类
    /*enum class CLIENT_CONTEXT_TEXT {
        CLIENT_CONTEXT_TEXT   // 端状态信息
    }*/
}

/**
 * 设备端 NLU 及聊天 API 响应数据类
 */
data class InsideRcChatResponse(
    // 基本响应信息
    /**
     * 0为成功, 其他为失败
     * */
    val code: Int,                    // 0为成功，其他为失败
    /**
     * 错误信息
     * */
    val msg: String,                  // 错误信息
    /**
     * 日志id
     * */
    val logid: String,                // 日志id
    /**
     * request id, 与 request 中的 qid 一致
     * */
    val qid: String,                  // request id，与request中的qid一致
    /**
     * 是否为流式结束包, 0-未结束, 1-结束
     * */
    val is_end: Int,                  // 是否为流式结束包，0-未结束，1-结束
    /**
     * 指令数组, 参考 dcs directive 协议
     * */
    val data: List<Directive>?,       // 指令数组，参考dcs directive协议

    // 对话内容相关
    /**
     * 该字段表示输入中，当前 messages 中最后一轮用户 query 的内容
     * */
    val user_query: String,           // 该字段表示输入中，当前messages中最后一轮用户query的内容
    /**
     * 该字段仅用于上下文推理, 替换大模型输入 messages 中的 role=assistant 的 content 内容一般端不需要关注内容，具体格式可参考 AssistantAnswer
     * */
    val assistant_answer: String,     // 该字段仅用于上下文推理，替换大模型输入messages中的role=assistant的content内容
    /**
     * 结束原因
     * */
    val finish_reason: String,

    val source: String,
    /**
     * 是否清空上下文 0 不需要 1需要
     * */
    val need_clear_history: Int,      // 是否清空上下文，0不需要，1需要

    // 使用统计信息（仅当is_end=1时输出）
    /**
     * 仅当 is_end=1 时输出该参数
     * */
    val usage: Usage? = null,

    // 对话请求ID
    /**
     * 传入 dialog_request_id 或 event 时，返回请求中的生效的 dialog_request_id
     * */
    val dialog_request_id: String?,    // 传入dialog_request_id或event时，返回请求中的生效的dialog_request_id

    val debug: Any
) {
    /**
     * 指令数据类
     */
    data class Directive(
        // 添加directive相关字段
        /**
         * 指令类型
         * */
        val header: Any,             // 指令类型
        /**
         * 指令内容，根据具体指令类型有不同结构
         * */
        val payload: Any,         // 指令内容，根据具体指令类型有不同结构
        /**
         * 指令属性
         * */
        val property: Any,             // 指令属性
    )

    /**
     * 使用统计数据类
     */
    data class Usage(
        /**
         * 输入 tokens 数
         * */
        val prompt_tokens: Int,       // 输入tokens数
        /**
         * 回答 tokens 数
         * */
        val completion_tokens: Int,   // 回答tokens数
        /**
         * tokens 总数
         * */
        val total_tokens: Int,        // tokens总数

        /**
         * 传入 dialog_request_id 或 event 时，返回请求中的生效的 dialog_request_id
         * */
        val dialog_request_id: String
    )
}