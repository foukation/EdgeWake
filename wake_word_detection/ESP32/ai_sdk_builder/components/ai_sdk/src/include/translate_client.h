/**
 * @file translate_client.h
 * @brief 文本翻译客户端
 *
 * 此文件定义了文本翻译客户端，用于处理翻译 API 请求。
 * 支持机器翻译 (v1) 和模型翻译 (v2) 两种模式。
 *
 * @note 此为内部头文件，仅供 SDK 内部使用
 *
 * ============================================================================
 * 内存安全改造说明（2025-01）
 * ============================================================================
 *
 * 将响应解析函数改为静态函数，避免在 lambda 中捕获 this 指针：
 * - parseResponse() - 静态函数，解析翻译响应
 *
 * 原因：
 * - TranslateClient 是 AIFoundationKitImpl 的成员，生命周期较长
 * - 但如果 AIFoundationKit 被销毁，TranslateClient 也会被销毁
 * - 此时如果有异步回调正在执行，访问 this 会导致崩溃
 */
#pragma once

#include <string>
#include <functional>
#include <map>

#include "ai_sdk/types/translate.h"

namespace ai_sdk {

/**
 * @class TranslateClient
 * @brief 文本翻译客户端
 *
 * 处理文本翻译 API 请求。
 * 支持机器翻译和模型翻译两种模式。
 */
class TranslateClient {
public:
    // ========================================================================
    // 回调类型定义
    // ========================================================================

    /**
     * @brief 成功回调类型
     */
    using SuccessCallback = std::function<void(const TranslateResponse& response)>;

    /**
     * @brief 错误回调类型
     */
    using ErrorCallback = std::function<void(const std::string& error)>;

    /**
     * @enum TranslateMode
     * @brief 翻译模式
     */
    enum class TranslateMode {
        MACHINE,  ///< 机器翻译 (v1)
        MODEL     ///< 模型翻译 (v2)
    };

    // ========================================================================
    // 构造与析构
    // ========================================================================

    /**
     * @brief 构造函数
     */
    TranslateClient();

    /**
     * @brief 析构函数
     */
    ~TranslateClient();

    // 禁止拷贝
    TranslateClient(const TranslateClient&) = delete;
    TranslateClient& operator=(const TranslateClient&) = delete;

    // ========================================================================
    // 公开方法
    // ========================================================================

    /**
     * @brief 发送翻译请求
     *
     * @param request 请求参数
     * @param mode 翻译模式
     * @param onSuccess 成功回调
     * @param onError 错误回调
     */
    void sendRequest(
        const TranslationRequest& request,
        TranslateMode mode,
        SuccessCallback onSuccess,
        ErrorCallback onError);

private:
    /**
     * @brief 构建请求 URL
     *
     * @param mode 翻译模式
     *
     * @return 完整的 API URL
     */
    std::string buildUrl(TranslateMode mode);

    /**
     * @brief 构建请求头
     *
     * @return 请求头 map
     */
    std::map<std::string, std::string> buildHeaders();

    /**
     * @brief 构建请求体
     *
     * @param request 请求参数
     *
     * @return JSON 字符串
     */
    std::string buildBody(const TranslationRequest& request);

    /**
     * @brief 解析响应 JSON（静态函数）
     *
     * 内存安全说明：
     * - 使用静态函数，避免在 lambda 中捕获 this 指针
     * - lambda 只需捕获回调函数，不依赖 TranslateClient 实例
     *
     * @param json JSON 字符串
     * @param response 输出响应对象
     *
     * @return true 解析成功，false 解析失败
     */
    static bool parseResponse(const std::string& json, TranslateResponse& response);
};

}  // namespace ai_sdk
