# 嵌入式 Linux AI-SDK 开发方案 - 深度分析

## 一、如何覆盖 90% 的 Linux 嵌入式系统？

### 核心策略

1. 使用 POSIX 标准 API
2. 使用广泛支持的库 (libcurl, ALSA, libwebsockets)
3. 抽象平台特有部分
4. 支持交叉编译 (CMake)
5. 支持静态链接 + 动态链接

---

## 二、市场上的嵌入式 Linux 平台分布

| 架构 | 市场份额 | 典型设备 |
|--------|----------|----------|
| ARM Cortex-A (32/64-bit) | **70%+** | 树莓派, RK3399, 全志, i.MX6/8 |
| MIPS | **10%** | 路由器, MTK 芯片 |
| x86/x64 | **10%** | 工控机, 边缘服务器 |
| RISC-V | **5%** | 新兴平台 |
| 其他 | **5%** | PowerPC, ARC 等 |

---

## 三、推荐技术栈

### 3.1 网络通信

| 功能 | 推荐 | 原因 |
|------|------|------|
| HTTP/HTTPS | **libcurl** | 几乎所有 Linux 都有，支持交叉编译，支持静态链接 |
| WebSocket | **libwebsockets** | 轻量级，纯 C，已用于数千万设备，支持 mbedTLS |
| JSON | **cJSON** | 嵌入式标准，超小体积，无依赖 |
| TLS | **mbedTLS** 或 **OpenSSL** | mbedTLS 更适合嵌入式 |

### 3.2 音频播放

| 功能 | 推荐 | 原因 |
|------|------|------|
| 音频输出 | **ALSA (libasound)** | Linux 内核标配，所有嵌入式 Linux 都有 |
| 备选 | **PulseAudio / PipeWire** | 部分系统使用 |
| MP3 解码 | **libmpg123** | 成熟稳定，体积小 |
| Opus 解码 | **libopus** | 官方参考实现 |
| 重采样 | **libsamplerate** | 高质量重采样 |

### 3.3 系统接口

| 功能 | API | 说明 |
|------|------|------|
| 线程 | **pthread** | POSIX 标准，所有 Linux 支持 |
| 定时器 | **timer_create** | POSIX 标准 |
| 互斥锁 | **pthread_mutex** | POSIX 标准 |
| 条件变量 | **pthread_cond** | POSIX 标准 |
| 套接字 | **socket()** | POSIX 标准 |

---

## 四、架构设计

```
厂商应用
    │
    │  libai_sdk_linux.a / libai_sdk_linux.so
    │
┌───┴─────────────────────────────────────┐
│           AI SDK 核心层               │
│  - AsrIntelligentDialogue             │
│  - TtsPlayer                           │
│  - AIAssistantManager                  │
└───┬──────────┬──────────┬──────────────┘
    │          │          │
┌───┴───┐┌───┴───┐┌───┴───┐
│IHttp   ││IWebSocket││IAudio   │  抽象接口
│Client  ││Client   ││Output  │
└───┬───┘└───┬───┘└───┬───┘
    │          │          │
┌───┴───┐┌───┴───┐┌───┴───┐
│libcurl ││libws    ││ALSA    │  默认实现
└───────┘└───────┘└───────┘
```

---

## 五、抽象接口设计

### 5.1 HTTP Client 接口

