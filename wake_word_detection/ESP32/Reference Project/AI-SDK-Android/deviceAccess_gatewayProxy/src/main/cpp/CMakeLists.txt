cmake_minimum_required(VERSION 3.18.1)

project("native_url_config")

# 编译优化选项（增加逆向难度）
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++17")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3")                      # 最高优化
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fvisibility=hidden")     # 隐藏符号
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -ffunction-sections")     # 函数分段
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fdata-sections")         # 数据分段

# 链接器选项
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,--gc-sections")  # 移除未使用段
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,--strip-all")    # 去除符号表

# 创建共享库
add_library(
        native_url_config
        SHARED
        native_config.cpp
)

# 链接 log 库（用于调试，生产环境可移除）
find_library(log-lib log)
target_link_libraries(native_url_config ${log-lib})

