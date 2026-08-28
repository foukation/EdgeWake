package com.example.service_api.config

object ApiUrl {
    const val OCR_URL = "http://36.213.71.200:5692/multimodal"

    const val INTENTION_CLIENT_TOKEN = "http://36.213.71.163:11453/api/v1/web/token"
    const val INTENTION_CLIENT_APP = "http://36.213.71.163:11453/api/v1/web/appList"
    const val INTENTION_CLIENT_ACTIONS = "http://36.213.71.163:11508/api/v1/web/generateAction"
    const val INTENTION_CLIENT_ACTIONS_MUL = "http://36.213.71.163:11453/api/v1/web/multGenerateAction"
    const val INTENTION_CLIENT_NORMALIZE_TIME = "http://36.213.71.163:11453/api/v1/web/normalize-time"
    const val CREATE_TRIP_URL = "http://36.213.71.163:11453/api/v1/task"
    const val GET_TRIP_URL = "http://36.213.71.163:11453/api/v1/task"
    const val DEL_TRIP_URL = "http://36.213.71.163:11453/api/v1/task"
    const val LLM_URL = "http://36.213.71.163:11470/predict"

}