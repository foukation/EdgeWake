package com.cmdc.ai.assist.constraint

/**
 * 语音文件识别结果查询 API 请求数据类
 */
data class SpeechRecognitionQueryRequest(
    /**
     * 任务 id 列表，如 task_ids 为空，返回所有任务结果列表；单次查询任务数不超过 200 个
     * */
    val task_ids: List<String>
)

/**
 * 语音文件识别结果查询 API 响应数据类
 */
data class SpeechRecognitionQueryResponse(
    /**
     * 结果码 0 为成功
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
     * 返回数据
     * */
    data class Data(
        /**
         * 唯一的 log id，用于问题定位 (注意这里是 Long 类型)
         * */
        val log_id: Long,
        /**
         * 多个任务的结果
         * */
        val tasks_info: List<TaskInfo>?,  // 多个任务的结果
        /**
         * 错误码
         * */
        val error_code: String? = null,
        /**
         * 错误信息
         * */
        val error_msg: String? = null
    )

    /**
     * 任务结果
     * */
    data class TaskInfo(
        /**
         * 任务 id
         * */
        val task_id: String,
        /**
         * 任务状态(Running、Failure、Success)
         * */
        val task_status: String,
        /**
         * 转写结果的 json 格式
         * */
        val task_result: TaskResult?
    )

    /**
     * 转写结果的 json 格式
     * */
    data class TaskResult(
        /**
         * 语料编号
         * */
        val corpus_no: String? = null,
        /**
         * 转写结果是字符串数组
         * */
        val result: List<String>? = null,
        /**
         * 音频时长(毫秒)
         * */
        val audio_duration: Int? = null,
        /**
         * 转写详细结果是对象数组
         * */
        val detailed_result: List<DetailedResult>? = null,
        /**
         * 错误码
         * */
        val err_no: Int? = null,
        /**
         * 错误信息
         * */
        val err_msg: String? = null
    )

    /**
     * 转写详细结果
     * */
    data class DetailedResult(
        /**
         * 识别结果数组
         * */
        val res: List<String>? = null,
        /**
         * 开始时间(毫秒)
         * */
        val begin_time: Int? = null,
        /**
         * 结束时间(毫秒)
         * */
        val end_time: Int? = null,
        /**
         * 词信息
         * */
        val words_info: List<Any>? = null,
        /**
         * 序列号
         * */
        val sn: String? = null,
        /**
         * 语料编号
         * */
        val corpus_no: String? = null
    )
}