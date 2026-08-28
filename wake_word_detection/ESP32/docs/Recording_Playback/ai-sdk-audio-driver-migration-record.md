# ai-sdk 音频驱动移植 - 完成记录

> 执行时间: 2026-03-05
> 编译验证: 2026-03-09
> 配置层实现: 2026-03-09
> 录音模块实现: 2026-03-09
> MP3解码器实现: 2026-03-09
> TTS播放器实现: 2026-03-10
> 头文件重组: 2026-03-10
> ASR集成层实现: 2026-03-10
> 状态: ✅ 已完成驱动移植 + 配置层 + 录音模块 + MP3解码器 + TTS播放器 + 头文件重组 + ASR集成层，编译验证通过

---

## 一、已完成的文件

### 1. 基类文件

| 文件 | 位置 | 状态 |
|------|------|------|
| `audio_codec.h` | `ai_sdk_builder/components/ai_sdk/include/ai_sdk/audio/` | ✅ 已创建 |
| `audio_codec.cc` | `ai_sdk_builder/components/ai_sdk/src/audio/` | ✅ 已创建 |

**修改内容**：
- 添加 `ai_sdk` 命名空间
- 移除 `#include "board.h"`
- 移除 `#include "settings.h"` 和相关持久化代码
- 添加详细注释

### 2. 驱动文件

| 驱动 | 头文件 | 实现文件 | 状态 |
|------|--------|----------|------|
| ES8311 | `codecs/es8311_audio_codec.h` | `codecs/es8311_audio_codec.cc` | ✅ 已创建 |
| ES8388 | `codecs/es8388_audio_codec.h` | `codecs/es8388_audio_codec.cc` | ✅ 已创建 |
| ES8374 | `codecs/es8374_audio_codec.h` | `codecs/es8374_audio_codec.cc` | ✅ 已创建 |
| ES8389 | `codecs/es8389_audio_codec.h` | `codecs/es8389_audio_codec.cc` | ✅ 已创建 |
| BoxAudio | `codecs/box_audio_codec.h` | `codecs/box_audio_codec.cc` | ✅ 已创建 |
| NoCodec | `codecs/no_audio_codec.h` | `codecs/no_audio_codec.cc` | ✅ 已创建 |
| Dummy | `codecs/dummy_audio_codec.h` | `codecs/dummy_audio_codec.cc` | ✅ 已创建 |

**修改内容**：
- 添加 `ai_sdk` 命名空间
- 修改 `#include "audio_codec.h"` 为 `#include "ai_sdk/audio/audio_codec.h"`
- 修改宏名称 `AUDIO_CODEC_DMA_*` 为 `AI_SDK_AUDIO_CODEC_DMA_*`
- 添加详细注释

### 3. 构建配置文件

| 文件 | 修改内容 |
|------|----------|
| `CMakeLists.txt` | 添加所有音频源文件、添加 `src/audio/codecs` 到 `PRIV_INCLUDE_DIRS`、添加 `esp_codec_dev` 依赖 |
| `idf_component.yml`（位于 `ai_sdk_builder/main/`） | 添加 `espressif/esp_codec_dev: '~1.5.4'` 依赖 |

### 4. 配置层文件（2026-03-09 新增）

| 文件 | 位置 | 状态 |
|------|------|------|
| `audio_config.h` | `ai_sdk_builder/components/ai_sdk/include/ai_sdk/audio/` | ✅ 已创建 |
| `audio_config.cc` | `ai_sdk_builder/components/ai_sdk/src/audio/` | ✅ 已创建 |

**内容**：
- `AudioHardwareType` 枚举：定义 9 种硬件类型（ES8311/ES8388/ES8374/ES8389/BoxAudioCodec/NoCodecDuplex/NoCodecSimplex/NoCodecSimplexPdm/Dummy）
- `AudioConfig` 结构体：包含所有驱动构造函数参数的并集，带有合理默认值和详细注释
- `CreateAudioCodec()` 工厂函数：根据 `hardware_type` 创建对应的驱动实例

### 5. 录音模块文件（2026-03-09 新增）

| 文件 | 位置 | 状态 |
|------|------|------|
| `audio_input.h` | `ai_sdk_builder/components/ai_sdk/src/audio/` | ✅ 已创建（内部头文件） |
| `audio_input.cc` | `ai_sdk_builder/components/ai_sdk/src/audio/` | ✅ 已创建 |

