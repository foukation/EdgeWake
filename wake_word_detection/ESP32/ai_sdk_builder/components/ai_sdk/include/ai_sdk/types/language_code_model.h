/**
 * @file language_code_model.h
 * @brief 模型翻译语言代码常量定义
 *
 * 此文件定义了模型翻译 (v2 API) 支持的所有语言代码。
 *
 * 用于 textTranslateWithModel() 方法的 targetLanguage 和 sourceLanguage 参数。
 *
 * @note 机器翻译 (v1 API) 使用 LanguageCode 中的代码
 * @see language_code.h
 */
#pragma once

namespace ai_sdk {

/**
 * @namespace LanguageCodeModel
 * @brief 模型翻译语言代码常量
 *
 * 包含约 90 种语言的代码定义。
 * 注意：代码格式与机器翻译不同。
 *
 * 使用示例:
 * @code
 * TranslationRequest request;
 * request.targetLanguage = LanguageCodeModel::ZH;  // 中文(简体)
 * request.originText = "Hello";
 * kit.textTranslateWithModel(request, onSuccess, onError);
 * @endcode
 */
namespace LanguageCodeModel {
    // === 自动检测 ===
    constexpr const char* AUTO = "auto";             ///< 自动检测

    // === 常用语言 ===
    constexpr const char* ZH = "zh";                 ///< 中文(简体)
    constexpr const char* ZH_TW = "zh_tw";           ///< 中文(繁体)
    constexpr const char* EN = "en";                 ///< 英语
    constexpr const char* JA = "ja";                 ///< 日语
    constexpr const char* KO = "ko";                 ///< 韩语
    constexpr const char* FR = "fr";                 ///< 法语
    constexpr const char* DE = "de";                 ///< 德语
    constexpr const char* RU = "ru";                 ///< 俄语
    constexpr const char* ES = "es";                 ///< 西班牙语
    constexpr const char* PT = "pt";                 ///< 葡萄牙语
    constexpr const char* IT = "it";                 ///< 意大利语
    constexpr const char* VI = "vi";                 ///< 越南语
    constexpr const char* TH = "th";                 ///< 泰语
    constexpr const char* AR = "ar";                 ///< 阿拉伯语
    constexpr const char* HI = "hi";                 ///< 印地语
    constexpr const char* ID = "id";                 ///< 印尼语
    constexpr const char* NL = "nl";                 ///< 荷兰语
    constexpr const char* PL = "pl";                 ///< 波兰语
    constexpr const char* TR = "tr";                 ///< 土耳其语
    constexpr const char* HU = "hu";                 ///< 匈牙利语
    constexpr const char* CS = "cs";                 ///< 捷克语
    constexpr const char* EL = "el";                 ///< 希腊语

