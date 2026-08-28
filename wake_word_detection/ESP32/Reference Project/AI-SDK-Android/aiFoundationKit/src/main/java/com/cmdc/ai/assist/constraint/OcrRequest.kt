package com.cmdc.ai.assist.constraint

/**
 * Ocr API 请求体参数数据类
 */
data class OcrRequest(
    // 图片参数，类型为 url/pdf_file/ofd_file 四选一
    /**
     * 请求识别的图片数据
     * */
    val image: String,

    // URL 参数，类型为 image/pdf_file/ofd_file 四选一
    private val url: String? = null,

    // PDF 文件参数，类型为 image/url/ofd_file 四选一
    private val pdf_file: String? = null,

    // PDF 文件页码
    private val pdf_file_num: String? = null,

    // OFD 文件参数，类型为 image/url/pdf_file 四选一
    private val ofd_file: String? = null,

    // OFD 文件页码
    private val ofd_file_num: String? = null,

    // 语言类型，支持多种语言选项
    private val language_type: String? = null,

    // 是否检测方向，可选值为 true/false
    private val detect_direction: String? = null,

    // 是否检测语言，可选值为 true/false
    private val detect_language: String? = null,

    // 是否按段落输出，可选值为 true/false
    private val paragraph: String? = null,

    // 是否返回识别结果中每一行的置信度，可选值为 true/false
    private val probability: String? = null,

    // 是否定位文本行顶点位置，可选值为 true/false
    private val vertexes_location: String? = null
)

/**
 * 语言类型枚举
 */
private enum class LanguageType(val value: String) {
    CHN_ENG("CHN_ENG"),
    ENG("ENG"),
    JAP("JAP"),
    KOR("KOR"),
    FRE("FRE"),
    SPA("SPA"),
    POR("POR"),
    GER("GER"),
    ITA("ITA"),
    RUS("RUS")
}