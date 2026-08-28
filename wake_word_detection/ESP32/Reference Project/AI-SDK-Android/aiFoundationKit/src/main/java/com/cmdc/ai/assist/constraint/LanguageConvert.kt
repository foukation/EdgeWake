package com.cmdc.ai.assist.constraint
/**
 * 多语言支持枚举类
 *
 * 该枚举类定义了翻译系统支持的所有语言及其相关属性。
 * 数据来源：翻译_语种.csv 文件
 *
 * @param displayName 语言的中文显示名称
 * @param code 语言的国际标准代码
 * @param detectSupported 是否支持自动语种检测
 */
enum class LanguageConvert(
    private val displayName: String,
    val code: String,
    val detectSupported: Boolean
) {
    /** 下索布语 - 代码: los, 检测: 不支持 */
    LANG_LOS("下索布语", "los", false),

    /** 世界语 - 代码: epo, 检测: 支持 */
    LANG_EPO("世界语", "epo", true),

    /** 中古法语 - 代码: frm, 检测: 不支持 */
    LANG_FRM("中古法语", "frm", false),

    /** 中文(文言文) - 代码: wyw, 检测: 不支持 */
    LANG_WYW("中文(文言文)", "wyw", false),

    /** 中文(简体) - 代码: zh, 检测: 支持 */
    LANG_ZH("中文(简体)", "zh", true),

    /** 中文(粤语) - 代码: yue, 检测: 支持 */
    LANG_YUE("中文(粤语)", "yue", true),

    /** 中文(繁体) - 代码: cht, 检测: 支持 */
    LANG_CHT("中文(繁体)", "cht", true),

    /** 丹麦语 - 代码: dan, 检测: 支持 */
    LANG_DAN("丹麦语", "dan", true),

    /** 乌克兰语 - 代码: ukr, 检测: 支持 */
    LANG_UKR("乌克兰语", "ukr", true),

    /** 乌尔都语 - 代码: urd, 检测: 不支持 */
    LANG_URD("乌尔都语", "urd", false),

    /** 书面挪威语 - 代码: nob, 检测: 支持 */
    LANG_NOB("书面挪威语", "nob", true),

    /** 亚美尼亚语 - 代码: arm, 检测: 支持 */
    LANG_ARM("亚美尼亚语", "arm", true),

    /** 亚齐语 - 代码: ach, 检测: 不支持 */
    LANG_ACH("亚齐语", "ach", false),

    /** 他加禄语 - 代码: tgl, 检测: 支持 */
    LANG_TGL("他加禄语", "tgl", true),

    /** 伊努克提图特语 - 代码: iku, 检测: 不支持 */
    LANG_IKU("伊努克提图特语", "iku", false),

    /** 伊博语 - 代码: ibo, 检测: 不支持 */
    LANG_IBO("伊博语", "ibo", false),

    /** 伊多语 - 代码: ido, 检测: 不支持 */
    LANG_IDO("伊多语", "ido", false),

    /** 低地德语 - 代码: log, 检测: 支持 */
    LANG_LOG("低地德语", "log", true),

    /** 俄语 - 代码: ru, 检测: 支持 */
    LANG_RU("俄语", "ru", true),

    /** 保加利亚语 - 代码: bul, 检测: 支持 */
    LANG_BUL("保加利亚语", "bul", true),

    /** 信德语 - 代码: snd, 检测: 不支持 */
    LANG_SND("信德语", "snd", false),

    /** 修纳语 - 代码: sna, 检测: 不支持 */
    LANG_SNA("修纳语", "sna", false),

    /** 俾路支语 - 代码: bal, 检测: 不支持 */
    LANG_BAL("俾路支语", "bal", false),

    /** 僧伽罗语 - 代码: sin, 检测: 不支持 */
    LANG_SIN("僧伽罗语", "sin", false),

    /** 克丘亚语 - 代码: que, 检测: 不支持 */
    LANG_QUE("克丘亚语", "que", false),

    /** 克什米尔语 - 代码: kas, 检测: 不支持 */
    LANG_KAS("克什米尔语", "kas", false),

    /** 克林贡语 - 代码: kli, 检测: 不支持 */
    LANG_KLI("克林贡语", "kli", false),

    /** 克罗地亚语 - 代码: hrv, 检测: 支持 */
    LANG_HRV("克罗地亚语", "hrv", true),

    /** 克里克语 - 代码: cre, 检测: 不支持 */
    LANG_CRE("克里克语", "cre", false),

    /** 克里米亚鞑靼语 - 代码: cri, 检测: 不支持 */
    LANG_CRI("克里米亚鞑靼语", "cri", false),

    /** 冰岛语 - 代码: ice, 检测: 支持 */
    LANG_ICE("冰岛语", "ice", true),

    /** 切罗基语 - 代码: chr, 检测: 不支持 */
    LANG_CHR("切罗基语", "chr", false),

    /** 刚果语 - 代码: kon, 检测: 不支持 */
    LANG_KON("刚果语", "kon", false),

    /** 加利西亚语 - 代码: glg, 检测: 支持 */
    LANG_GLG("加利西亚语", "glg", true),

    /** 加拿大法语 - 代码: frn, 检测: 不支持 */
    LANG_FRN("加拿大法语", "frn", false),

    /** 加泰罗尼亚语 - 代码: cat, 检测: 支持 */
    LANG_CAT("加泰罗尼亚语", "cat", true),

    /** 匈牙利语 - 代码: hu, 检测: 支持 */
    LANG_HU("匈牙利语", "hu", true),

    /** 北方萨米语 - 代码: sme, 检测: 不支持 */
    LANG_SME("北方萨米语", "sme", false),

    /** 北索托语 - 代码: ped, 检测: 不支持 */
    LANG_PED("北索托语", "ped", false),

    /** 南恩德贝莱语 - 代码: nbl, 检测: 不支持 */
    LANG_NBL("南恩德贝莱语", "nbl", false),

    /** 南索托语 - 代码: sot, 检测: 不支持 */
    LANG_SOT("南索托语", "sot", false),

    /** 南非荷兰语 - 代码: afr, 检测: 支持 */
    LANG_AFR("南非荷兰语", "afr", true),

    /** 博杰普尔语 - 代码: bho, 检测: 不支持 */
    LANG_BHO("博杰普尔语", "bho", false),

    /** 卡努里语 - 代码: kau, 检测: 不支持 */
    LANG_KAU("卡努里语", "kau", false),

    /** 卡拜尔语 - 代码: kab, 检测: 支持 */
    LANG_KAB("卡拜尔语", "kab", true),

    /** 卡纳达语 - 代码: kan, 检测: 支持 */
    LANG_KAN("卡纳达语", "kan", true),

    /** 卡舒比语 - 代码: kah, 检测: 不支持 */
    LANG_KAH("卡舒比语", "kah", false),

    /** 卢干达语 - 代码: lug, 检测: 不支持 */
    LANG_LUG("卢干达语", "lug", false),

    /** 卢旺达语 - 代码: kin, 检测: 支持 */
    LANG_KIN("卢旺达语", "kin", true),

    /** 卢森堡语 - 代码: ltz, 检测: 不支持 */
    LANG_LTZ("卢森堡语", "ltz", false),

    /** 卢森尼亚语 - 代码: ruy, 检测: 不支持 */
    LANG_RUY("卢森尼亚语", "ruy", false),

    /** 印古什语 - 代码: ing, 检测: 不支持 */
    LANG_ING("印古什语", "ing", false),

    /** 印地语 - 代码: hi, 检测: 支持 */
    LANG_HI("印地语", "hi", true),

    /** 印尼语 - 代码: id, 检测: 不支持 */
    LANG_ID("印尼语", "id", false),

    /** 叙利亚语 - 代码: syr, 检测: 不支持 */
    LANG_SYR("叙利亚语", "syr", false),

    /** 古吉拉特语 - 代码: guj, 检测: 支持 */
    LANG_GUJ("古吉拉特语", "guj", true),

    /** 古希腊语 - 代码: gra, 检测: 不支持 */
    LANG_GRA("古希腊语", "gra", false),

    /** 古英语 - 代码: eno, 检测: 不支持 */
    LANG_ENO("古英语", "eno", false),

    /** 吉尔吉斯语 - 代码: kir, 检测: 不支持 */
    LANG_KIR("吉尔吉斯语", "kir", false),

    /** 哈卡钦语 - 代码: hak, 检测: 不支持 */
    LANG_HAK("哈卡钦语", "hak", false),

    /** 因特语 - 代码: ina, 检测: 不支持 */
    LANG_INA("因特语", "ina", false),

    /** 土库曼语 - 代码: tuk, 检测: 不支持 */
    LANG_TUK("土库曼语", "tuk", false),

    /** 土耳其语 - 代码: tr, 检测: 支持 */
    LANG_TR("土耳其语", "tr", true),

    /** 塔吉克语 - 代码: tgk, 检测: 不支持 */
    LANG_TGK("塔吉克语", "tgk", false),

    /** 塞尔维亚-克罗地亚语 - 代码: sec, 检测: 不支持 */
    LANG_SEC("塞尔维亚-克罗地亚语", "sec", false),

    /** 塞尔维亚语 - 代码: srp, 检测: 支持 */
    LANG_SRP("塞尔维亚语", "srp", true),

    /** 塞尔维亚语（西里尔） - 代码: src, 检测: 不支持 */
    LANG_SRC("塞尔维亚语（西里尔）", "src", false),

    /** 夏威夷语 - 代码: haw, 检测: 不支持 */
    LANG_HAW("夏威夷语", "haw", false),

    /** 契维语 - 代码: twi, 检测: 不支持 */
    LANG_TWI("契维语", "twi", false),

    /** 奥克语 - 代码: oci, 检测: 不支持 */
    LANG_OCI("奥克语", "oci", false),

    /** 奥塞梯语 - 代码: oss, 检测: 不支持 */
    LANG_OSS("奥塞梯语", "oss", false),

    /** 奥杰布瓦语 - 代码: oji, 检测: 不支持 */
    LANG_OJI("奥杰布瓦语", "oji", false),

    /** 奥罗莫语 - 代码: orm, 检测: 不支持 */
    LANG_ORM("奥罗莫语", "orm", false),

    /** 奥里亚语 - 代码: ori, 检测: 支持 */
    LANG_ORI("奥里亚语", "ori", true),

    /** 威尔士语 - 代码: wel, 检测: 不支持 */
    LANG_WEL("威尔士语", "wel", false),

    /** 孔卡尼语 - 代码: kok, 检测: 不支持 */
    LANG_KOK("孔卡尼语", "kok", false),

    /** 孟加拉语 - 代码: ben, 检测: 不支持 */
    LANG_BEN("孟加拉语", "ben", false),

    /** 宿务语 - 代码: ceb, 检测: 不支持 */
    LANG_CEB("宿务语", "ceb", false),

    /** 富拉尼语 - 代码: ful, 检测: 不支持 */
    LANG_FUL("富拉尼语", "ful", false),

    /** 尼泊尔语 - 代码: nep, 检测: 不支持 */
    LANG_NEP("尼泊尔语", "nep", false),

    /** 巴什基尔语 - 代码: bak, 检测: 不支持 */
    LANG_BAK("巴什基尔语", "bak", false),

    /** 巴斯克语 - 代码: baq, 检测: 支持 */
    LANG_BAQ("巴斯克语", "baq", true),

    /** 巴西葡萄牙语 - 代码: pot, 检测: 不支持 */
    LANG_POT("巴西葡萄牙语", "pot", false),

    /** 巽他语 - 代码: sun, 检测: 不支持 */
    LANG_SUN("巽他语", "sun", false),

    /** 布列塔尼语 - 代码: bre, 检测: 不支持 */
    LANG_BRE("布列塔尼语", "bre", false),

    /** 希伯来语 - 代码: heb, 检测: 支持 */
    LANG_HEB("希伯来语", "heb", true),

    /** 希利盖农语 - 代码: hil, 检测: 不支持 */
    LANG_HIL("希利盖农语", "hil", false),

    /** 希腊语 - 代码: el, 检测: 不支持 */
    LANG_EL("希腊语", "el", false),

    /** 帕皮阿门托语 - 代码: pap, 检测: 不支持 */
    LANG_PAP("帕皮阿门托语", "pap", false),

    /** 库尔德语 - 代码: kur, 检测: 不支持 */
    LANG_KUR("库尔德语", "kur", false),

    /** 康瓦尔语 - 代码: cor, 检测: 不支持 */
    LANG_COR("康瓦尔语", "cor", false),

    /** 弗留利语 - 代码: fri, 检测: 不支持 */
    LANG_FRI("弗留利语", "fri", false),

    /** 德语 - 代码: de, 检测: 支持 */
    LANG_DE("德语", "de", true),

    /** 德顿语 - 代码: tet, 检测: 不支持 */
    LANG_TET("德顿语", "tet", false),

    /** 意大利语 - 代码: it, 检测: 支持 */
    LANG_IT("意大利语", "it", true),

    /** 意第绪语 - 代码: yid, 检测: 不支持 */
    LANG_YID("意第绪语", "yid", false),

    /** 扎扎其语 - 代码: zaz, 检测: 不支持 */
    LANG_ZAZ("扎扎其语", "zaz", false),

    /** 拉丁语 - 代码: lat, 检测: 支持 */
    LANG_LAT("拉丁语", "lat", true),

    /** 拉特加莱语 - 代码: lag, 检测: 不支持 */
    LANG_LAG("拉特加莱语", "lag", false),

    /** 拉脱维亚语 - 代码: lav, 检测: 支持 */
    LANG_LAV("拉脱维亚语", "lav", true),

    /** 挪威语 - 代码: nor, 检测: 支持 */
    LANG_NOR("挪威语", "nor", true),

    /** 捷克语 - 代码: cs, 检测: 支持 */
    LANG_CS("捷克语", "cs", true),

    /** 掸语 - 代码: sha, 检测: 不支持 */
    LANG_SHA("掸语", "sha", false),

    /** 提格利尼亚语 - 代码: tir, 检测: 不支持 */
    LANG_TIR("提格利尼亚语", "tir", false),

    /** 文达语 - 代码: ven, 检测: 不支持 */
    LANG_VEN("文达语", "ven", false),

    /** 斯洛伐克语 - 代码: sk, 检测: 不支持 */
    LANG_SK("斯洛伐克语", "sk", false),

    /** 斯洛文尼亚语 - 代码: slo, 检测: 支持 */
    LANG_SLO("斯洛文尼亚语", "slo", true),

    /** 斯瓦希里语 - 代码: swa, 检测: 支持 */
    LANG_SWA("斯瓦希里语", "swa", true),

    /** 新挪威语 - 代码: nno, 检测: 支持 */
    LANG_NNO("新挪威语", "nno", true),

    /** 旁遮普语 - 代码: pan, 检测: 支持 */
    LANG_PAN("旁遮普语", "pan", true),

    /** 日语 - 代码: jp, 检测: 支持 */
    LANG_JP("日语", "jp", true),

    /** 普什图语 - 代码: pus, 检测: 不支持 */
    LANG_PUS("普什图语", "pus", false),

    /** 曼克斯语 - 代码: glv, 检测: 不支持 */
    LANG_GLV("曼克斯语", "glv", false),

    /** 本巴语 - 代码: bem, 检测: 不支持 */
    LANG_BEM("本巴语", "bem", false),

    /** 林加拉语 - 代码: lin, 检测: 不支持 */
    LANG_LIN("林加拉语", "lin", false),

    /** 林堡语 - 代码: lim, 检测: 不支持 */
    LANG_LIM("林堡语", "lim", false),

    /** 柏柏尔语 - 代码: ber, 检测: 支持 */
    LANG_BER("柏柏尔语", "ber", true),

    /** 格陵兰语 - 代码: kal, 检测: 不支持 */
    LANG_KAL("格陵兰语", "kal", false),

    /** 格鲁吉亚语 - 代码: geo, 检测: 不支持 */
    LANG_GEO("格鲁吉亚语", "geo", false),

    /** 桑海语 - 代码: sol, 检测: 不支持 */
    LANG_SOL("桑海语", "sol", false),

    /** 梵语 - 代码: san, 检测: 不支持 */
    LANG_SAN("梵语", "san", false),

    /** 楚瓦什语 - 代码: chv, 检测: 不支持 */
    LANG_CHV("楚瓦什语", "chv", false),

    /** 比斯拉马语 - 代码: bis, 检测: 不支持 */
    LANG_BIS("比斯拉马语", "bis", false),

    /** 比林语 - 代码: bli, 检测: 不支持 */
    LANG_BLI("比林语", "bli", false),

    /** 毛利语 - 代码: mao, 检测: 不支持 */
    LANG_MAO("毛利语", "mao", false),

    /** 毛里求斯克里奥尔语 - 代码: mau, 检测: 不支持 */
    LANG_MAU("毛里求斯克里奥尔语", "mau", false),

    /** 沃洛夫语 - 代码: wol, 检测: 不支持 */
    LANG_WOL("沃洛夫语", "wol", false),

    /** 法罗语 - 代码: fao, 检测: 不支持 */
    LANG_FAO("法罗语", "fao", false),

    /** 法语 - 代码: fra, 检测: 支持 */
    LANG_FRA("法语", "fra", true),

    /** 波兰语 - 代码: pl, 检测: 支持 */
    LANG_PL("波兰语", "pl", true),

    /** 波斯尼亚语 - 代码: bos, 检测: 不支持 */
    LANG_BOS("波斯尼亚语", "bos", false),

    /** 波斯语 - 代码: per, 检测: 支持 */
    LANG_PER("波斯语", "per", true),

    /** 泰卢固语 - 代码: tel, 检测: 支持 */
    LANG_TEL("泰卢固语", "tel", true),

    /** 泰米尔语 - 代码: tam, 检测: 支持 */
    LANG_TAM("泰米尔语", "tam", true),

    /** 泰语 - 代码: th, 检测: 支持 */
    LANG_TH("泰语", "th", true),

    /** 海地语 - 代码: ht, 检测: 不支持 */
    LANG_HT("海地语", "ht", false),

    /** 爪哇语 - 代码: jav, 检测: 不支持 */
    LANG_JAV("爪哇语", "jav", false),

    /** 爱尔兰语 - 代码: gle, 检测: 支持 */
    LANG_GLE("爱尔兰语", "gle", true),

    /** 爱沙尼亚语 - 代码: est, 检测: 支持 */
    LANG_EST("爱沙尼亚语", "est", true),

    /** 瑞典语 - 代码: swe, 检测: 支持 */
    LANG_SWE("瑞典语", "swe", true),

    /** 瓜拉尼语 - 代码: grn, 检测: 不支持 */
    LANG_GRN("瓜拉尼语", "grn", false),

    /** 瓦隆语 - 代码: wln, 检测: 支持 */
    LANG_WLN("瓦隆语", "wln", true),

    /** 白俄罗斯语 - 代码: bel, 检测: 支持 */
    LANG_BEL("白俄罗斯语", "bel", true),

    /** 盖尔语 - 代码: gla, 检测: 不支持 */
    LANG_GLA("盖尔语", "gla", false),

    /** 祖鲁语 - 代码: zul, 检测: 不支持 */
    LANG_ZUL("祖鲁语", "zul", false),

    /** 科萨语 - 代码: xho, 检测: 不支持 */
    LANG_XHO("科萨语", "xho", false),

    /** 科西嘉语 - 代码: cos, 检测: 不支持 */
    LANG_COS("科西嘉语", "cos", false),

    /** 突尼斯阿拉伯语 - 代码: tua, 检测: 不支持 */
    LANG_TUA("突尼斯阿拉伯语", "tua", false),

    /** 立陶宛语 - 代码: lit, 检测: 不支持 */
    LANG_LIT("立陶宛语", "lit", false),

    /** 索马里语 - 代码: som, 检测: 支持 */
    LANG_SOM("索马里语", "som", true),

    /** 约鲁巴语 - 代码: yor, 检测: 不支持 */
    LANG_YOR("约鲁巴语", "yor", false),

    /** 缅甸语 - 代码: bur, 检测: 支持 */
    LANG_BUR("缅甸语", "bur", true),

    /** 罗姆语 - 代码: ro, 检测: 不支持 */
    LANG_RO("罗姆语", "ro", false),

    /** 罗曼什语 - 代码: roh, 检测: 不支持 */
    LANG_ROH("罗曼什语", "roh", false),

    /** 罗马尼亚语 - 代码: rom, 检测: 不支持 */
    LANG_ROM("罗马尼亚语", "rom", false),

    /** 老挝语 - 代码: lao, 检测: 不支持 */
    LANG_LAO("老挝语", "lao", false),

    /** 聪加语 - 代码: tso, 检测: 不支持 */
    LANG_TSO("聪加语", "tso", false),

    /** 胡帕语 - 代码: hup, 检测: 不支持 */
    LANG_HUP("胡帕语", "hup", false),

    /** 自动检测 - 代码: auto, 检测: 不支持 */
    LANG_AUTO("自动检测", "auto", false),

    /** 艾马拉语 - 代码: aym, 检测: 不支持 */
    LANG_AYM("艾马拉语", "aym", false),

    /** 芬兰语 - 代码: fin, 检测: 不支持 */
    LANG_FIN("芬兰语", "fin", false),

    /** 苏格兰语 - 代码: sco, 检测: 不支持 */
    LANG_SCO("苏格兰语", "sco", false),

    /** 苗语 - 代码: hmn, 检测: 不支持 */
    LANG_HMN("苗语", "hmn", false),

    /** 英语 - 代码: en, 检测: 支持 */
    LANG_EN("英语", "en", true),

    /** 荷兰语 - 代码: nl, 检测: 支持 */
    LANG_NL("荷兰语", "nl", true),

    /** 菲律宾语 - 代码: fil, 检测: 支持 */
    LANG_FIL("菲律宾语", "fil", true),

    /** 萨丁尼亚语 - 代码: srd, 检测: 不支持 */
    LANG_SRD("萨丁尼亚语", "srd", false),

    /** 萨摩亚语 - 代码: sm, 检测: 不支持 */
    LANG_SM("萨摩亚语", "sm", false),

    /** 葡萄牙语 - 代码: pt, 检测: 支持 */
    LANG_PT("葡萄牙语", "pt", true),

    /** 西弗里斯语 - 代码: fry, 检测: 支持 */
    LANG_FRY("西弗里斯语", "fry", true),

    /** 西班牙语 - 代码: spa, 检测: 支持 */
    LANG_SPA("西班牙语", "spa", true),

    /** 西里西亚语 - 代码: sil, 检测: 不支持 */
    LANG_SIL("西里西亚语", "sil", false),

    /** 西非书面语 - 代码: nqo, 检测: 不支持 */
    LANG_NQO("西非书面语", "nqo", false),

    /** 豪萨语 - 代码: hau, 检测: 不支持 */
    LANG_HAU("豪萨语", "hau", false),

    /** 越南语 - 代码: vie, 检测: 支持 */
    LANG_VIE("越南语", "vie", true),

    /** 迈蒂利语 - 代码: mai, 检测: 支持 */
    LANG_MAI("迈蒂利语", "mai", true),

    /** 迪维希语 - 代码: div, 检测: 不支持 */
    LANG_DIV("迪维希语", "div", false),

    /** 逻辑语 - 代码: loj, 检测: 不支持 */
    LANG_LOJ("逻辑语", "loj", false),

    /** 那不勒斯语 - 代码: nea, 检测: 不支持 */
    LANG_NEA("那不勒斯语", "nea", false),

    /** 邦板牙语 - 代码: pam, 检测: 不支持 */
    LANG_PAM("邦板牙语", "pam", false),

    /** 阿塞拜疆语 - 代码: aze, 检测: 支持 */
    LANG_AZE("阿塞拜疆语", "aze", true),

    /** 阿姆哈拉语 - 代码: amh, 检测: 支持 */
    LANG_AMH("阿姆哈拉语", "amh", true),

    /** 阿尔及利亚阿拉伯语 - 代码: arq, 检测: 不支持 */
    LANG_ARQ("阿尔及利亚阿拉伯语", "arq", false),

    /** 阿尔巴尼亚语 - 代码: alb, 检测: 支持 */
    LANG_ALB("阿尔巴尼亚语", "alb", true),

    /** 阿拉伯语 - 代码: ara, 检测: 支持 */
    LANG_ARA("阿拉伯语", "ara", true),

    /** 阿拉贡语 - 代码: arg, 检测: 不支持 */
    LANG_ARG("阿拉贡语", "arg", false),

    /** 阿斯图里亚斯语 - 代码: ast, 检测: 不支持 */
    LANG_AST("阿斯图里亚斯语", "ast", false),

    /** 阿肯语 - 代码: aka, 检测: 不支持 */
    LANG_AKA("阿肯语", "aka", false),

    /** 阿萨姆语 - 代码: asm, 检测: 不支持 */
    LANG_ASM("阿萨姆语", "asm", false),

    /** 鞑靼语 - 代码: tat, 检测: 不支持 */
    LANG_TAT("鞑靼语", "tat", false),

    /** 韩语 - 代码: kor, 检测: 支持 */
    LANG_KOR("韩语", "kor", true),

    /** 马其顿语 - 代码: mac, 检测: 不支持 */
    LANG_MAC("马其顿语", "mac", false),

    /** 马拉加斯语 - 代码: mg, 检测: 支持 */
    LANG_MG("马拉加斯语", "mg", true),

    /** 马拉地语 - 代码: mar, 检测: 不支持 */
    LANG_MAR("马拉地语", "mar", false),

    /** 马拉雅拉姆语 - 代码: mal, 检测: 支持 */
    LANG_MAL("马拉雅拉姆语", "mal", true),

    /** 马来语 - 代码: may, 检测: 支持 */
    LANG_MAY("马来语", "may", true),

    /** 马绍尔语 - 代码: mah, 检测: 不支持 */
    LANG_MAH("马绍尔语", "mah", false),

    /** 马耳他语 - 代码: mlt, 检测: 支持 */
    LANG_MLT("马耳他语", "mlt", true),

    /** 高地索布语 - 代码: ups, 检测: 不支持 */
    LANG_UPS("高地索布语", "ups", false),

    /** 高棉语 - 代码: hkm, 检测: 支持 */
    LANG_HKM("高棉语", "hkm", true),

    /** 黑山语 - 代码: mot, 检测: 不支持 */
    LANG_MOT("黑山语", "mot", false),

    /** 齐切瓦语 - 代码: nya, 检测: 不支持 */
    LANG_NYA("齐切瓦语", "nya", false);

}
