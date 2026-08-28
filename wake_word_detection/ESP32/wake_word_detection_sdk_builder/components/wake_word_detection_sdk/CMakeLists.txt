# ===========================================================================
# Wake Word Detection SDK 组件配置文件
# ===========================================================================
#
# 该组件是基于 TFLite Micro 的自训练模型唤醒词检测 SDK。
# 提供：音频 -> Fbank 特征 -> 模型推理 -> 唤醒解码 的核心能力。
#
# 目录结构：
#   include/          公开 API 头文件，上层业务可 #include "xxx.h" 引用
#   src/              实现源文件
#   src/include/      内部私有头文件（fbank.h / fft.h），对外不可见
#
# 依赖组件：
#   - esp-tflite-micro   TFLite Micro 推理引擎（在项目 main/idf_component.yml 声明）
#
# 编译产物：
#   静态库按芯片保存到 lib/${IDF_TARGET}/libwake_word_detection_sdk.a
# ===========================================================================

idf_component_register(
    SRCS
        "src/feature_pipeline.cc"
        "src/recognize_commands.cc"
        "src/offline_decode.cc"
        "src/fbank.cc"
        "src/fft.cc"
        "src/model.cc"

    # 公开头文件目录（对外可见）
    INCLUDE_DIRS
        "include"

    # 私有头文件目录（仅 SDK 内部使用）
    PRIV_INCLUDE_DIRS
        "src/include"

    # 依赖组件
    REQUIRES
        esp-tflite-micro
)

# 降低告警等级以便编译引擎源码（fbank/fft/tflite 相关）
target_compile_options(${COMPONENT_LIB} PRIVATE
    -Wno-maybe-uninitialized
    -Wno-missing-field-initializers
    -Wno-error=sign-compare
    -Wno-error=double-promotion
    -Wno-type-limits)

# ===========================================================================
# 静态库自动复制与优化（POST_BUILD）
# ===========================================================================
#
# 编译完成后，把静态库复制到 lib/${IDF_TARGET}/ 并 strip 调试符号。
# --strip-debug: 只移除调试信息、保留符号表，可大幅减小体积且不影响链接。
# ===========================================================================

set(_strip_tool "${CMAKE_STRIP}")

add_custom_command(TARGET ${COMPONENT_LIB} POST_BUILD
    # 1. 创建芯片专用目录 lib/${IDF_TARGET}/
    COMMAND ${CMAKE_COMMAND} -E make_directory
        "${CMAKE_CURRENT_SOURCE_DIR}/lib/${IDF_TARGET}"

    # 2. 复制静态库到芯片专用目录
    COMMAND ${CMAKE_COMMAND} -E copy
        "$<TARGET_FILE:${COMPONENT_LIB}>"
        "${CMAKE_CURRENT_SOURCE_DIR}/lib/${IDF_TARGET}/libwake_word_detection_sdk.a"

    # 3. Strip 调试符号（发布版本优化）
    COMMAND ${_strip_tool} --strip-debug
        "${CMAKE_CURRENT_SOURCE_DIR}/lib/${IDF_TARGET}/libwake_word_detection_sdk.a"

    COMMENT "Wake Word Detection SDK: Building libwake_word_detection_sdk.a for ${IDF_TARGET}"
)
