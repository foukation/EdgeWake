# AI-SDK 自动管理音频迁移方案

> 创建时间: 2026-03-10
> 更新时间: 2026-03-16
> 状态: 方式 A 核心改造已完成（工作 1-6 + Board/Display/MCP 适配），工作 7（Application 改造）待实施

---

## 一、背景

### 1.1 当前状态

当前项目（xiaozhi-esp32）已集成 AI-SDK 语音助手功能，但采用的是 **"手动管理音频"模式（方式 B）**：

- SDK 仅作为 WebSocket 协议层替换
- 主项目的 `AudioService` 完全控制麦克风和扬声器
- 音频数据流：麦克风 → AudioService → Opus 编码 → AiSdkProtocol → Opus 解码 → PCM → `asr_.sendAudio()`
- TTS 播放：`Speak` 指令未实现（`ESP_LOGW("not implemented")`）

### 1.2 目标

迁移到 **"SDK 自动管理音频"模式（方式 A）**：

- SDK 内部管理麦克风录音（`AudioInput`）和 TTS 播放（`TtsPlayer`）
- 使用 `initAudio()` / `setRecording()` / `setAutoPlayTts()` API
- TTS 自动播放：SDK 收到 `Speak` 指令后自动下载 MP3 并播放
- 简化 `AiSdkProtocol`，移除 Opus 编解码和 PCM 缓冲逻辑

### 1.3 关键差异对比

| 特性 | 当前（手动模式） | 目标（自动模式） |
|------|---|---|
| 麦克风录音 | AudioService 管理 | SDK AudioInput 管理 |
| 音频编码 | Opus 编码后再解码回 PCM | 无编码，SDK 直接采集 PCM |
| 数据发送 | `AiSdkProtocol::SendAudio()` | SDK 内部自动发送 |
| TTS 播放 | 未实现 | SDK TtsPlayer 自动播放 |
| AFE 前端处理 | AudioService 提供 | 无（SDK 不含 AFE） |
| 唤醒词检测 | AudioService 提供 | 无（需另行处理） |

---

## 二、方案选择

### 方案 A：完全替换（推荐先做验证）

SDK 的 `AudioInput` 完全替代 `AudioService` 的麦克风录音功能。

```
优点：
- 实现简单，改动集中在 AiSdkProtocol
- TTS 播放自动工作
- 无冗余 Opus 编解码开销

缺点：
- 失去 AFE（回声消除/噪声抑制）— SDK 的 AudioInput 不含 AFE
- 失去唤醒词检测 — 需要使用按键或其他方式触发
- 需要独立创建 AudioCodec 实例，与主项目的 Board 音频配置解耦
```

### 方案 B：混合模式（保留 AFE/唤醒词）

保留 `AudioService` 用于 AFE 和唤醒词检测，但录音数据通过 SDK 发送，TTS 由 SDK 播放。

```
优点：
- 保留 AFE 前端处理（录音质量更好）
- 保留唤醒词检测
- TTS 使用 SDK 的 TtsPlayer

缺点：
- 需要协调两套音频管道
- 需要修改 AudioService 添加 "PCM 直出" 模式
- 复杂度更高
```

### 当前选择

**已确认：方案 A（完全替换）+ 改造现有 AiSdkProtocol**

实施方式：不新建 Protocol 类，直接改造现有 `AiSdkProtocol`，同时修改 `Application` 在 AI SDK 模式下跳过 `AudioService` 初始化。

选择理由：
1. **消除冗余编解码** — 当前 Opus 编码→解码→PCM 的流程纯属浪费 CPU 和 ~30-50KB RAM
2. **架构更简洁** — SDK 内部 2 个 FreeRTOS 任务（AudioInput + TtsPlayer）替代主项目 3 个任务
3. **TTS 一体化** — SDK 自动处理 Speak 指令的下载、解码、播放
4. **内存更优** — 方案 A 总计 ~63-80KB vs 当前 ~70-110KB
5. **已验证可行** — Phase 1 的 8 个阶段全部编译验证通过
6. **改动量最小** — 约 50-80 行修改/新增，20-30 行删除

不新建 Protocol 类的理由：
- `Protocol` 接口中 `SendAudio(AudioStreamPacket)` 接收 Opus 格式，方案 A 不需要此方法
- 新旧两个类的回调逻辑（`SetupAsrCallbacks`）完全相同，造成代码重复
- `Application` 无论如何都需要改造（跳过 AudioService），不如集中改动

---

## 三、工作清单

### 3.1 预编译库更新（工作 1-5）

