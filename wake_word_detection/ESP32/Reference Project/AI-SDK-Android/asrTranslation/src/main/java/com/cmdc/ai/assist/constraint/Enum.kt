package com.cmdc.ai.assist.constraint

/**
 * 用于定义翻译类型。
 *
 * 每个枚举常量都代表一种翻译方向，并与其对应的整数代码关联
 */
enum class TranslationTypeCode(val code: Int) {
    /**
     * ZH_TO_EN 代表从中文到英文的翻译方向，关联代码为 0
     */
    ZH_TO_EN(0),

    /**
     * EN_TO_ZH 代表从英文到中文的翻译方向，关联代码为 1
     */
    EN_TO_ZH(1),
}

enum class Language(val languageCode: String) {

    /**
     * 中文（普通话）
     * */
    CHINESE("zh"),

    /**
     * 英语
     * */
    ENGLISH("en"),

    /**
     * 粤语
     * */
    CANTONESE("yue"),

    /**
     * 日语
     * */
    JAPANESE("jp"),

    /**
     * 韩语
     * */
    KOREAN("kor"),

    /**
     * 法语
     * */
    FRENCH("fra"),

    /**
     * 西班牙语
     * */
    SPANISH("spa"),

    /**
     * 泰语
     * */
    THAI("th"),

    /**
     * 阿拉伯语(巴林)
     * */
    ARABIC("ara"),

    /**
     * 俄语
     * */
    RUSSIAN("ru"),

    /**
     * 葡萄牙语
     * */
    PORTUGUESE("pt"),

    /**
     * 德语
     * */
    GERMAN("de"),

    /**
     * 意大利语
     * */
    ITALIAN("it"),

    /**
     * 希腊语
     * */
    GREEK("el"),

    /**
     * 荷兰语
     * */
    DUTCH("nl"),

    /**
     * 波兰语
     * */
    POLISH("pl"),

    /**
     * 保加利亚语
     * */
    BULGARIAN("bul"),

    /**
     * 丹麦语
     * */
    DANISH("dan"),

    /**
     * 芬兰语
     * */
    FINNISH("fin"),

    /**
     * 捷克语
     * */
    CZECH("cs"),

    /**
     * 罗马尼亚语
     * */
    ROMANIAN("rom"),

    /**
     * 瑞典语
     * */
    SWEDISH("swe"),

    /**
     * 匈牙利语
     * */
    HUNGARIAN("hu"),

    /**
     * 越南语
     * */
    VIETNAMESE("vie"),

    /**
     * 印度尼西亚语
     * */
    INDONESIAN("id"),

    /**
     * 加泰罗尼亚语
     * */
    CATALAN("cat"),

    /**
     * 希伯来语
     * */
    HEBREW("heb"),

    /**
     * 印地语
     * */
    HINDI("hi"),

    /**
     * 马来语
     * */
    MALAY("may"),

    /**
     * 挪威语
     * */
    NORWEGIAN("nor"),

    /**
     * 冰岛语
     * */
    ICELANDIC("ice"),

    /**
     * 菲律宾语
     * */
    FILIPINO("fil"),

    /**
     * 高棉语
     * */
    KHMER("hkm"),

    /**
     * 克罗地亚语
     * */
    CROATIAN("hrv"),

    /**
     * 拉脱维亚语
     * */
    LATVIAN("lav"),

    /**
     * 孟加拉语
     * */
    BENGALI("ben"),

    /**
     * 尼泊尔语
     * */
    NEPALI("nep"),

    /**
     * 南非荷兰语
     * */
    AFRIKAANS("afr"),

    /**
     * 斯洛伐克语
     * */
    SLOVAK("sk"),

    /**
     * 僧伽罗语
     * */
    SINHALA("sin"),

    /**
     * 塞尔维亚语(拉丁文)
     * */
    SERBIAN("srp"),

    /**
     * 斯瓦希里语(坦桑尼亚)
     * */
    SWAHILI("swa"),

    /**
     * 土耳其语
     * */
    TURKISH("tr"),

    /**
     * 乌克兰语
     * */
    UKRAINIAN("ukr"),

    /**
     * 亚美尼亚语
     * */
    ARMENIAN("arm")
}