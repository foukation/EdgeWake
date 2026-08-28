/**
 * @file ai_foundation_kit.h
 * @brief AI 功能基础工具包公开 API
 *
 * 此文件定义了 AIFoundationKit 类，提供以下功能：
 * - 大模型闲聊（Chatbot）：支持流式和非流式请求
 * - 文本翻译：机器翻译 (v1) 和模型翻译 (v2)
 *
 * 使用方式:
 * @code
 * auto& kit = AIAssistantManager::getInstance().aiFoundationKit();
 *
 * // Chatbot 闲聊（流式）
 * ChatbotCompletionRequest req;
 * req.messages = {{"user", "Hello!"}};
 * req.stream = true;
 * std::string requestId = kit.largeModelChatbot(req,
 *     [](const ChatbotCompletionResponse& resp) { ... },
 *     [](const std::string& error) { ... });
 *
 * // 取消流式请求
 * kit.cancelStreamRequest(requestId);
 *
 * // 文本翻译（机器翻译）
 * TranslationRequest transReq;
 * transReq.targetLanguage = LanguageCode::ZH;
 * transReq.originText = "Hello";
 * kit.textTranslate(transReq,
 *     [](const TranslateResponse& resp) { ... },
 *     [](const std::string& error) { ... });
 * @endcode
 */
#pragma once

#include <string>
#include <functional>
#include <memory>

#include "ai_sdk/types/chatbot.h"
#include "ai_sdk/types/translate.h"
#include "ai_sdk/types/content_summary.h"

namespace ai_sdk {

// 前向声明
class AIFoundationKitImpl;

/**
 * @class AIFoundationKit
 * @brief AI 功能基础工具包
 *
 * 提供大模型闲聊和文本翻译功能。
 * 通过 AIAssistantManager::aiFoundationKit() 获取实例。
 */
class AIFoundationKit {
public:
    // ========================================================================
    // 回调类型定义
    // ========================================================================

    /**
     * @brief Chatbot 成功回调类型
     *
     * @param response 响应结果
     *
     * 流式请求时，此回调会被多次调用，每次返回一个增量数据。
     * 非流式请求时，此回调只调用一次，返回完整结果。
     */
    using ChatbotSuccessCallback = std::function<void(const ChatbotCompletionResponse& response)>;

    /**
     * @brief Chatbot 错误回调类型
     *
     * @param error 错误信息
     */
    using ChatbotErrorCallback = std::function<void(const std::string& error)>;

    /**
     * @brief 翻译成功回调类型
     *
     * @param response 翻译响应结果
     */
    using TranslateSuccessCallback = std::function<void(const TranslateResponse& response)>;

    /**
     * @brief 翻译错误回调类型
     *
     * @param error 错误信息
     */
    using TranslateErrorCallback = std::function<void(const std::string& error)>;

    /**
     * @brief 内容摘要成功回调类型
     *
     * @param response 响应结果
     *
     * 流式请求时，此回调会被多次调用，每次返回一个增量数据。
     * 非流式请求时，此回调只调用一次，返回完整结果。
     */
    using ContentSummarySuccessCallback = std::function<void(const ContentSummaryResponse& response)>;

    /**
     * @brief 内容摘要错误回调类型
     *
     * @param error 错误信息
     */
    using ContentSummaryErrorCallback = std::function<void(const std::string& error)>;

    // ========================================================================
    // 构造与析构
    // ========================================================================

    /**
     * @brief 构造函数
     */
    AIFoundationKit();

    /**
     * @brief 析构函数
     */
    ~AIFoundationKit();

    // 禁止拷贝
    AIFoundationKit(const AIFoundationKit&) = delete;
    AIFoundationKit& operator=(const AIFoundationKit&) = delete;

    // 允许移动
    AIFoundationKit(AIFoundationKit&&) noexcept;
    AIFoundationKit& operator=(AIFoundationKit&&) noexcept;

    // ========================================================================
    // 大模型闲聊（Chatbot）
    // ========================================================================

    /**
     * @brief 大模型闲聊
     *
     * 发送 Chatbot 闲聊请求，支持流式和非流式模式。
     *
     * @param request 请求参数
     * @param onSuccess 成功回调
     *        - 流式模式：多次调用，每次返回增量数据
     *        - 非流式模式：调用一次，返回完整结果
     * @param onError 错误回调
     *
     * @return 请求 ID，用于取消流式请求
     *
     * @note 流式请求可通过 cancelStreamRequest() 取消
     *
     * 使用示例:
     * @code
     * ChatbotCompletionRequest req;
     * req.messages = {{"user", "你好"}};
     * req.stream = true;
     *
     * std::string requestId = kit.largeModelChatbot(req,
     *     [](const ChatbotCompletionResponse& resp) {
     *         // 处理响应
     *         for (const auto& choice : resp.choices) {
     *             std::cout << choice.delta.content;
     *         }
     *     },
     *     [](const std::string& error) {
     *         // 处理错误
     *         std::cerr << "Error: " << error << std::endl;
     *     });
     * @endcode
     */
    std::string largeModelChatbot(
        const ChatbotCompletionRequest& request,
        ChatbotSuccessCallback onSuccess,
        ChatbotErrorCallback onError);

