cmake_minimum_required(VERSION 3.16)

set(PROJECT_VER "1.0.0")

add_compile_options(-Wno-missing-field-initializers)

include($ENV{IDF_PATH}/tools/cmake/project.cmake)
project(ai_sdk_demo)

# Link prebuilt ai_sdk library BEFORE the component group so IDF's
# outer --start-group can resolve all transitive dependencies automatically.
target_link_options(${CMAKE_PROJECT_NAME}.elf BEFORE PRIVATE
    -Wl,--whole-archive
    "${CMAKE_SOURCE_DIR}/components/ai_sdk/lib/${IDF_TARGET}/libai_sdk.a"
    -Wl,--no-whole-archive
)
