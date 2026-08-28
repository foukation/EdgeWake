/**
 * @file chatbot.h
 * @brief Chatbot 闲聊数据结构定义
 *
 * 此文件定义了 Chatbot 闲聊功能的数据结构：
 * - ChatbotMessage: 对话消息
 * - ChatbotCompletionRequest: 请求参数
 * - ChatbotCompletionResponse: 响应结果
 *
 * @note 数据结构设计与服务端 API 保持一致
 */
#pragma once

#include <string>
#include <vector>
#include <cstdint>

namespace ai_sdk {

/**
 * @struct ChatbotMessage
 * @brief 对话消息结构
 */
struct ChatbotMessage {
    /**
     * @brief 消息角色
     *
     * 取值:
     * - "user": 用户消息
     * - "assistant": 助手回复
     * - "system": 系统提示词
     */
    std::string role;

    /**
     * @brief 消息内容
     */
    std::string content;

    /**
     * @brief 默认构造函数
     */
    ChatbotMessage() = default;

    /**
     * @brief 带参数构造函数
     *
     * @param r 角色
     * @param c 内容
     */
    ChatbotMessage(const std::string& r, const std::string& c)
        : role(r), content(c) {}
};

/**
 * @struct ChatbotCompletionRequest
 * @brief Chatbot 闲聊请求参数
 */
struct ChatbotCompletionRequest {
    /**
     * @brief 对话消息列表
     *
     * 必选参数。
     * 示例: [{"role": "user", "content": "Hello!"}]
     */
    std::vector<ChatbotMessage> messages;

    /**
     * @brief 使用的模型
     *
     * 必选参数。
     * 默认值: "jiutian_75b"
     */
    std::string model = "jiutian_75b";

    /**
     * @brief 是否启用流式响应
     *
     * 可选参数。
     * - true: 流式返回，回调多次调用
     * - false: 一次性返回完整结果
     */
    bool stream = false;

    /**
     * @brief 采样温度
     *
     * 可选参数，范围 0-2。
     * - 较高的值会使输出更随机
     * - 较低的值会使输出更确定
     * - -1 表示不设置（使用服务器默认值）
     */
    double temperature = -1;

    /**
     * @brief 核采样参数
     *
     * 可选参数，范围 0-1。
     * 替代 temperature 的另一种采样方法。
     * - -1 表示不设置（使用服务器默认值）
     */
    double top_p = -1;
};

/**
 * @struct ChatbotMessageResponse
 * @brief Chatbot 响应消息结构
 */
struct ChatbotMessageResponse {
    std::string role;      ///< 角色，如 "assistant"
    std::string content;   ///< 消息内容
    int index = 0;         ///< 索引
    std::string type;      ///< 类型
    std::string status;    ///< 状态
};

/**
 * @struct ChatbotChoice
 * @brief Chatbot 响应选项
 */
struct ChatbotChoice {
    int index = 0;                      ///< 索引，从 0 开始
    ChatbotMessageResponse delta;       ///< 流式返回的消息（增量）
    ChatbotMessageResponse message;     ///< 非流式返回的完整消息
    std::string finish_reason;          ///< 完成原因
};

/**
 * @struct TokensUsage
 * @brief Token 使用量统计
 */
struct TokensUsage {
    int prompt_tokens = 0;      ///< 输入提示消耗的 token 数
    int completion_tokens = 0;  ///< 生成回复消耗的 token 数
    int total_tokens = 0;       ///< 总共消耗的 token 数
};

/**
 * @struct ChatbotCompletionResponse
 * @brief Chatbot 闲聊响应结果
 */
struct ChatbotCompletionResponse {
    std::string id;                     ///< 响应的唯一 ID
    std::string object;                 ///< 对象类型标识
    int64_t created = 0;                ///< 响应创建时间的时间戳
    std::string model;                  ///< 使用的模型名称
    std::vector<ChatbotChoice> choices; ///< 回复选项列表
    TokensUsage usage;                  ///< 令牌使用统计
};

}  // namespace ai_sdk
