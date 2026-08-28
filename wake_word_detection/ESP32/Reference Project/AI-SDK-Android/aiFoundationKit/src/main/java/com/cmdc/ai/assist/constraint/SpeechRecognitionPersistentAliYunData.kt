package com.cmdc.ai.assist.constraint

import com.google.gson.annotations.SerializedName

/**
 * 阿里云持续语音识别 WebSocket 下行消息数据模型。
 *
 * 用于解析 ASR 模块返回的统一下行协议消息，包括识别就绪、识别结果、心跳以及错误消息。
 * 不同消息类型返回的字段不完全相同，因此字段均使用可空类型。
 */
data class SpeechRecognitionPersistentAliYunData(

    /**
     * 消息类型。
     *
     * 可能取值：
     * - ready：识别就绪
     * - RESULT：识别结果
     * - heartbeat：心跳
     * - error：错误
     */
    @SerializedName("type")
    val type: String? = null,

    /**
     * 会话或连接唯一标识。
     */
    @SerializedName("trace_id")
    val traceId: String? = null,

    /**
     * 错误码。
     *
     * 仅当 type 为 error 时返回。
     */
    @SerializedName("err_no")
    val errorNumber: Int? = null,

    /**
     * 错误信息。
     *
     * 仅当 type 为 error 时返回。
     */
    @SerializedName("err_msg")
    val errorMessage: String? = null,

    /**
     * 会话标识。
     *
     * 错误消息中可能返回该字段。
     */
    @SerializedName("sn")
    val serialNumber: String? = null,

    /**
     * 识别结果数据。
     *
     * 主要在 type 为 RESULT 时返回；error 场景下可能为空对象。
     */
    @SerializedName("data")
    val data: Data? = null
) {

    /**
     * 阿里云持续语音识别结果内容。
     */
    data class Data(

        /**
         * 当前识别文本。
         *
         * 中间结果为当前句识别中的文本，最终结果为完整识别文本。
         */
        @SerializedName("text")
        val text: String? = null,

        /**
         * 是否为最终结果。
         *
         * false 表示中间识别结果，true 表示最终识别结果。
         */
        @SerializedName("is_final")
        val isFinal: Boolean? = null,

        /**
         * 当前实际使用的识别供应商。
         *
         * 可能取值包括 baidu、tencent、aliyun、volc。
         */
        @SerializedName("vendor")
        val vendor: String? = null,

        /**
         * 句首时间，单位毫秒。
         *
         * 可选字段。
         */
        @SerializedName("start_time")
        val startTime: Long? = null,

        /**
         * 句尾时间，单位毫秒。
         *
         * 可选字段。
         */
        @SerializedName("end_time")
        val endTime: Long? = null,

        /**
         * 句序号，从 0 递增。
         *
         * 可选字段。
         */
        @SerializedName("index")
        val index: Int? = null
    )
}
