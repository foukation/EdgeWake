# ===========================================================================
# Wake Word SDK 组件配置文件（预编译库版本）
# ===========================================================================
#
# 该文件用于链接预编译的 wake_word_sdk 静态库。
# 静态库由 wake_word_sdk_builder 项目编译生成。
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
# 使用此 SDK 的项目必须在 idf_component.yml 中声明以下依赖：
# - espressif/esp-sr: ~2.2.0
# - 78/esp-opus-encoder: ~2.4.1
#

# 注册组件（无源文件，只有头文件和预编译库）
idf_component_register(
    # ========================================================================
    # 公开头文件目录
    # ========================================================================
    INCLUDE_DIRS
        "include"

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
# 链接预编译静态库
# ============================================================================
#
# 根据目标芯片选择对应的静态库。
# 静态库路径: lib/${IDF_TARGET}/libwake_word_sdk.a
#

set(WAKE_WORD_SDK_LIB "${CMAKE_CURRENT_SOURCE_DIR}/lib/${IDF_TARGET}/libwake_word_sdk.a")

if(EXISTS ${WAKE_WORD_SDK_LIB})
    # 链接静态库
    target_link_libraries(${COMPONENT_LIB} INTERFACE ${WAKE_WORD_SDK_LIB})
    message(STATUS "Wake Word SDK: Linking ${WAKE_WORD_SDK_LIB}")
else()
    # 静态库不存在，显示警告
    message(WARNING "Wake Word SDK: Library not found for ${IDF_TARGET}")
    message(WARNING "  Expected: ${WAKE_WORD_SDK_LIB}")
    message(WARNING "  Please run build_all.ps1 in wake_word_sdk_builder to generate the library")
endif()
