# RTOS AI-SDK 麦克风录制 & 音频播放 - TODO List

> 创建时间: 2026-03-05
> 最后更新: 2026-03-10
> 状态: ✅ 本次需求范围（麦克风录制 + 音频播放 + ASR 集成）全部完成，编译验证通过

---

## 一、基础设施

> ✅ 2026-03-05 已完成

### 1.1 添加组件依赖

| 组件 | 版本 | 用途 | 来源 |
|------|------|------|------|
| esp_codec_dev | ~1.5.4 | 音频芯片驱动 | ESP Component Registry |
| esp_audio_codec | ~2.4.0 | MP3/Opus 编解码 | ESP Component Registry |
| esp_audio_effects | ~1.2.0 | 重采样/音效处理 | ESP Component Registry |

### 1.2 创建目录结构

| 目录 | 用途 |
|------|------|
| `include/ai_sdk/audio/` | 头文件 |
| `src/audio/` | 实现文件 |
| `src/audio/codecs/` | 硬件驱动封装 |

---

## 二、驱动封装参考 (当前项目实现)

> 以下为当前项目中已有的驱动封装实现，ai-sdk 可参考这些实现

### 2.1 有芯片方案 - 驱动封装总览

| 封装类 | 芯片 | 录音通道 | 播放通道 | I2C | I2S 模式 | 驱动来源 |
|--------|------|----------|----------|-----|----------|----------|
| Es8311AudioCodec | ES8311 | 1 | 1 | ✅ | 标准模式 | esp_codec_dev |
| Es8388AudioCodec | ES8388 | 1-2 | 2 | ✅ | 标准模式 | esp_codec_dev |
| Es8374AudioCodec | ES8374 | 1 | 1 | ✅ | 标准模式 | esp_codec_dev |
| Es8389AudioCodec | ES8389 | 1 | 1 | ✅ | 标准模式 | esp_codec_dev |
| BoxAudioCodec | ES8311+ES7210 | 4 | 1 | ✅ | TDM 模式 | esp_codec_dev |

### 2.2 无芯片方案 - 驱动封装总览

| 封装类 | 方案 | 录音通道 | 播放通道 | I2C | 驱动来源 |
|--------|------|----------|----------|-----|----------|
| NoAudioCodecDuplex | I2S 双工 | 1 | 1 | ❌ | driver/i2s |
| NoAudioCodecSimplex | I2S 单工 | 1 | 1 | ❌ | driver/i2s |
| NoAudioCodecSimplexPdm | PDM + I2S | 1 | 1 | ❌ | driver/i2s + driver/i2s_pdm |

### 2.3 有芯片方案 - 驱动 API 详情

#### ES8311 (Es8311AudioCodec)

| 驱动组件 | API | 用途 | 录音 | 播放 |
|----------|-----|------|------|------|
| **I2S** | `i2s_new_channel()` | 创建 I2S 通道 | ✅ | ✅ |
| | `i2s_channel_init_std_mode()` | 初始化标准模式 | ✅ | ✅ |
| **esp_codec_dev** | `audio_codec_new_i2s_data()` | I2S 数据接口 | ✅ | ✅ |
| | `audio_codec_new_i2c_ctrl()` | I2C 控制接口 | ✅ | ✅ |
| | `audio_codec_new_gpio()` | GPIO 接口 | - | ✅ |
| | `es8311_codec_new()` | ES8311 驱动 | ✅ | ✅ |
| | `esp_codec_dev_new()` | 创建设备 | ✅ | ✅ |
| | `esp_codec_dev_read()` | 读取音频 | ✅ | - |
| | `esp_codec_dev_write()` | 写入音频 | - | ✅ |
| | `esp_codec_dev_set_out_vol()` | 设置音量 | - | ✅ |
| | `esp_codec_dev_set_in_gain()` | 设置增益 | ✅ | - |

#### ES8388 (Es8388AudioCodec)

