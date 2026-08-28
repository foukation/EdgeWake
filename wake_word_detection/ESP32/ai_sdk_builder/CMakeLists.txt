# ===========================================================================
# AI SDK 独立编译项目 - 项目根配置
# ===========================================================================
#
# 该项目用于独立编译 AI SDK 静态库，与主项目配置隔离。
#
# 优势：
# - 配置简洁：不包含 LVGL、esp-sr、BMI270 等无关组件
# - 编译快速：只编译 AI SDK 及其必要依赖
# - 芯片切换干净：每个芯片独立 build 目录
#
# 使用方法：
# 1. 复制 ai_sdk 源码到 components/ai_sdk
# 2. 确保 components/ai_sdk/CMakeLists.txt 使用源码编译配置
# 3. 运行 build_all.ps1 批量编译所有芯片
#
# ===========================================================================

cmake_minimum_required(VERSION 3.16)

# 项目名称
set(PROJECT_NAME "ai_sdk_builder")

# 包含 ESP-IDF 构建系统
include($ENV{IDF_PATH}/tools/cmake/project.cmake)

# 定义项目
project(${PROJECT_NAME})

