/**
 * @file translate.h
 * @brief 文本翻译数据结构定义
 *
 * 此文件定义了文本翻译功能的数据结构：
 * - TranslationRequest: 翻译请求参数
 * - TranslateResponse: 翻译响应结果
 *
 * @note 语言代码定义在 language_code.h 和 language_code_model.h 中
 */
#pragma once

#include <string>

namespace ai_sdk {

/**
 * @struct TranslationRequest
 * @brief 翻译请求参数
 */
struct TranslationRequest {
    /**
     * @brief 目标语言代码
     *
     * 必填参数，指定翻译的目标语言。
     * 注意：不可设置为 "auto"。
     *
     * 机器翻译 (v1) 使用 LanguageCode 中的代码
     * 模型翻译 (v2) 使用 LanguageCodeModel 中的代码
     *
     * @see LanguageCode
     * @see LanguageCodeModel
     */
    std::string targetLanguage;

    /**
     * @brief 源文本内容
     *
     * 必填参数，需要翻译的原始文本。
     * 不能为空字符串。
     */
    std::string originText;

    /**
     * @brief 源语言代码
     *
     * 可选参数，指定源文本的语言。
     * 可设置为 "auto" 进行自动语种识别。
     * 默认值: "auto"
     *
     * @see LanguageCode
     * @see LanguageCodeModel
     */
    std::string sourceLanguage = "auto";
};

/**
 * @struct TextTranslateResult
 * @brief 翻译结果数据
 */
struct TextTranslateResult {
    std::string translateText;   ///< 翻译后的文本
    std::string sourceLanguage;  ///< 检测到的源语言
    std::string targetLanguage;  ///< 目标语言
};

/**
 * @struct TranslateResponse
 * @brief 翻译响应结果
 */
struct TranslateResponse {
    /**
     * @brief 响应状态码
     *
     * 0 表示成功，其他值表示失败。
     */
    int code = 0;

    /**
     * @brief 响应消息
     *
     * 通常是失败时的返回失败信息，成功时可能为空。
     */
    std::string msg;

    /**
     * @brief 翻译结果数据
     */
    TextTranslateResult data;
};

}  // namespace ai_sdk