| 驱动组件 | API | 用途 | 录音 | 播放 | 特点 |
|----------|-----|------|------|------|------|
| **esp_codec_dev** | `es8388_codec_new()` | ES8388 驱动 | ✅ | ✅ | - |
| | `esp_codec_dev_new()` | 创建输入设备 | ✅ | - | input_dev_ |
| | `esp_codec_dev_new()` | 创建输出设备 | - | ✅ | output_dev_ |
| | `esp_codec_dev_read()` | 读取音频 | ✅ | - | - |
| | `esp_codec_dev_write()` | 写入音频 | - | ✅ | - |

**特点**: 创建两个独立设备 (input_dev_ + output_dev_)

#### ES8374 (Es8374AudioCodec)

| 驱动组件 | API | 用途 | 录音 | 播放 |
|----------|-----|------|------|------|
| **esp_codec_dev** | `es8374_codec_new()` | ES8374 驱动 | ✅ | ✅ |
| | `esp_codec_dev_new()` | 创建设备 (x2) | ✅ | ✅ |
| | `esp_codec_dev_read()` | 读取音频 | ✅ | - |
| | `esp_codec_dev_write()` | 写入音频 | - | ✅ |

#### ES8389 (Es8389AudioCodec)

| 驱动组件 | API | 用途 | 录音 | 播放 |
|----------|-----|------|------|------|
| **esp_codec_dev** | `es8389_codec_new()` | ES8389 驱动 | ✅ | ✅ |
| | `esp_codec_dev_new()` | 创建设备 (x2) | ✅ | ✅ |
| | `esp_codec_dev_read()` | 读取音频 | ✅ | - |
| | `esp_codec_dev_write()` | 写入音频 | - | ✅ |

#### BoxAudioCodec (ES8311 + ES7210)

| 驱动组件 | API | 用途 | 录音 | 播放 | 说明 |
|----------|-----|------|------|------|------|
| **I2S** | `i2s_channel_init_std_mode()` | 初始化 TX | - | ✅ | 播放通道 |
| | `i2s_channel_init_tdm_mode()` | 初始化 RX | ✅ | - | 4通道 TDM |
| **esp_codec_dev** | `es8311_codec_new()` | ES8311 驱动 | - | ✅ | 仅 DAC 模式 |
| | `es7210_codec_new()` | ES7210 驱动 | ✅ | - | 4 通道 ADC |
| | `esp_codec_dev_new()` | 创建输出设备 | - | ✅ | output_dev_ |
| | `esp_codec_dev_new()` | 创建输入设备 | ✅ | - | input_dev_ |
| | `esp_codec_dev_read()` | 读取 4 通道音频 | ✅ | - | - |
| | `esp_codec_dev_write()` | 写入音频 | - | ✅ | - |
| | `esp_codec_dev_set_in_channel_gain()` | 设置通道增益 | ✅ | - | 4 通道独立增益 |

**特点**: ES8311 仅用于播放 (DAC 模式)，ES7210 仅用于录音 (4 通道 ADC)

### 2.4 无芯片方案 - 驱动 API 详情

#### NoAudioCodecDuplex (I2S 双工)

| 驱动组件 | API | 用途 | 录音 | 播放 |
|----------|-----|------|------|------|
| **driver/i2s** | `i2s_new_channel()` | 创建 TX+RX 通道 | ✅ | ✅ |
| | `i2s_channel_init_std_mode()` | 初始化标准模式 | ✅ | ✅ |
| | `i2s_channel_read()` | 读取音频 (32-bit) | ✅ | - |
| | `i2s_channel_write()` | 写入音频 (32-bit) | - | ✅ |
| **软件实现** | 音量计算 | 软件音量 | - | ✅ |

**特点**: 使用同一组 I2S 引脚 (bclk/ws/dout/din)，32-bit 数据需要位移

#### NoAudioCodecSimplex (I2S 单工)

| 驱动组件 | API | 用途 | 录音 | 播放 |
|----------|-----|------|------|------|
| **driver/i2s** | `i2s_new_channel()` | 创建 TX 通道 | - | ✅ |
| | `i2s_new_channel()` | 创建 RX 通道 | ✅ | - |
| | `i2s_channel_init_std_mode()` | 初始化标准模式 | ✅ | ✅ |
| | `i2s_channel_read()` | 读取音频 | ✅ | - |
| | `i2s_channel_write()` | 写入音频 | - | ✅ |

