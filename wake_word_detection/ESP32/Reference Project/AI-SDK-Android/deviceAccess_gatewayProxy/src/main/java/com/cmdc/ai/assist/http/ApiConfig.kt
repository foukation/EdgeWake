package com.cmdc.ai.assist.http

import com.cmdc.ai.assist.native.NativeUrlProvider

object ApiConfig {

    /*const val GATEWAY_BASE_URL = "https://z5f3vhk2.cxzfdm.com:30101"*/

    /*const val TERMINAL_INTELLIGENT_SERVICE_PLATFORM_BASE_URL_TEST =
        "https://62b98tux.cxzfdm.com:30101"*/

    val WSS_WEBSOCKET_ASR_BASE_URL_TEST: String
        get() = "wss://62b98tux.cxzfdm.com:30101"

    val WSS_WEBSOCKET_ASR_ALIYUN_BASE_URL_TEST: String
        get() = "wss://z5f3vhk2.cxzfdm.com:30101"

    // 从 Native 层获取 URL（生产环境）
    val TERMINAL_INTELLIGENT_SERVICE_PLATFORM_BASE_URL: String
        get() = NativeUrlProvider.getTerminalServiceUrlCached()
    val BASE_URL: String
        get() = NativeUrlProvider.getAiPaasUrlCached()

    /*const val WSS_WEBSOCKET_ASR_BASE_URL = "wss://ivs.chinamobiledevice.com:11443/app-ws/v1"*/
    val WSS_WEBSOCKET_ASR_BASE_URL: String
        get() = NativeUrlProvider.getWebSocketUrlCached()
    // 测试环境 URL（从 Native 层获取）
    val TERMINAL_INTELLIGENT_SERVICE_PLATFORM_BASE_URL_TEST: String
        get() = NativeUrlProvider.getTerminalServiceUrlTestCached()

    const val TIMEOUT: Long = 15000L

    var useAgent: Boolean = false
    var agentBaseUrl = ""
    var apiToken = ""
    var auth_token = ""

    // API 路径（基于动态 URL 构建）
    val ASR_INTELLIGENT_DIALOGUE_API_TEST: String
        get() = NativeUrlProvider.getWebSocketUrlTestCached()

    val ASR_INTELLIGENT_DIALOGUE_API: String
        get() = "$WSS_WEBSOCKET_ASR_BASE_URL/app-ws/v2/asr"

    val AUTOMATIC_SPEECH_RECOGNITION_API: String
        get() = "$WSS_WEBSOCKET_ASR_BASE_URL/app-ws/v1/asr"

    val AUTOMATIC_SPEECH_RECOGNITION_PERSISTENT_API: String
        get() = "$WSS_WEBSOCKET_ASR_BASE_URL/app-ws/v1/long-asr"

    val AUTOMATIC_SPEECH_RECOGNITION_PERSISTENT_15376_API: String
        get() = "$WSS_WEBSOCKET_ASR_BASE_URL/app-ws/v2/long-asr"

    val AUTOMATIC_SPEECH_RECOGNITION_PERSISTENT_ALIYUN_API: String
        get() = "$WSS_WEBSOCKET_ASR_BASE_URL/lingxi/ws/asr/realtime"

    val ASR_TRANSLATION_API: String
        get() = "$WSS_WEBSOCKET_ASR_BASE_URL/app-ws/v1/realtime_speech_trans"
    const val GATEWAY_API = "/apgp/pl"
    const val AVAILABLE_MODELS_API = "/v1/models"
    const val MODEL_CHAT_API = "/llm/v1/chat/completions"
    const val OBTAIN_DEVICE_INFORMATION_API = "/v2/customer/device/secret/info"
    const val DEVICE_DATA_REPORT_API = "/v2/customer/device/report"
    const val CHAT_COMPLETIONS_API = "/device-api/ai/v1/chat/completions"

    const val CHAT_BOT_COMPLETIONS_API = "/device-api/ai/v2/chat/completions"
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

    const val TEXT_TRANSLATE_ALI_YUN_API = "/device-api/ai/v2/text-translate"

}