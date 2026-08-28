package com.cmdc.ai.assist.constraint

/**
 * Ocr API 响应数据结构
 */
data class OcrResponse(
    val code: Int,
    val data: ResponseData,
    val msg: String
)

/**
 * Ocr API 响应数据内容
 */
data class ResponseData(
    /**
     * 识别结果数组
     * */
    val words_result: List<WordResult>,
    /**
     * 唯一的 log id，用于问题定位
     * */
    val log_id: String,
    /**
     * 识别结果数，表示 words_result 的元素个数
     * */
    val words_result_num: Int
)

/**
 * 识别结果项
 */
data class WordResult(
    /**
     * 识别结果字符串
     * */
    val words: String,
    /**
     * 位置数组（坐标 0 点为左上角）
     * */
    val location: Location
)

/**
 * 文字位置信息
 */
data class Location(
    /**
     * 表示定位位置的长方形左上顶点的垂直坐标
     * */
    val top: Int,
    /**
     * 表示定位位置的长方形左上顶点的水平坐标
     * */
    val left: Int,
    /**
     * 表示定位位置的长方形的宽度
     * */
    val width: Int,
    /**
     * 表示定位位置的长方形的高度
     * */
    val height: Int
)