**特点**: 录音和播放使用不同的 I2S 端口和引脚

#### NoAudioCodecSimplexPdm (PDM 麦克风 + I2S 播放)

| 驱动组件 | API | 用途 | 录音 | 播放 |
|----------|-----|------|------|------|
| **driver/i2s** | `i2s_channel_init_std_mode()` | 初始化播放 | - | ✅ |
| **driver/i2s_pdm** | `i2s_channel_init_pdm_rx_mode()` | 初始化 PDM 录音 | ✅ | - |
| | `i2s_channel_read()` | 读取 PDM 音频 (16-bit) | ✅ | - |
| | `i2s_channel_write()` | 写入音频 (32-bit) | - | ✅ |
| **软件实现** | 增益计算 | 软件增益 | ✅ | - |

**特点**: PDM 麦克风输出 16-bit，无需位移

### 2.5 有芯片 vs 无芯片 关键区别

| 对比项 | 有芯片 | 无芯片 |
|--------|--------|--------|
| **录音 API** | `esp_codec_dev_read()` | `i2s_channel_read()` |
| **播放 API** | `esp_codec_dev_write()` | `i2s_channel_write()` |
| **音量控制** | `esp_codec_dev_set_out_vol()` (硬件) | 代码计算 (软件) |
| **增益控制** | `esp_codec_dev_set_in_gain()` (硬件) | 代码计算 (软件) |
| **数据位宽** | 16-bit | 32-bit (需位移 >> 12) |
| **I2C 控制** | 需要 | 不需要 |
| **PA 控制** | 芯片内置或 GPIO | GPIO |

### 2.6 驱动 API 汇总表

| API | ES8311 | ES8388 | ES8374 | ES8389 | Box | 无芯片 |
|-----|--------|--------|--------|--------|-----|--------|
| `i2s_new_channel()` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `i2s_channel_init_std_mode()` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `i2s_channel_init_tdm_mode()` | - | - | - | - | ✅ | - |
| `i2s_channel_init_pdm_rx_mode()` | - | - | - | - | - | ✅ |
| `audio_codec_new_i2s_data()` | ✅ | ✅ | ✅ | ✅ | ✅ | - |
| `audio_codec_new_i2c_ctrl()` | ✅ | ✅ | ✅ | ✅ | ✅ | - |
| `audio_codec_new_gpio()` | ✅ | ✅ | ✅ | ✅ | ✅ | - |
| `es8311_codec_new()` | ✅ | - | - | - | ✅ | - |
| `es8388_codec_new()` | - | ✅ | - | - | - | - |
| `es8374_codec_new()` | - | - | ✅ | - | - | - |
| `es8389_codec_new()` | - | - | - | ✅ | - | - |
| `es7210_codec_new()` | - | - | - | - | ✅ | - |
| `esp_codec_dev_new()` | ✅ | ✅ | ✅ | ✅ | ✅ | - |
| `esp_codec_dev_read()` | ✅ | ✅ | ✅ | ✅ | ✅ | - |
| `esp_codec_dev_write()` | ✅ | ✅ | ✅ | ✅ | ✅ | - |
| `i2s_channel_read()` | - | - | - | - | - | ✅ |
| `i2s_channel_write()` | - | - | - | - | - | ✅ |

---

## 三、驱动分类

### 3.1 有芯片方案 - 使用 esp_codec_dev

| 芯片 | 类型 | 功能 | 录音 | 播放 | 适用场景 |
|------|------|------|------|------|----------|
| **ES8311** | Codec | ADC + DAC | 1 通道 | 1 通道 | 最常用，通用开发板 |
| **ES8388** | Codec | ADC + DAC | 2 通道 | 2 通道 | 老款开发板 |
| **ES8374** | Codec | ADC + DAC | 2 通道 | 2 通道 | 特定开发板 |
| **ES8389** | Codec | ADC + DAC | 2 通道 | 2 通道 | 特定开发板 |
| **ES7210** | ADC | 仅 ADC | 4 通道 | 无 | ESP-BOX 麦克风阵列 |
| **ES7243** | ADC | 仅 ADC | 2 通道 | 无 | 麦克风阵列 |
| **ES8156** | DAC | 仅 DAC | 无 | 2 通道 | ESP-BOX-Lite 播放 |
| **AW88298** | 智能 DAC | 仅 DAC | 无 | 1 通道 | M5Stack CoreS3 |
| **TAS5805M** | DAC | 仅 DAC | 无 | 2 通道 | 高端音频设备 |

