# 系统记忆 · wake_word_detection 模块分析

> 更新时间：2026-09-09
> 仓库：foukation/EdgeWake ｜ 分支：dev

## 一、模块全景

`wake_word_detection/ESP32` 是「小智 AI（xiaozhi-esp32）」语音机器人固件，内部**并行存在两条离线语音唤醒技术路线**：

| 路线 | 位置 | 引擎 | 说明 |
|------|------|------|------|
| **路线一：ESP-SR** | `main/audio/wake_words/` | 乐鑫 WakeNet/MultiNet（闭源库） | 生产级，开箱即用，带 AEC |
| **路线二：灵犀自研 KWS** | `wake_word_detection_sdk_builder*` | 自研 wekws → TFLite-Micro | 唤醒词「灵犀灵犀」，算法自主可控，v0.9.0 参考级 |

---

## 二、路线一：ESP-SR（main/audio/wake_words/）

策略模式，抽象基类 `WakeWord`（`main/audio/wake_word.h`），3 个实现：

| 实现 | 芯片 | 引擎 | 特点 |
|------|------|------|------|
| `EspWakeWord` | ESP32/C3 | WakeNet | 同步检测，无 AEC，不回传音频 |
| `AfeWakeWord` | S3/P4 | AFE+WakeNet | 独立任务、AEC 回声消除、Opus 回传 |
| `CustomWakeWord` | S3/P4 | MultiNet | 自定义命令词，读 index.json |

- 选型逻辑在 `main/audio/audio_service.cc` 的 `SetModelsList()`：优先级 `MultiNet > AFE > 纯WakeNet`，按芯片+模型自动决定。
- 数据流：`麦克风 → 重采样16k → wake_word_->Feed() → 命中回调 on_wake_word_detected`（单线程 EventGroup 循环）。
- 另有 `components/wake_word_sdk/`：正在把唤醒能力抽成独立 SDK（`WakeWordConfig + ReadAssetFunc` 解耦），新旧两套并存。
- 已知隐患：`AfeWakeWord` 用 `wake_words_[model_index-1]` 无越界检查；`EspWakeWord` 无 AEC，外放易自唤醒。

---

## 三、路线二：灵犀 TFLite KWS（重点）

### builder 与 demo 关系（源码保护分发）
```
sdk_builder（有源码 src/）──编译──> libwake_word_detection_sdk.a（strip 符号）──> sdk_builder_demo（只有头文件 + .a）
```
- `wake_word_detection_sdk_builder`：编库工程，`app_main` 空，`build_all.ps1` 多芯片编库，POST_BUILD 自动 copy+strip 到 `lib/${chip}/`。
- `wake_word_detection_sdk_builder_demo`：接入验证 Demo，只有 `include/` 公开头 + 预编库，内嵌 4 段 WAV 跑离线识别。
- 目的：交付时不暴露算法源码。

### 算法链路
```
16k/16bit单声道 PCM
 → fbank/fft 提特征（40帧×40维 Mel，预加重0.97，Hamming窗，512点FFT）
 → TFLite-Micro 推理（g_model ~345KB，tensor_arena 512KB@PSRAM）
 → 解码（流式 RecognizeCommands / 离线 offline::DecodeOutputs）
 → 命中回调
```
- 唤醒词单类别 `LINGXILINGXI`，阈值 0.5，非重叠窗口步长 400ms。
- 仅支持 esp32s3；依赖 `esp-tflite-micro ^1.3.5`；ESP-IDF v5.4.3；ABI 强约束（IDF/编译器/C++标准须一致）。
- 特征参数编译期对齐训练侧 Python（wekws/kws_tflite），训推一致性好。

---

## 四、待优化清单（灵犀 KWS）

### 🔴 必须修（Bug）
1. `recognize_commands.cc`：`static float global_max_prob` / `window_max_scores` 跨录音不清零、非可重入/非线程安全 → 流式使用结果污染。应改类成员 + reset。
2. `feature_pipeline.cc`：`AcceptWaveform` 每次 `new/delete int16_t[]`，唤醒热路径逐窗口堆分配 → 卡顿+碎片。`total_len` 有界，应改固定成员缓冲。
3. `offline_decode.cc`：`static int triggered[512]` 非可重入；`frame_scores` 与调用方缓冲无尺寸校验 → 越界隐患。

### 🟠 建议（性能/健壮）
4. `fbank.cc`：`frame_data[400]`/`fft_real[512]`/`power[256]` 魔法数硬编码 → 改用 settings 的 constexpr 派生。
5. demo：每窗口 3 次 `vTaskDelay(1ms)` → 拖慢吞吐，建议降优先级/合并。
6. demo：1600 个 float 逐元素拷进 model_input → 让 FeaturePipeline 直接写 model_input_buffer 省一次拷贝。

### 🟡 产品化 / 待核实
7. 阈值 0.5 硬编码两处，无运行时调参 API。
8. 疑似缺特征 CMVN 归一化 → 需与训练 pipeline 核对（也可能已 bake 进模型）。
9. builder `README.md` 仍是模板占位（"XXXX"），文档待补。
10. 流式残差 `num_frames_residual` 与 fbank 实际消耗帧数用两套公式 → 长流可能帧漂移。

---

## 五、关键文件索引
- 抽象接口：`main/audio/wake_word.h`、`components/wake_word_sdk/include/wake_word_sdk/*.h`
- ESP-SR 实现：`main/audio/wake_words/{afe,esp,custom}_wake_word.cc`
- 编排：`main/audio/audio_service.cc`
- 灵犀 SDK 源码：`wake_word_detection_sdk_builder/components/wake_word_detection_sdk/src/{fbank,fft,feature_pipeline,offline_decode,recognize_commands,model}.cc`
- 灵犀 SDK 头：同目录 `include/*.h`（`micro_model_settings.h` 存放全部参数）
- Demo 推理：`wake_word_detection_sdk_builder_demo/main/main_functions.cc`
- 协议文档：`wake_word_detection_sdk_builder_demo/灵犀AI语音唤醒SDK接口协议文档.md`