#### 工作 1：更新预编译版头文件 — `asr_intelligent_dialogue.h`

- **源文件**: `ai_sdk_builder/components/ai_sdk/include/ai_sdk/asr_intelligent_dialogue.h`
- **目标文件**: `components/ai_sdk/include/ai_sdk/asr_intelligent_dialogue.h`
- **操作**: 用源码版替换预编译版，暴露音频相关 API
- **新增 API**:
  - `bool initAudio(AudioCodec* codec)` — 初始化内置音频模块
  - `void setRecording(bool enable)` — 控制麦克风录音开关
  - `void setAutoPlayTts(bool enable)` — 设置 TTS 自动播放开关
- **新增依赖**: `#include "ai_sdk/audio/audio_codec.h"`

#### 工作 2：复制音频模块公开头文件

创建 `components/ai_sdk/include/ai_sdk/audio/` 目录，复制以下文件：

| 源文件 | 目标文件 |
|--------|----------|
| `ai_sdk_builder/.../include/ai_sdk/audio/audio_codec.h` | `components/ai_sdk/include/ai_sdk/audio/audio_codec.h` |
| `ai_sdk_builder/.../include/ai_sdk/audio/audio_config.h` | `components/ai_sdk/include/ai_sdk/audio/audio_config.h` |

#### 工作 3：更新预编译版 CMakeLists.txt

**文件**: `components/ai_sdk/CMakeLists.txt`

REQUIRES 列表添加音频依赖：

```cmake
# 当前：
REQUIRES
    esp_http_client esp_timer json esp_websocket_client
    tcp_transport http_parser esp-tls mbedtls

# 改为：
REQUIRES
    esp_http_client esp_timer json esp_websocket_client
    tcp_transport http_parser esp-tls mbedtls
    esp_codec_dev driver esp_audio_effects esp_audio_codec
```

#### 工作 4：更新 main/idf_component.yml

添加音频相关的管理组件依赖：

```yaml
dependencies:
  # 现有依赖...
  # 新增音频依赖：
  espressif/esp_codec_dev: '~1.5.4'
  espressif/esp_audio_effects: '~1.2.0'
  espressif/esp_audio_codec: '~2.4.0'
```

> 注意：检查主项目的 `idf_component.yml` 是否已有这些依赖（可能通过其他途径已引入）。

#### 工作 5：重新编译预编译库

```powershell
cd ai_sdk_builder
./build_all.ps1
```

编译完成后，将 `ai_sdk_builder/components/ai_sdk/lib/` 下所有 `.a` 文件复制到 `components/ai_sdk/lib/`。

支持的目标芯片：
- Xtensa: esp32, esp32s2, esp32s3
- RISC-V: esp32c2, esp32c3, esp32c5, esp32c6, esp32p4

---

### 3.2 代码改造（工作 6-7）

#### 工作 6：改造 AiSdkProtocol

**文件**: `main/protocols/ai_sdk_protocol.h` 和 `main/protocols/ai_sdk_protocol.cc`

##### 6.1 改造原则

在现有 `AiSdkProtocol` 类上进行改造，不新建类。改动集中在4处：
1. 构造函数 — 用 AudioCodec 替换 OpusDecoder
2. `OpenAudioChannel()` — 添加 `setRecording(true)`
3. `CloseAudioChannel()` — 添加 `setRecording(false)`
4. `SendAudio()` — 清空为空操作

##### 6.2 移除的内容

| 移除项 | 原因 |
|--------|------|
| `#include <opus_decoder.h>` | 不再需要 Opus 解码 |
| `#include "audio_service.h"` | 不再依赖 AudioService（OPUS_FRAME_DURATION_MS 来自此头文件） |
| `std::unique_ptr<OpusDecoderWrapper> opus_decoder_` | 不再需要 |
| `std::vector<uint8_t> audio_buffer_` | SDK 内部处理缓冲 |
| `static constexpr size_t SEND_THRESHOLD = 5120` | SDK 内部处理 |
| `SendAudio()` 中的 Opus→PCM 解码逻辑 | SDK 自动录音，不再需要主项目发送音频 |

##### 6.3 新增的内容

| 新增项 | 用途 |
|--------|------|
| `#include "ai_sdk/audio/audio_config.h"` | AudioConfig + CreateAudioCodec |
| `ai_sdk::AudioCodec* codec_` | 持有 SDK 音频驱动实例（构造函数中创建，析构函数中释放） |
| 构造函数中创建 AudioCodec 并调用 `initAudio()` | SDK 内部管理录音和 TTS |
| `OpenAudioChannel()` 中 `asr_.setRecording(true)` | 开始录音 |
| `CloseAudioChannel()` 中 `asr_.setRecording(false)` | 停止录音 |

