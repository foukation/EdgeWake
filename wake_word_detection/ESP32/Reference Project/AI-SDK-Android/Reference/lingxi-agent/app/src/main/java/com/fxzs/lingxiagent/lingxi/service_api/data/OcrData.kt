package com.example.service_api.data

data class OcrResult(
    val state:String,
    val content: ArrayList<Float>,
)

data class IsOcrResult(
    val state:String,
    val content: OcrContent,
)

data class OcrContent(
    val state: Int,
    val coord: ArrayList<Float>,
)

data class QueryParamsForm(
    val imagePath: String,
    val query: String,
)

data class QueryParams(
    val task_type: String,
    val stream: Boolean,
    val image: ImgContent,
    val query: String,
)

data class ImgContent(
    val type: String,
    val width: Int,
    val height: Int,
    val img_encoded: String,
)