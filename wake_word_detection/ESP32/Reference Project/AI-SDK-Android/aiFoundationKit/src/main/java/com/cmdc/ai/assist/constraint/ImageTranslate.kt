package com.cmdc.ai.assist.constraint

/**
 * 图片翻译 API 请求数据类
 */
data class ImageTranslationRequest(
    /**
     * 请求翻译的图片数据
     * */
    val imageBase64: String,
    /**
     * 源语种方向
     * */
    val from: String,
    /**
     * 译文语种方向
     * */
    val to: String,
    /**
     * 固定值: 3
     * */
    val v: Int = 3,
    /**
     * 图片粘贴类型: 0 - 关闭文字粘贴 1 - 返回原始粘贴 2 - 返回块状粘贴
     * */
    val paste: Int
) {
    /**
     * 图片粘贴类型枚举
     */
    enum class PasteType(val value: Int) {
        /**
         * "关闭文字贴合"
         * */
        NONE(0),

        /**
         * "返回整图贴合"
         * */
        ORIGINAL(1),

        /**
         * "返回块区贴合"
         * */
        BLOCK(2)
    }

}

/**
 * 图片翻译 API 响应数据类
 */
data class ImageTranslationResponse(
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
         * 源语种方向
         * */
        val from: String,
        /**
         * 目标语种方向
         * */
        val to: String,
        /**
         * 分段翻译内容
         * */
        val content: List<Content>?,
        /**
         * 未分段翻译原文
         * */
        val sumSrc: String?,
        /**
         * 未分段翻译译文
         * */
        val sumDst: String?,
        /**
         * 图片贴合 (整屏贴合)
         * */
        val pasteImg: String?
    )

    /**
     * 翻译内容
     * */
    data class Content(
        /**
         * 识别原文
         * */
        val src: String,
        /**
         * 翻译译文
         * */
        val dst: String,
        /**
         * 原文原始矩形区域位置;
         *
         * 格式："rect":"0 0 321 199"矩形的位置信息，依次顺序left, top, wide, high (以图片左上角顶点为坐标原点)
         * */
        val rect: String,
        /**
         * 翻译译文行数。
         *
         * 表示该分段信息是原文的多少行合并在一起
         * */
        val lineCount: Int,
        /**
         * 译文贴合矩形位置。
         *
         * （坐标0点为左上角），坐标顺序左上，右上，右下，左下
         * */
        val points: List<Point>?,
        /**
         * 分段贴合图片。
         *
         * paste=2有效，base64格式
         * */
        val pasteImg: String,
    )

    private data class Rect(
        val left: Float,            // 左边距
        val top: Float,             // 上边距
        val wide: Float,            // 宽度
        val high: Float             // 高度（以图片左上角原点为基准点）
    )

    /**
     * 译文贴合矩形位置。
     *
     * （坐标0点为左上角），坐标顺序左上，右上，右下，左下
     * */
    data class Point(
        /**
         * X 坐标
         * */
        val x: Float,
        /**
         * Y 坐标
         * */
        val y: Float
    )

    /**
     * 额外信息类
     */
    /*internal data class AdditionalInfo(
        val lineCount: Int?,        // 合并行数，表示试分段信息是否需要的多少行合并在一起
        val pasteImg: String?       // 分段贴合图片，paste=2 有效，base64 格式
    )*/
}