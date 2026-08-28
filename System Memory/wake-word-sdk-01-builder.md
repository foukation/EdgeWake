# 系统记忆：wake_word_detection_sdk_builder（编库工程）

> 记录时间：2026-08-28 · 来源：AI 系统记忆（user memory + repo memory）与磁盘实态交叉核对
> 姊妹篇：`wake-word-sdk-02-demo.md`（demo 工程）· 参考模式：`ai_sdk_builder` / `ai_sdk_builder_demo`

---

## 1. 定位与分工

| 项 | 内容 |
|---|---|
| 目录 | `e:\github\ESP32-RTOS-AI-SDK\wake_word_detection_sdk_builder\` |
| 角色 | **编库工程**（对标 `ai_sdk_builder`）：编译产出预编译静态库，不对接真实硬件业务 |
| 产物 | `components/wake_word_detection_sdk/lib/${IDF_TARGET}/libwake_word_detection_sdk.a`（POST_BUILD 自动 copy + `--strip-debug`），`build_all.ps1` 另备份到 `output/${chip}/` |
| 入口 | `main/main.c` = 空壳 `app_main()`，仅满足 ESP-IDF 构建系统，无需实际运行 |
| 芯片 | 当前仅 **esp32s3**（`build_all.ps1` 的 `$chips` 只列 esp32s3；`esp32p4` 预留在 `$previewChips`，需 `--preview`） |
| 顶层工程名 | `project(wake_word_detection_sdk_builder)` |

## 2. 核心组件 `components/wake_word_detection_sdk/`（源码在 builder 侧，demo 侧无）

```
components/wake_word_detection_sdk/
├── CMakeLists.txt          # SRCS 6 源 + INCLUDE_DIRS include + PRIV_INCLUDE_DIRS src/include + REQUIRES esp-tflite-micro
│                           # POST_BUILD: mk lib/${IDF_TARGET}/ → copy .a → strip --strip-debug
│                           # -Wno 抑制: maybe-uninitialized/missing-field-initializers/sign-compare/double-promotion/type-limits
├── include/                # 公开 API（接入方只看这里，5 个头，无前缀）
│   ├── feature_pipeline.h      # class FeaturePipeline: AcceptWaveform(int16*,len)→bool / Reset()；内部残差 remained_wav_[1024]
│   ├── recognize_commands.h    # 流式后处理：RecognizeCommands + PreviousResultsQueue(kMaxResults=50)
│   ├── offline_decode.h        # 离线后处理：namespace offline { DecodeOutputs / DecodeParams / DecodeResult / WakeEvent }
│   ├── model.h                 # extern g_model[] / g_model_len（xxd 转出的 tflite FlatBuffer C 数组）
│   └── micro_model_settings.h  # 全部编译期常量（见 §4）
├── src/                    # 实现：feature_pipeline.cc / recognize_commands.cc / offline_decode.cc / fbank.cc / fft.cc / model.cc
│   └── include/            # 私有头：fbank.h / fft.h（对外不可见）
└── lib/
    └── esp32s3/libwake_word_detection_sdk.a   # ≈379KB（strip 后；build 树内 523KB）
```

- **注意**：`README.md` 是百度空模板（"tflitemicro-esp32s3／百度Hi讨论群：XXXX"）——**用户明确要求保持不动**（demo 侧同名 README 已由用户自行删除）。
- 历史渊源：原目录名 `tflitemicro-esp32s3`（已改名），百度内部工程（曾有 BCLOUD/ci.yml，builder 侧重构时已删）。

## 3. 依赖（main/idf_component.yml）

```yaml
dependencies:
  espressif/esp-tflite-micro: ^1.3.5          # 必需（源码用 tflite）
  espressif/esp_codec_dev: ">=1.4.0,<1.5.0"   # SDK 零引用，ES7210 麦克风 demo 遗留——用户决定"先留不删"
