# 唤醒词 SDK 接入文档（ESP32-S3 / TFLite Micro）

本工程是「灵犀灵犀」唤醒词（KWS）在 ESP32-S3 上的参考实现：模型与测试音频都编译进固件，跑通「PCM 音频 → Fbank 特征 → TFLite Micro 推理 → 唤醒解码」全链路。

- 芯片：ESP32-S3（需带 PSRAM，默认按 N16R8 / 8MB Octal PSRAM 配置）
- 模型：唤醒模型已内置于 SDK 库中（约 345KB tflite FlatBuffer）
- 输入：16kHz / 16bit / 单声道 PCM
- 输出：唤醒概率逐帧序列，解码为「是否唤醒 + 最大概率 + 触发事件列表」

### 版本信息

| 项目 | 说明 |
|---|---|
| 版本 | 0.9.0 |
| 唤醒词 | 灵犀灵犀（LINGXILINGXI，单类别检测）|
| 目标芯片 | ESP32-S3（需带 PSRAM，默认 N16R8 / 8MB Octal）|
| ESP-IDF | v5.4.3（编库版本，接入方须同大版本且 ≥ 此版本）|
| 编译器 | xtensa-esp-elf 14.2.0 |
| Flash | ≥ 2MB（分区 partitions_singleapp_large.csv，app 1.5MB）|
| 依赖 | esp-tflite-micro ^1.3.5 |
| 交付形式 | 预编译静态库 libwake_word_detection_sdk.a + 公开头文件 |

### 主要功能

- 「PCM → Fbank 特征 → TFLite Micro 推理 → 唤醒解码」全链路参考实现
- 流式（RecognizeCommands）+ 离线整段（offline::DecodeOutputs）两套后处理
- 模型与 4 个测试音频编入固件，开箱即验

---

## 1. 代码结构

本工程是「唤醒词」在 ESP32-S3 上的参考实现，唤醒 SDK 以**预编译静态库**的形式提供，接入方只需引用公开头文件并链接静态库即可，无需关心内部实现。

```
wake_word_detection_sdk_builder_demo/            # 参考工程
├── CMakeLists.txt                               # 顶层工程
├── sdkconfig.defaults.esp32s3                   # PSRAM / 看门狗等必要配置
├── components/
│   └── wake_word_detection_sdk/                 # 唤醒 SDK（预编库）
│       ├── CMakeLists.txt                       #   链接静态库
│       ├── include/                             #   公开头（接入方只需关心这里）
│       │   ├── feature_pipeline.h               #     喂音频 AcceptWaveform + 特征窗口
│       │   ├── recognize_commands.h             #     流式后处理（滑窗最大值 + 阈值 + 抑制期）
│       │   ├── offline_decode.h                 #     离线后处理（多窗口合并 + 逐帧触发解码）
│       │   ├── model.h                          #     模型数据 g_model 声明
│       │   └── micro_model_settings.h           #     音频/模型参数（采样率、帧长、类别等）
│       └── lib/
│           └── esp32s3/
│               └── libwake_word_detection_sdk.a #   预编译静态库
├── main/                                        # 应用示例代码
│   ├── main.cc                                  #   app_main 入口，创建 48KB 栈的 tensorflow 任务
│   ├── main_functions.h/.cc                     #   示例 API：setup() / ProcessFileOffline()
│   ├── command_responder.h/.cc                  #   唤醒回调，默认打印日志（业务在此替换）
│   ├── CMakeLists.txt
│   └── idf_component.yml                        #   依赖 esp-tflite-micro
└── test_data/
    ├── CMakeLists.txt                           # EMBED_FILES 把 4 个 wav 编入固件
    ├── seg_001.wav / seg_002.wav / seg_003.wav  # 含唤醒词（正样本）
    └── sample_004.wav                           # 背景噪声（负样本）
```

接入方通常只需要关心三处：`include/feature_pipeline.h`（喂音频）、`include/recognize_commands.h` 或 `include/offline_decode.h`（拿结果解码）、`main/command_responder.cc`（拿结果做业务）。初始化/推理入口示例见 `main/main_functions.cc` 的 `setup()`。