##### 6.4 方法改造对照表

| 方法 | 旧实现 | 新实现 |
|------|--------|--------|
| 构造函数 | 创建 OpusDecoder + SetupCallbacks | 创建 AudioConfig → CreateAudioCodec → `codec->Start()` → `initAudio(codec)` → `setAutoPlayTts(true)` → SetupCallbacks |
| 析构函数 | 停止 asr | 停止 asr + `delete codec_` |
| `Start()` | 空操作 | 空操作（不变） |
| `OpenAudioChannel()` | `asr_.start()` | `asr_.start()` + `asr_.setRecording(true)` |
| `CloseAudioChannel()` | 刷 PCM 缓冲 + `asr_.stop()` | `asr_.setRecording(false)` + `asr_.stop()` |
| `SendAudio()` | Opus→PCM→缓冲→sendAudio | **空操作**（返回 true，SDK 内部 AudioInput 已在自动录音并发送） |
| `IsAudioChannelOpened()` | 检查连接+错误+超时 | 不变 |
| Speak 回调 | `ESP_LOGW("not implemented")` | 通知 Application TTS 状态（SDK 自动播放） |
| `SendStartListening()` | 空操作 | 空操作（不变） |
| `SendStopListening()` | `CloseAudioChannel()` | `CloseAudioChannel()`（不变） |

##### 6.5 改造后的 ai_sdk_protocol.h

```cpp
#ifndef AI_SDK_PROTOCOL_H
#define AI_SDK_PROTOCOL_H

#include "protocol.h"
#include "ai_sdk/ai_assistant_manager.h"
#include "ai_sdk/audio/audio_config.h"

#include <vector>
#include <cstdint>
#include <memory>
#include <chrono>

class AiSdkProtocol : public Protocol {
public:
    AiSdkProtocol();
    ~AiSdkProtocol() override;

    bool Start() override;
    bool OpenAudioChannel() override;
    void CloseAudioChannel() override;
    bool IsAudioChannelOpened() const override;
    bool SendAudio(std::unique_ptr<AudioStreamPacket> packet) override;
    void SendStartListening(ListeningMode mode) override;
    void SendStopListening() override;

protected:
    bool SendText(const std::string& text) override;

private:
    ai_sdk::AsrIntelligentDialogue& asr_;

    // SDK 管理的音频驱动实例（由 CreateAudioCodec 创建）
    ai_sdk::AudioCodec* codec_ = nullptr;

    // 超时管理
    mutable std::chrono::steady_clock::time_point last_incoming_time_;
    bool error_occurred_ = false;
    static constexpr int TIMEOUT_MS = 15000;

    bool IsTimeout() const;
    void SetupAsrCallbacks();

    // 创建音频硬件配置（从 Board 的 config.h 宏定义转换）
    ai_sdk::AudioConfig CreateAudioConfig();
};

#endif // AI_SDK_PROTOCOL_H
```

##### 6.6 改造后的 ai_sdk_protocol.cc（核心改动部分）