### 3.2 无芯片方案 - 使用 ESP-IDF 内置驱动

| 方案 | 驱动库 | 录音 | 播放 | 说明 |
|------|------|------|------|------|
| **I2S 直连** | driver/i2s | ✅ | ✅ | 直接使用 I2S，无需芯片 |
| **PDM 麦克风** | driver/i2s_pdm | ✅ | ❌ | 数字麦克风，仅录音 |
| **ADC 麦克风** | driver/adc | ✅ | ❌ | 模拟麦克风，仅录音 |

### 3.3 常用组合方案

| 方案 | 录音芯片 | 播放芯片 | 使用库 | 适用开发板 |
|------|----------|----------|------|----------|
| 单芯片 | ES8311 | ES8311 | esp_codec_dev | 通用开发板 |
| ESP-BOX | ES7210 | ES8311 | esp_codec_dev | ESP-BOX 系列 |
| ESP-BOX-Lite | ES7243 | ES8156 | esp_codec_dev | ESP-BOX-Lite |
| I2S 直连 | 无 | 无 | driver/i2s | 简单开发板 |

---

## 四、配置层

> ✅ 2026-03-09 已完成

1. ✅ 定义 AudioHardwareType 硬件类型枚举
2. ✅ 定义 AudioConfig 硬件配置结构体

---

## 五、硬件抽象层 (HAL)

> ✅ 2026-03-05 已完成（驱动移植），2026-03-09 补充配置层和工厂函数

3. ✅ 定义 IAudioCodec 硬件抽象接口
4. ✅ 实现 ES8311 Codec 驱动
5. ✅ 实现 BoxAudioCodec 驱动 (ES8311+ES7210)
6. ✅ 实现 NoCodec 驱动 (I2S 直连)
7. ✅ 实现 ES8388 Codec 驱动
8. ✅ 实现 PDM Codec 驱动
9. ✅ 实现 Codec 工厂函数

---

## 六、音频解码层

> ✅ 2026-03-09 已完成（IAudioDecoder 接口 + Mp3Decoder 实现 + 工厂函数）

10. ✅ 定义 IAudioDecoder 解码器接口
    - `AudioDecodeInfo` 结构体（采样率、位深、声道数、比特率）
    - `DecodeResult` 枚举（kOk/kNeedMore/kError）
    - `IAudioDecoder` 抽象类（Open/Decode/GetInfo/Close）
    - 流式解码设计：输入不需要帧对齐
11. ✅ 实现 MP3 解码器
    - `Mp3Decoder` 类：基于 esp_audio_codec 的 Simple Decoder API
    - 使用 `esp_audio_simple_dec_*`（非低级帧解码 API）
    - 内部自动处理 MP3 帧同步和 ID3 标签跳过
    - 支持任意大小输入数据块（适合 HTTP 流式下载）
    - 首次解码成功后自动缓存音频信息
    - 内存占用：~20-30 KB（由 esp_audio_codec 管理）
12. ~~实现 Opus 解码器~~ — 暂不需要，当前服务器只返回 MP3 格式
13. ✅ 实现解码器工厂函数
    - `CreateAudioDecoder(AudioFormatType)` 工厂函数
    - `AudioFormatType` 枚举（当前仅 kMp3）
    - 使用 `std::nothrow` 安全分配内存

---

## 七、音频编码层

> 不在本次需求范围（麦克风录制 + 音频播放不涉及编码）

14. 定义 IAudioEncoder 编码器接口
15. 实现 Opus 编码器

---

## 八、重采样层

> ✅ 2026-03-10 已完成（内嵌在 TtsPlayer 中，方案 A）

