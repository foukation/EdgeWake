package com.cmdc.ai.assist.constraint

/**
 * 翻译API请求体数据结构
 *
 * 用于封装翻译接口的请求参数
 *
 * @param targetLanguage 目标语言代码，必填参数，不可设置为auto
 * @param originText 源文本内容，必填参数，不能为空
 * @param sourceLanguage 源语言代码，可选参数，可设置为auto进行自动识别
 */
data class TranslationRequest(
    /**
     * 目标语言代码
     *
     * 必填参数，指定翻译的目标语言
     * 注意：不可设置为"auto"
     *
     * 示例值：
     * - "zh" - 中文(简体)
     * - "en" - 英语
     * - "ja" - 日语
     * - "ko" - 韩语
     *
     * 参考：1.1.9语言列表
     */
    val targetLanguage: String,

    /**
     * 源文本内容
     *
     * 必填参数，需要翻译的原始文本
     * 不能为空字符串
     *
     * 示例值："你好"
     */
    val originText: String,

    /**
     * 源语言代码
     *
     * 可选参数，指定源文本的语言
     * 可设置为"auto"进行自动语种识别
     * 默认值："auto"
     *
     * 示例值：
     * - "auto" - 自动识别
     * - "zh" - 中文
     * - "en" - 英语
     *
     * 参考：1.1.9语言列表
     */
    val sourceLanguage: String = "auto"
)