```cpp
// include/ai_sdk/platform/http_client.h

#pragma once
#include <string>
#include <functional>
#include <memory>

namespace ai_sdk {

/**
 * @brief HTTP 客户端抽象接口
 * 
 * 平台实现此接口以支持 HTTP 通信
 */
class IHttpClient {
public:
    virtual ~IHttpClient() = default;
    
    /**
     * @brief GET 请求
     * 
     * @param url 请求 URL
     * @param headers 请求头
     * @param response 响应内容
     * @return int HTTP 状态码，-1 表示失败
     */
    virtual int get(const std::string& url, 
                    const std::map<std::string, std::string>& headers,
                    std::string& response) = 0;
    
    /**
     * @brief POST 请求
     * 
     * @param url 请求 URL
     * @param headers 请求头
     * @param body 请求体
     * @param response 响应内容
     * @return int HTTP 状态码，-1 表示失败
     */
    virtual int post(const std::string& url,
                     const std::map<std::string, std::string>& headers,
                     const std::string& body,
                     std::string& response) = 0;
    
    /**
     * @brief 流式下载
     * 
     * @param url 下载 URL
     * @param callback 数据回调
     * @return int HTTP 状态码，-1 表示失败
     */
    virtual int streamDownload(const std::string& url,
                               std::function<void(const uint8_t*, size_t)> callback) = 0;
};

/**
 * @brief 创建默认 HTTP 客户端 (libcurl 实现)
 */
std::unique_ptr<IHttpClient> createDefaultHttpClient();

} // namespace ai_sdk
```

### 5.2 WebSocket Client 接口

```cpp
// include/ai_sdk/platform/websocket_client.h

#pragma once
#include <string>
#include <functional>
#include <memory>

namespace ai_sdk {

/**
 * @brief WebSocket 客户端抽象接口
 */
class IWebSocketClient {
public:
    virtual ~IWebSocketClient() = default;
    
    /**
     * @brief 连接 WebSocket 服务器
     * 
     * @param url WebSocket URL (ws:// 或 wss://)
     * @return true 连接成功
     * @return false 连接失败
     */
    virtual bool connect(const std::string& url) = 0;
    
    /**
     * @brief 断开连接
     */
    virtual void disconnect() = 0;
    
    /**
     * @brief 发送二进制数据
     * 
     * @param data 数据指针
     * @param len 数据长度
     * @return true 发送成功
     * @return false 发送失败
     */
    virtual bool sendBinary(const uint8_t* data, size_t len) = 0;
    
    /**
     * @brief 发送文本数据
     * 
     * @param text 文本内容
     * @return true 发送成功
     * @return false 发送失败
     */
    virtual bool sendText(const std::string& text) = 0;
    
    /**
     * @brief 设置回调函数
     * 
     * @param on_connected 连接成功回调
     * @param on_message 消息回调
     * @param on_disconnected 断开连接回调
     * @param on_error 错误回调
     */
    virtual void setCallbacks(
        std::function<void()> on_connected,
        std::function<void(const uint8_t*, size_t, bool is_binary)> on_message,
        std::function<void()> on_disconnected,
        std::function<void(int code, const std::string& reason)> on_error) = 0;
    
    /**
     * @brief 检查是否已连接
     */
    virtual bool isConnected() const = 0;
};

/**
 * @brief 创建默认 WebSocket 客户端 (libwebsockets 实现)
 */
std::unique_ptr<IWebSocketClient> createDefaultWebSocketClient();

} // namespace ai_sdk
```

### 5.3 Audio Output 接口

