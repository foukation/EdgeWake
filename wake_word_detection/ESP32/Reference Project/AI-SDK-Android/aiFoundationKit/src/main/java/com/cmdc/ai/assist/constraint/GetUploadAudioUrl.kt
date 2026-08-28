package com.cmdc.ai.assist.constraint

/**
 * 文件上传 API 请求数据类
 */
data class UploadAudioUrlRequest(

    /**
     * 文件名
     * */
    val filename: String,
    /**
     * 文件大小
     * */
    val filesize: String
)

/**
 * 文件上传 API 响应数据类
 */
data class UploadAudioUrlResponse(
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
     * 返回数据，包含文件上传 url、文件访问 url
     * */
    data class Data(
        /**
         * 文件上传 url，通过 put 方式上传对应文件
         * */
        val uploadUrl: String,
        /**
         * 文件访问 url，文件上传后用于访问的 url
         * */
        val accessUrl: String
    )
}