    // === 其他语言 ===
    constexpr const char* DA = "da";                 ///< 丹麦语
    constexpr const char* UK = "uk";                 ///< 乌克兰语
    constexpr const char* UR = "ur";                 ///< 乌尔都语
    constexpr const char* UZ = "uz";                 ///< 乌兹别克语
    constexpr const char* HY = "hy";                 ///< 亚美尼亚语
    constexpr const char* TL = "tl";                 ///< 他加禄语
    constexpr const char* BG = "bg";                 ///< 保加利亚语
    constexpr const char* SI = "si";                 ///< 僧伽罗语
    constexpr const char* HR = "hr";                 ///< 克罗地亚语
    constexpr const char* IS = "is";                 ///< 冰岛语
    constexpr const char* GL = "gl";                 ///< 加利西亚语
    constexpr const char* CA = "ca";                 ///< 加泰罗尼亚语
    constexpr const char* AF = "af";                 ///< 南非荷兰语
    constexpr const char* KN = "kn";                 ///< 卡纳达语
    constexpr const char* KK = "kk";                 ///< 哈萨克语
    constexpr const char* GU = "gu";                 ///< 古吉拉特语
    constexpr const char* KY = "ky";                 ///< 吉尔吉斯语
    constexpr const char* TG = "tg";                 ///< 塔吉克语
    constexpr const char* SR = "sr";                 ///< 塞尔维亚语
    constexpr const char* CY = "cy";                 ///< 威尔士语
    constexpr const char* BN = "bn";                 ///< 孟加拉语
    constexpr const char* NE = "ne";                 ///< 尼泊尔语
    constexpr const char* EU = "eu";                 ///< 巴斯克语
    constexpr const char* HE = "he";                 ///< 希伯来语
    constexpr const char* KU = "ku";                 ///< 库尔德语
    constexpr const char* LA = "la";                 ///< 拉丁语
    constexpr const char* LV = "lv";                 ///< 拉脱维亚语
    constexpr const char* NO = "no";                 ///< 挪威语
    constexpr const char* SK = "sk";                 ///< 斯洛伐克语
    constexpr const char* SL = "sl";                 ///< 斯洛文尼亚语
    constexpr const char* SW = "sw";                 ///< 斯瓦希里语
    constexpr const char* PA = "pa";                 ///< 旁遮普语
    constexpr const char* PASHTO = "ps";             ///< 普什图语
    constexpr const char* KA = "ka";                 ///< 格鲁吉亚语
    constexpr const char* MARATHI = "mr";            ///< 马拉地语
    constexpr const char* FA = "fa";                 ///< 波斯语
    constexpr const char* TE = "te";                 ///< 泰卢固语
    constexpr const char* TA = "ta";                 ///< 泰米尔语
    constexpr const char* JW = "jw";                 ///< 爪哇语
    constexpr const char* GA = "ga";                 ///< 爱尔兰语
    constexpr const char* ET = "et";                 ///< 爱沙尼亚语
    constexpr const char* SV = "sv";                 ///< 瑞典语
    constexpr const char* BE = "be";                 ///< 白俄罗斯语
    constexpr const char* ZU = "zu";                 ///< 祖鲁语
    constexpr const char* LT = "lt";                 ///< 立陶宛语
    constexpr const char* SO = "so";                 ///< 索马里语
    constexpr const char* YO = "yo";                 ///< 约鲁巴语
    constexpr const char* MY = "my";                 ///< 缅甸语
    constexpr const char* RO = "ro";                 ///< 罗马尼亚语
    constexpr const char* LO = "lo";                 ///< 老挝语
    constexpr const char* FI = "fi";                 ///< 芬兰语
    constexpr const char* HMN = "hmn";               ///< 苗语
    constexpr const char* SD = "sd";                 ///< 信德语
    constexpr const char* FY = "fy";                 ///< 西弗里斯语
    constexpr const char* HA = "ha";                 ///< 豪萨语
    constexpr const char* AZ = "az";                 ///< 阿塞拜疆语
    constexpr const char* AM = "am";                 ///< 阿姆哈拉语
    constexpr const char* SQ = "sq";                 ///< 阿尔巴尼亚语
    constexpr const char* TT = "tt";                 ///< 鞑靼语
    constexpr const char* MK = "mk";                 ///< 马其顿语
    constexpr const char* MG = "mg";                 ///< 马拉加斯语
    constexpr const char* ML = "ml";                 ///< 马拉雅拉姆语
    constexpr const char* MS = "ms";                 ///< 马来语
    constexpr const char* MT = "mt";                 ///< 马耳他语
    constexpr const char* KM = "km";                 ///< 高棉语
    constexpr const char* NY = "ny";                 ///< 齐切瓦语
    constexpr const char* SN = "sn";                 ///< 修纳语
    constexpr const char* CO = "co";                 ///< 科西嘉语
    constexpr const char* IG = "ig";                 ///< 伊博语
    constexpr const char* XH = "xh";                 ///< 科萨语
    constexpr const char* SU = "su";                 ///< 巽他语
    constexpr const char* HT = "ht";                 ///< 海地克里奥尔语
    constexpr const char* LB = "lb";                 ///< 卢森堡语
    constexpr const char* MI = "mi";                 ///< 毛利语
    constexpr const char* SM = "sm";                 ///< 萨摩亚语
    constexpr const char* GD = "gd";                 ///< 苏格兰盖尔语
    constexpr const char* ST = "st";                 ///< 塞索托语
    constexpr const char* CEB = "ceb";               ///< 宿务语
    constexpr const char* EO = "eo";                 ///< 世界语
    constexpr const char* MN = "mn";                 ///< 蒙古语
    constexpr const char* TK = "tk";                 ///< 土库曼语
}

}  // namespace ai_sdk
