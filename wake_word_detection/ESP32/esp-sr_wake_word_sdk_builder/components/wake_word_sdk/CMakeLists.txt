# ===========================================================================
# Wake Word SDK 组件配置文件
# ===========================================================================
#
# 该文件定义了 wake_word_sdk 组件的编译配置，用于 ESP-IDF 构建系统。
#
# ============================================================================
# 组件介绍
# ============================================================================
#
# wake_word_sdk 是用于 ESP32 设备的语音唤醒 SDK，提供以下功能：
# - 语音唤醒词检测（支持 Wakenet 和 Multinet）
# - 支持多种唤醒词实现（EspWakeWord、AfeWakeWord、CustomWakeWord）
# - 唤醒词音频的 Opus 编码
#
# ============================================================================
# 头文件目录结构
# ============================================================================
#
# include/wake_word_sdk/          公开 API 头文件，上层业务可引用
# ├── wake_word.h                 唤醒词抽象接口
# └── wake_word_types.h           类型定义和配置结构体
#
# src/                            源文件
# ├── include/                    内部头文件，对外不可见
# │   ├── esp_wake_word.h         ESP32/C3 用（Wakenet 无 AFE）
# │   ├── afe_wake_word.h         ESP32-S3/P4 用（Wakenet + AFE）
# │   └── custom_wake_word.h      ESP32-S3/P4 用（Multinet 自定义词）
# ├── esp_wake_word.cc
# ├── afe_wake_word.cc
# ├── custom_wake_word.cc
# └── wake_word_factory.cc        工厂函数
#
# ============================================================================
# 使用方式
# ============================================================================
#
# #include "wake_word_sdk/wake_word.h"
# #include "wake_word_sdk/wake_word_types.h"
#
# // 使用工厂函数创建唤醒词实例
# auto wake_word = wake_word_sdk::CreateWakeWord(config, models_list);
#
# ============================================================================
# 依赖组件
# ============================================================================
#
# - esp-sr               ESP-IDF 语音识别组件（Wakenet、Multinet、AFE）
# - freertos             FreeRTOS 实时操作系统
# - esp-opus-encoder     Opus 音频编码器（可选，用于编码唤醒词音频）
#

# 根据目标芯片选择源文件
if(CONFIG_IDF_TARGET_ESP32S3 OR CONFIG_IDF_TARGET_ESP32P4)
    # ESP32-S3 和 ESP32-P4 支持 AFE 和 Multinet
    set(WAKE_WORD_SRCS
        "src/afe_wake_word.cc"
        "src/custom_wake_word.cc"
        "src/wake_word_factory.cc"
    )
else()
    # ESP32/C3 等芯片使用简单的 Wakenet
    set(WAKE_WORD_SRCS
        "src/esp_wake_word.cc"
        "src/wake_word_factory.cc"
    )
endif()

idf_component_register(
    # ========================================================================
    # 源文件列表
    # ========================================================================
    SRCS
        ${WAKE_WORD_SRCS}

    # ========================================================================
    # 公开头文件目录
    # ========================================================================
    # 上层业务可以通过 #include "wake_word_sdk/xxx.h" 引用
    INCLUDE_DIRS
        "include"

    # ========================================================================
    # 私有头文件目录
    # ========================================================================
    # 仅 SDK 内部使用，上层业务无法引用
    PRIV_INCLUDE_DIRS
        "src/include"

    # ========================================================================
    # 依赖组件
    # ========================================================================
    REQUIRES
        esp-sr               # ESP-IDF 语音识别组件
        freertos             # FreeRTOS
        esp-opus-encoder     # Opus 编码器
        esp-opus             # Opus 库（提供 opus.h）
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
# 多芯片支持：
# 静态库按芯片类型保存到 lib/${IDF_TARGET}/libwake_word_sdk.a
#

# 获取工具链的 strip 工具
set(_strip_tool "${CMAKE_STRIP}")

add_custom_command(TARGET ${COMPONENT_LIB} POST_BUILD
    # 1. 创建芯片专用目录 lib/${IDF_TARGET}/
    COMMAND ${CMAKE_COMMAND} -E make_directory 
        "${CMAKE_CURRENT_SOURCE_DIR}/lib/${IDF_TARGET}"
    
    # 2. 复制静态库到芯片专用目录
    COMMAND ${CMAKE_COMMAND} -E copy 
        "$<TARGET_FILE:${COMPONENT_LIB}>" 
        "${CMAKE_CURRENT_SOURCE_DIR}/lib/${IDF_TARGET}/libwake_word_sdk.a"
    
    # 3. Strip 调试符号（发布版本优化）
    COMMAND ${_strip_tool} --strip-debug 
        "${CMAKE_CURRENT_SOURCE_DIR}/lib/${IDF_TARGET}/libwake_word_sdk.a"
    
    COMMENT "Wake Word SDK: Building libwake_word_sdk.a for ${IDF_TARGET}"
)
