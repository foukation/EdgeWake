package com.cmdc.ai.assist.constraint

/**
 * 图片生成 API 请求数据类
 */
data class ImageGenerationRequest(
    /**
     *  用户 ID
     * */
    val userId: String,
    /**
     * 参数列表
     * */
    val params: Map<String, Any>? = null,
    /**
     * 用户输入的文本
     * */
    val query: String,
    /**
     * "601"，文心一格
     * */
    val style: String = "601",

    /**
     * 业务令牌, 调用方自定义字符串, 推荐随机 16 位字符串, callback 时会透传给业务服务端, 可用于业务方数据的安全校验
     * */
    val token: String? = null,
    /**
     * 图片生成的数量，最多 4 张
     * */
    val batchSize: Int? = null,
    /**
     * 参考图的链接
     * */
    val referenceUrl: String? = null,
    /**
     * 1-10, referenceUrl 填写时必填，为参考图相似度
     * */
    val changeDegree: Int? = null,
    /**
     * 图片高度，宽高支持的尺寸： 512x512、640x360、360x640、1024x1024、1280x720、720x1280
     * */
    val height: Int? = null,
    /**
     * 图片宽度，宽高支持的尺寸：512x512、640x360、360x640、1024x1024、1280x720、720x1280
     * */
    val width: Int? = null,
    /**
     * 有如下三个值 bd_std、bd_std_artistic、bd_std_anime 风格名称与服务的对应关系如下： bd_std: 通用生图服务（默认值，生图速度快约5秒） bd_std_artistic: 艺术化生图服务（生图速度约15秒） bd_std_anime: 二次元生图服务（生图速度约15秒）
     * */
    val model: String? = null
) {

}

/**
 * 图片生成 API 响应数据类
 */
data class ImageGenerationResponse(
    /**
     * 结果码，0 为成功
     * */
    val code: Int,
    /**
     * 返回数据
     * */
    val data: Data?,
    /**
     * 消息
     * */
    val msg: String,
) {
    /**
     * 响应数据内容
     */
    data class Data(
        /**
         * 图片 url 地址数组，图片的 url 地址有效期 1 个小时
         * */
        val images: List<String>? = null,
        /**
         * 失败状态码
         * */
        val errCode: Int? = null,
        /**
         * 执行出错原因，可能命中黄反或其他原因
         * */
        val errMsg: String? = null,
    )

}