---

## 2. 初始化

唯一的初始化入口是 `setup()`（`main/main_functions.h` 的 `setup()`），无参数、无返回值，内部完成：

1. `tflite::GetModel(g_model)` 并校验 schema 版本；
2. 注册 16 个算子（`MicroMutableOpResolver<16>`：Conv2D / DepthwiseConv2D / FullyConnected / Softmax / Logistic / Transpose / Slice / Concatenation 等）；
3. 创建 `MicroInterpreter`，arena 为 `512KB`，通过 `EXT_RAM_BSS_ATTR` 放在 PSRAM；
4. `AllocateTensors()`；
5. 校验输入张量：`float32`，元素总数必须为 1600（`[1,1600]` 或 `[1,40,40]` 都接受）；
6. 把除 input(0) 之外的所有输入张量（模型的 cache / hidden state）清零；
7. 构造 `FeaturePipeline` 与 `RecognizeCommands`。

调用方式（见 `main/main.cc`）：

```c
#include "main_functions.h"

static void tf_main(void *arg) {
    setup();                       // 只调用一次
    for (int i = 0; i < 4; i++) {  // demo：跑 4 个内置 wav
        ProcessFileOffline(i);
    }
    vTaskDelete(NULL);
}

extern "C" void app_main(void) {
    // 栈至少 48KB：Fbank 与解释器调用栈较深
    xTaskCreate(tf_main, "tensorflow", 48 * 1024, NULL, 8, NULL);
    vTaskDelete(NULL);
}
```

注意事项：

- 必须在独立任务里跑，栈 ≥ 48KB，`app_main` 默认栈不够；
- `AllocateTensors()` 在本模型上通常很快完成（实测约几十毫秒）；若换更大的模型可能较慢；
- `setup()` 失败时不会崩溃，但 `interpreter` / `feature_provider` 保持为 `nullptr`，后续调用会直接返回，请看串口日志（TAG=`tf_main`）定位；
- PSRAM 必须打开（`CONFIG_SPIRAM_ALLOW_BSS_SEG_EXTERNAL_MEMORY=y`），否则 512KB arena 放不进内部 SRAM。

---

## 3. 输入音频

### 3.1 音频格式（硬性要求）

参数集中定义在 `include/micro_model_settings.h`，与训练侧 `dataset_conf.fbank_conf` 一致，不要单独改：

- 采样率：16000 Hz
- 位宽 / 声道：16bit 有符号小端、单声道（`int16_t` 裸 PCM，不带 WAV 头）
- 帧长 / 帧移：25ms（400 采样）/ 10ms（160 采样）
- Fbank 维度：40 mel bins，预加重 0.97，dither 关闭
- 一次推理窗口：40 帧 → **6640 采样（415ms）**
  （`kMaxAudioSampleSize + (kFeatureCount-1) * 160 = 400 + 39*160`）

### 3.2 输入接口

```cpp
// include/feature_pipeline.h
bool FeaturePipeline::AcceptWaveform(const int16_t* wav, size_t wav_len);
void FeaturePipeline::Reset();
```

- `wav`：int16 PCM 指针；`wav_len`：采样点个数（不是字节数）。
- 返回 `true` 表示已凑够 40 帧并写满 `feature_data_`（1600 个 float），可以送去推理；返回 `false` 表示数据还不够，已缓存到内部残差（`remained_wav_`，上限 1024 采样）。
- 支持一次喂大块（例如 16000 个采样），内部按 wekws 逻辑消耗整数帧并保留残差。
- 每次开始一个独立窗口前调用 `Reset()` 清残差；连续流式喂数据时不要调用。

### 3.3 接入自己的音频源（麦克风 / 网络流）

Demo 的音频来自编入固件的 WAV（跳过 44 字节头，按 5 秒分块、400ms 步长切窗）。接自己的音频源时替换成如下循环即可，其余不用改：