```cpp
#include "ai_sdk_protocol.h"
#include "board.h"  // 获取 I2C handle 和板级配置

#include <esp_log.h>
#include <cJSON.h>
#include <cstring>

#define TAG "AiSdkProtocol"

// ============================================================================
// 构造函数 — 创建 AudioCodec 并初始化 SDK 音频模块
// ============================================================================

AiSdkProtocol::AiSdkProtocol()
    : asr_(ai_sdk::AIAssistantManager::getInstance().asrIntelligentDialogueHelp()) {
    ESP_LOGI(TAG, "Initializing AI SDK Protocol (auto-audio mode)");

    // 1. 创建音频硬件配置
    ai_sdk::AudioConfig audio_cfg = CreateAudioConfig();

    // 2. 通过工厂函数创建 AudioCodec
    codec_ = ai_sdk::CreateAudioCodec(audio_cfg);
    if (codec_) {
        codec_->Start();
        ESP_LOGI(TAG, "AudioCodec created and started");

        // 3. 初始化 SDK 内置音频模块（内部创建 AudioInput + TtsPlayer）
        asr_.initAudio(codec_);

        // 4. 启用 TTS 自动播放
        asr_.setAutoPlayTts(true);
        ESP_LOGI(TAG, "SDK audio initialized, auto-play TTS enabled");
    } else {
        ESP_LOGE(TAG, "Failed to create AudioCodec!");
    }

    SetupAsrCallbacks();
}

AiSdkProtocol::~AiSdkProtocol() {
    ESP_LOGI(TAG, "Destroying AI SDK Protocol");
    if (asr_.isConnected()) {
        asr_.stop();
    }
    // 释放 SDK 创建的 AudioCodec
    delete codec_;
    codec_ = nullptr;
}

// ============================================================================
// OpenAudioChannel — 启动连接并开始录音
// ============================================================================

bool AiSdkProtocol::OpenAudioChannel() {
    ESP_LOGI(TAG, "Opening audio channel...");
    error_occurred_ = false;

    if (!asr_.start()) {
        ESP_LOGE(TAG, "Failed to start ASR connection");
        return false;
    }

    // SDK 自动录音：开始采集麦克风数据并发送到云端
    asr_.setRecording(true);
    ESP_LOGI(TAG, "Audio channel opened, recording started");
    return true;
}

// ============================================================================
// CloseAudioChannel — 停止录音并关闭连接
// ============================================================================

void AiSdkProtocol::CloseAudioChannel() {
    ESP_LOGI(TAG, "Closing audio channel...");

    // 停止录音
    asr_.setRecording(false);

    // 关闭 WebSocket 连接
    asr_.stop();
    ESP_LOGI(TAG, "Audio channel closed, recording stopped");
}

// ============================================================================
// SendAudio — 空操作（SDK 内部 AudioInput 自动录音并发送）
// ============================================================================

bool AiSdkProtocol::SendAudio(std::unique_ptr<AudioStreamPacket> packet) {
    // SDK 的 AudioInput 已经在录音任务中自动采集和发送音频
    // 主项目的 AudioService 不再参与音频数据流
    return true;
}

// ============================================================================
// Speak 回调改造
// ============================================================================

// 在 SetupAsrCallbacks() 的 onDialogueResult 回调中：
// Speak 分支改为：
//
// } else if (result.directive == "Speak") {
//     ESP_LOGI(TAG, "Speak directive, SDK auto-playing TTS");
//     // SDK 自动播放，通知 Application 进入 TTS 状态
//     cJSON* root = cJSON_CreateObject();
//     if (root) {
//         cJSON_AddStringToObject(root, "type", "tts");
//         cJSON_AddStringToObject(root, "state", "start");
//         if (on_incoming_json_) {
//             on_incoming_json_(root);
//         }
//         cJSON_Delete(root);
//     }
// }
```

##### 6.7 硬件配置 — CreateAudioConfig() 实现

从 Board 的 `config.h` 宏定义转换为 SDK 的 `AudioConfig`。需要在 `AiSdkProtocol` 中实现转换函数：

```cpp
ai_sdk::AudioConfig AiSdkProtocol::CreateAudioConfig() {
    ai_sdk::AudioConfig cfg;
    auto& board = Board::GetInstance();

    // 通过 Board 提供的 I2C 总线句柄
    cfg.i2c_master_handle = board.GetI2cMasterHandle();
    cfg.i2c_port = AUDIO_I2C_PORT;  // 来自 config.h

    // 从 config.h 宏定义获取引脚配置
    cfg.input_sample_rate = AUDIO_INPUT_SAMPLE_RATE;
    cfg.output_sample_rate = AUDIO_OUTPUT_SAMPLE_RATE;
    cfg.mclk = AUDIO_I2S_GPIO_MCLK;
    cfg.bclk = AUDIO_I2S_GPIO_BCLK;
    cfg.ws = AUDIO_I2S_GPIO_WS;
    cfg.dout = AUDIO_I2S_GPIO_DOUT;
    cfg.din = AUDIO_I2S_GPIO_DIN;
    cfg.pa_pin = AUDIO_CODEC_PA_PIN;

    // 硬件类型 — 需要根据 Board 类型确定
    // 方式1：Board 类新增 GetAudioHardwareType() 方法（推荐）
    // 方式2：通过 config.h 宏定义（需要新增 AUDIO_HARDWARE_TYPE 宏）
    // 方式3：Kconfig 配置项
    cfg.hardware_type = board.GetAudioHardwareType();

    return cfg;
}
```

> **注意**: `Board::GetAudioHardwareType()` 是需要新增的方法，返回 `ai_sdk::AudioHardwareType` 枚举值。
> 每个 Board 子类（如 `EspBox3Board`）需要实现此方法，返回对应的硬件类型。
> 或者也可以通过在 `config.h` 中新增 `#define AUDIO_HARDWARE_TYPE kEs8311` 宏来实现。