**内容**：
- `AudioInput` 类：麦克风录音模块，PIMPL 模式
- 公开 API：`Initialize()` / `Start()` / `Stop()` / `SetRecording()` / `IsRecording()` / `SetAudioDataCallback()`
- 内部实现（Impl 类）：
  - FreeRTOS 录音任务（栈 3072B，优先级 8）
  - EventGroup 控制录音开关（暂停时零 CPU 开销）
  - 重采样（硬件采样率 → 16kHz，使用 esp_audio_effects，按需初始化）
  - 声道转换（多声道 → 单声道，取左声道）
  - 数据累积（5120 字节后通过回调输出，匹配 sendAudio 建议块大小）
- 输出格式：PCM 16-bit 小端序，16kHz，单声道
- 内存预算：~8-10 KB（含任务栈）

**新增依赖**：
- `espressif/esp_audio_effects: '~1.2.0'`（音频重采样）
- CMakeLists.txt 中添加 `esp_audio_effects` 到 REQUIRES

### 6. MP3 解码器文件（2026-03-09 新增）

| 文件 | 位置 | 状态 |
|------|------|------|
| `audio_decoder.h` | `ai_sdk_builder/components/ai_sdk/src/audio/` | ✅ 已创建（内部头文件） |
| `audio_decoder.cc` | `ai_sdk_builder/components/ai_sdk/src/audio/` | ✅ 已创建 |
| `mp3_decoder.h` | `ai_sdk_builder/components/ai_sdk/src/audio/` | ✅ 已创建 |
| `mp3_decoder.cc` | `ai_sdk_builder/components/ai_sdk/src/audio/` | ✅ 已创建 |

**内容**：
- `IAudioDecoder` 抽象接口：统一的音频解码器接口（Open/Decode/GetInfo/Close）
- `AudioDecodeInfo` 结构体：解码后的音频参数（采样率、位深、声道数、比特率）
- `DecodeResult` 枚举：解码结果（kOk/kNeedMore/kError）
- `AudioFormatType` 枚举：音频格式类型（当前仅 kMp3）
- `CreateAudioDecoder()` 工厂函数：根据格式类型创建解码器实例
- `Mp3Decoder` 类：基于 esp_audio_codec 的 Simple Decoder API 实现
  - 使用 `esp_audio_simple_dec_*` API（非低级帧解码 API）
  - 内部自动处理 MP3 帧同步和 ID3 标签跳过
  - 输入不需要帧对齐，支持任意大小数据块（适合 HTTP 流式下载）
  - 首次解码成功后自动缓存音频信息
  - 内存占用：MP3 解码器内部约 20-30 KB（由 esp_audio_codec 管理）

**新增依赖**：
- `espressif/esp_audio_codec: '~2.4.0'`（MP3 解码）
- CMakeLists.txt 中添加 `esp_audio_codec` 到 REQUIRES
- CMakeLists.txt 中添加 `src/audio` 到 PRIV_INCLUDE_DIRS（mp3_decoder.h 等内部头文件）

---

### 7. TTS 播放器文件（2026-03-10 新增）

| 文件 | 位置 | 状态 |
|------|------|------|
| `tts_player.h` | `ai_sdk_builder/components/ai_sdk/src/audio/` | ✅ 已创建（内部头文件） |
| `tts_player.cc` | `ai_sdk_builder/components/ai_sdk/src/audio/` | ✅ 已创建 |

**内容**：
- `TtsPlayState` 枚举：播放状态（kIdle/kPlaying/kError）
- `TtsPlayer` 类：TTS 语音播放模块，PIMPL 模式
- 公开 API：`Initialize()` / `Play()` / `Stop()` / `IsPlaying()` / `GetState()` / `SetVolume()`
- 回调：`SetStateCallback()` / `SetCompletionCallback()`
- 内部实现（Impl 类）：
  - FreeRTOS 播放任务（栈 4096B，优先级 5）
  - HTTP 流式下载（esp_http_client，分块接收 2048B）
  - MP3 解码（使用 IAudioDecoder / Mp3Decoder）
  - 重采样（内嵌在流水线中，MP3 采样率 → 硬件输出采样率）
  - PCM 输出（AudioCodec::OutputData()）
- 流水线：HTTP 下载 → MP3 解码 → 重采样 → 扬声器播放
- 内存预算：~35-40 KB（含 MP3 解码器 ~20-30KB）

---

