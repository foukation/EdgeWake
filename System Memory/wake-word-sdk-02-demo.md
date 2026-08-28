# 系统记忆：wake_word_detection_sdk_builder_demo（demo 工程）

> 记录时间：2026-08-28 · 来源：AI 系统记忆（user memory + repo memory）与磁盘实态交叉核对
> 姊妹篇：`wake-word-sdk-01-builder.md`（编库工程）· 参考模式：`ai_sdk_builder_demo`

---

## 1. 定位与分工

| 项 | 内容 |
|---|---|
| 目录 | `e:\github\ESP32-RTOS-AI-SDK\wake_word_detection_sdk_builder_demo\` |
| 角色 | **demo 工程**（对标 `ai_sdk_builder_demo`）：引用 builder 产出的**预编译静态库**，跑离线识别验证，是对外交付参考 |
| 引用方式 | `components/wake_word_detection_sdk/` = **无源码预编库组件**（src/ 不存在，源码保护）；stub 占位源 + `target_link_libraries(INTERFACE -Wl,--start-group lib/${IDF_TARGET}/libwake_word_detection_sdk.a $<TARGET_FILE:__idf_espressif__esp-tflite-micro> -Wl,--end-group)` |
| 顶层工程名 | `project(wake_word_detection_sdk_demo)` |
| 入口 | `main/main.cc`：`app_main` → `xTaskCreate(tf_main, "tensorflow", 48*1024, prio 8)` → `setup()` 一次 + 循环 `ProcessFileOffline(0..3)` |
| 对外文档 | `灵犀AI语音唤醒SDK接口协议文档.md`（9 章完整版 v0.9.0，即原 INTEGRATION.md 校订后并入；**黑盒视角，不得暴露源码/编库信息**） |

### 🐞 链接目标名大坑（已踩已修）
esp-tflite-micro 是**托管组件**，CMake target 带 `espressif__` 前缀：正确写法 `$<TARGET_FILE:__idf_espressif__esp-tflite-micro>`；写成 `__idf_esp-tflite-micro` 报 `No target`。

## 2. 目录结构（当前实态）

```
wake_word_detection_sdk_builder_demo/
├── CMakeLists.txt / sdkconfig / sdkconfig.defaults / sdkconfig.defaults.esp32s3
├── 灵犀AI语音唤醒SDK接口协议文档.md        # 对外协议文档 v0.9.0（9章）
├── components/wake_word_detection_sdk/
│   ├── include/（5 公开头，与 builder 侧保持一致拷贝）
│   ├── lib/esp32s3/libwake_word_detection_sdk.a   # 从 builder 复制
│   └── CMakeLists.txt（stub + 链接预编库）
├── main/
│   ├── main.cc                 # app_main：创建 48KB 栈 tensorflow 任务（Fbank/解释器调用栈深，app_main 默认栈不够）
│   ├── main_functions.{h,cc}   # setup() / ProcessFileOffline(int file_idx)
│   ├── command_responder.{h,cc}# RespondToCommand()：流式路径业务替换点（当前离线 demo 未调用，用户决定保留当模板）
│   ├── CMakeLists.txt          # SRCS 3 文件；PRIV_REQUIRES 须含 wake_word_detection_sdk
│   └── idf_component.yml       # esp-tflite-micro ^1.3.5 + esp_codec_dev（零引用，先留）
└── test_data/                  # EMBED_FILES 编入 4 个 wav + CMakeLists.txt
    ├── seg_001.wav / seg_002.wav / seg_003.wav   # 正样本（含"灵犀灵犀"）
    └── sample_004.wav          # 负样本背景噪声（代码里叫 bg_noise）
