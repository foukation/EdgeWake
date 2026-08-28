# ===========================================================================
# AI SDK 组件配置文件 - 预编译库版本
# ===========================================================================
#
# 该文件用于配置使用预编译静态库的 AI SDK 组件。
#
# ============================================================================
# 使用方法
# ============================================================================
#
# 1. 确保静态库文件已存在：lib/libai_sdk.a
# 2. 将此文件名为 CMakeLists.txt。
# 3. src/ 目录可以删除（源码保护）
# 4. 编译项目验证功能
#
# ============================================================================
# 依赖组件
# ============================================================================
#
# 使用此预编译库的项目必须包含以下组件：
# - esp_http_client     HTTP/HTTPS 通信
# - esp_timer           硬件定时器支持
# - json                cJSON 库（JSON 数据处理）
# - esp_websocket_client WebSocket 通信（用于 ASR）
# - esp_codec_dev       音频芯片驱动（音频模块）
# - driver              ESP-IDF I2S/I2C/GPIO 驱动（音频模块）
# - esp_audio_effects   重采样/音效处理（音频模块）
# - esp_audio_codec     MP3 解码（音频模块）
#
# ============================================================================
# ABI 兼容性注意
# ============================================================================
#
# 静态库必须满足以下条件才能正常使用：
# - ESP-IDF 版本须与编译库时的版本相同（或以上版本）
# - 编译器版本必须相同
# - 目标芯片会自动匹配（lib/${IDF_TARGET}/libai_sdk.a）
# - C++ 标准必须相同（默认 C++17）
#
# 支持的芯片：
# - Xtensa 架构：esp32, esp32s2, esp32s3
# - RISC-V 架构：esp32c2, esp32c3, esp32c5, esp32c6, esp32c61, esp32p4
#

# ESP-IDF 要求 SRCS 不能为空，创建占位文件
file(WRITE "${CMAKE_CURRENT_BINARY_DIR}/ai_sdk_stub.c" 
     "/* AI SDK stub file for prebuilt library */\n")

idf_component_register(
    # ========================================================================
    # 占位源文件
    # ========================================================================
    # ESP-IDF 要求 SRCS 不能为空，使用自动生成的占位文件
    SRCS 
        "${CMAKE_CURRENT_BINARY_DIR}/ai_sdk_stub.c"

    # ========================================================================
    # 公开头文件目录
    # ========================================================================
    # 上层业务可以通过 #include "ai_sdk/xxx.h" 引用
    INCLUDE_DIRS
        "include"

    # ========================================================================
    # 依赖组件
    # ========================================================================
    # 必须与源码版本保持一致
    REQUIRES
        esp_http_client      # HTTP/HTTPS 通信
        esp_timer            # 硬件定时器支持
        json                 # JSON 数据处理
        esp_websocket_client # WebSocket 通信（用于 ASR）
	esp_codec_dev        # 音频芯片驱动（音频模块）
        driver               # ESP-IDF I2S/I2C/GPIO 驱动（音频模块）
        esp_audio_effects    # 重采样/音效处理（音频模块）
        esp_audio_codec      # MP3 解码（音频模块）
        tcp_transport        # TCP 传输层（esp_http_client 依赖）
        http_parser          # HTTP 解析（esp_http_client 依赖）
        esp-tls              # TLS/SSL 支持（HTTPS 依赖）
        mbedtls              # 加密库（TLS 依赖）
)

# ============================================================================
# 链接预编译静态库
# ============================================================================
#
# 多芯片支持：
# 根据 ${IDF_TARGET} 自动选择正确的预编译库。
# 支持的芯片：esp32, esp32s2, esp32s3, esp32c2, esp32c3, esp32c5, esp32c6, esp32c61, esp32p4
#
# 目录结构：
# lib/
# ├── esp32/libai_sdk.a
# ├── esp32s2/libai_sdk.a
# ├── esp32s3/libai_sdk.a
# └── ... (其他芯片)
#
# 解决静态库链接顺序问题：
# 使用 --start-group / --end-group 允许链接器多次扫描库，
# 解决循环依赖和顺序问题。
#

# 根据目标芯片选择正确的库
set(AI_SDK_LIB_PATH "${CMAKE_CURRENT_LIST_DIR}/lib/${IDF_TARGET}/libai_sdk.a")
set(ESP_AUDIO_SIMPLE_DEC_LIB_PATH "${PROJECT_DIR}/managed_components/espressif__esp_audio_codec/lib/${IDF_TARGET}/libesp_audio_simple_dec.a")
set(ESP_AUDIO_CODEC_LIB_PATH "${PROJECT_DIR}/managed_components/espressif__esp_audio_codec/lib/${IDF_TARGET}/libesp_audio_codec.a")

# 检查库是否存在
if(NOT EXISTS "${AI_SDK_LIB_PATH}")
    message(FATAL_ERROR 
        "\n"
        "═══════════════════════════════════════════════════════════\n"
        "  AI SDK ERROR: No prebuilt library for ${IDF_TARGET}\n"
        "═══════════════════════════════════════════════════════════\n"
        "  Expected: ${AI_SDK_LIB_PATH}\n"
        "\n"
        "  Supported targets:\n"
        "    - esp32, esp32s2, esp32s3\n"
        "    - esp32c2, esp32c3, esp32c5, esp32c6, esp32c61\n"
        "    - esp32p4\n"
        "═══════════════════════════════════════════════════════════\n"
    )
endif()

# Verify external prebuilt codec libs used by AI SDK MP3 decoder path.
if(NOT EXISTS "${ESP_AUDIO_SIMPLE_DEC_LIB_PATH}")
    message(FATAL_ERROR
        "AI SDK ERROR: Missing ${ESP_AUDIO_SIMPLE_DEC_LIB_PATH}. "
        "Please make sure espressif/esp_audio_codec is downloaded correctly."
    )
endif()

if(NOT EXISTS "${ESP_AUDIO_CODEC_LIB_PATH}")
    message(FATAL_ERROR
        "AI SDK ERROR: Missing ${ESP_AUDIO_CODEC_LIB_PATH}. "
        "Please make sure espressif/esp_audio_codec is downloaded correctly."
    )
endif()

target_link_libraries(${COMPONENT_LIB} INTERFACE 
    # Keep AI SDK and codec decode libs in the same link group so static
    # resolution can satisfy symbols referenced by libai_sdk.a (e.g. MP3/simple-dec).
    # Use real archive paths to avoid relying on internal/private CMake target names.
    "-Wl,--start-group"
    "${AI_SDK_LIB_PATH}"
    "${ESP_AUDIO_SIMPLE_DEC_LIB_PATH}"
    "${ESP_AUDIO_CODEC_LIB_PATH}"
    $<TARGET_FILE:__idf_esp_http_client>
    $<TARGET_FILE:__idf_json>
    $<TARGET_FILE:__idf_esp_timer>
    $<TARGET_FILE:__idf_espressif__esp_websocket_client>
    "-Wl,--end-group"
)

# ============================================================================
# 编译信息提示
# ============================================================================
message(STATUS "")
message(STATUS "╔═══════════════════════════════════════════════════════════╗")
message(STATUS "║  AI SDK: Using precompiled library                        ║")
message(STATUS "╠═══════════════════════════════════════════════════════════╣")
message(STATUS "║  Target:  ${IDF_TARGET}")
message(STATUS "║  Library: lib/${IDF_TARGET}/libai_sdk.a")
message(STATUS "║  Headers: include/ai_sdk/                                 ║")
message(STATUS "╚═══════════════════════════════════════════════════════════╝")
message(STATUS "")