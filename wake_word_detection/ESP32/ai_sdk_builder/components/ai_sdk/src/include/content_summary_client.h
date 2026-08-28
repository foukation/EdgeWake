/**
 * @file content_summary_client.h
 * @brief 内容摘要客户端
 *
 * 此文件定义了内容摘要客户端，用于处理内容摘要 API 请求。
 * 支持流式和非流式请求。
 *
 * @note 此为内部头文件，仅供 SDK 内部使用
 *
 * ============================================================================
 * 内存安全说明
 * ============================================================================
 *
 * 将响应解析函数改为静态函数，避免在 lambda 中捕获 this 指针：
 * - parseResponse() - 静态函数，解析内容摘要响应
 */
#pragma once

#include <string>
#include <functional>
#include <memory>
#include <map>

#include "ai_sdk/types/content_summary.h"
#include "sse_client.h"

namespace ai_sdk {

/**
 * @class ContentSummaryClient
 * @brief 内容摘要客户端
 *
 * 处理内容摘要 API 请求。
 * 支持流式和非流式两种模式。
 */
class ContentSummaryClient {
public:
    // ========================================================================
    // 回调类型定义
    // ========================================================================

    /**
     * @brief 成功回调类型
     */
    using SuccessCallback = std::function<void(const ContentSummaryResponse& response)>;

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
    ContentSummaryClient();

    /**
     * @brief 析构函数
     */
    ~ContentSummaryClient();

    // 禁止拷贝
    ContentSummaryClient(const ContentSummaryClient&) = delete;
    ContentSummaryClient& operator=(const ContentSummaryClient&) = delete;

    // ========================================================================
    // 公开方法
    // ========================================================================

    /**
     * @brief 发送内容摘要请求
     *
     * 根据 request.stream 参数决定使用流式或非流式模式。
     *
     * @param request 请求参数
     * @param onSuccess 成功回调
     * @param onError 错误回调
     *
     * @return 请求 ID（仅流式模式有效）
     */
    std::string sendRequest(
        const ContentSummaryRequest& request,
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
     * @brief 构建请求头
     * @return 请求头 map
     */
    std::map<std::string, std::string> buildHeaders();

    /**
     * @brief 构建请求体
     * @param request 请求参数
     * @return JSON 字符串
     */
    std::string buildBody(const ContentSummaryRequest& request);

    /**
     * @brief 发送非流式请求
     */
    void sendNonStreamRequest(
        const ContentSummaryRequest& request,
        SuccessCallback onSuccess,
        ErrorCallback onError);

    /**
     * @brief 发送流式请求
     * @return 请求 ID
     */
    std::string sendStreamRequest(
        const ContentSummaryRequest& request,
        SuccessCallback onSuccess,
        ErrorCallback onError);

    /**
     * @brief 解析响应 JSON（静态函数）
     * @param json JSON 字符串
     * @param response 输出响应对象
     * @return true 解析成功，false 解析失败
     */
    static bool parseResponse(const std::string& json, ContentSummaryResponse& response);
};

}  // namespace ai_sdk
