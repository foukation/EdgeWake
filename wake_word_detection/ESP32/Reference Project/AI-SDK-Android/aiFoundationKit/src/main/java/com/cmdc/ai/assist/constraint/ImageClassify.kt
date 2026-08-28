package com.cmdc.ai.assist.constraint

/**
 * 图像识别 API 请求数据类
 */
data class ImageRecognitionRequest(

    /**
     * 图像数据，base64 编码，要求 base64 编码后大小不超过 8M，最短边至少 15px，最长边最大 8192px,支持 jpg/png/bmp 格式。注意：图片需要 base64 编码、去掉编码头（data:image/jpg;base64,）。
     * 和 url 二选一
     * */
    val imageBase64: String? = null,
    /**
     * 图片完整 URL，URL 长度不超过 1024 字节，URL 对应的图片 base64 编码后大小不超过 8M，最短边至少 15px，最长边最大 8192px,支持 jpg/png/bmp格式，当 image 字段存在时 url 字段失效。
     * 和 image 二选一
     *  */
    val url: String? = null,
    /**
     * 用于控制返回结果是否带有百科信息，若不输入此参数，则默认不返回百科结果；若输入此参数，会根据输入的整数返回相应个数的百科信息
     * */
    val baikeNum: Int

)

/**
 * 图像识别 API 响应数据类
 */
data class ImageRecognitionResponse(
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
         * 返回结果数目，及 result 数组中的元素个数，最多返回 5 个结果
         * */
        val result_num: Int,
        /**
         * 识别结果数组
         * */
        val result: List<Result>?
    ) {
        /**
         * 识别结果项
         */
        data class Result(
            /**
             * 置信度
             * */
            val score: Float,
            val root: String,
            /**
             * 图片中的物体或场景名称
             * */
            val keyword: String,
            /**
             * 对应识别结果的百科词条名称
             * */
            val baike_info: BaikeInfo? = null,
        )

        /**
         * 百科信息
         */
        data class BaikeInfo(
            /**
             * 对应识别结果百度百科页面链接
             * */
            val baike_url: String? = null,
            /**
             * 对应识别结果百科图片链接
             * */
            val image_url: String? = null,
            /**
             * 对应识别结果百科内容描述
             * */
            val description: String? = null
        )
    }

}