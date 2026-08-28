package com.example.service_api.data

/**
 * @param session_id 会话id
 * @param image 图片信息 首次请求不需要图片
 * @param text_prompt 查询内容
 */
data class LLmQueryParams(
    val session_id: String,
    val image: String?,
    val text_prompt: String,
)

data class PredictionAction(
    val action_type: String,
    val from: List<Double>,
    val text: String,
    val target: String,
    val direction: String,
    val to: List<Double>,
    val thought: String,
    val app_id: String,
    val app_name: String,
    val package_name: String
)

data class PredictionMetadata(
    val processing_time_ms: Double
)

data class Prediction(
    val action: PredictionAction,
    val metadata: PredictionMetadata,
    val session_id: String,
    val text: String,
    var action_expand: List<PredictionAction>
)

data class Data(
    val prediction: Prediction,
    val request_id: String
)

data class LlmQueryResult(
    val data: Data,
    val message: String,
    val success: Boolean
)