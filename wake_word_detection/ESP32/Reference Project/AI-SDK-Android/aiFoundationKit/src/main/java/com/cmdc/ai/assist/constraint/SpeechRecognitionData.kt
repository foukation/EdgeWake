package com.cmdc.ai.assist.constraint

import java.io.Serializable

/**
 * 语音识别数据模型类，用于存储和处理语音识别服务返回的数据。
 * 实现了Serializable接口以支持序列化操作。
 */
class SpeechRecognitionData : Serializable {

    /**
     * 表示请求处理状态
     * 通常为"ok"表示成功，或包含错误信息
     */
    var status: String? = null

    /**
     * 表示结果类型
     * 常见值包括"mid_result"(中间结果)或"fin_result"(最终结果)
     */
    var type: String? = null

    /**
     * 包含语音识别的文本结果
     * 可能是部分结果或完整结果，取决于type和end字段
     */
    var result: String? = null

    /**
     * 会话唯一标识符(session number)
     * 用于跟踪特定的语音识别请求
     */
    var sn: String? = null

    /**
     * 标识当前结果是否为最终结果
     * 0表示中间结果，继续等待更多数据
     * 1表示最终结果，识别过程结束
     */
    var end: Int? = null

    /**
     * 包含未经处理的原始识别结果
     * 通常与result字段内容相同，但可能包含更多原始信息
     */
    var rawResult: String? = null

}