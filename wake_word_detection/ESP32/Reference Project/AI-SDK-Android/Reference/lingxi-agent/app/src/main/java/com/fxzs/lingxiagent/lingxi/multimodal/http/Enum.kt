package com.ai.multimodal.http

enum class ResCode(val alias: String) {
    SUCCESS("success"),
    ERROR("error"),
    AUTH_ERR("401"),
    PARAM_ERR("400"),
    SERVE_ERR("500"),
}
