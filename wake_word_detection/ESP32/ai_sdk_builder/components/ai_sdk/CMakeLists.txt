# AI SDK 组件配置文件
#
# 该文件定义了 ai_sdk 组件的编译配置，用于 ESP-IDF 构建系统。
#
# ============================================================================
# 组件介绍
# ============================================================================
#
# ai_sdk 是用于 ESP32 设备的 AI/IoT 云端连接 SDK，提供以下功能：
# - HTTP 客户端封装（支持 HTTPS）
# - 设备注册认证
# - 网关/代理配置
# - 设备数据上报（心跳）
# - WebSocket 通信（用于 ASR 语音识别）
#
# ============================================================================
# 头文件目录结构
# ============================================================================
#
# include/ai_sdk/               公开 API 头文件，上层业务可引用
# ├── ai_assistant_manager.h    SDK 入口，初始化配置
# ├── ai_sdk_log.h              日志控制接口（动态调整日志级别）
# ├── asr_intelligent_dialogue.h 语音识别与智能对话
# ├── speech_recognition_persistent.h 持续语音识别
# ├── gate_way.h                设备注册、网关获取、数据上报
# ├── types.h                   数据结构聚合头文件
# ├── audio/                    音频驱动相关
# │   ├── audio_codec.h         音频编解码器基类（厂商需继承实现）
# │   └── audio_config.h        音频配置与工厂函数（厂商需提供实现）
# └── types/                    按功能域拆分的数据结构
#     ├── common.h              通用常量（ResCode）
#     ├── gateway.h             网关相关（AgentUseCode, AgentServeData, GatewayInfo）
#     ├── device.h              设备注册（DeviceInfoRequest, DeviceData, DeviceInfoResponse）
#     ├── report.h              数据上报（DeviceReportRequest, DeviceReportData, DeviceReportResponse）
#     ├── voice_assistant.h     语音助手（AsrResult, DialogueResult）
#     └── speech_recognition_persistent_result.h  持续语音识别（SpeechRecognitionPersistentResult）
#
# src/include/                  内部头文件，仅 SDK 内部使用，对外不可见
# ├── api_config.h              API 端点配置
# ├── assist_utils.h            工具函数（URL 构建、签名生成）
# ├── http_client.h             HTTP 客户端封装
# ├── asr_websocket.h           WebSocket 客户端封装
# ├── device_client.h           设备认证客户端
# ├── gateway_client.h          网关客户端
# └── report_client.h           上报客户端
#
# src/audio/codecs/             音频驱动实现，仅 SDK 内部使用
# ├── es8311_audio_codec.h/cc   ES8311 音频编解码器
# ├── es8388_audio_codec.h/cc   ES8388 音频编解码器
# ├── es8374_audio_codec.h/cc   ES8374 音频编解码器
# ├── es8389_audio_codec.h/cc   ES8389 音频编解码器
# ├── box_audio_codec.h/cc      ESP-BOX 驱动 (ES8311+ES7210)
# ├── no_audio_codec.h/cc       无芯片驱动 (I2S 直连)
# └── dummy_audio_codec.h/cc    空实现驱动 (测试用)
#
# src/audio/                    音频模块内部实现（私有头文件 + 实现）
# ├── audio_decoder.h           音频解码器接口与工厂函数（内部）
# ├── audio_input.h             麦克风录音模块（内部，由 AsrIntelligentDialogue 管理）
# ├── tts_player.h              TTS 播放模块（内部，由 AsrIntelligentDialogue 管理）
# ├── mp3_decoder.h/cc          MP3 解码器实现（基于 esp_audio_codec）
# ├── audio_input.cc            麦克风录音模块实现
# ├── audio_decoder.cc          音频解码器工厂函数实现
# └── tts_player.cc             TTS 播放器实现（HTTP 下载 → MP3 解码 → 重采样 → 播放）
#
# ============================================================================
# 使用方式
# ============================================================================
#
# 方式 1：引用入口头文件（推荐，自动包含所有依赖）
#   #include "ai_sdk/ai_assistant_manager.h"
#
# 方式 2：按需引用（精细控制编译依赖）
#   #include "ai_sdk/types/device.h"       // 只需要设备相关类型
#   #include "ai_sdk/types/voice_assistant.h"  // 只需要语音助手类型
#
# ============================================================================
# 依赖组件
# ============================================================================
#
# - esp_http_client     ESP-IDF HTTP 客户端（用于云端通信）
# - esp_timer           ESP32 硬件定时器（用于定时上报）
# - json                cJSON 库（用于 JSON 数据解析和生成）
# - esp_websocket_client WebSocket 客户端（用于 ASR 实时通信）
# - esp_audio_codec     音频编解码器（用于 MP3 解码等）
#