注意 `feature_provider` / `feature_buffer` / `model_input_buffer` 目前定义在 `main_functions.cc` 的匿名命名空间里，外部拿不到，所以这段循环请直接写在 `main_functions.cc` 内（新增一个你自己的函数，或替换 `ProcessFileOffline`），或先把这几个符号导出。

```cpp
// mic_read() 返回 16kHz/16bit/mono 的 PCM
int16_t pcm[6640];
while (mic_read(pcm, 6640) == 6640) {
    feature_provider->Reset();
    if (!feature_provider->AcceptWaveform(pcm, 6640)) continue;

    for (int i = 0; i < kFeatureElementCount; i++) {
        model_input_buffer[i] = feature_buffer[i];   // float32，无需量化
    }
    if (interpreter->Invoke() != kTfLiteOk) continue;
    // → 见第 4 节取结果
}
```

工程里已带 `espressif/esp_codec_dev` 依赖（面向立创·实战派 ESP32-S3 的 ES7210 麦克风），但当前代码未接麦克风采集，需要自行补 I2S/codec 初始化。

---

## 4. 获取唤醒结果

模型输出张量 `interpreter->output(0)` 为 `float32`，形状 `[1, 40, 1]`：40 个时间帧 × 1 个类别（`LINGXILINGXI`）的概率，每帧对应 10ms。`<FILLER>`（负样本）不是独立输出通道。

有两套后处理，按场景选一套。

### 4.1 流式：RecognizeCommands + RespondToCommand

```cpp
// include/recognize_commands.h — RecognizeCommands::ProcessLatestResults()
const char* found_command = nullptr;
float score = 0, max_prob = 0;
bool is_new_command = false;
recognizer->ProcessLatestResults(output, current_time_ms,
                                 &found_command, &score,
                                 &is_new_command, &max_prob);
RespondToCommand(current_time_ms, found_command, score, is_new_command, max_prob);
```

- 对窗口内 40 帧做 max-over-time pooling，取最大帧概率；
- 默认参数：`average_window_duration_ms=1000, detection_threshold=0.5, suppression_ms=2000, minimum_count=1`；
- `is_new_command=true` 即为一次唤醒（超阈值且不在 2 秒抑制期内），`score` 是本次窗口最大概率，`max_prob` 是历史全局最大概率；
- `current_time_ms` 必须单调递增，否则返回 `kTfLiteError`。

业务替换点是 `main/command_responder.cc` 的 `RespondToCommand()`，当前实现只在唤醒时打印：

```
detected: True
max_prob: 0.987654321
events: [{'frame': 123, 'timestamp_s': 1.230000, 'score': 0.987654321}]
```

### 4.2 离线整段音频：ProcessFileOffline + offline::DecodeOutputs

`bool ProcessFileOffline(int file_idx)`（`file_idx` 0~3 对应 3 个唤醒样本 + 1 个噪声样本）跑完整段音频并返回是否唤醒。内部把各窗口输出按帧对齐合并（重叠帧取最大值），再用 `offline::DecodeOutputs()` 解码，参数与 Python 侧 `decode_outputs` 对齐：

```cpp
// include/offline_decode.h
DecodeParams { threshold = 0.5f; window_size = 10; trigger_level = 1; min_event_gap_s = 2.0f; }

DecodeResult {
  bool  detected;      // 是否唤醒（num_events > 0）
  float max_prob;      // 全段逐帧最大概率
  int   num_events;    // 触发事件数，上限 10
  WakeEvent events[10];// { int frame; float timestamp_s; float score; }
}
```

串口输出形如（TAG=`tf_main`，与 Python `compare_onnx_tflite_output.py` 对齐，便于逐位比对）：

```
I (xxxxx) tf_main: ------------ seg_001 (offline) ------------
I (xxxxx) tf_main: audio_duration_ms: 2800
I (xxxxx) tf_main: outputs_windows: 7
I (xxxxx) tf_main: detected: True
I (xxxxx) tf_main: wake_word: LINGXILINGXI
I (xxxxx) tf_main: max_prob: 0.999089
I (xxxxx) tf_main: events: 1
I (xxxxx) tf_main:   [0] frame=159 timestamp_s=1.590000 score=0.731059
```

