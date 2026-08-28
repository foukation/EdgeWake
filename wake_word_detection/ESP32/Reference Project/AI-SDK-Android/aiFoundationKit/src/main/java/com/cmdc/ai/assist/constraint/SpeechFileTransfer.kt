package com.cmdc.ai.assist.constraint

/**
 * 语音文件识别 API 请求数据类
 */
data class SpeechRecognitionRequest(
    /**
     * 云端可外网访问的 url 链接，音频大小不超过 500MB
     *
     * */
    val speech_url: String,
    /**
     * ["mp3","wav","pcm","m4a","amr"]单声道，编码 16bits 位深
     * */
    val format: String,
    /**
     * [80001 (中文普通话场识别标准版)，80006 (中文音视频字幕模型)，1737 (英文模型)]
     * */
    val pid: Int,
    /**
     * [16000] 固定值
     */
    internal val rate: Int = 16000
) {
    /**
     * 音频格式枚举
     */
    enum class AudioFormat(val value: String) {
        /**
         * mp3
         * */
        MP3("mp3"),

        /**
         * wav
         * */
        WAV("wav"),

        /**
         * pcm
         * */
        PCM("pcm"),

        /**
         * m4a
         * */
        M4A("m4a"),

        /**
         * amr
         * */
        AMR("amr")
    }

    /**
     * 语言模型枚举
     */
    enum class LanguageModel(val value: Int) {
        /**
         * 中文语音近场识别模型极速版
         * */
        CHINESE_STANDARD(80001),

        /**
         * 中文音视频字幕模型
         * */
        CHINESE_SUBTITLE(80006),

        /**
         * 英文模型
         * */
        ENGLISH(1737)
    }
}

/**
 * 语音文件识别 API 响应数据类
 */
data class SpeechRecognitionResponse(
    /**
     * 结果码，0 为成功
     * */
    val code: Int,
    /**
     * 消息
     * */
    val msg: String,
    /**
     * 返回数据
     * */
    val data: Data?
) {
    /**
     * 响应数据内容
     */
    data class Data(
        /**
         * 唯一的 log id，用于问题定位
         * */
        val log_id: String,
        /**
         * 任务状态
         * */
        val task_status: String,
        /**
         * 任务 id
         * */
        val task_id: String,
        /**
         * 错误码
         * */
        val error_code: Int,
        /**
         * 错误信息
         * */
        val error_msg: String,
    )

}