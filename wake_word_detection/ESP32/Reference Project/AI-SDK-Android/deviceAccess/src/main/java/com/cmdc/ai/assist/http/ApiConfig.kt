package com.cmdc.ai.assist.http

object ApiConfig {

    /*const val GATEWAY_BASE_URL = "https://z5f3vhk2.cxzfdm.com:30101"*/

    /*const val TERMINAL_INTELLIGENT_SERVICE_PLATFORM_BASE_URL =
        "https://62b98tux.cxzfdm.com:30101"*/

    const val TERMINAL_INTELLIGENT_SERVICE_PLATFORM_BASE_URL =
        "https://ivs.chinamobiledevice.com:11443"
    const val BASE_URL = "https://aqua-digital.aipaas.com"

    /*const val WSS_WEBSOCKET_ASR_BASE_URL = "wss://ivs.chinamobiledevice.com:11443/app-ws/v1"*/
    const val WSS_WEBSOCKET_ASR_BASE_URL = "wss://ivs.chinamobiledevice.com:11443"

    const val TIMEOUT: Long = 15000L

    var useAgent: Boolean = false
    var agentBaseUrl = ""
    var apiToken = ""
    var auth_token = ""

    const val ASR_INTELLIGENT_DIALOGUE_API = "$WSS_WEBSOCKET_ASR_BASE_URL/app-ws/v2/asr"
    const val AUTOMATIC_SPEECH_RECOGNITION_API = "$WSS_WEBSOCKET_ASR_BASE_URL/app-ws/v1/asr"
    const val AUTOMATIC_SPEECH_RECOGNITION_PERSISTENT_API = "$WSS_WEBSOCKET_ASR_BASE_URL/app-ws/v1/long-asr"
    const val ASR_TRANSLATION_API = "$WSS_WEBSOCKET_ASR_BASE_URL/app-ws/v1/realtime_speech_trans"
    const val GATEWAY_API = "/apgp/pl"
    const val AVAILABLE_MODELS_API = "/v1/models"
    const val MODEL_CHAT_API = "/llm/v1/chat/completions"
    const val OBTAIN_DEVICE_INFORMATION_API = "/v2/customer/device/secret/info"
    const val DEVICE_DATA_REPORT_API = "/v2/customer/device/report"
    const val CHAT_COMPLETIONS_API = "/device-api/ai/v1/chat/completions"
    const val TEXT_TO_AUDIO_API = "/device-api/ai/v2/text-to-audio"
    const val INSIDE_RC_CHAT_API = "/device-api/ai/v1/rc-chat"
    const val TEXT_TO_IMAGE_API = "/device-api/ai/v1/sync-draw"
    const val IMAGE_CLASSIFY_API = "/device-api/ai/v1/image-classify"
    const val SPEECH_FILE_TRANSFER_API = "/device-api/ai/v1/speech-file-transfer"
    const val SPEECH_FILE_TRANSFER_QUERY_API = "/device-api/ai/v1/speech-file-transfer-query"
    const val GET_UPLOAD_AUDIO_URL_API = "/device-api/ai/v1/get-upload-audio-url"
    const val IMAGE_TRANSLATE_API = "/device-api/ai/v1/image-translate"
    const val OCR_API = "/device-api/ai/v1/ocr"
    const val WORD_QUERY_API = "/device-api/ai/v1/word-query"
    const val NOTE_SUMMARY_API = "/device-api/ai/v1/note-summary"
    const val TEXT_TRANSLATE_API = "/device-api/ai/v1/text-translate"

}