```

已清理：SDK 源码 13 文件（删出 main/）、百度内部文件 BCLOUD/ci.yml、空模板 README.md（用户删）、死代码 `loop()`/`LatestAudioTimestamp()`、悬空 `extern g_current_test_file`。

## 3. setup() 初始化流程（main_functions.cc）

1. `tflite::GetModel(g_model)` + schema 版本校验；
2. `MicroMutableOpResolver<16>` 注册 16 算子（FullyConnected/Reshape/Softmax/DepthwiseConv2D/Conv2D/AvgPool/MaxPool/Add/Sub/Mul/Quantize/Dequantize/Transpose/Slice/Concatenation/Logistic）——**模型用到未注册算子会报 `Could not add XXX op`**；
3. `tensor_arena` = **512KB**，`EXT_RAM_BSS_ATTR` 放 PSRAM（须 `CONFIG_SPIRAM_ALLOW_BSS_SEG_EXTERNAL_MEMORY=y`）；
4. `AllocateTensors()`：本模型实测很快（日志 I(1262)→I(1292) ≈30ms；曾误传 10~30s，文档已修）；
5. 输入张量校验：float32、元素总数 1600（[1,1600] 或 [1,40,40] 都收），否则 `Bad input tensor`；
6. input(1..) 状态张量（cache/hidden state）清零；换窗口/重跑文件前也要重新清零（与 Python reset_states 一致）；
7. 构造 `FeaturePipeline`（`kFeatureElementCount`, `feature_buffer`）与 `RecognizeCommands`（默认参数）。

## 4. 离线链路 ProcessFileOffline()（方案A重写，wav 解析在 demo 层）

- **内嵌符号**（EMBED_FILES 命名规则：`.`/`-`→`_`）：`_binary_seg_001_wav_start/_end` 等 4 组（原中文名 wav 在 Windows 汇编器报 `can't create .obj: Invalid argument` → 已重命名纯英文）。文件大小用 `end-start` 自动算，不写死字节数。
- 常量推导自公开头：`kMinSamplesForFeatures = 400 + 39*160 = 6640`；`kChunkSamples = 5*16000`；`kWindowStrideSamples = 400ms*16 = 6400`；WAV 头 44 字节。
- 走法：分块(5s) → 块内非重叠窗(400ms 步长， foremost 40帧) → 每窗 `Reset()` + `AcceptWaveform(audio_ptr, 6640)` 提特征 → 拷进 `model_input_buffer` → `Invoke()` → 收输出 `[1,40,1]` 存 output_buffer（最多 64 窗）。
- **每窗口循环里有 3 处 `vTaskDelay(pdMS_TO_TICKS(1))` 让出 CPU**（喂特征前/Invoke 前/Invoke 后）——无延时会让 IDLE 喂不了狗。
- 解码：`offline::DecodeOutputs(frame_scores, &num_total_frames, output_ptrs, num_windows, stride=40帧, class_index=0, params, &result)`（多窗重叠帧取 max 合并 → 阈值触发解码）。默认 params：threshold=0.5 / window_size=10 / trigger_level=1 / min_event_gap_s=2.0。
- 输出格式对齐 Python `compare_onnx_tflite_output.py`：audio_duration_ms / outputs_windows / detected / wake_word(`kCategoryLabels[0]`，**2026-08-26 已由 XIAODUXIAODU 改名 LINGXILINGXI**) / max_prob / events[frame,timestamp_s,score]。

## 5. 判决参数调优（两套阈值不共享，按路径二选一调，值越小越灵敏/误唤醒越多）

| 路径 | 位置 | 推荐调 | 保持默认 |
|---|---|---|---|
| 离线 | `ProcessFileOffline` 里的 `DecodeParams` | `threshold=0.5`、`trigger_level=1` | `window_size=10`、`min_event_gap_s=2.0` |
| 流式 | `RecognizeCommands` 构造参数 | `detection_threshold=0.5` | `suppression_ms=2000`、`average_window_duration_ms=1000`、`minimum_count=1` |

`ProcessLatestResults` 的 `current_time_ms` **必须单调递增**，否则返回 kTfLiteError。

## 6. 关键 sdkconfig（demo 侧，2026-08-26 三坑全修后的固化态）

- 基准 `sdkconfig.defaults`（含 WDT60 + SINGLE_APP_LARGE）+ `.esp32s3`（PSRAM 现代符号 Octal/80M/ALLOW_BSS）。
- `CONFIG_PARTITION_TABLE_SINGLE_APP_LARGE=y`：固件 `0x10dce0`(≈1.10MB) 塞不进默认 SINGLE_APP 1MB factory，须 1.5MB 大分区；Flash 保持 **2MB**（与 git 历史 9a0dbd5 一致，未改 16MB）。
- DRAM 溢出 `281416 bytes` = 512KB arena 掉进内部 DRAM 的特征签名 → 一定是 PSRAM 没开。
- 改 defaults 后：`del sdkconfig` → `idf.py set-target esp32s3` → `idf.py build`。

## 7. 烧录验证结果（2026-08-25，COM8，✅通过）

- `seg_001/002/003` → `detected: True`（max_prob 0.99+，wake_word: LINGXILINGXI）；`bg_noise` → `detected: False`。
- ⚠️ 每段推理后出 **Task Watchdog 警告**（非崩溃）：`task_wdt: IDLE0` + `tensorflow` 栈顶在 Invoke——单次 Invoke 占满 CPU0 数秒所致；PANIC 未开只打红字不重启；60s 超时配置已固化（注意 Kconfig 上限 60）。

---
*本文件为 AI 记忆快照，供跨会话恢复上下文；与 builder 侧记忆（01 号文件）配套阅读。*
