# ===========================================================================
# Wake Word SDK Builder 顶层 CMakeLists.txt
# ===========================================================================
#
# 该文件是 wake_word_sdk_builder 项目的顶层 CMake 配置。
# 用于编译 wake_word_sdk 组件并生成静态库。
#

# 最低 CMake 版本要求
cmake_minimum_required(VERSION 3.16)

# 包含 ESP-IDF 项目配置
include($ENV{IDF_PATH}/tools/cmake/project.cmake)

# 项目名称
project(wake_word_sdk_builder)