    /**
     * @brief 取消流式请求
     *
     * 取消正在进行的流式请求。
     *
     * @param requestId 请求 ID，由 largeModelChatbot() 返回
     *
     * @return true 取消成功，false 请求不存在或已完成
     */
    bool cancelStreamRequest(const std::string& requestId);

    // ========================================================================
    // 文本翻译
    // ========================================================================

    /**
     * @brief 文本翻译（机器翻译 v1）
     *
     * 使用机器翻译引擎进行文本翻译。
     * 支持 200+ 种语言。
     *
     * @param request 翻译请求参数
     *        - targetLanguage: 目标语言，使用 LanguageCode 中的常量
     *        - originText: 源文本
     *        - sourceLanguage: 源语言（可选，默认 "auto"）
     * @param onSuccess 成功回调
     * @param onError 错误回调
     *
     * @see LanguageCode
     *
     * 使用示例:
     * @code
     * TranslationRequest req;
     * req.targetLanguage = LanguageCode::ZH;  // 翻译为中文
     * req.originText = "Hello, world!";
     *
     * kit.textTranslate(req,
     *     [](const TranslateResponse& resp) {
     *         std::cout << "翻译结果: " << resp.data.translateText << std::endl;
     *     },
     *     [](const std::string& error) {
     *         std::cerr << "翻译失败: " << error << std::endl;
     *     });
     * @endcode
     */
    void textTranslate(
        const TranslationRequest& request,
        TranslateSuccessCallback onSuccess,
        TranslateErrorCallback onError);

    /**
     * @brief 文本翻译（模型翻译 v2）
     *
     * 使用大模型进行文本翻译，翻译质量更高。
     * 支持约 90 种语言。
     *
     * @param request 翻译请求参数
     *        - targetLanguage: 目标语言，使用 LanguageCodeModel 中的常量
     *        - originText: 源文本
     *        - sourceLanguage: 源语言（可选，默认 "auto"）
     * @param onSuccess 成功回调
     * @param onError 错误回调
     *
     * @see LanguageCodeModel
     *
     * 使用示例:
     * @code
     * TranslationRequest req;
     * req.targetLanguage = LanguageCodeModel::ZH;  // 翻译为中文
     * req.originText = "Hello, world!";
     *
     * kit.textTranslateWithModel(req,
     *     [](const TranslateResponse& resp) {
     *         std::cout << "翻译结果: " << resp.data.translateText << std::endl;
     *     },
     *     [](const std::string& error) {
     *         std::cerr << "翻译失败: " << error << std::endl;
     *     });
     * @endcode
     */
    void textTranslateWithModel(
        const TranslationRequest& request,
        TranslateSuccessCallback onSuccess,
        TranslateErrorCallback onError);

    // ========================================================================
    // 内容摘要
    // ========================================================================

    /**
     * @brief 内容摘要
     *
     * 对长文本进行智能摘要处理，支持流式和非流式模式。
     *
     * @param request 请求参数
     *        - content: 需要摘要的文本内容
     *        - stream: 是否启用流式响应（默认 true）
     *        - language: 摘要语言（默认 "auto"）
     * @param onSuccess 成功回调
     *        - 流式模式：多次调用，每次返回增量数据
     *        - 非流式模式：调用一次，返回完整结果
     * @param onError 错误回调
     *
     * @return 请求 ID，用于取消流式请求
     *
     * @note 流式请求可通过 cancelStreamRequest() 取消
     *
     * 使用示例:
     * @code
     * ContentSummaryRequest req;
     * req.content = "这是一段很长的会议记录...";
     * req.stream = true;
     * req.language = "Chinese";
     *
     * std::string requestId = kit.contentSummary(req,
     *     [](const ContentSummaryResponse& resp) {
     *         // 处理响应
     *         std::cout << resp.data.content;
     *     },
     *     [](const std::string& error) {
     *         // 处理错误
     *         std::cerr << "Error: " << error << std::endl;
     *     });
     *
     * // 可选：取消请求
     * // kit.cancelStreamRequest(requestId);
     * @endcode
     */
    std::string contentSummary(
        const ContentSummaryRequest& request,
        ContentSummarySuccessCallback onSuccess,
        ContentSummaryErrorCallback onError);

private:
    std::unique_ptr<AIFoundationKitImpl> impl_;  ///< PIMPL 实现指针
};

}  // namespace ai_sdk
