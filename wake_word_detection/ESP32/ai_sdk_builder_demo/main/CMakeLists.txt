idf_component_register(
    SRCS "repro_persistent_asr_start_failure.cpp" "main.cc" "test_ai_sdk.cpp" "test_asr_pcm.cpp" "test_voice_assistant.cpp"
         "test_speech_recognition_persistent.cpp" "test_inside_rc_chat.cpp"
         "test_voice_assistant_persistent.cpp"
         "test_chatbot.cpp" "test_translate.cpp" "test_content_summary.cpp"
    INCLUDE_DIRS "."
    EMBED_FILES "test_asr.pcm" "test_asr2.pcm"
    REQUIRES ai_sdk nvs_flash esp_event esp_wifi esp_netif
)