预期结果：`seg_001/002/003` → `detected: True`；`bg_noise` → `detected: False`。这也是验证接入是否正确的最快方式。

调阈值的位置见第 5 节。

---

## 5. 可设置的参数

所有参数都是编译期常量，没有运行时配置接口，改完需要重新 build。下面这组只影响判决松紧，不涉及模型和特征，是接入时唯一需要调的参数。

**只推荐调 `threshold` 和 `trigger_level` 这两个，两者都是值越小越灵敏（唤醒越容易，误唤醒也越多），值越大越保守。其余参数保持默认。**

离线解码，在 `main/main_functions.cc` 的 `ProcessFileOffline()` 里构造 `DecodeParams` 处：

- `params.threshold = 0.5f` — **推荐调整**。逐帧触发阈值，越小越灵敏。最常动的就是这个。
- `params.trigger_level = 1` — **推荐调整**。窗口内需要几帧超阈值才算一次事件，越小越灵敏。设为 1 表示单帧命中即触发（当前最灵敏）；设 2~3 可显著压误唤醒，代价是漏检上升。
- `params.window_size = 10` — 触发帧要落在多少帧（10 帧 = 100ms）的窗口内。保持默认。
- `params.min_event_gap_s = 2.0f` — 两次事件的最小间隔，防止一次说话被拆成多次上报。保持默认。

流式判决，改 `RecognizeCommands` 的构造参数（`include/recognize_commands.h` 的 `RecognizeCommands` 构造函数默认值）：

- `detection_threshold = 0.5f` — **推荐调整**。同上，窗口最大概率的阈值，越小越灵敏。
- `suppression_ms = 2000` — 一次唤醒后的抑制期，期内不再上报。保持默认。
- `average_window_duration_ms = 1000` — 历史结果保留时长，超时的结果会被丢弃。保持默认。
- `minimum_count = 1` — 队列中至少多少条结果才开始判决，用于避免刚启动时的抖动。保持默认。

注意这两套阈值语义相近但不共享，谁生效取决于你走离线还是流式路径，别只改一边。

---

## 6. 编译、烧录、监视

### 6.1 环境

- ESP-IDF v5.x（工程用 `idf_component.yml` 托管依赖，首次 build 会自动拉取 `esp-tflite-micro`、`esp_codec_dev`）
- 目标芯片：`esp32s3`，Flash ≥ 2MB（分区表 `partitions_singleapp_large.csv`，app 分区 1.5MB）

### 6.2 命令

```bash
. $IDF_PATH/export.sh          # Windows: %IDF_PATH%\export.bat
idf.py set-target esp32s3      # 会自动应用 sdkconfig.defaults.esp32s3
idf.py build
idf.py -p COM5 flash monitor   # Linux/macOS 换成 /dev/ttyUSB0 等
```

- 只监视：`idf.py -p COM5 monitor`，退出快捷键 `Ctrl+]`
- 改了 sdkconfig 默认值后：`idf.py fullclean && idf.py build`

### 6.3 关键 sdkconfig（缺一不可）

```
CONFIG_SPIRAM=y
CONFIG_SPIRAM_MODE_OCT=y
CONFIG_SPIRAM_SPEED_80M=y
CONFIG_SPIRAM_ALLOW_BSS_SEG_EXTERNAL_MEMORY=y   # 512KB tensor_arena 放 PSRAM
CONFIG_PARTITION_TABLE_SINGLE_APP_LARGE=y       # app 分区 1.5MB，否则约 1.1MB 固件放不下
CONFIG_ESP_TASK_WDT_TIMEOUT_S=60                # 合法范围 1~60 秒；单次 Invoke 可达数秒
```

### 6.4 常见问题