```
ai_sdk_builder/components/ai_sdk/
├── include/ai_sdk/
│   └── audio/
│       ├── audio_codec.h          # 基类头文件（公开 API）
│       └── audio_config.h         # 配置枚举 + 结构体 + 工厂函数（公开 API）
└── src/
    └── audio/
        ├── audio_codec.cc          # 基类实现
        ├── audio_config.cc         # Codec 工厂函数实现
        ├── audio_decoder.h         # 解码器接口 + 工厂函数（内部头文件）
        ├── audio_decoder.cc        # 解码器工厂函数实现
        ├── audio_input.h           # 麦克风录音模块（内部头文件）
        ├── audio_input.cc          # 录音模块实现
        ├── tts_player.h            # TTS 播放模块（内部头文件）
        ├── tts_player.cc           # TTS 播放器实现
        ├── mp3_decoder.h           # MP3 解码器（内部头文件）
        ├── mp3_decoder.cc          # MP3 解码器实现
        └── codecs/
            ├── es8311_audio_codec.h/cc
            ├── es8388_audio_codec.h/cc
            ├── es8374_audio_codec.h/cc
            ├── es8389_audio_codec.h/cc
            ├── box_audio_codec.h/cc
            ├── no_audio_codec.h/cc
            └── dummy_audio_codec.h/cc
```

---

### 8. ASR 集成层（2026-03-10 新增）

| 文件 | 改动 | 状态 |
|------|------|------|
| `asr_intelligent_dialogue.h` | 添加 3 个公开方法声明 | ✅ 已完成 |
| `asr_intelligent_dialogue.cc` | 添加 AudioInput/TtsPlayer 成员和方法实现 | ✅ 已完成 |

**新增公开 API（`AsrIntelligentDialogue`）**：

| 方法 | 描述 |
|------|------|
| `initAudio(AudioCodec* codec)` | 初始化内置音频模块（创建 AudioInput + TtsPlayer） |
| `setRecording(bool enable)` | 控制麦克风录音开关（true = 采集并发送音频） |
| `setAutoPlayTts(bool enable)` | 控制 TTS 自动播放开关（true = Speak 指令自动播放） |

**Impl 内部新增成员**：
- `std::unique_ptr<AudioInput> audio_input_`：麦克风录音模块，由 `initAudio()` 创建
- `std::unique_ptr<TtsPlayer> tts_player_`：TTS 播放模块，由 `initAudio()` 创建
- `std::atomic<bool> auto_play_tts_{false}`：TTS 自动播放开关

**Speak 指令处理**：
- `parseMessage()` 中 `"Speak"` 分支：解析 `payload.url`，若 `auto_play_tts_` 为 true 则调用 `tts_player_->Play(url)`
- `stop()` 中：调用 `audio_input_->SetRecording(false)` 暂停录音，调用 `tts_player_->Stop()` 停止播放

**厂商使用示例**：

```cpp
#include "ai_sdk/asr_intelligent_dialogue.h"
#include "ai_sdk/audio/audio_config.h"

// 1. 创建并启动 AudioCodec
AudioConfig config;
config.hardware_type = AudioHardwareType::kEs8311;
// ... 填写引脚配置
AudioCodec* codec = CreateAudioCodec(config);
codec->Start();

// 2. 初始化 SDK 内置音频模块
auto& asr = AIAssistantManager::getInstance().asrIntelligentDialogueHelp();
asr.setCallbacks(...);
asr.initAudio(codec);        // SDK 内部管理录音和 TTS
asr.setAutoPlayTts(true);    // 收到 Speak 指令自动播放

// 3. 启动语音识别
asr.start();
asr.setRecording(true);      // 开始录音

// 4. 对话结束后暂停
asr.setRecording(false);
asr.stop();
```

---

## 二、编译验证

### 1. 驱动层编译验证

> ✅ **2026-03-09 编译验证通过**

```bash
cd ai_sdk_builder
idf.py build
# 编译成功，所有音频驱动文件通过编译
```

### 2. 配置层编译验证

> ✅ **2026-03-09 编译验证通过**

```bash
cd ai_sdk_builder
idf.py build
# 编译成功，audio_config.h/cc 通过编译
```

### 3. 录音模块编译验证

> ✅ **2026-03-09 编译验证通过**

```bash
cd ai_sdk_builder
idf.py build
# 编译成功，audio_input.h/cc 通过编译
# 修复了一个 uint32_t 格式符警告（%d → (int) 强制转换）
```

### 4. MP3 解码器编译验证

> ✅ **2026-03-09 编译验证通过**

```bash
cd ai_sdk_builder
idf.py build
# 编译成功，audio_decoder.h/cc + mp3_decoder.h/cc 通过编译
# 修复了一个缺少 #include <new> 的错误（std::nothrow 需要此头文件）
```

### 5. ASR 集成层编译验证

> ✅ **2026-03-10 编译验证通过**

