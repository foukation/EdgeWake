# ai-sdk TTS 硬件适配 - 三个方案详细对比

## 概述

ai-sdk 添加 TTS 播放功能时，需要解决硬件适配问题。本文档对比三种方案，并给出推荐。

---

## 当前现状

### ai-sdk 已有的功能

| 功能 | 模块 | 说明 |
|------|------|------|
| 智能对话 | AsrIntelligentDialogue | WebSocket 连接，发送语音，接收对话结果 |
| TTS 配置 | TtsConfig | 音色/语速/音调/音量配置 |
| 对话结果 | DialogueResult | 返回 Speak 指令和 TTS URL |

### ai-sdk 缺少的功能

| 功能 | 说明 |
|------|------|
| TTS URL 下载 | 需要客户端自己实现 |
| 音频解码 | MP3/Opus -> PCM |
| 音频播放 | 硬件输出 |

### 当前 TTS 数据流

```
1. 客户端发送语音（PCM）-> 服务器
2. 服务器返回 DialogueResult，包含：
   - directive: "Speak"
   - payload: {"url": "https://tts.xxx.com/audio.mp3"}
3. 客户端需要自己下载 URL 并播放
```

---

## 方案对比总览

| 方案 | ai-sdk 工作量 | 厂商工作量 | 硬件支持 | 推荐度 |
|------|--------------|----------|---------|-------|
| 1. 内置驱动 | 大 | 小 | 自己维护 | ⭐⭐ |
| 2. 接口抽象 | 小 | 中 | 厂商维护 | ⭐⭐⭐ |
| 3. ESP-ADF | 小 | 小 | 官方维护 | ⭐⭐⭐⭐ |

---

## 方案 1：ai-sdk 内置驱动

### 使用的技术

| 技术 | 说明 |
|------|------|
| esp_codec_dev | 乐鑫音频芯片驱动组件 |
| esp_audio_codec | Opus/MP3 解码器 |
| esp_audio_effects | 重采样器 |
| esp_http_client | HTTP 下载 |
| driver/i2s | I2S 音频传输 |
| driver/i2c | I2C 控制通信 |

### 架构

```
ai-sdk
├── TTS Player (新增)
│   ├── URL 下载
│   ├── 音频解码
│   └── 播放队列
├── Audio HAL (新增)
└── 驱动层 (新增)
    ├── ES8311驱动
    ├── ES8388驱动
    ├── ES8374驱动
    ├── NoCodec驱动
    └── PDM驱动
```

### ai-sdk 需要实现的功能

| 模块 | 功能 | 复杂度 |
|------|------|--------|
| TTS Player | URL 下载 + 解码 + 播放控制 | 高 |
| Audio HAL | 统一接口定义 | 低 |
| ES8311 驱动 | 集成 esp_codec_dev | 中 |
| ES8388 驱动 | 集成 esp_codec_dev | 中 |
| ES8374 驱动 | 集成 esp_codec_dev | 中 |
| NoCodec 驱动 | I2S 直连 | 中 |
| PDM 驱动 | I2S PDM 模式 | 中 |

### 厂商需要做的事

1. 选择芯片类型
2. 配置 GPIO 引脚
3. 编译、烧录

### 支持的硬件

需要 ai-sdk 自己维护，包括：
- ES8311、ES8388、ES8374、ES8389
- AW88298、ES7210
- I2S 直连、PDM

### 优点

1. **厂商接入简单** - 只需配置引脚
2. **不依赖外部框架** - 自包含
3. **可控性强** - 完全控制实现

### 缺点

1. **ai-sdk 工作量大** - 需要实现所有驱动
2. **维护压力大** - 新硬件需要 ai-sdk 更新
3. **重复造轮子** - 乐鑫已经有现成方案

---

## 方案 2：ai-sdk 定义接口，厂商实现

### 使用的技术

| 技术 | 说明 |
|------|------|
| esp_audio_codec | Opus/MP3 解码器 |
| esp_audio_effects | 重采样器 |
| esp_http_client | HTTP 下载 |
| IAudioOutput 接口 | ai-sdk 定义的抽象接口 |

### 架构

```
ai-sdk
├── TTS Player (新增)
│   ├── URL 下载
│   ├── 音频解码
│   └── 播放队列
└── IAudioOutput 接口 (新增)
        |
        v
厂商实现
├── MyAudioOutput
└── 硬件驱动 (厂商自己维护)
```

### ai-sdk 需要实现的功能

| 模块 | 功能 | 复杂度 |
|------|------|--------|
| TTS Player | URL 下载 + 解码 + 播放控制 | 高 |
| IAudioOutput | 接口定义 | 低 |

### 接口定义

```cpp
class IAudioOutput {
public:
    virtual void OutputData(const int16_t* data, size_t samples) = 0;
    virtual int GetSampleRate() = 0;
    virtual void SetVolume(int volume) = 0;
    virtual void Enable(bool enable) = 0;
};
```

### 厂商需要做的事

1. 实现 IAudioOutput 接口
2. 选择并集成硬件驱动（esp_codec_dev 或自己写）
3. 配置 GPIO 引脚
4. 传入 ai-sdk

### 支持的硬件

由厂商自己维护，可支持任何硬件

### 优点