```

## 4. 模型与音频参数（include/micro_model_settings.h，全部 constexpr，与训练侧 dataset_conf.fbank_conf 一致，禁止单独改）

| 参数 | 值 | 备注 |
|---|---|---|
| 采样率 `kAudioSampleFrequency` | 16000 Hz | |
| Fbank 维度 `kFeatureSize` | 40 mel bins | |
| 帧长/帧移 | 25ms(400采样) / 10ms(160采样) | `kFeatureDurationMs`/`kFeatureStrideMs` |
| 推理窗口 `kFeatureCount` | 40 帧 → **1600 float32**（[1,40,40] 或 [1,1600]) | 一个窗口=6640 采样=415ms |
| Fbank 细节 | 预加重 0.97，dither 0（关），log 下界 1.2e-7 | `kFbankPreemphCoeff` 等 |
| 输出 | **[1,40,1]**：40 帧 × 1 类别概率 | `<FILLER>` 是负样本标识 ID=-1，**不是输出通道** |
| `kCategoryCount` / 标签 | **1** / `"LINGXILINGXI"` | 单类别检测器，detected=True 必是唯一词；标签是显示牌 |
| 分块参数 | `kChunkSeconds=5`，`kWindowStrideMs=400`（=40 帧，非重叠窗） | 与 Python `kws_tflite._iter_windows` 对齐 |
| 模型体积 | ≈345KB tflite FlatBuffer（g_model 在 model.cc，编译进库） | |

## 5. 关键 sdkconfig（builder 侧，2026-08-26 已修正齐）

- **基准 `sdkconfig.defaults`（无后缀）必须存在** —— IDF 的 defaults 读取规则：未设 `SDKCONFIG_DEFAULTS` 时只读基准文件；基准缺失则 `.esp32s3` 陪读文件**永不被读**（曾因缺基准导致 defaults 全部失效）。
- `.esp32s3` 已改现代符号：~~`CONFIG_ESP32S3_SPIRAM_SUPPORT`~~（IDF4.x 废弃）→ `CONFIG_SPIRAM=y` + `CONFIG_SPIRAM_MODE_OCT=y` + `CONFIG_SPIRAM_SPEED_80M=y` + `CONFIG_SPIRAM_ALLOW_BSS_SEG_EXTERNAL_MEMORY=y`。
- `CONFIG_ESP_TASK_WDT_TIMEOUT_S=60`（放基准文件；**合法范围 1~60**，写 240 超范围会被 Kconfig 拒绝退回默认 5——这就是当年"defaults 生效了但 WDT 仍是 5"的真因）。
- `CONFIG_FREERTOS_CHECK_STACKOVERFLOW_CANARY=y`。
- 只产 .a 不影响功能，改这些纯为 demo/builder 一致性；改 defaults 后需 `del sdkconfig` → `idf.py set-target esp32s3` → build 才会重读。

## 6. 构建流程

- 单芯片：`cd wake_word_detection_sdk_builder` → `idf.py set-target esp32s3` → `idf.py build`（✅ 2026-08-25 实测编译通过）。
- 脚本：`.\build_all.ps1`（逐芯片 清 build → set-target → build → 校验 → 备份 output/${chip}/ → build 目录改名 build_${chip} 留档）。
- ⚠️ 必须先 **cd 进本工程目录**再编；在仓库根目录直接编会撞根工程 freetype 的 `.component_hash/CHECKSUMS.json` 损坏错误（与 SDK 无关的老坑，多次复现）。
- 环境：ESP-IDF **v5.4.3**，xtensa-esp-elf **14.2.0**（esp-14.2.0_20250730）。曾踩工具链不完整坑：`fatal error: cannot execute 'cc1'` = 工具链目录被截断（仅150MB），修法 `idf_tools.py install xtensa-esp-elf` 补装，注意杀软白名单。

## 7. ABI 交付约束（预编库对接方必须一致）

- ESP-IDF **同大版本且 ≥ v5.4.3**；编译器 **xtensa-esp-elf 14.2.0**；C++ 标准一致。
- 芯片自动匹配 `lib/${IDF_TARGET}/`；当前**只有 esp32s3**，其它芯片需在本工程先编库（esp32p4 走 `--preview`）。

---
*本文件为 AI 记忆快照，供跨会话恢复上下文；与 demo 侧记忆（02 号文件）配套阅读。*