```cpp
// include/ai_sdk/platform/audio_output.h

#pragma once
#include <cstdint>
#include <memory>
#include <string>

namespace ai_sdk {

/**
 * @brief 音频输出抽象接口
 */
class IAudioOutput {
public:
    virtual ~IAudioOutput() = default;
    
    /**
     * @brief 初始化音频输出
     * 
     * @param sample_rate 采样率 (如 16000, 44100, 48000)
     * @param channels 通道数 (1=单声道, 2=立体声)
     * @param bits_per_sample 位深 (如 16)
     * @return true 初始化成功
     * @return false 初始化失败
     */
    virtual bool init(int sample_rate, int channels, int bits_per_sample = 16) = 0;
    
    /**
     * @brief 写入 PCM 数据
     * 
     * @param data PCM 数据
     * @param samples 采样点数
     * @return true 写入成功
     * @return false 写入失败
     */
    virtual bool write(const int16_t* data, size_t samples) = 0;
    
    /**
     * @brief 设置音量
     * 
     * @param volume 音量 (0-100)
     */
    virtual void setVolume(int volume) = 0;
    
    /**
     * @brief 获取当前音量
     * 
     * @return int 当前音量 (0-100)
     */
    virtual int getVolume() const = 0;
    
    /**
     * @brief 暂停播放
     */
    virtual void pause() = 0;
    
    /**
     * @brief 恢复播放
     */
    virtual void resume() = 0;
    
    /**
     * @brief 关闭音频输出
     */
    virtual void close() = 0;
    
    /**
     * @brief 获取设备名称 (用于调试)
     */
    virtual std::string getDeviceName() const = 0;
};

/**
 * @brief 音频后端类型
 */
enum class AudioBackend {
    ALSA,           // Linux ALSA
    PULSEAUDIO,     // PulseAudio
    PIPEWIRE,       // PipeWire
    AUTO,           // 自动选择
};

/**
 * @brief 创建音频输出实例
 * 
 * @param backend 音频后端类型
 * @param device 设备名称 (如 "default", "hw:0,0")，空字符串使用默认设备
 * @return std::unique_ptr<IAudioOutput> 音频输出实例
 */
std::unique_ptr<IAudioOutput> createAudioOutput(
    AudioBackend backend = AudioBackend::AUTO,
    const std::string& device = "");

} // namespace ai_sdk
```

---

## 六、依赖库版本

| 库 | 最低版本 | 说明 |
|-----|----------|------|
| libcurl | 7.50+ | 所有主流发行版 |
| libwebsockets | 4.0+ | 支持 mbedTLS |
| libasound (ALSA) | 1.0+ | Linux 内核标配 |
| libmpg123 | 1.25+ | MP3 解码 |
| libopus | 1.3+ | Opus 解码 |
| cJSON | 1.7+ | JSON 解析 |
| mbedTLS | 2.16+ | TLS 加密 |

---

## 七、项目目录结构

```
ai_sdk_linux/
├── CMakeLists.txt                  # 主 CMake 配置
├── cmake/
│   ├── toolchains/
│   │   ├── arm-linux-gnueabihf.cmake
│   │   ├── aarch64-linux-gnu.cmake
│   │   ├── mipsel-linux-gnu.cmake
│   │   └── riscv64-linux-gnu.cmake
│   └── FindLibWebSockets.cmake
├── include/
│   └── ai_sdk/
│       ├── ai_assistant_manager.h
│       ├── asr_intelligent_dialogue.h
│       ├── tts_player.h
│       ├── audio_config.h
│       ├── types/
│       │   ├── voice_assistant.h
│       │   └── common.h
│       └── platform/
│           ├── http_client.h
│           ├── websocket_client.h
│           └── audio_output.h
├── src/
│   ├── core/
│   │   ├── ai_assistant_manager.cc
│   │   ├── asr_intelligent_dialogue.cc
│   │   └── tts_player.cc
│   └── platform/
│       ├── http_client_curl.cc
│       ├── websocket_client_lws.cc
│       ├── audio_output_alsa.cc
│       └── audio_output_pulse.cc  # 可选
├── third_party/
│   └── cJSON/
│       ├── cJSON.c
│       └── cJSON.h
├── examples/
│   ├── simple_asr/
│   │   └── main.cc
│   └── tts_playback/
│       └── main.cc
└── README.md
```

---

## 八、CMake 配置

