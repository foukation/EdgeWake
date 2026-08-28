/**
 * @file inside_rc_chat.h
 * @brief 文本链路智能问答数据结构定义
 *
 * 此文件定义了 insideRcChat 文本链路智能问答功能的数据结构：
 * - InsideRcChatMessage: 对话消息
 * - InsideRcChatRequest: 请求参数
 *
 * insideRcChat 是文本输入的智能对话接口，跳过语音识别（ASR）环节，
 * 直接将文本发送给 NLU + 对话后端，返回与语音助手完全相同的 DialogueResult。
 *
 * 与 largeModelChatbot（Chatbot 闲聊）的区别：
 * - Chatbot 走通用大模型，仅返回纯文本
 * - insideRcChat 走智能对话服务（DCS 协议），返回指令集
 *   （Speak/Play/Nlu/RenderStreamCard/RenderMultiImageCard 等）
 *
 * @note 数据结构设计与服务端 API 及 Android SDK 保持一致
 */
#pragma once

#include <string>
#include <vector>
#include <map>

namespace ai_sdk {

/**
 * @struct InsideRcChatMessage
 * @brief 对话消息结构
 *
 * 用于构建对话上下文。
 * messages 数组中成员数必须为奇数，奇数位 role 为 "user"，偶数位为 "assistant"。
 */
struct InsideRcChatMessage {
    /**
     * @brief 消息角色
     *
     * 取值:
     * - "user": 用户消息
     * - "assistant": 助手回复
     */
    std::string role;

    /**
     * @brief 消息内容，不能为空
     */
    std::string content;

    /**
     * @brief 默认构造函数
     */
    InsideRcChatMessage() = default;

    /**
     * @brief 带参数构造函数
     *
     * @param r 角色
     * @param c 内容
     */
    InsideRcChatMessage(const std::string& r, const std::string& c)
        : role(r), content(c) {}
};

/**
 * @struct InsideRcChatRequest
 * @brief 文本链路智能问答请求参数
 *
 * messages 规则：
 * - 成员数必须为奇数
 * - 奇数位（第1、3、5条...）的 role 必须为 "user"
 * - 偶数位（第2、4条...）的 role 必须为 "assistant"
 * - 最后一条是当前用户问题，前面的是历史对话
 *
 * 使用示例（单轮）：
 * @code
 * InsideRcChatRequest req;
 * req.qid = "uuid-001";
 * req.third_user_id = "user-001";
 * req.cuid = config.deviceId;
 * req.messages = {{"user", "今天天气怎么样"}};
 * req.stream = true;
 * @endcode
 *
 * 使用示例（多轮）：
 * @code
 * req.messages = {
 *     {"user", "今天天气怎么样"},
 *     {"assistant", "今天北京晴天，气温25度"},
 *     {"user", "那上海呢"}
 * };
 * @endcode
 *
 * @note version、rc_version、client_context 由 SDK 内部自动填充，调用方无需设置
 */
struct InsideRcChatRequest {
    // ========================================================================
    // 必选参数
    // ========================================================================

    /**
     * @brief 请求 ID，推荐使用 UUID
     *
     * 用于追踪请求和关联多轮对话。
     * 同一个问题的多次流式响应会有相同的 qid。
     */
    std::string qid;

    /**
     * @brief 第三方自定义用户 ID
     */
    std::string third_user_id;

    /**
     * @brief 设备 ID
     *
     * 通常使用 AIAssistConfig::deviceId
     */
    std::string cuid;

    /**
     * @brief 对话消息列表
     *
     * 必选参数。成员数必须为奇数。
     * 最后一条是当前用户问题，前面的是历史对话。
     */
    std::vector<InsideRcChatMessage> messages;

    // ========================================================================
    // 可选参数
    // ========================================================================

    /**
     * @brief 是否启用流式响应
     *
     * - true: 流式返回（text/event-stream），回调多次调用
     * - false: 一次性返回完整结果（application/json）
     *
     * 默认: false
     */
    bool stream = false;

    /**
     * @brief 指定 bot 路径
     *
     * 不做 NLU 分发，直接将请求路由到指定 bot。
     * 格式: fxzsos://{bot_id}/{path}
     * 为空表示使用默认 NLU 分发。
     */
    std::string url;

    /**
     * @brief 透传给 bot 的参数
     *
     * 键值对形式，透传给下游 bot 使用。
     */
    std::map<std::string, std::string> params;

    /**
     * @brief 客户端 IP
     *
     * 用于协助定位服务。
     */
    std::string client_ip;

    /**
     * @brief 请求 DCS 时使用的 dialog_request_id
     *
     * 用于填充指令中的 dialogRequestId。
     * 如果传入了有效的 event 参数，则以 event 中的值为准。
     */
    std::string dialog_request_id;

    /**
     * @brief 调试标志
     *
     * 0: 正常模式（默认）
     * 非0: 开启调试信息
     */
    int is_debug = 0;
};

}  // namespace ai_sdk