16. ✅ 实现重采样器封装 (内嵌在 TtsPlayer 中，使用 esp_audio_effects)
    - 不单独封装为独立类，直接在 TtsPlayer 流水线中处理
    - MP3 解码采样率 → 硬件输出采样率
    - 参考主项目 audio_service.cc 的 output_resampler_ 实现

---

## 九、TTS 播放器

> ✅ 2026-03-10 已完成（TtsPlayer 类实现）

17. ✅ 定义 TtsPlayer 类接口
18. ✅ 实现 TtsPlayer 核心逻辑
    - FreeRTOS 播放任务（栈 4096B，优先级 5）
    - PIMPL 模式隐藏实现细节
    - 状态管理（kIdle/kPlaying/kError）
    - 状态变更回调 + 播放完成回调
19. ✅ 实现 HTTP 流式下载功能
    - esp_http_client 流式读取（分块 2048B）
    - HTTPS 证书验证（esp_crt_bundle_attach）
    - HTTP 超时 15 秒
20. ✅ 实现播放流程管理 (HTTP 下载 → MP3 解码 → 重采样 → PCM 播放)
    - 单任务串行处理整条流水线
    - 按需初始化重采样器（首次解码成功后根据音频信息判断）
    - 内存预算：~35-40 KB（含 MP3 解码器）

---

## 十、音频输入 (麦克风)

> ✅ 2026-03-09 已完成（AudioInput 类实现）

21. ✅ 定义 AudioInput 类接口
22. ✅ 实现 AudioInput 核心逻辑（支持录音启停开关）
    - FreeRTOS 录音任务（栈 3072B，优先级 8）
    - EventGroup 控制录音开关
    - 重采样（硬件采样率 → 16kHz，按需初始化）
    - 声道转换（多声道 → 单声道）
    - 数据累积（5120 字节后回调输出）
    - 内存预算：~8-10 KB
23. ~~集成 ESP-SR AFE 音频前端处理 (AEC/NS/VAD)~~ — 后续支持
    - AFE 是录音质量增强工具（回声消除 AEC、噪声抑制 NS、语音活动检测 VAD）
    - 依赖 `espressif/esp-sr` 组件，仅支持 ESP32-S3，体积大，需要 PSRAM
    - 基础麦克风录制不需要 AFE，后续作为优化项加入

---

## 十一、ASR 集成层

> ✅ 2026-03-10 已完成（initAudio / setRecording / setAutoPlayTts + Speak 指令处理）
> 方案 C：SDK 内置自动播放，提供 setAutoPlayTts() 开关

24. ✅ 扩展 AsrIntelligentDialogue (initAudio/setAutoPlayTts/setRecording)
    - `initAudio(AudioCodec* codec)`：创建内部 `AudioInput` + `TtsPlayer`，绑定 Codec
    - `setRecording(bool enable)`：控制 `AudioInput` 录音开关
    - `setAutoPlayTts(bool enable)`：TTS 自动播放开关
    - `audio_input_->SetAudioDataCallback` 连接到 `sendAudio()`
25. ✅ 实现 DialogueResult 处理 (解析 Speak 指令，调用 TTS，支持 setAutoPlayTts 开关)
    - `parseMessage()` 中 `"Speak"` 分支：提取 `payload.url`
    - 若 `auto_play_tts_` 为 true 则调用 `tts_player_->Play(url)`
    - `stop()` 中暂停录音并停止 TTS 播放
26. ~~实现 ASR/TTS 状态协调 (播放时暂停录音)~~ — 后续优化

---

## 十二、电源管理

> 不在本次需求范围

27. 实现自动休眠机制 (15秒无活动关闭输入/输出)
28. 实现 PA 功放引脚动态控制

---

## 十三、测试

> 不在本次需求范围

29. 编写单元测试
30. 编写集成测试 (多开发板硬件测试)
31. 编写性能测试 (内存/CPU/延迟/功耗)

---

## 十四、文档

> 不在本次需求范围

32. 编写 API 文档
33. 编写厂商集成指南
34. 编写示例代码

---

## 十五、Linux 平台移植 (P2)

> 不在本次需求范围