```cmake
# CMakeLists.txt

cmake_minimum_required(VERSION 3.10)
project(ai_sdk_linux VERSION 1.0.0 LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 14)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 选项
option(BUILD_SHARED_LIBS "Build shared library" OFF)
option(ENABLE_PULSEAUDIO "Enable PulseAudio support" OFF)
option(USE_MBEDTLS "Use mbedTLS instead of OpenSSL" ON)

# 查找依赖
find_package(CURL REQUIRED)
find_package(PkgConfig REQUIRED)
pkg_check_modules(ALSA REQUIRED alsa)

# libwebsockets
pkg_check_modules(LIBWEBSOCKETS libwebsockets)
if(NOT LIBWEBSOCKETS_FOUND)
    # 尝试 find_package
    find_package(LibWebSockets REQUIRED)
endif()

# 可选 PulseAudio
if(ENABLE_PULSEAUDIO)
    pkg_check_modules(PULSEAUDIO REQUIRED libpulse-simple)
endif()

# 音频解码库
pkg_check_modules(MPG123 libmpg123)
pkg_check_modules(OPUS opus)
pkg_check_modules(SAMPLERATE samplerate)

# 源文件
set(SOURCES
    src/core/ai_assistant_manager.cc
    src/core/asr_intelligent_dialogue.cc
    src/core/tts_player.cc
    src/platform/http_client_curl.cc
    src/platform/websocket_client_lws.cc
    src/platform/audio_output_alsa.cc
    third_party/cJSON/cJSON.c
)

if(ENABLE_PULSEAUDIO)
    list(APPEND SOURCES src/platform/audio_output_pulse.cc)
endif()

# 构建库
if(BUILD_SHARED_LIBS)
    add_library(ai_sdk_linux SHARED ${SOURCES})
else()
    add_library(ai_sdk_linux STATIC ${SOURCES})
endif()

# 包含目录
target_include_directories(ai_sdk_linux PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_CURRENT_SOURCE_DIR}/third_party
)

target_include_directories(ai_sdk_linux PRIVATE
    ${CURL_INCLUDE_DIRS}
    ${ALSA_INCLUDE_DIRS}
    ${LIBWEBSOCKETS_INCLUDE_DIRS}
)

# 链接库
target_link_libraries(ai_sdk_linux
    ${CURL_LIBRARIES}
    ${ALSA_LIBRARIES}
    ${LIBWEBSOCKETS_LIBRARIES}
    pthread
)

if(MPG123_FOUND)
    target_link_libraries(ai_sdk_linux ${MPG123_LIBRARIES})
    target_compile_definitions(ai_sdk_linux PRIVATE HAVE_MPG123)
endif()

if(OPUS_FOUND)
    target_link_libraries(ai_sdk_linux ${OPUS_LIBRARIES})
    target_compile_definitions(ai_sdk_linux PRIVATE HAVE_OPUS)
endif()

if(SAMPLERATE_FOUND)
    target_link_libraries(ai_sdk_linux ${SAMPLERATE_LIBRARIES})
    target_compile_definitions(ai_sdk_linux PRIVATE HAVE_SAMPLERATE)
endif()

if(ENABLE_PULSEAUDIO)
    target_link_libraries(ai_sdk_linux ${PULSEAUDIO_LIBRARIES})
    target_compile_definitions(ai_sdk_linux PRIVATE HAVE_PULSEAUDIO)
endif()

# 安装
install(TARGETS ai_sdk_linux
    LIBRARY DESTINATION lib
    ARCHIVE DESTINATION lib
)

install(DIRECTORY include/ai_sdk
    DESTINATION include
)
```

---

## 九、交叉编译工具链示例

### 9.1 ARM 64-bit (aarch64)

```cmake
# cmake/toolchains/aarch64-linux-gnu.cmake

set(CMAKE_SYSTEM_NAME Linux)
set(CMAKE_SYSTEM_PROCESSOR aarch64)

set(CMAKE_C_COMPILER aarch64-linux-gnu-gcc)
set(CMAKE_CXX_COMPILER aarch64-linux-gnu-g++)

set(CMAKE_FIND_ROOT_PATH /usr/aarch64-linux-gnu)

set(CMAKE_FIND_ROOT_PATH_MODE_PROGRAM NEVER)
set(CMAKE_FIND_ROOT_PATH_MODE_LIBRARY ONLY)
set(CMAKE_FIND_ROOT_PATH_MODE_INCLUDE ONLY)
```

