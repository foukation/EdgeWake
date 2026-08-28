cmake_minimum_required(VERSION 3.22.1)

project("amapkey")

# 编译选项：开启优化、去除符号表，增加逆向难度
add_compile_options(
    -O2
    -fvisibility=hidden        # 隐藏所有非 JNI_EXPORT 符号
    -fstack-protector-strong   # 栈保护
)

add_library(
    amapkey          # .so 库名 → libamapkey.so
    SHARED
    amap_key.cpp
)

# 链接 Android 日志库（可选，调试时使用）
find_library(log-lib log)

target_link_libraries(
    amapkey
    ${log-lib}
)

# 去除调试符号，减小体积并增加逆向难度
set_target_properties(amapkey PROPERTIES
    LINK_FLAGS "-Wl,--strip-all"
)
