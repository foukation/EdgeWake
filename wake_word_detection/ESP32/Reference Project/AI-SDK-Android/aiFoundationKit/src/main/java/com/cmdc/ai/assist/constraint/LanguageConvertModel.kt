package com.cmdc.ai.assist.constraint

/**
 * 多语言支持枚举类
 *
 * 该枚举类定义了翻译系统支持的所有语言及其相关属性。
 * 数据来源：语言.docx 文件
 *
 * @param displayName 语言的中文显示名称
 * @param code 语言的国际标准代码
 * @param detectSupported 是否支持自动语种检测
 */
enum class LanguageConvertModel(
    private val displayName: String,
    val code: String,
    val detectSupported: Boolean
) {
    /** 英语 - 代码: en, 检测: 支持 */
    LANG_EN("英语", "en", true),

    /** 简体中文 - 代码: zh, 检测: 支持 */
    LANG_ZH("简体中文", "zh", true),

    /** 繁体中文 - 代码: zh_tw, 检测: 支持 */
    LANG_ZH_TW("繁体中文", "zh_tw", true),

    /** 俄语 - 代码: ru, 检测: 支持 */
    LANG_RU("俄语", "ru", true),

    /** 日语 - 代码: ja, 检测: 支持 */
    LANG_JA("日语", "ja", true),

    /** 韩语 - 代码: ko, 检测: 支持 */
    LANG_KO("韩语", "ko", true),

    /** 西班牙语 - 代码: es, 检测: 支持 */
    LANG_ES("西班牙语", "es", true),

    /** 法语 - 代码: fr, 检测: 支持 */
    LANG_FR("法语", "fr", true),

    /** 葡萄牙语 - 代码: pt, 检测: 支持 */
    LANG_PT("葡萄牙语", "pt", true),

    /** 德语 - 代码: de, 检测: 支持 */
    LANG_DE("德语", "de", true),

    /** 意大利语 - 代码: it, 检测: 支持 */
    LANG_IT("意大利语", "it", true),

    /** 泰语 - 代码: th, 检测: 支持 */
    LANG_TH("泰语", "th", true),

    /** 越南语 - 代码: vi, 检测: 支持 */
    LANG_VI("越南语", "vi", true),

    /** 印度尼西亚语 - 代码: id, 检测: 支持 */
    LANG_ID("印度尼西亚语", "id", true),

    /** 马来语 - 代码: ms, 检测: 支持 */
    LANG_MS("马来语", "ms", true),

    /** 阿拉伯语 - 代码: ar, 检测: 支持 */
    LANG_AR("阿拉伯语", "ar", true),

    /** 印地语 - 代码: hi, 检测: 支持 */
    LANG_HI("印地语", "hi", true),

    /** 希伯来语 - 代码: he, 检测: 支持 */
    LANG_HE("希伯来语", "he", true),

    /** 缅甸语 - 代码: my, 检测: 支持 */
    LANG_MY("缅甸语", "my", true),

    /** 泰米尔语 - 代码: ta, 检测: 支持 */
    LANG_TA("泰米尔语", "ta", true),

    /** 乌尔都语 - 代码: ur, 检测: 支持 */
    LANG_UR("乌尔都语", "ur", true),

    /** 孟加拉语 - 代码: bn, 检测: 支持 */
    LANG_BN("孟加拉语", "bn", true),

    /** 波兰语 - 代码: pl, 检测: 支持 */
    LANG_PL("波兰语", "pl", true),

    /** 荷兰语 - 代码: nl, 检测: 支持 */
    LANG_NL("荷兰语", "nl", true),

    /** 罗马尼亚语 - 代码: ro, 检测: 支持 */
    LANG_RO("罗马尼亚语", "ro", true),

    /** 土耳其语 - 代码: tr, 检测: 支持 */
    LANG_TR("土耳其语", "tr", true),

    /** 高棉语 - 代码: km, 检测: 支持 */
    LANG_KM("高棉语", "km", true),

    /** 老挝语 - 代码: lo, 检测: 不支持 */
    LANG_LO("老挝语", "lo", true),

    /** 粤语 - 代码: yue, 检测: 支持 */
    LANG_YUE("粤语", "yue", true),

    /** 捷克语 - 代码: cs, 检测: 支持 */
    LANG_CS("捷克语", "cs", true),

    /** 希腊语 - 代码: el, 检测: 支持 */
    LANG_EL("希腊语", "el", true),

    /** 瑞典语 - 代码: sv, 检测: 支持 */
    LANG_SV("瑞典语", "sv", true),

    /** 匈牙利语 - 代码: hu, 检测: 支持 */
    LANG_HU("匈牙利语", "hu", true),

    /** 丹麦语 - 代码: da, 检测: 支持 */
    LANG_DA("丹麦语", "da", true),

    /** 芬兰语 - 代码: fi, 检测: 支持 */
    LANG_FI("芬兰语", "fi", true),

    /** 乌克兰语 - 代码: uk, 检测: 支持 */
    LANG_UK("乌克兰语", "uk", true),

    /** 保加利亚语 - 代码: bg, 检测: 支持 */
    LANG_BG("保加利亚语", "bg", true),

    /** 塞尔维亚语 - 代码: sr, 检测: 支持 */
    LANG_SR("塞尔维亚语", "sr", true),

    /** 泰卢固语 - 代码: te, 检测: 支持 */
    LANG_TE("泰卢固语", "te", true),

    /** 南非荷兰语 - 代码: af, 检测: 支持 */
    LANG_AF("南非荷兰语", "af", true),

    /** 亚美尼亚语 - 代码: hy, 检测: 不支持 */
    LANG_HY("亚美尼亚语", "hy", true),

    /** 阿萨姆语 - 代码: as, 检测: 不支持 */
    LANG_AS("阿萨姆语", "as", true),

    /** 阿斯图里亚斯语 - 代码: ast, 检测: 不支持 */
    LANG_AST("阿斯图里亚斯语", "ast", true),

    /** 巴斯克语 - 代码: eu, 检测: 支持 */
    LANG_EU("巴斯克语", "eu", true),

    /** 白俄罗斯语 - 代码: be, 检测: 支持 */
    LANG_BE("白俄罗斯语", "be", true),

    /** 波斯尼亚语 - 代码: bs, 检测: 支持 */
    LANG_BS("波斯尼亚语", "bs", true),

    /** 加泰罗尼亚语 - 代码: ca, 检测: 支持 */
    LANG_CA("加泰罗尼亚语", "ca", true),

    /** 宿务语 - 代码: ceb, 检测: 不支持 */
    LANG_CEB("宿务语", "ceb", true),

    /** 克罗地亚语 - 代码: hr, 检测: 支持 */
    LANG_HR("克罗地亚语", "hr", true),

    /** 埃及阿拉伯语 - 代码: arz, 检测: 不支持 */
    LANG_ARZ("埃及阿拉伯语", "arz", true),

    /** 爱沙尼亚语 - 代码: et, 检测: 支持 */
    LANG_ET("爱沙尼亚语", "et", true),

    /** 加利西亚语 - 代码: gl, 检测: 支持 */
    LANG_GL("加利西亚语", "gl", true),

    /** 格鲁吉亚语 - 代码: ka, 检测: 支持 */
    LANG_KA("格鲁吉亚语", "ka", true),

    /** 古吉拉特语 - 代码: gu, 检测: 支持 */
    LANG_GU("古吉拉特语", "gu", true),

    /** 冰岛语 - 代码: is, 检测: 支持 */
    LANG_IS("冰岛语", "is", true),

    /** 爪哇语 - 代码: jv, 检测: 不支持 */
    LANG_JV("爪哇语", "jv", true),

    /** 卡纳达语 - 代码: kn, 检测: 支持 */
    LANG_KN("卡纳达语", "kn", true),

    /** 哈萨克语 - 代码: kk, 检测: 支持 */
    LANG_KK("哈萨克语", "kk", true),

    /** 拉脱维亚语 - 代码: lv, 检测: 支持 */
    LANG_LV("拉脱维亚语", "lv", true),

    /** 立陶宛语 - 代码: lt, 检测: 支持 */
    LANG_LT("立陶宛语", "lt", true),

    /** 卢森堡语 - 代码: lb, 检测: 不支持 */
    LANG_LB("卢森堡语", "lb", true),

    /** 马其顿语 - 代码: mk, 检测: 支持 */
    LANG_MK("马其顿语", "mk", true),

    /** 马加希语 - 代码: mai, 检测: 不支持 */
    LANG_MAI("马加希语", "mai", true),

    /** 马耳他语 - 代码: mt, 检测: 支持 */
    LANG_MT("马耳他语", "mt", true),

    /** 马拉地语 - 代码: mr, 检测: 支持 */
    LANG_MR("马拉地语", "mr", true),

    /** 美索不达米亚阿拉伯语 - 代码: acm, 检测: 不支持 */
    LANG_ACM("美索不达米亚阿拉伯语", "acm", true),

    /** 摩洛哥阿拉伯语 - 代码: ary, 检测: 不支持 */
    LANG_ARY("摩洛哥阿拉伯语", "ary", true),

    /** 内志阿拉伯语 - 代码: ars, 检测: 不支持 */
    LANG_ARS("内志阿拉伯语", "ars", true),

    /** 尼泊尔语 - 代码: ne, 检测: 支持 */
    LANG_NE("尼泊尔语", "ne", true),

    /** 北阿塞拜疆语 - 代码: az, 检测: 支持 */
    LANG_AZ("北阿塞拜疆语", "az", true),

    /** 北黎凡特阿拉伯语 - 代码: apc, 检测: 不支持 */
    LANG_APC("北黎凡特阿拉伯语", "apc", true),

    /** 北乌兹别克语 - 代码: uz, 检测: 支持 */
    LANG_UZ("北乌兹别克语", "uz", true),

    /** 书面语挪威语 - 代码: nb, 检测: 支持 */
    LANG_NB("书面语挪威语", "nb", true),

    /** 新挪威语 - 代码: nn, 检测: 支持 */
    LANG_NN("新挪威语", "nn", true),

    /** 奥克语 - 代码: oc, 检测: 不支持 */
    LANG_OC("奥克语", "oc", true),

    /** 奥里亚语 - 代码: or, 检测: 支持 */
    LANG_OR("奥里亚语", "or", true),

    /** 邦阿西楠语 - 代码: pag, 检测: 不支持 */
    LANG_PAG("邦阿西楠语", "pag", true),

    /** 西西里语 - 代码: scn, 检测: 不支持 */
    LANG_SCN("西西里语", "scn", true),

    /** 信德语 - 代码: sd, 检测: 不支持 */
    LANG_SD("信德语", "sd", true),

    /** 僧伽罗语 - 代码: si, 检测: 支持 */
    LANG_SI("僧伽罗语", "si", true),

    /** 斯洛伐克语 - 代码: sk, 检测: 支持 */
    LANG_SK("斯洛伐克语", "sk", true),

    /** 斯洛文尼亚语 - 代码: sl, 检测: 支持 */
    LANG_SL("斯洛文尼亚语", "sl", true),

    /** 南黎凡特阿拉伯语 - 代码: ajp, 检测: 不支持 */
    LANG_AJP("南黎凡特阿拉伯语", "ajp", true),

    /** 斯瓦希里语 - 代码: sw, 检测: 支持 */
    LANG_SW("斯瓦希里语", "sw", true),

    /** 他加禄语 - 代码: tl, 检测: 支持 */
    LANG_TL("他加禄语", "tl", true),

    /** 塔伊兹-亚丁阿拉伯语 - 代码: acq, 检测: 不支持 */
    LANG_ACQ("塔伊兹-亚丁阿拉伯语", "acq", true),

    /** 托斯克阿尔巴尼亚语 - 代码: sq, 检测: 支持 */
    LANG_SQ("托斯克阿尔巴尼亚语", "sq", true),

    /** 突尼斯阿拉伯语 - 代码: aeb, 检测: 不支持 */
    LANG_AEB("突尼斯阿拉伯语", "aeb", true),

    /** 威尼斯语 - 代码: vec, 检测: 不支持 */
    LANG_VEC("威尼斯语", "vec", true),

    /** 瓦莱语 - 代码: war, 检测: 不支持 */
    LANG_WAR("瓦莱语", "war", true),

    /** 威尔士语 - 代码: cy, 检测: 支持 */
    LANG_CY("威尔士语", "cy", true),

    /** 西波斯语 - 代码: fa, 检测: 支持 */
    LANG_FA("西波斯语", "fa", true);

}