- 串口停在 `AllocateTensors start`：通常很快就过；若长时间（超过 1 分钟）无输出检查 PSRAM 配置。
- `Bad input tensor: ... elements=N (expected 1600)`：模型输入维度与 `kFeatureCount * kFeatureSize` 不一致。
- `Could not add XXX op`：模型用到了未注册的算子，在 `setup()` 里补 `micro_op_resolver.AddXXX()`。
- 触发任务看门狗：确认 `CONFIG_ESP_TASK_WDT_TIMEOUT_S` 已生效，且自己的推理循环里有 `vTaskDelay()` 让出 CPU。
- 固件超出分区：模型 345KB + 4 个 wav 约 350KB，若换更大模型请改用更大 Flash 或自定义分区表。

---

## 7. API 参考

唤醒 SDK 的公开接口集中如下（详细语义见第 3、4 节）。

### FeaturePipeline（include/feature_pipeline.h）
| 接口 | 签名 | 说明 |
|---|---|---|
| AcceptWaveform | `bool AcceptWaveform(const int16_t* wav, size_t wav_len)` | 喂 int16 PCM（wav_len=采样点数），凑够 40 帧返回 true 并写满特征 |
| Reset | `void Reset()` | 清内部残差，开始一个独立窗口前调用 |

### RecognizeCommands（include/recognize_commands.h，流式）
| 接口 | 签名 |
|---|---|
| 构造 | `RecognizeCommands(int32_t average_window_duration_ms=1000, float detection_threshold=0.5f, int32_t suppression_ms=2000, int32_t minimum_count=1)` |
| ProcessLatestResults | `TfLiteStatus ProcessLatestResults(const TfLiteTensor* latest_results, int32_t current_time_ms, const char** found_command, float* score, bool* is_new_command, float* max_prob)` |

### offline（include/offline_decode.h，离线）
| 接口/结构 | 定义 |
|---|---|
| DecodeOutputs | `void DecodeOutputs(float* frame_scores, int* num_total_frames, const float* const* outputs, int num_windows, int window_stride_frames, int wakeword_class_index, const DecodeParams& params, DecodeResult* result)` |
| DecodeParams | `{ float threshold=0.5; int window_size=10; int trigger_level=1; float min_event_gap_s=2.0; }` |
| DecodeResult | `{ bool detected; float max_prob; int num_events; WakeEvent events[10]; }` |
| WakeEvent | `{ int frame; float timestamp_s; float score; }` |

### 应用示例（main/main_functions.h）
| 接口 | 签名 | 说明 |
|---|---|---|
| setup | `void setup()` | 初始化模型/解释器/特征管道，只调一次 |
| ProcessFileOffline | `bool ProcessFileOffline(int file_idx)` | 跑内置 wav（0~3），返回是否唤醒 |

---

## 8. ABI 兼容性注意事项

唤醒 SDK 以预编译静态库 `libwake_word_detection_sdk.a` 交付，接入方编译环境须与编库环境一致，否则可能链接失败或运行崩溃：

- ESP-IDF 版本：编库用 v5.4.3，须同大版本且 ≥ 此版本
- 编译器：xtensa-esp-elf 14.2.0，须一致
- C++ 标准：须与编库一致
- 目标芯片：自动匹配 `lib/${IDF_TARGET}/libwake_word_detection_sdk.a`；当前仅提供 esp32s3，其它芯片需重新编库
- 关键 sdkconfig（PSRAM/分区/看门狗）见第 6.3 节

---

## 9. 更新日志

### [0.9.0] - 2026-08-26
- 首个交付版本：ESP32-S3「灵犀灵犀」离线唤醒参考实现
- 单类别检测模型：输入 [1,40,40]（1600 float32），输出 [1,40,1]（40 帧概率）
- 提供流式 / 离线两套后处理
- 模型 + 4 个测试音频（3 正样本 + 1 噪声）编入固件，开箱验证

未验证说明：本文档中的参数、接口签名、日志格式均取自当前代码；`idf.py` 命令未在本机执行（本环境无 ESP-IDF 与开发板），请在实际环境中确认串口号与 IDF 版本。