---

#### 工作 7：改造 Application（跳过 AudioService）

**文件**: `main/application.cc`

##### 7.1 改造原因

当 `CONFIG_USE_AI_SDK_PROTOCOL` 启用时，SDK 内部通过 `AudioInput` 和 `TtsPlayer` 管理音频。
如果主项目的 `AudioService` 同时启动，会与 SDK 争抢 I2S 总线，导致冲突。

因此需要在 `Application::Initialize()` 中条件跳过 `AudioService` 的初始化。

##### 7.2 改造位置

`Application::Initialize()` 中的音频初始化部分（约第 72-87 行）：

```cpp
// 当前代码（第 72-87 行）：
auto codec = board.GetAudioCodec();
audio_service_.Initialize(codec);
audio_service_.Start();

AudioServiceCallbacks callbacks;
callbacks.on_send_queue_available = [this]() { ... };
callbacks.on_wake_word_detected = [this]() { ... };
callbacks.on_vad_change = [this]() { ... };
audio_service_.SetCallbacks(callbacks);
```

##### 7.3 改造后代码

```cpp
// 改造后：
#ifndef CONFIG_USE_AI_SDK_PROTOCOL
    // 非 AI SDK 模式：使用主项目的 AudioService 管理音频
    auto codec = board.GetAudioCodec();
    audio_service_.Initialize(codec);
    audio_service_.Start();

    AudioServiceCallbacks callbacks;
    callbacks.on_send_queue_available = [this]() {
        xEventGroupSetBits(event_group_, MAIN_EVENT_SEND_AUDIO);
    };
    callbacks.on_wake_word_detected = [this](const std::string& wake_word) {
        xEventGroupSetBits(event_group_, MAIN_EVENT_WAKE_WORD_DETECTED);
    };
    callbacks.on_vad_change = [this](bool speaking) {
        xEventGroupSetBits(event_group_, MAIN_EVENT_VAD_CHANGE);
    };
    audio_service_.SetCallbacks(callbacks);
#else
    // AI SDK 模式：音频由 SDK 内部管理（AudioInput + TtsPlayer）
    // 不初始化 AudioService，避免 I2S 总线冲突
    ESP_LOGI(TAG, "AI SDK Protocol mode: AudioService skipped, SDK manages audio");
#endif
```

##### 7.4 关联影响

`Application` 中其他使用 `audio_service_` 的地方也需要用 `#ifdef` 保护：

| 位置 | 代码 | 处理方式 |
|------|------|----------|
| Run() 循环中的 SEND_AUDIO 事件 | `audio_service_.PopPacketFromSendQueue()` | `#ifndef CONFIG_USE_AI_SDK_PROTOCOL` 保护 |
| HandleToggleChatEvent() | `audio_service_.EnableVoiceProcessing()` 等 | `#ifndef` 保护，AI SDK 模式下不需要 |
| HandleWakeWordDetectedEvent() | `audio_service_.EncodeWakeWord()` 等 | `#ifndef` 保护，AI SDK 模式下无唤醒词 |
| OnStateChanged() | `audio_service_.PlaySound()`, `audio_service_.Stop()` | 需要判断 — PlaySound 可能需要保留 |
| ResetProtocol() | `audio_service_.Stop()` / `Start()` | `#ifndef` 保护 |

> **注意**: `audio_service_.PlaySound()` 比较特殊 — 提示音播放（如连接成功音效）是否也由 SDK 管理需要进一步确认。
> 暂时方案：AI SDK 模式下不播放提示音，或使用 SDK 的 TtsPlayer 播放本地音效文件。

---

### 3.3 回调改造细节（工作 6 续）

当 `setAutoPlayTts(true)` 后，SDK 内部自动处理 `Speak` 指令：

```
收到 Speak 指令（DialogueResult.directive == "Speak"）
    → SDK 内部 parseMessage() 解析 payload.url
    → SDK TtsPlayer 自动下载 MP3
    → MP3 解码 + 重采样（MP3采样率 → 硬件输出采样率）
    → AudioCodec::OutputData() 输出到扬声器
    → TTS 播放完成
```

`onDialogueResult` 回调中的 `Speak` 分支改造：

