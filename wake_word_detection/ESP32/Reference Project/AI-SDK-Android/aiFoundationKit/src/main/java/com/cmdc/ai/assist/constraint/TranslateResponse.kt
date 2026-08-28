package com.cmdc.ai.assist.constraint

/**
 * 翻译 API 响应体
 * 用于接收翻译服务返回的结果数据
 */
data class TranslateResponse(
    /**
     * 响应状态码
     * 0 表示成功，其他值表示失败
     */
    val code: Int,

    /**
     * 响应消息
     * 通常是失败时的返回失败信息，成功时可能为空或包含成功信息
     */
    val msg: String,

    /**
     * 翻译结果数据
     * 包含具体的翻译内容和相关信息
     */
    val data: TextTranslateResp
)

/**
 * 翻译结果数据类
 * 包含翻译的源文本、目标文本以及语言信息
 */
data class TextTranslateResp(
    /**
     * 翻译后的文本
     * 示例值：你好
     */
    val translateText: String,

    /**
     * 源语言
     * 详见语言列表，如：en（英文）
     */
    val sourceLanguage: String,

    /**
     * 目标语言
     * 详见语言列表，如：zh（中文）
     */
    val targetLanguage: String
)

/**
 * Translate API 错误码枚举
 */
enum class TranslateApiErrorCode(val code: Int, val message: String) {
    /** 成功 */
    SUCCESS(0, "成功"),

    /** 认证token错误 */
    UNAUTHORIZED(401, "认证token错误"),

    /** 参数错误 */
    BAD_REQUEST(400, "参数错误"),

    /** 服务内部服务器错误 */
    INTERNAL_ERROR(500, "服务内部服务器错误"),

    /** 敏感词错误 */
    SENSITIVE_WORD(20003, "Hit sensitive word"),

    /** 认证未知错误 */
    AUTH_UNKNOWN_ERROR(31001, "auth unknown error"),

    /** 用户用量达限 */
    AUTH_USAGE_LIMIT(31005, "auth open api usage limit reached"),

    /** 认证内部错误 */
    AUTH_INTERNAL_ERROR(31006, "auth internal error"),

    /** 翻译请求超时 */
    TRANSLATE_TIMEOUT(31101, "translate request timeout"),

    /** 翻译内部错误 */
    TRANSLATE_INTERNAL_ERROR(31102, "translate internal error"),

    /** 翻译参数错误 */
    TRANSLATE_PARAMETER_ERROR(31103, "translate parameter error"),

    /** 翻译频率受限 */
    TRANSLATE_FREQUENCY_LIMITED(31104, "translate frequency limited"),

    /** 翻译目标语言不支持 */
    TRANSLATE_LANGUAGE_NOT_SUPPORTED(31105, "translate target language not supported"),

    /** 翻译查询字符串过长 */
    TRANSLATE_QUERY_TOO_LONG(31106, "translate query string too long"),

    /** 引擎查询字符串过长 */
    ENGINE_QUERY_TOO_LONG(31201, "engine-biz query string too long"),

    /** 引擎查询字符串为空 */
    ENGINE_QUERY_EMPTY(31202, "engine-biz query string is empty"),

    /** 引擎参数无效 */
    ENGINE_PARAMETER_INVALID(31203, "engine-biz parameter invalid"),

    /** 系统内部错误 */
    SYSTEM_INTERNAL_ERROR(282000, "internal error"),

    /** 缺少必需参数 */
    MISSING_REQUIRED_PARAMS(282003, "missing required parameter(s)"),

    /** 无效参数 */
    INVALID_PARAMETERS(282004, "invalid parameter(s)")
}