idf_component_register(
    # ========================================================================
    # 源文件列表
    # ========================================================================
    SRCS
        "src/api_config.cc"                # API 配置管理
        "src/gateway_client.cc"            # 网关客户端（代理配置）
        "src/device_client.cc"             # 设备客户端（注册认证）
        "src/report_client.cc"             # 上报客户端（数据上报和心跳）
        "src/http_client.cc"               # HTTP 客户端封装
        "src/ai_assistant_manager.cc"      # AI 助手管理器（中央管理类）
        "src/gate_way.cc"                  # GateWay 网关转发层
        "src/assist_utils.cc"              # 辅助工具类（URL 构建和签名生成）
        "src/asr_websocket.cc"             # WebSocket 客户端封装（用于 ASR 通信）
        "src/asr_intelligent_dialogue.cc"  # ASR 智能对话
        "src/speech_recognition_persistent.cc"  # 持续语音识别
        "src/ai_sdk_log.cc"                # 日志控制接口
            # AIFoundationKit 相关
        "src/ai_foundation_kit.cc"         # AIFoundationKit 实现
        "src/chatbot_client.cc"            # Chatbot 闲聊客户端
        "src/translate_client.cc"          # 文本翻译客户端
        "src/sse_client.cc"                # SSE 流式客户端
        "src/content_summary_client.cc"    # 内容摘要客户端
        "src/inside_rc_chat_client.cc"     # 文本链路智能问答客户端

        # 音频相关 (移植自 ESP32-RTOS-AI-SDK)
        "src/audio/audio_config.cc"        # 音频配置与工厂函数
        "src/audio/audio_codec.cc"         # 音频编解码器基类实现
        "src/audio/audio_input.cc"         # 麦克风录音模块
        "src/audio/audio_decoder.cc"       # 音频解码器工厂函数
        "src/audio/mp3_decoder.cc"         # MP3 解码器实现
        "src/audio/tts_player.cc"          # TTS 播放器实现
        "src/audio/codecs/es8311_audio_codec.cc"   # ES8311 驱动
        "src/audio/codecs/es8388_audio_codec.cc"   # ES8388 驱动
        "src/audio/codecs/es8374_audio_codec.cc"   # ES8374 驱动
        "src/audio/codecs/es8389_audio_codec.cc"   # ES8389 驱动
        "src/audio/codecs/box_audio_codec.cc"      # ESP-BOX 驱动 (ES8311+ES7210)
        "src/audio/codecs/no_audio_codec.cc"       # 无芯片驱动 (I2S 直连)
        "src/audio/codecs/dummy_audio_codec.cc"   # 空实现驱动 (测试用)

    # ========================================================================
    # 公开头文件目录
    # ========================================================================
    # 上层业务可以通过 #include "ai_sdk/xxx.h" 引用
    INCLUDE_DIRS
        "include"

    # ========================================================================
    # 私有头文件目录
    # ========================================================================
    # 仅 SDK 内部使用，上层业务无法引用
    # 使用 PRIV_INCLUDE_DIRS 确保编译隔离
    PRIV_INCLUDE_DIRS
        "src/include"
        "src/audio"           # 音频模块内部头文件（mp3_decoder.h 等）
        "src/audio/codecs"    # 音频驱动内部头文件

    # ========================================================================
    # 依赖组件
    # ========================================================================
    REQUIRES
        esp_http_client      # HTTP/HTTPS 通信
        esp_timer            # 硬件定时器支持
        json                 # JSON 数据处理
        esp_websocket_client # WebSocket 通信（用于 ASR）
        esp_codec_dev        # 音频编解码器驱动（用于音频输入输出）
        driver               # ESP-IDF 驱动（I2S, I2C, GPIO 等）
        esp_audio_effects    # 音频效果处理（重采样等）
        esp_audio_codec      # 音频编解码（MP3 解码等）
)

# ============================================================================
# 静态库自动复制与优化
# ============================================================================
#
# 编译完成后，自动将静态库复制到 lib 目录并进行优化。
# 用于生成发布包时使用。
#
# 优化内容：
# - 移除调试符号（--strip-debug）：减小 60-80% 体积
# - 保留符号表：确保链接正常工作
#
# 注意：
# - 开发调试时可注释 strip 命令
# - 发布版本建议保留 strip 命令
#
# 多芯片支持：
# 静态库按芯片类型保存到 lib/${IDF_TARGET}/libai_sdk.a
# 例如：lib/esp32s3/libai_sdk.a, lib/esp32c3/libai_sdk.a
#

# 获取工具链的 strip 工具
# 使用 CMAKE_STRIP 自动适配目标芯片的工具链
set(_strip_tool "${CMAKE_STRIP}")

add_custom_command(TARGET ${COMPONENT_LIB} POST_BUILD
    # 1. 创建芯片专用目录 lib/${IDF_TARGET}/
    COMMAND ${CMAKE_COMMAND} -E make_directory 
        "${CMAKE_CURRENT_SOURCE_DIR}/lib/${IDF_TARGET}"
    
    # 2. 复制静态库到芯片专用目录
    COMMAND ${CMAKE_COMMAND} -E copy 
        "$<TARGET_FILE:${COMPONENT_LIB}>" 
        "${CMAKE_CURRENT_SOURCE_DIR}/lib/${IDF_TARGET}/libai_sdk.a"
    
    # 3. Strip 调试符号（发布版本优化）
    # --strip-debug: 只移除调试信息，保留符号表
    # 这样可以大幅减小体积，同时保证链接正常
    COMMAND ${_strip_tool} --strip-debug 
        "${CMAKE_CURRENT_SOURCE_DIR}/lib/${IDF_TARGET}/libai_sdk.a"
    
    COMMENT "AI SDK: Building libai_sdk.a for ${IDF_TARGET}"
)