```cpp
// 旧代码（仅打印警告）：
} else if (result.directive == "Speak") {
    ESP_LOGW(TAG, "Speak directive received, TTS URL playback not implemented");
}

// 新代码（SDK 自动播放 + 通知 Application）：
} else if (result.directive == "Speak") {
    ESP_LOGI(TAG, "Speak directive, SDK auto-playing TTS");
    // SDK 内部自动播放 TTS，此处通知 Application 更新 UI 状态
    cJSON* root = cJSON_CreateObject();
    if (root) {
        cJSON_AddStringToObject(root, "type", "tts");
        cJSON_AddStringToObject(root, "state", "start");
        if (on_incoming_json_) {
            on_incoming_json_(root);
        }
        cJSON_Delete(root);
    }
}
```

---

### 3.4 文件改动总结（原计划）

| 文件 | 操作 | 改动量 |
|------|------|--------|
| `main/protocols/ai_sdk_protocol.h` | 修改 | 移除 Opus 相关成员，新增 `codec_` 和 `CreateAudioConfig()` |
| `main/protocols/ai_sdk_protocol.cc` | 修改 | 4处核心改动（构造/Open/Close/SendAudio）+ Speak 回调 |
| `main/application.cc` | 修改 | `Initialize()` 中 `#ifdef` 条件跳过 AudioService |
| `main/application.cc` | 修改 | 其他 `audio_service_` 调用处添加 `#ifdef` 保护 |

**预计总改动量**: 约 80-100 行修改/新增，40-50 行删除

---

## 四、实施记录（2026-03-16）

> ✅ 工作 6（AiSdkProtocol 改造）+ 相关适配工作已完成
> ⏳ 工作 7（Application 改造）待实施

### 4.1 提交记录

| # | Commit | 时间 | 描述 | 涉及文件 |
|---|--------|------|------|----------|
| 1 | `7a3665e` | 11:29 | A/B 模式 Kconfig + 条件编译框架 | `Kconfig.projbuild`, `ai_sdk_protocol.h/cc`, `sdkconfig.defaults` |
| 2 | `54e7f61` | 12:13 | CreateAudioConfig() 实际实现（Simplex/Duplex） | `ai_sdk_protocol.cc` |
| 3 | `16fa1a3` | 12:26 | sdkconfig 启用 `CONFIG_AI_SDK_AUTO_AUDIO=y` | `sdkconfig` |
| 4 | `b0ee336` | 14:29 | CMake 板级目录 include + 条件头文件引用 | `CMakeLists.txt`, `ai_sdk_protocol.cc` |
| 5 | `b35e244` | 17:40 | Board 层 nullptr 返回 + 音量回调空指针检查 | `compact_wifi_board.cc`, `mcp_server.cc` |
| 6 | `d1aef3b` | 18:15 | Display 层 UpdateStatusBar nullptr 保护 | `lvgl_display.cc` |

**实际改动量**: 9 个文件，193 行增加 / 78 行删除

### 4.2 工作 6 实施详情 — AiSdkProtocol 改造

#### 4.2.1 Kconfig 配置（`7a3665e`）

在 `main/Kconfig.projbuild` 中新增 `CONFIG_AI_SDK_AUTO_AUDIO` 配置项：

```
config AI_SDK_AUTO_AUDIO
    bool "AI SDK auto-manage audio (Mode A)"
    default n
    depends on USE_AI_SDK_PROTOCOL
```

通过 `sdkconfig.defaults` 和 `sdkconfig` 默认启用。

#### 4.2.2 条件编译框架（`7a3665e`）

`ai_sdk_protocol.h` 和 `ai_sdk_protocol.cc` 使用 `#ifdef CONFIG_AI_SDK_AUTO_AUDIO` 实现 A/B 模式切换：

| 代码区域 | 方式 A（`CONFIG_AI_SDK_AUTO_AUDIO` 定义） | 方式 B（未定义） |
|---------|----------------------------------------|----------------|
| 头文件包含 | `#include "config.h"` (板级宏) | `#include "audio_service.h"` (Opus) |
| 构造函数 | `CreateAudioConfig()` → `CreateAudioCodec()` → `initAudio()` | `OpusDecoderWrapper` 初始化 |
| `OpenAudioChannel()` | 跳过 `audio_buffer_.clear()` | 清空 PCM 缓冲区 |
| `CloseAudioChannel()` | 跳过剩余缓冲发送 | 发送剩余 PCM 缓冲 |
| `SendAudio()` | 返回 true（空操作） | Opus→PCM→缓冲→sendAudio |
| 成员变量 | 仅 `codec_` + `CreateAudioConfig()` | `opus_decoder_` + `audio_buffer_` + `SEND_THRESHOLD` |

#### 4.2.3 CreateAudioConfig() 实际实现（`54e7f61`）

**与原方案设计的差异**：