### 9.2 编译命令

```bash
# 本机编译
mkdir build && cd build
cmake ..
make -j$(nproc)

# 交叉编译 ARM64
mkdir build-aarch64 && cd build-aarch64
cmake -DCMAKE_TOOLCHAIN_FILE=../cmake/toolchains/aarch64-linux-gnu.cmake ..
make -j$(nproc)
```

---

## 十、支持的交叉编译工具链

| 目标架构 | 工具链示例 | 应用场景 |
|----------|--------------|----------|
| ARM 32-bit | arm-linux-gnueabihf-gcc | 树莓派 3B, i.MX6 |
| ARM 64-bit | aarch64-linux-gnu-gcc | 树莓派 4, RK3399 |
| MIPS | mipsel-linux-gnu-gcc | 路由器 |
| RISC-V | riscv64-linux-gnu-gcc | 新兴平台 |
| x86_64 | gcc | 工控机 |

---

## 十一、厂商使用方式

```cpp
// 厂商代码示例

#include <ai_sdk/ai_assistant_manager.h>
#include <ai_sdk/audio_config.h>
#include <iostream>

int main() {
    // 1. 获取 ASR 实例
    auto& asr = ai_sdk::AIAssistantManager::getInstance().asrIntelligentDialogueHelp();
    
    // 2. 初始化音频 (使用 ALSA 默认设备)
    ai_sdk::AudioConfig audio_config;
    audio_config.backend = ai_sdk::AudioBackend::ALSA;
    audio_config.device = "default";
    audio_config.sample_rate = 16000;
    
    if (!asr.initAudio(audio_config)) {
        std::cerr << "Failed to init audio" << std::endl;
        return -1;
    }
    
    // 3. 设置回调
    asr.setCallbacks(
        []() { 
            std::cout << "Connected" << std::endl; 
        },
        [](const ai_sdk::AsrResult& result) { 
            std::cout << "ASR: " << result.text << std::endl; 
        },
        [](const ai_sdk::DialogueResult& result) {
            std::cout << "Answer: " << result.assistant_answer_content << std::endl;
            // TTS 会自动播放
        },
        [](int code, const std::string& msg) { 
            std::cerr << "Error: " << code << " - " << msg << std::endl; 
        },
        []() { 
            std::cout << "Complete" << std::endl; 
        }
    );
    
    // 4. 启动
    asr.start();
    
    // 5. 发送音频 (从麦克风录制)
    // asr.sendAudio(pcm_data, len);
    
    // 保持运行
    while (true) {
        sleep(1);
    }
    
    return 0;
}
```

---

## 十二、与 ESP32 版本的对比

| 项目 | ESP32 版本 | Linux 版本 |
|------|------------|------------|
| 网络 | esp_http_client, esp_websocket_client | libcurl, libwebsockets |
| 音频 | esp_codec_dev | ALSA |
| 线程 | FreeRTOS Task | pthread |
| 定时器 | esp_timer | timer_create |
| JSON | cJSON | cJSON (相同) |
| TLS | esp_tls (mbedTLS) | mbedTLS / OpenSSL |
| 编译 | ESP-IDF CMake | 标准 CMake |
| 输出 | libai_sdk.a (多架构) | libai_sdk_linux.a / .so |

---

## 十三、覆盖 90% 的关键点总结

| 策略 | 说明 |
|------|------|
| POSIX 标准 | pthread, socket, timer - 所有 Linux 支持 |
| ALSA | Linux 内核标配，100% 覆盖 |
| libcurl | 广泛可用，支持静态链接 |
| libwebsockets | 轻量级，支持 mbedTLS，适合嵌入式 |
| CMake | 支持交叉编译，易于集成 |
| 抽象接口 | 允许用户替换平台实现 |
| 静态链接 | 减少运行时依赖 |
