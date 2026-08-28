package com.cmdc.ai.assist.constraint

/**
 * 语言模型枚举类
 *
 * @param pid 模型的唯一标识符
 * @param model 语言模型的名称
 * @param des 语言模型的标点符号支持描述
 */
enum class LanguageModel(
    val pid: Int,
    val model: String,
    val des: String
) {

    CHINESE_MANDARIN_BASIC_PUNCTUATION(1537, "中文普通话", "弱标点（逗号，句号）"),
    CHINESE_MANDARIN_ENHANCED_PUNCTUATION(15372, "中文普通话", "加强标点（逗号、句号、问号、感叹号）"),
    CHINESE_DIALECTS_BASIC_PUNCTUATION(15376, "中文多方言", "弱标点（逗号，句号）"),
    ENGLISH_NO_PUNCTUATION(1737, "英语", "无标点"),
    ENGLISH_ENHANCED_PUNCTUATION(17372, "英语", "加强标点（逗号、句号、问号） "),

}