| 设计 | 原方案（§6.7） | 实际实现 |
|------|---------------|---------|
| 硬件类型确定 | 运行时 `board.GetAudioHardwareType()` | 编译时 `#ifdef AUDIO_I2S_METHOD_SIMPLEX` |
| 引脚配置来源 | `Board::GetI2cMasterHandle()` + 板级宏 | 纯板级 `config.h` 宏定义 |
| 支持的硬件 | 所有类型（ES8311 等） | 仅无芯片方案（Simplex + Duplex） |
| I2C 配置 | 需要 | 不需要（无芯片方案无 I2C） |

实际实现了两种 I2S 模式：

```cpp
#ifdef AUDIO_I2S_METHOD_SIMPLEX
    // Simplex：独立 I2S 端口（INMP441 + MAX98357）
    cfg.hardware_type = ai_sdk::AudioHardwareType::kNoCodecSimplex;
    cfg.spk_bclk / spk_ws / spk_dout  // 扬声器引脚
    cfg.mic_sck / mic_ws / mic_din     // 麦克风引脚
#else
    // Duplex：共享 I2S 引脚
    cfg.hardware_type = ai_sdk::AudioHardwareType::kNoCodecDuplex;
    cfg.bclk / ws / dout / din         // 共用引脚
#endif
```

#### 4.2.4 编译依赖打通（`b0ee336`）

**此改动在原方案中未预见。**

`ai_sdk_protocol.cc` 在方式 A 时需要访问板级 `config.h` 中的宏定义（`AUDIO_INPUT_SAMPLE_RATE`、GPIO 引脚等），但板级目录不在 include path 中。

解决方案：
- `main/CMakeLists.txt` 中将 `boards/${BOARD_TYPE}` 加入 `INCLUDE_DIRS`
- `ai_sdk_protocol.cc` 中条件包含 `#include "config.h"`

### 4.3 文档未预见的适配工作

以下改动在原方案（§3.2 工作 6-7）中**未被预见**，是实施过程中发现必须处理的关联影响。

#### 4.3.1 Board 层改造（`b35e244`）

**文件**: `main/boards/bread-compact-wifi/compact_wifi_board.cc`

**问题**: 方式 A 下 SDK 通过 `CreateAudioCodec()` 自行创建 I2S 通道。如果 Board 也创建 `AudioCodec`，会导致 I2S 通道冲突。

**解决方案**:

1. `GetAudioCodec()` 在 `CONFIG_AI_SDK_AUTO_AUDIO` 时返回 `nullptr`：

```cpp
virtual AudioCodec* GetAudioCodec() override {
#ifdef CONFIG_AI_SDK_AUTO_AUDIO
    return nullptr;  // SDK 自行管理 I2S
#elif defined(AUDIO_I2S_METHOD_SIMPLEX)
    static NoAudioCodecSimplex audio_codec(...);
    return &audio_codec;
#else
    static NoAudioCodecDuplex audio_codec(...);
    return &audio_codec;
#endif
}
```

2. 4 个音量控制回调添加空指针检查：

```cpp
volume_up_button_.OnClick([this]() {
    auto codec = GetAudioCodec();
    if (!codec) return;  // 方式 A：SDK 管理音频
    // ... 原有音量逻辑 ...
});
```

#### 4.3.2 MCP Server 适配（`b35e244`）

**文件**: `main/mcp_server.cc`

`SetOutputVolume` 工具回调中 `board.GetAudioCodec()` 可能返回 nullptr，添加空指针检查：

```cpp
auto codec = board.GetAudioCodec();
if (!codec) return false;  // 方式 A：音频由 SDK 管理
```

#### 4.3.3 Display 层适配（`d1aef3b`）

**文件**: `main/display/lvgl_display/lvgl_display.cc`

`UpdateStatusBar()` 中静音图标更新逻辑依赖 `codec->output_volume()`，codec 为 nullptr 时崩溃。

**解决方案**: 将静音图标更新包裹在 `if (codec)` 中：

```cpp
if (codec) {
    if (codec->output_volume() == 0 && !muted_) {
        muted_ = true;
        lv_label_set_text(mute_label_, FONT_AWESOME_VOLUME_XMARK);
    } else if (codec->output_volume() > 0 && muted_) {
        muted_ = false;
        lv_label_set_text(mute_label_, "");
    }
}
```

### 4.4 实际文件改动总结

