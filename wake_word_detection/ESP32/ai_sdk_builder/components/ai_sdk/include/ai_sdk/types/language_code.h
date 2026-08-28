/**
 * @file language_code.h
 * @brief 机器翻译语言代码常量定义
 *
 * 此文件定义了机器翻译 (v1 API) 支持的所有语言代码。
 * 数据来源: 翻译_语种.csv 文件
 *
 * 用于 textTranslate() 方法的 targetLanguage 和 sourceLanguage 参数。
 *
 * @note 模型翻译 (v2 API) 使用 LanguageCodeModel 中的代码
 * @see language_code_model.h
 */
#pragma once

namespace ai_sdk {

/**
 * @namespace LanguageCode
 * @brief 机器翻译语言代码常量
 *
 * 包含 200+ 种语言的代码定义。
 *
 * 使用示例:
 * @code
 * TranslationRequest request;
 * request.targetLanguage = LanguageCode::ZH;  // 中文(简体)
 * request.originText = "Hello";
 * kit.textTranslate(request, onSuccess, onError);
 * @endcode
 */
namespace LanguageCode {
    // === 自动检测 ===
    constexpr const char* AUTO = "auto";         ///< 自动检测

    // === 常用语言 ===
    constexpr const char* ZH = "zh";             ///< 中文(简体)
    constexpr const char* CHT = "cht";           ///< 中文(繁体)
    constexpr const char* YUE = "yue";           ///< 中文(粤语)
    constexpr const char* WYW = "wyw";           ///< 中文(文言文)
    constexpr const char* EN = "en";             ///< 英语
    constexpr const char* JP = "jp";             ///< 日语
    constexpr const char* KOR = "kor";           ///< 韩语
    constexpr const char* FRA = "fra";           ///< 法语
    constexpr const char* DE = "de";             ///< 德语
    constexpr const char* RU = "ru";             ///< 俄语
    constexpr const char* SPA = "spa";           ///< 西班牙语
    constexpr const char* PT = "pt";             ///< 葡萄牙语
    constexpr const char* IT = "it";             ///< 意大利语
    constexpr const char* VIE = "vie";           ///< 越南语
    constexpr const char* TH = "th";             ///< 泰语
    constexpr const char* ARA = "ara";           ///< 阿拉伯语
    constexpr const char* HI = "hi";             ///< 印地语
    constexpr const char* ID = "id";             ///< 印尼语
    constexpr const char* NL = "nl";             ///< 荷兰语
    constexpr const char* PL = "pl";             ///< 波兰语
    constexpr const char* TR = "tr";             ///< 土耳其语
    constexpr const char* HU = "hu";             ///< 匈牙利语
    constexpr const char* CS = "cs";             ///< 捷克语
    constexpr const char* EL = "el";             ///< 希腊语

