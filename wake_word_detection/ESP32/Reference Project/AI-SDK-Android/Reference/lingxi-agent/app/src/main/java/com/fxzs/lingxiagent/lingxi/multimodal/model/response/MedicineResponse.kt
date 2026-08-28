package com.ai.multimodal.model.response

// 闹钟信息
data class Alarm(
    val id: Int,
    val time: String,
    val label: String,
    val enabled: Boolean,
    val repeat: List<String>
)

// 响应内容
data class MedicineResponseContent(
    val alarms: List<Alarm>
)

// 完整响应数据结构
data class MedicineResponse(
    val state: String,
    val content: MedicineResponseContent,
    val time: Double
)