```bash
cd ai_sdk_builder
idf.py build
# 编译成功，asr_intelligent_dialogue.h/cc 中的新增代码通过编译
# 新增：initAudio() / setRecording() / setAutoPlayTts()
# 新增：AudioInput 和 TtsPlayer 集成
```

### 6. 下一步工作

1. ~~**配置层** - 定义 `AudioHardwareType` 枚举和 `AudioConfig` 结构体，实现 Codec 工厂函数~~ ✅ 已完成
2. ~~**音频输入（麦克风录制）** - 实现 `AudioInput` 类~~ ✅ 已完成
3. ~~**音频解码层** - MP3 解码器~~ ✅ 已完成
4. ~~**重采样器封装**~~ ✅ 已完成（内嵌在 TtsPlayer 中，方案 A）
5. ~~**TTS 播放器（音频播放）** - HTTP 流式下载 → 解码 → 重采样 → 播放管线~~ ✅ 已完成
6. ~~**头文件重组** - 将 audio_decoder.h / audio_input.h / tts_player.h 移至私有目录~~ ✅ 已完成
7. ~~**ASR 集成** - Speak 指令处理 + setAutoPlayTts 开关~~ ✅ 已完成
8. ~~**编译验证** - 验证新增代码编译通过~~ ✅ 已完成（2026-03-10）

---

## 三、关键修改说明

### 3.1 移除的依赖

| 依赖 | 原用途 | 替代方案 |
|------|--------|----------|
| `board.h` | 无实际使用 | 直接删除 |
| `settings.h` | 音量持久化存储 | 使用默认值 |

### 3.2 命名空间

所有类和常量都放在 `ai_sdk` 命名空间中：

```cpp
namespace ai_sdk {

class AudioCodec { ... };
class Es8311AudioCodec : public AudioCodec { ... };
// ...

} // namespace ai_sdk
```

### 3.3 宏名称变更

```cpp
// 原名称
#define AUDIO_CODEC_DMA_DESC_NUM 6
#define AUDIO_CODEC_DMA_FRAME_NUM 240

// 新名称
#define AI_SDK_AUDIO_CODEC_DMA_DESC_NUM 6
#define AI_SDK_AUDIO_CODEC_DMA_FRAME_NUM 240
```

---

## 四、使用示例

### 方式 1：通过工厂函数创建（推荐）

```cpp
#include "ai_sdk/audio/audio_config.h"
#include "ai_sdk/audio/audio_codec.h"

// 配置 ES8311 驱动
ai_sdk::AudioConfig config;
config.hardware_type = ai_sdk::AudioHardwareType::kEs8311;
config.input_sample_rate = 16000;
config.output_sample_rate = 16000;
config.i2c_master_handle = i2c_handle;  // 已创建的 I2C 主机句柄
config.i2c_port = I2C_NUM_0;
config.mclk = GPIO_NUM_2;
config.bclk = GPIO_NUM_17;
config.ws   = GPIO_NUM_47;
config.dout = GPIO_NUM_15;
config.din  = GPIO_NUM_16;
config.pa_pin = GPIO_NUM_46;
config.codec_addr = 0x18;

// 创建驱动实例
ai_sdk::AudioCodec* codec = ai_sdk::CreateAudioCodec(config);

// 启动
codec->Start();

// 设置音量
codec->SetOutputVolume(50);

// 录音（麦克风输入）
std::vector<int16_t> buffer(1024);
codec->InputData(buffer);

// 播放（扬声器输出）
codec->OutputData(buffer);

// 释放
delete codec;
```

### 方式 2：直接创建驱动实例

```cpp
#include "ai_sdk/audio/codecs/es8311_audio_codec.h"

// 创建 ES8311 驱动实例
void* i2c_master_handle = ...; // I2C 主机句柄

auto codec = new ai_sdk::Es8311AudioCodec(
    i2c_master_handle,
    I2C_NUM_0,
    16000,  // 输入采样率
    16000,  // 输出采样率
    GPIO_NUM_0,   // MCLK
    GPIO_NUM_1,   // BCLK
    GPIO_NUM_2,   // WS
    GPIO_NUM_3,   // DOUT
    GPIO_NUM_4,   // DIN
    GPIO_NUM_5,   // PA 引脚
    0x18,         // ES8311 地址
    true,          // 使用 MCLK
    false          // PA 不反转
);

// 启动
codec->Start();

// 设置音量
codec->SetOutputVolume(50);

// 读取音频
std::vector<int16_t> buffer(1024);
codec->InputData(buffer);

// 写入音频
codec->OutputData(buffer);

// 释放
delete codec;
```

---

## 五、统计

