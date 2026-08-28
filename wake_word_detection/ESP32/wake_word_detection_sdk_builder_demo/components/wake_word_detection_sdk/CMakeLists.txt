# ===========================================================================
# Wake Word Detection SDK 组件配置文件 - 预编译库版本
# ===========================================================================
#
# 该文件用于配置使用预编译静态库的唤醒词检测 SDK 组件。
#
# ============================================================================
# 使用方法
# ============================================================================
#
# 1. 确保静态库文件已存在：lib/${IDF_TARGET}/libwake_word_detection_sdk.a
# 2. src/ 目录无需存在（源码保护，实现已在预编库中）
# 3. 上层业务通过 #include "xxx.h" 引用 include/ 下的公开头文件
#
# ============================================================================
# 依赖组件
# ============================================================================
#
# - esp-tflite-micro   TFLite Micro 推理引擎（在项目 main/idf_component.yml 声明）
#
# ============================================================================
# ABI 兼容性注意
# ============================================================================
#
# - ESP-IDF 版本须与编译库时相同（或以上）
# - 编译器版本必须相同
# - 目标芯片自动匹配 lib/${IDF_TARGET}/libwake_word_detection_sdk.a
# - C++ 标准必须相同
# ===========================================================================

# ESP-IDF 要求 SRCS 不能为空，创建占位文件
file(WRITE "${CMAKE_CURRENT_BINARY_DIR}/wake_word_detection_sdk_stub.c"
     "/* Wake Word Detection SDK stub file for prebuilt library */\n")

idf_component_register(
    # 占位源文件（预编库无需真实源码）
    SRCS
        "${CMAKE_CURRENT_BINARY_DIR}/wake_word_detection_sdk_stub.c"

    # 公开头文件目录
    INCLUDE_DIRS
        "include"

    # 依赖组件（须与源码版本保持一致）
    REQUIRES
        esp-tflite-micro
)

# ============================================================================
# 链接预编译静态库
# ============================================================================
#
# 根据 ${IDF_TARGET} 自动选择正确的预编译库。
# 使用 --start-group / --end-group 允许链接器多次扫描，解决循环依赖。
# ============================================================================

set(WWD_SDK_LIB_PATH "${CMAKE_CURRENT_LIST_DIR}/lib/${IDF_TARGET}/libwake_word_detection_sdk.a")

# 检查库是否存在
if(NOT EXISTS "${WWD_SDK_LIB_PATH}")
    message(FATAL_ERROR
        "\n"
        "═══════════════════════════════════════════════════════════\n"
        "  Wake Word Detection SDK ERROR: No prebuilt library for ${IDF_TARGET}\n"
        "═══════════════════════════════════════════════════════════\n"
        "  Expected: ${WWD_SDK_LIB_PATH}\n"
        "  当前仅提供 esp32s3 预编库，其它芯片请先用 builder 工程编译。\n"
        "═══════════════════════════════════════════════════════════\n"
    )
endif()

target_link_libraries(${COMPONENT_LIB} INTERFACE
    "-Wl,--start-group"
    "${WWD_SDK_LIB_PATH}"
    $<TARGET_FILE:__idf_espressif__esp-tflite-micro>
    "-Wl,--end-group"
)

message(STATUS "")
message(STATUS "╔═══════════════════════════════════════════════════════════╗")
message(STATUS "║  Wake Word Detection SDK: Using precompiled library       ║")
message(STATUS "╠═══════════════════════════════════════════════════════════╣")
message(STATUS "║  Target:  ${IDF_TARGET}")
message(STATUS "║  Library: lib/${IDF_TARGET}/libwake_word_detection_sdk.a")
message(STATUS "║  Headers: include/                                        ║")
message(STATUS "╚═══════════════════════════════════════════════════════════╝")
message(STATUS "")
