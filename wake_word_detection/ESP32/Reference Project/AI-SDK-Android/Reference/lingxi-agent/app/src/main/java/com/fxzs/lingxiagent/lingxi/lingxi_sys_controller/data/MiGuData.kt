package com.example.device_control.data

/**
 *创建者：ZyOng
 *描述：媒体
 *创建时间：2025/7/4 下午4:25
 */
data class MiGuData(
    val params: Params,
    val type: String
)

data class Params(
    val extra: Extra,
    val pageID: String
)

data class Extra(
    val autoSearch: Boolean,
    val searchHint: String,
    val searchType: String
)