35. 定义平台抽象接口 (IHttpClient/IWebSocketClient/IAudioOutput)
36. 实现 Linux 默认实现 (libcurl/libwebsockets/ALSA)
37. 创建 CMake 交叉编译构建系统

---

## 十六、唤醒词替换 (P3)

> 不在本次需求范围

38. WeKWS 模型训练
39. TFLite 模型转换与量化
40. ESP32 TFLite Micro 集成

---

## 统计

### 本次需求范围（麦克风录制 + 音频播放）

| 阶段 | 任务 | 状态 |
|------|------|------|
| 一、基础设施 | 添加组件依赖、创建目录结构 | ✅ 已完成 |
| 四、配置层 | AudioHardwareType + AudioConfig + 工厂函数 | ✅ 已完成 |
| 五、HAL 层 | AudioCodec 基类 + 7 种驱动实现 + 工厂函数 | ✅ 已完成 |
| 六、音频解码层 | IAudioDecoder 接口 + MP3 解码器 + 工厂函数 | ✅ 已完成 |
| 八、重采样层 | 重采样器（内嵌在 TtsPlayer 中） | ✅ 已完成 |
| 九、TTS 播放器 | TtsPlayer 类 + HTTP 流式下载 + 播放流水线 | ✅ 已完成 |
| 十、音频输入 | AudioInput 类（支持录音启停开关） | ✅ 已完成 |
| 十一、ASR 集成 | initAudio + setRecording + setAutoPlayTts + Speak 指令处理 | ✅ 已完成 |

### 全部任务统计

| 阶段 | 任务编号 | 任务数 | 本次需求 |
|------|----------|--------|----------|
| 一~五 (基础+HAL) | 1-9 | 9 | ✅ 已完成 |
| 六 (解码层) | 10-13 | 3* | ✅ 已完成（去掉 Opus） |
| 七 (编码层) | 14-15 | 2 | 不在范围（后续支持 Opus） |
| 八 (重采样) | 16 | 1 | ✅ 已完成（内嵌在 TtsPlayer 中） |
| 九 (TTS 播放器) | 17-20 | 4 | ✅ 已完成 |
| 十 (音频输入) | 21-22 | 2* | ✅ 已完成（去掉 AFE） |
| 十一 (ASR 集成) | 24-25 | 2* | 本次范围（去掉状态协调） |
| 十二~十六 | 27-40 | 14 | 不在范围 |
| **总计** | 1-40 | **40** | |

---

## 开发阶段建议

### Phase 1: 核心功能 ✅ 已完成（2026-03-05 ~ 2026-03-10）

```
基础设施 → 配置层 → HAL层 → 解码层 → TTS播放器 → ASR集成
```

### Phase 2: 完善功能（待开始）

```
ASR/TTS状态协调 → 测试 → API文档 → 示例代码
```

### Phase 3: 扩展功能（待开始）

```
AFE集成 → 编码层 → 电源管理 → Linux移植 → 唤醒词替换
```

---

## 硬件适配优先级

| 优先级 | 硬件类型 | 覆盖率 | 说明 |
|--------|----------|--------|------|
| P0 | ES8311 | 50%+ | 最常用 Codec |
| P0 | BoxAudioCodec | 25%+ | ESP-BOX 系列 |
| P0 | NoCodec (I2S直连) | 20%+ | 无芯片方案 |
| P1 | ES8388 | 5% | 老款 Codec |
| P2 | PDM | 5% | 数字麦克风 |

**P0 硬件覆盖 90%+ 开发板**

---

## 相关文档

- [ai-sdk-tts-implementation-plan.md](../ai-sdk-tts-implementation-plan.md) - TTS 实现详细方案
- [ai-sdk-tts-solutions-comparison.md](../ai-sdk-tts-solutions-comparison.md) - TTS 方案对比
- [project-hardware-adaptation-analysis.md](../project-hardware-adaptation-analysis.md) - 硬件适配分析
- [ai-sdk-linux-development-plan.md](../ai-sdk-linux-development-plan.md) - Linux 移植方案
- [wekws_replacement_analysis.md](../wekws_replacement_analysis.md) - 唤醒词替换方案