    // === 其他语言 ===
    constexpr const char* EPO = "epo";           ///< 世界语
    constexpr const char* FRM = "frm";           ///< 中古法语
    constexpr const char* DAN = "dan";           ///< 丹麦语
    constexpr const char* UKR = "ukr";           ///< 乌克兰语
    constexpr const char* URD = "urd";           ///< 乌尔都语
    constexpr const char* NOB = "nob";           ///< 书面挪威语
    constexpr const char* ARM = "arm";           ///< 亚美尼亚语
    constexpr const char* ACH = "ach";           ///< 亚齐语
    constexpr const char* TGL = "tgl";           ///< 他加禄语
    constexpr const char* IKU = "iku";           ///< 伊努克提图特语
    constexpr const char* IBO = "ibo";           ///< 伊博语
    constexpr const char* IDO = "ido";           ///< 伊多语
    constexpr const char* LOG = "log";           ///< 低地德语
    constexpr const char* BUL = "bul";           ///< 保加利亚语
    constexpr const char* SND = "snd";           ///< 信德语
    constexpr const char* SNA = "sna";           ///< 修纳语
    constexpr const char* BAL = "bal";           ///< 俾路支语
    constexpr const char* SIN = "sin";           ///< 僧伽罗语
    constexpr const char* QUE = "que";           ///< 克丘亚语
    constexpr const char* KAS = "kas";           ///< 克什米尔语
    constexpr const char* KLI = "kli";           ///< 克林贡语
    constexpr const char* HRV = "hrv";           ///< 克罗地亚语
    constexpr const char* CRE = "cre";           ///< 克里克语
    constexpr const char* CRI = "cri";           ///< 克里米亚鞑靼语
    constexpr const char* ICE = "ice";           ///< 冰岛语
    constexpr const char* CHR = "chr";           ///< 切罗基语
    constexpr const char* KON = "kon";           ///< 刚果语
    constexpr const char* GLG = "glg";           ///< 加利西亚语
    constexpr const char* FRN = "frn";           ///< 加拿大法语
    constexpr const char* CAT = "cat";           ///< 加泰罗尼亚语
    constexpr const char* SME = "sme";           ///< 北方萨米语
    constexpr const char* PED = "ped";           ///< 北索托语
    constexpr const char* NBL = "nbl";           ///< 南恩德贝莱语
    constexpr const char* SOT = "sot";           ///< 南索托语
    constexpr const char* AFR = "afr";           ///< 南非荷兰语
    constexpr const char* BHO = "bho";           ///< 博杰普尔语
    constexpr const char* KAU = "kau";           ///< 卡努里语
    constexpr const char* KAB = "kab";           ///< 卡拜尔语
    constexpr const char* KAN = "kan";           ///< 卡纳达语
    constexpr const char* KAH = "kah";           ///< 卡舒比语
    constexpr const char* LUG = "lug";           ///< 卢干达语
    constexpr const char* KIN = "kin";           ///< 卢旺达语
    constexpr const char* LTZ = "ltz";           ///< 卢森堡语
    constexpr const char* RUY = "ruy";           ///< 卢森尼亚语
    constexpr const char* ING = "ing";           ///< 印古什语
    constexpr const char* SYR = "syr";           ///< 叙利亚语
    constexpr const char* GUJ = "guj";           ///< 古吉拉特语
    constexpr const char* GRA = "gra";           ///< 古希腊语
    constexpr const char* ENO = "eno";           ///< 古英语
    constexpr const char* KIR = "kir";           ///< 吉尔吉斯语
    constexpr const char* HAK = "hak";           ///< 哈卡钦语
    constexpr const char* INA = "ina";           ///< 因特语
    constexpr const char* TUK = "tuk";           ///< 土库曼语
    constexpr const char* TGK = "tgk";           ///< 塔吉克语
    constexpr const char* SEC = "sec";           ///< 塞尔维亚-克罗地亚语
    constexpr const char* SRP = "srp";           ///< 塞尔维亚语
    constexpr const char* SRC = "src";           ///< 塞尔维亚语（西里尔）
    constexpr const char* HAW = "haw";           ///< 夏威夷语
    constexpr const char* TWI = "twi";           ///< 契维语
    constexpr const char* OCI = "oci";           ///< 奥克语
    constexpr const char* OSS = "oss";           ///< 奥塞梯语
    constexpr const char* OJI = "oji";           ///< 奥杰布瓦语
    constexpr const char* ORM = "orm";           ///< 奥罗莫语
    constexpr const char* ORI = "ori";           ///< 奥里亚语
    constexpr const char* WEL = "wel";           ///< 威尔士语
    constexpr const char* KOK = "kok";           ///< 孔卡尼语
    constexpr const char* BEN = "ben";           ///< 孟加拉语
    constexpr const char* CEB = "ceb";           ///< 宿务语
    constexpr const char* FUL = "ful";           ///< 富拉尼语
    constexpr const char* NEP = "nep";           ///< 尼泊尔语
    constexpr const char* BAK = "bak";           ///< 巴什基尔语
    constexpr const char* BAQ = "baq";           ///< 巴斯克语
    constexpr const char* POT = "pot";           ///< 巴西葡萄牙语
    constexpr const char* SUN = "sun";           ///< 巽他语
    constexpr const char* BRE = "bre";           ///< 布列塔尼语
    constexpr const char* HEB = "heb";           ///< 希伯来语
    constexpr const char* HIL = "hil";           ///< 希利盖农语
    constexpr const char* PAP = "pap";           ///< 帕皮阿门托语
    constexpr const char* KUR = "kur";           ///< 库尔德语
    constexpr const char* COR = "cor";           ///< 康瓦尔语
    constexpr const char* FRI = "fri";           ///< 弗留利语
    constexpr const char* TET = "tet";           ///< 德顿语
    constexpr const char* YID = "yid";           ///< 意第绪语
    constexpr const char* ZAZ = "zaz";           ///< 扎扎其语
    constexpr const char* LAT = "lat";           ///< 拉丁语
    constexpr const char* LAG = "lag";           ///< 拉特加莱语
    constexpr const char* LAV = "lav";           ///< 拉脱维亚语
    constexpr const char* NOR = "nor";           ///< 挪威语
    constexpr const char* SHA = "sha";           ///< 掸语
    constexpr const char* TIR = "tir";           ///< 提格利尼亚语
    constexpr const char* VEN = "ven";           ///< 文达语
    constexpr const char* SK = "sk";             ///< 斯洛伐克语
    constexpr const char* SLO = "slo";           ///< 斯洛文尼亚语
    constexpr const char* SWA = "swa";           ///< 斯瓦希里语
    constexpr const char* NNO = "nno";           ///< 新挪威语
    constexpr const char* PAN = "pan";           ///< 旁遮普语
    constexpr const char* PUS = "pus";           ///< 普什图语
    constexpr const char* GLV = "glv";           ///< 曼克斯语
    constexpr const char* BEM = "bem";           ///< 本巴语
    constexpr const char* LIN = "lin";           ///< 林加拉语
    constexpr const char* LIM = "lim";           ///< 林堡语
    constexpr const char* BER = "ber";           ///< 柏柏尔语
    constexpr const char* KAL = "kal";           ///< 格陵兰语
    constexpr const char* GEO = "geo";           ///< 格鲁吉亚语
    constexpr const char* SOL = "sol";           ///< 桑海语
    constexpr const char* SAN = "san";           ///< 梵语
    constexpr const char* CHV = "chv";           ///< 楚瓦什语
    constexpr const char* BIS = "bis";           ///< 比斯拉马语
    constexpr const char* BLI = "bli";           ///< 比林语
    constexpr const char* MAO = "mao";           ///< 毛利语
    constexpr const char* MAU = "mau";           ///< 毛里求斯克里奥尔语
    constexpr const char* WOL = "wol";           ///< 沃洛夫语
    constexpr const char* FAO = "fao";           ///< 法罗语
    constexpr const char* BOS = "bos";           ///< 波斯尼亚语
    constexpr const char* PER = "per";           ///< 波斯语
    constexpr const char* TEL = "tel";           ///< 泰卢固语
    constexpr const char* TAM = "tam";           ///< 泰米尔语
    constexpr const char* HT = "ht";             ///< 海地语
    constexpr const char* JAV = "jav";           ///< 爪哇语
    constexpr const char* GLE = "gle";           ///< 爱尔兰语
    constexpr const char* EST = "est";           ///< 爱沙尼亚语
    constexpr const char* SWE = "swe";           ///< 瑞典语
    constexpr const char* GRN = "grn";           ///< 瓜拉尼语
    constexpr const char* WLN = "wln";           ///< 瓦隆语
    constexpr const char* BEL = "bel";           ///< 白俄罗斯语
    constexpr const char* GLA = "gla";           ///< 盖尔语
    constexpr const char* ZUL = "zul";           ///< 祖鲁语
    constexpr const char* XHO = "xho";           ///< 科萨语
    constexpr const char* COS = "cos";           ///< 科西嘉语
    constexpr const char* TUA = "tua";           ///< 突尼斯阿拉伯语
    constexpr const char* LIT = "lit";           ///< 立陶宛语
    constexpr const char* SOM = "som";           ///< 索马里语
    constexpr const char* YOR = "yor";           ///< 约鲁巴语
    constexpr const char* BUR = "bur";           ///< 缅甸语
    constexpr const char* RO = "ro";             ///< 罗姆语
    constexpr const char* ROH = "roh";           ///< 罗曼什语
    constexpr const char* ROM = "rom";           ///< 罗马尼亚语
    constexpr const char* LAO = "lao";           ///< 老挝语
    constexpr const char* TSO = "tso";           ///< 聪加语
    constexpr const char* HUP = "hup";           ///< 胡帕语
    constexpr const char* AYM = "aym";           ///< 艾马拉语
    constexpr const char* FIN = "fin";           ///< 芬兰语
    constexpr const char* SCO = "sco";           ///< 苏格兰语
    constexpr const char* HMN = "hmn";           ///< 苗语
    constexpr const char* FIL = "fil";           ///< 菲律宾语
    constexpr const char* SRD = "srd";           ///< 萨丁尼亚语
    constexpr const char* SM = "sm";             ///< 萨摩亚语
    constexpr const char* FRY = "fry";           ///< 西弗里斯语
    constexpr const char* SIL = "sil";           ///< 西里西亚语
    constexpr const char* NQO = "nqo";           ///< 西非书面语
    constexpr const char* HAU = "hau";           ///< 豪萨语
    constexpr const char* MAI = "mai";           ///< 迈蒂利语
    constexpr const char* DIV = "div";           ///< 迪维希语
    constexpr const char* LOJ = "loj";           ///< 逻辑语
    constexpr const char* NEA = "nea";           ///< 那不勒斯语
    constexpr const char* PAM = "pam";           ///< 邦板牙语
    constexpr const char* AZE = "aze";           ///< 阿塞拜疆语
    constexpr const char* AMH = "amh";           ///< 阿姆哈拉语
    constexpr const char* ARQ = "arq";           ///< 阿尔及利亚阿拉伯语
    constexpr const char* ALB = "alb";           ///< 阿尔巴尼亚语
    constexpr const char* ARG = "arg";           ///< 阿拉贡语
    constexpr const char* AST = "ast";           ///< 阿斯图里亚斯语
    constexpr const char* AKA = "aka";           ///< 阿肯语
    constexpr const char* ASM = "asm";           ///< 阿萨姆语
    constexpr const char* TAT = "tat";           ///< 鞑靼语
    constexpr const char* MAC = "mac";           ///< 马其顿语
    constexpr const char* MG = "mg";             ///< 马拉加斯语
    constexpr const char* MAR = "mar";           ///< 马拉地语
    constexpr const char* MAL = "mal";           ///< 马拉雅拉姆语
    constexpr const char* MAY = "may";           ///< 马来语
    constexpr const char* MAH = "mah";           ///< 马绍尔语
    constexpr const char* MLT = "mlt";           ///< 马耳他语
    constexpr const char* UPS = "ups";           ///< 高地索布语
    constexpr const char* HKM = "hkm";           ///< 高棉语
    constexpr const char* MOT = "mot";           ///< 黑山语
    constexpr const char* NYA = "nya";           ///< 齐切瓦语
    constexpr const char* LOS = "los";           ///< 下索布语
}

}  // namespace ai_sdk
