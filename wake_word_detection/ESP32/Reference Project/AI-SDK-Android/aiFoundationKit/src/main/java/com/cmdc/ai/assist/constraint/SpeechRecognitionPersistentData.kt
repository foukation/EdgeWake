package com.cmdc.ai.assist.constraint

import com.google.gson.annotations.SerializedName

/**
 * 语音识别数据模型类，用于存储和处理语音识别服务返回的数据。
 * 实现了 Serializable 接口以支持序列化操作。
 */
data class SpeechRecognitionPersistentData(

    /**
     * 错误码
     * 0表示成功，非0表示出现错误
     */
    @SerializedName("err_no")
    val errorNumber: Int,

    /**
     * 错误信息描述
     * 当err_no为0时通常为"OK"
     */
    @SerializedName("err_msg")
    val errorMessage: String,

    /**
     * 日志ID
     * 用于追踪和调试的唯一标识符
     */
    @SerializedName("log_id")
    val logId: Long,

    /**
     * 序列号
     * 请求的唯一标识，格式通常为UUID_ws_序号
     */
    @SerializedName("sn")
    val serialNumber: String,

    /**
     * 识别类型
     * FIN_TEXT 表示最终文本识别结果
     */
    @SerializedName("type")
    val type: String,

    /**
     * 识别结果
     * 语音转换成的文本内容
     */
    @SerializedName("result")
    val result: String,

    /**
     * 开始时间
     * 语音片段在音频中的开始时间戳（毫秒）
     */
    @SerializedName("start_time")
    val startTime: Long,

    /**
     * 结束时间
     * 语音片段在音频中的结束时间戳（毫秒）
     */
    @SerializedName("end_time")
    val endTime: Long,

    /**
     * 产品ID
     * 标识使用的语音识别产品类型
     */
    @SerializedName("product_id")
    val productId: Int,

    /**
     * 产品线
     * 标识产品所属的业务线，如"open"表示开放平台
     */
    @SerializedName("product_line")
    val productLine: String

)