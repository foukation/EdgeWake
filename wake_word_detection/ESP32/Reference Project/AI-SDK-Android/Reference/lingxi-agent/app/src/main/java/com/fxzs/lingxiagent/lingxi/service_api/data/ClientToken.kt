package com.example.service_api.data

data class ClientApiGetTokenRes(
    val code: Int,
    val msg: String,
    var status: String,
    val data: String,
)