1. **ai-sdk 工作量小** - 只定义接口
2. **灵活性高** - 厂商可以支持任何硬件
3. **不依赖外部框架** - 轻量级

### 缺点

1. **厂商工作量中** - 需要实现接口
2. **厂商技术门槛** - 需要懂硬件驱动
3. **质量不可控** - 依赖厂商实现质量

---

## 方案 3：使用 ESP-ADF（推荐）

### 使用的技术

| 技术 | 说明 |
|------|------|
| ESP-ADF | 乐鑫官方音频开发框架 |
| esp_audio | ESP-ADF 的统一播放 API |
| audio_pipeline | 音频管道 |
| esp_codec_dev | 音频芯片驱动（ESP-ADF 内置） |
| http_stream | HTTP 音频流 |
| mp3_decoder | MP3 解码器 |
| i2s_stream | I2S 输出流 |

### 架构

```
ai-sdk
├── TTS Player (新增)
│   └── 调用 esp_audio API
        |
        v
ESP-ADF (乐鑫官方)
├── esp_audio (统一 API)
├── audio_pipeline (音频管道)
├── http_stream (HTTP 下载)
├── mp3_decoder (MP3 解码)
├── i2s_stream (I2S 输出)
└── esp_codec_dev (硬件驱动)
        |
        v
硬件 (厂商的板子)
```

### ai-sdk 需要实现的功能

| 模块 | 功能 | 复杂度 |
|------|------|--------|
| TTS Player | 调用 esp_audio API | 低 |

ai-sdk 不需要：
- 不需要实现音频解码
- 不需要实现硬件驱动
- 不需要处理 I2S/I2C

### 厂商需要做的事

1. 引入 ESP-ADF 依赖
2. 选择匹配的开发板配置
3. 配置硬件引脚（menuconfig）
4. 编译、烧录

### 支持的硬件

#### 支持的音频芯片（官方维护）

| 芯片 | 播放 | 录音 | 说明 |
|------|------|------|------|
| ES8311 | ✓ | ✓ | 单编解码器，最常用 |
| ES8388 | ✓ | ✓ | 双编解码器 |
| ES8374 | ✓ | ✓ | 双编解码器 |
| ES8389 | ✓ | ✓ | 双编解码器 |
| ES7210 | ✗ | ✓ | 4通道 ADC |
| ES7243 | ✗ | ✓ | ADC |
| AW88298 | ✓ | ✗ | 智能功放 |
| TAS6805M | ✓ | ✗ | 功放 |
| ZL38063 | ✓ | ✓ | DSP |
| CJC8910 | ✓ | ✓ | 编解码器 |

#### 支持的开发板（官方维护）

| 开发板 | 芯片 | 说明 |
|--------|------|------|
| ESP32-LyraT | ES8388 | 经典音频开发板 |
| ESP32-LyraT-Mini | ES8311 | 迷你版 |
| ESP32-S3-Korvo-2 | ES8311+ES7210 | S3 音频开发板 |
| ESP32-C3-Lyra | ES8311 | C3 音频开发板 |

#### 支持的音频格式

| 格式 | 解码 | 编码 |
|------|------|------|
| MP3 | ✓ | ✗ |
| AAC | ✓ | ✗ |
| Opus | ✓ | ✓ |
| FLAC | ✓ | ✗ |
| WAV | ✓ | ✓ |
| OGG | ✓ | ✗ |
| AMR | ✓ | ✓ |

### 优点

1. **ai-sdk 工作量小** - 只需调用 API
2. **厂商工作量小** - 只需配置
3. **官方维护** - 乐鑫持续更新
4. **硬件支持好** - 已适配多种芯片
5. **功能完善** - 支持 MP3/AAC/Opus/FLAC
6. **类似 Android** - 调用 API 即可
7. **不重复造轮子** - 使用现成方案

### 缺点

1. **增加依赖** - 需要引入 ESP-ADF
2. **包大小增加** - ESP-ADF 较重
3. **学习成本** - 需要理解 ESP-ADF 架构

---

## 推荐方案：ESP-ADF

### 推荐原因

1. **不重复造轮子** - 乐鑫已经做好了
2. **官方维护更可靠** - 持续更新和 bug 修复
3. **硬件兼容性好** - 已适配多种芯片
4. **双方工作量都小** - ai-sdk 和厂商都轻松
5. **类似 Android** - 调用 API 即可，不关心硬件

### 适用场景

- 需要支持多种硬件的项目
- 希望减少维护成本的项目
- 需要播放多种音频格式的项目

### 不适用场景

- 对包大小极度敏感的项目
- 使用 ESP-ADF 不支持的特殊硬件

---

## 总结

| 方案 | ai-sdk 工作量 | 厂商工作量 | 硬件支持 | 推荐度 |
|------|--------------|----------|---------|-------|
| 1. 内置驱动 | 大 | 小 | 自己维护 | ⭐⭐ |
| 2. 接口抽象 | 小 | 中 | 厂商维护 | ⭐⭐⭐ |
| 3. ESP-ADF | 小 | 小 | 官方维护 | ⭐⭐⭐⭐ |

**推荐使用方案 3（ESP-ADF）**，因为：
- 双方工作量都最小
- 硬件由乐鑫官方维护
- 功能最完善
