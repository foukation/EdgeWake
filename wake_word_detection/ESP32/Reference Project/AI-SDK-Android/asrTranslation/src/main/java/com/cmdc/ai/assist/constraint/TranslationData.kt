package com.cmdc.ai.assist.constraint

import java.io.Serializable

/**
 * 用于封装翻译相关的数据，实现序列化以便在网络传输或存储时保持对象的状态。
 * 该类主要包含三种信息：语音识别结果(asrResult)、翻译结果(translationResult)以及数据类型(type)。
 * type 为 MID，表示中间结果，FIN 为最终结果。
 */
class TranslationData : Serializable {

    /**
     * 用于存储语音识别的结果
     *
     * */
    var asrResult: String? = null
    /**
     * 存储翻译的结果
     * */
    var translationResult: String? = null
    /**
     * 用于标识结果的类型，可能是 MID（中间结果）或 FIN（最终结果），初始值为 null
     * */
    var type: String? = null // MID or FIN

}