| 项目 | 数量 |
|------|------|
| 创建的文件 | 26 个（驱动 16 + 配置层 2 + 录音模块 2 + 解码器 4 + TTS 播放器 2） |
| 修改的文件 | 4 个（CMakeLists.txt, idf_component.yml, asr_intelligent_dialogue.h, asr_intelligent_dialogue.cc） |
| 新增代码行数 | ~2900 行 |
| 头文件重组 | 将 audio_decoder.h / audio_input.h / tts_player.h 从公开 API 迁移至私有目录（`include/ai_sdk/audio/` → `src/audio/`） |
| ASR 集成 | 在 `AsrIntelligentDialogue` 添加 `initAudio()` / `setRecording()` / `setAutoPlayTts()`，实现 Speak 指令自动播放 |

---

## 六、架构设计说明

### 6.1 整体语音交互流程（本次需求目标）

```
用户说话 → 麦克风 → AudioCodec → AudioInput → sendAudio() → 云端 ASR
                                                                ↓
用户听到 ← 扬声器 ← AudioCodec ← TtsPlayer ← Speak 指令 ← DialogueResult
```

### 6.2 AudioInput 调用链路

AudioInput 作为 SDK 内部模块，由 AsrIntelligentDialogue 管理（第五步 ASR 集成后）：

```
厂商代码:
  asr.initAudio(codec)          → AudioInput.Initialize(codec)
  asr.setRecording(true)        → AudioInput.SetRecording(true)
  asr.setRecording(false)       → AudioInput.SetRecording(false)

SDK 内部:
  AudioInput 录音任务:
    codec->InputData() → 重采样 → 声道转换 → 累积 5120B → 回调
                                                              ↓
  回调内部:
    asr.sendAudio(data, 5120)   → WebSocket → 云端 ASR
```

### 6.3 关键设计决策

| 决策 | 选择 | 理由 |
|------|------|------|
| AudioInput 不直接暴露给厂商 | 通过 AsrIntelligentDialogue 管理 | 与 Android SDK 风格一致，最少厂商代码 |
| 不做 Opus 编码 | 直接发送 PCM | 云端 ASR 接受 PCM，主项目也是解码回 PCM 再发 |
| 不做 AFE | 后续优化 | 仅支持 ESP32-S3，需要 PSRAM，基础功能不需要 |
| 回调而非队列 | AudioDataCallback | SDK 不管理发送线程，回调内直接 sendAudio |
| PIMPL 模式 | 隐藏实现细节 | 与 AsrIntelligentDialogue 风格一致 |
| 帧时长 20ms | 匹配主项目 | 320 samples@16kHz |
| 累积 5120 字节 | 匹配 sendAudio 建议 | 160ms@16kHz 16bit Mono，与 Android CHUNK_SIZE 一致 |

### 6.4 内存预算

| 模块 | 内存 | 说明 |
|------|------|------|
| AudioInput（录音） | ~8-10 KB | 任务栈 3072B + 缓冲区 ~5-7KB |
| Mp3Decoder（解码） | ~20-30 KB | esp_audio_codec 内部缓冲（由库管理） |
| TtsPlayer（TTS 播放） | ~35-40 KB | 任务栈 4096B + HTTP 缓冲 2048B + MP3 解码器 + 重采样 + PCM 缓冲 |
| 录音和播放通常不同时进行 | — | TTS 播放时暂停录音可节省内存 |

### 6.5 依赖组件

| 组件 | 版本 | 用途 | 来源 |
|------|------|------|------|
| espressif/esp_codec_dev | ~1.5.4 | 音频芯片驱动 | ESP Component Registry |
| espressif/esp_websocket_client | * | WebSocket 通信 | ESP Component Registry |
| espressif/esp_audio_effects | ~1.2.0 | 音频重采样 | ESP Component Registry |
| espressif/esp_audio_codec | ~2.4.0 | MP3 解码 | ESP Component Registry |
| driver | (内置) | I2S/I2C/GPIO | ESP-IDF 内置 |
| esp_http_client | (内置) | HTTP 通信 | ESP-IDF 内置 |

### 6.6 Speak 指令格式（TTS 播放需要）

服务器返回的 Speak 指令在 `DialogueResult` 中：
- `result.directive` = `"Speak"`
- `result.payload` = `{"url": "https://tts.xxx.com/audio.mp3"}` (JSON 字符串)
- MP3 格式，通过 HTTP URL 下载

### 6.7 云端 ASR 音频要求

- 格式：PCM 16-bit 小端序
- 采样率：16000 Hz
- 声道：单声道
- 建议块大小：5120 字节（160ms）
- 发送方式：`asr.sendAudio(data, len)` → WebSocket binary
