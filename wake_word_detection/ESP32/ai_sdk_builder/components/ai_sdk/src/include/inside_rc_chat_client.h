/**
 * @file inside_rc_chat_client.h
 * @brief 文本链路智能问答客户端
 *
 * 此文件定义了文本链路智能问答客户端，用于处理 insideRcChat API 请求。
 * 支持流式（SSE）和非流式（HTTP POST）请求。
 *
 * insideRcChat 是文本输入的智能对话接口，跳过 ASR，
 * 直接将文本发送给 NLU + 对话后端，返回 DCS 指令集。
 * 响应类型为 DialogueResult，与语音助手回调完全一致。
 *
 * @note 此为内部头文件，仅供 SDK 内部使用
 *
 * ============================================================================
 * 内存安全说明
 * ============================================================================
 *
 * 将响应解析函数改为静态函数，避免在 lambda 中捕获 this 指针：
 * - parseResponse() - 静态函数，解析 insideRcChat 响应并多次回调 DialogueResult
 */
#pragma once

#include <string>
#include <functional>
#include <memory>
#include <map>

#include "ai_sdk/types/inside_rc_chat.h"
#include "ai_sdk/types/voice_assistant.h"
#include "sse_client.h"

namespace ai_sdk {

/**
 * @class InsideRcChatClient
 * @brief 文本链路智能问答客户端
 *
 * 处理 insideRcChat API 请求。
 * 支持流式和非流式两种模式。
 * 响应通过 DialogueResult 回调，与语音助手的回调结构完全一致。
 */
class InsideRcChatClient {
public:
    // ========================================================================
    // 回调类型定义
    // ========================================================================

    /**
     * @brief 成功回调类型
     *
     * 每个 DCS 指令回调一次 DialogueResult。
     * is_end=1 时额外回调一次，附带 assistant_answer_content。
     */
    using SuccessCallback = std::function<void(const DialogueResult& result)>;

    /**
     * @brief 错误回调类型
     */
    using ErrorCallback = std::function<void(const std::string& error)>;

    // ========================================================================
    // 构造与析构
    // ========================================================================

    /**
     * @brief 构造函数
     */
    InsideRcChatClient();

    /**
     * @brief 析构函数
     */
    ~InsideRcChatClient();

    // 禁止拷贝
    InsideRcChatClient(const InsideRcChatClient&) = delete;
    InsideRcChatClient& operator=(const InsideRcChatClient&) = delete;

    // ========================================================================
    // 公开方法
    // ========================================================================

    /**
     * @brief 发送文本链路智能问答请求
     *
     * 根据 request.stream 参数决定使用流式或非流式模式。
     * SDK 会自动注入 version、rc_version、client_context 等内部字段。
     *
     * @param request 请求参数
     * @param onSuccess 成功回调，每个 DCS 指令回调一次
     * @param onError 错误回调
     *
     * @return 请求 ID（仅流式模式有效，可用于取消请求）
     */
    std::string sendRequest(
        const InsideRcChatRequest& request,
        SuccessCallback onSuccess,
        ErrorCallback onError);

    /**
     * @brief 取消流式请求
     *
     * @param requestId 请求 ID
     *
     * @return true 取消成功，false 请求不存在
     */
    bool cancelRequest(const std::string& requestId);

private:
    std::unique_ptr<SSEClient> sse_client_;  ///< SSE 客户端

    /**
     * @brief 构建请求 URL
     * @return 完整的 API URL
     */
    std::string buildUrl();

    /**
     * @brief 构建请求头（AI Server Headers，不含 Bearer token）
     * @return 请求头 map
     */
    std::map<std::string, std::string> buildHeaders();

    /**
     * @brief 构建请求体
     *
     * 自动注入以下内部字段：
     * - version / rc_version: 从 AIAssistConfig::centralConfigVersion 取值
     * - client_context: 从 AIAssistConfig::dialogueTtsConfig 构建 SpeechState
     *
     * @param request 请求参数
     * @return JSON 字符串
     */
    std::string buildBody(const InsideRcChatRequest& request);

    /**
     * @brief 发送非流式请求
     */
    void sendNonStreamRequest(
        const InsideRcChatRequest& request,
        SuccessCallback onSuccess,
        ErrorCallback onError);

    /**
     * @brief 发送流式请求
     * @return 请求 ID
     */
    std::string sendStreamRequest(
        const InsideRcChatRequest& request,
        SuccessCallback onSuccess,
        ErrorCallback onError);

    /**
     * @brief 解析响应并回调 DialogueResult（静态函数）
     *
     * 解析 insideRcChat 响应 JSON，遍历 data[] 指令数组，
     * 对每个 Directive 构建 DialogueResult 并调用 onSuccess。
     * is_end=1 时额外回调一次，附带 assistant_answer_content。
     *
     * 内存安全说明：
     * - 使用静态函数，避免在 lambda 中捕获 this 指针
     * - lambda 只需捕获回调函数，不依赖 InsideRcChatClient 实例
     *
     * @param json JSON 字符串
     * @param onSuccess 成功回调
     * @param onError 错误回调
     */
    static void parseResponse(
        const std::string& json,
        SuccessCallback onSuccess,
        ErrorCallback onError);
};

}  // namespace ai_sdk