| 文件 | Commit | 操作 | 改动说明 |
|------|--------|------|----------|
| `main/Kconfig.projbuild` | `7a3665e` | 新增 | `CONFIG_AI_SDK_AUTO_AUDIO` 配置项（17 行） |
| `main/protocols/ai_sdk_protocol.h` | `7a3665e` | 修改 | 条件编译 Opus/AudioCodec 成员 + `CreateAudioConfig()` 声明 |
| `main/protocols/ai_sdk_protocol.cc` | `7a3665e`+`54e7f61`+`b0ee336` | 修改 | A/B 条件编译 + CreateAudioConfig 实现 + 板级头文件 |
| `sdkconfig.defaults` | `7a3665e` | 新增 | `CONFIG_AI_SDK_AUTO_AUDIO=y` 默认值 |
| `sdkconfig` | `16fa1a3` | 新增 | `CONFIG_AI_SDK_AUTO_AUDIO=y` |
| `main/CMakeLists.txt` | `b0ee336` | 修改 | 板级目录加入 include path |
| `main/boards/bread-compact-wifi/compact_wifi_board.cc` | `b35e244` | 修改 | `GetAudioCodec()→nullptr` + 音量回调空指针检查 |
| `main/mcp_server.cc` | `b35e244` | 修改 | `SetOutputVolume` 空指针检查 |
| `main/display/lvgl_display/lvgl_display.cc` | `d1aef3b` | 修改 | `UpdateStatusBar()` codec nullptr 保护 |

### 4.5 待完成工作

#### 工作 7：Application 改造（未实施）

`main/application.cc` 中的 `AudioService` 初始化和调用尚未添加 `#ifdef CONFIG_AI_SDK_AUTO_AUDIO` 条件编译保护。详见 §3.2 工作 7 的原方案设计。

**当前状态**：方式 A 下 Board 的 `GetAudioCodec()` 返回 nullptr，`AudioService` 初始化时收到 nullptr 可能导致问题。需要尽快完成此工作。

#### 有芯片方案支持（未实施）

`CreateAudioConfig()` 当前仅支持无芯片方案（Simplex/Duplex）。有芯片方案（ES8311、ES8388 等）的配置仍为注释中的 TODO，需要在适配其他板子时实现。

---

## 五、编译验证

完成所有改造后，需要验证：

1. **编译通过** — `idf.py build` 无错误
2. **链接通过** — 预编译库中的音频符号正确解析
3. **录音功能** — 麦克风数据通过 SDK 自动采集并发送到云端
4. **ASR 识别** — 云端返回正确的识别结果
5. **TTS 播放** — 收到 Speak 指令后自动下载 MP3 并播放
6. **多轮对话** — `start()/stop()` 反复调用正常
7. **录音/播放互斥** — 如果实现了 ASR/TTS 状态协调（任务 #26），验证播放时自动暂停录音

---

## 六、风险和注意事项

### 6.1 AFE 缺失

SDK 的 `AudioInput` 不包含回声消除（AEC）和噪声抑制（NS）。在实际使用中：
- TTS 播放时不暂停录音会导致回声（已知推迟任务 #26）
- 嘈杂环境下识别率可能下降

### 6.2 唤醒词缺失

SDK 不提供唤醒词检测功能。迁移到方案 A 后需要使用其他方式触发录音：
- 按键触发
- 外部唤醒词模块
- 或回退到方案 B 保留 AudioService 的唤醒词功能

### 6.3 音频硬件冲突

已通过 Board 层 `GetAudioCodec()` 返回 nullptr 解决（`b35e244`）。SDK 通过 `CreateAudioCodec()` 独立创建 I2S 通道，Board 不再创建，避免冲突。

### 6.4 ABI 兼容性

重新编译的 `libai_sdk.a` 的 ESP-IDF 版本和编译器版本必须与主项目一致。

### 6.5 音量控制缺失（新发现）

方式 A 下物理音量按键直接 return（codec 为 nullptr），用户无法通过按键调节音量。后续可能需要通过 SDK API 转发音量控制。

### 6.6 静音图标不更新（新发现）

方式 A 下 `UpdateStatusBar()` 跳过静音图标更新。如果 SDK 内部静音了，UI 不会有任何反馈。后续可能需要 SDK 提供音量状态查询 API。

---

## 七、相关文档

- [ai-sdk-audio-todo.md](./ai-sdk-audio-todo.md) - 音频功能 TODO 列表
- [ai-sdk-audio-driver-migration-record.md](./ai-sdk-audio-driver-migration-record.md) - 音频驱动迁移记录
- [ai-sdk-tts-implementation-plan.md](../ai-sdk-tts-implementation-plan.md) - TTS 实现详细方案
