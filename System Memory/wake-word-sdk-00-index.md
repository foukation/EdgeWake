# 系统记忆：语音唤醒两模块 · 通用经验索引

> 记录时间：2026-08-28 · 来源：AI 系统记忆（user memory + repo memory）
> 明细见姊妹篇：`wake-word-sdk-01-builder.md`（编库）/ `wake-word-sdk-02-demo.md`（demo）

---

## 1. 两套唤醒方案辨析（易混，先分清）

| | A. TFLite Micro 自训模型（本项目） | B. ESP-SR 官方（`esp-sr_wake_word_sdk_builder/`） |
|---|---|---|
| 自定义唤醒词 | ✅ 唯一能"完全自定义训练模型"的路 | ❌ Wakenet 不能自训（乐鑫付费定制） |
| Multinet 命令词 | — | ⚠️ 可配词免训练但不准（本职是唤醒后命令词，无抗误触发，单独当主唤醒漏检+误唤醒双高） |
| 结构 | WakeNet 等价物：PCM→Fbank→TFLite Micro→解码 | Wakenet + MultiNet + AFE；三实现按芯片（esp32→纯 wakenet；s3/p4→AFE+wakenet+multinet），工厂 `CreateWakeWord()` |

## 2. 硬性接入规格（SDK 黑盒侧）

- 输入：**16kHz / 16bit 有符号小端 / 单声道**，int16 裸 PCM 不带 WAV 头；`AcceptWaveform` 的 len 参数=**采样点数**（非字节）。
- 一个推理窗口 = 40 帧 = **6640 采样（415ms）**；喂大块没问题（内部留 1024 采样残差），`Reset()` 只在开始独立窗口前调。
- 模型输入 float32 **1600** 个（不量化）；输出 [1,40,1] 40 帧概率，单类别 LINGXILINGXI。
- 必须独立任务 + **栈 ≥48KB**；tensor arena 512KB 必须 PSRAM。
- 每窗口推理间要有 `vTaskDelay(1)`，否则看门狗告警（IDLE0 喂不了狗）。
- 接麦克风：`esp_codec_dev` 已在依赖里（ES7210），但**采集代码 demo 未实现**，需自行补 I2S/codec 初始化后按"Reset→AcceptWaveform→Invoke→取输出"循环接入。

## 3. 踩坑清单（按阶段）

- **EMBED_FILES 中文文件名**：Windows 汇编器 `can't create *.wav.S.obj: Invalid argument` → wav 一律纯英文名（seg_001.wav 等）。
- **托管组件 target 名**：`esp-tflite-micro` 的 CMake target 是 `__idf_espressif__esp-tflite-micro`（带 `espressif__` 前缀），写错报 `No target`。
- **sdkconfig.defaults 机制**：基准文件（无后缀）不存在 → `.esp32s3` 陪读**永不被读**；`fullclean` 不删 sdkconfig；强制重读 = del sdkconfig → set-target → build。
- **WDT 超时上限**：`CONFIG_ESP_TASK_WDT_TIMEOUT_S` 合法范围 1~60，写 240 被 Kconfig 静默拒绝退回 5（"diff 显示改了但没生效"的真因）。
- **DRAM overflow ≈281416B** = PSRAM 没开、512KB arena 掉内部内存的特征签名。
- **app partition too small**：固件 ~1.10MB > 1MB 默认 factory → `CONFIG_PARTITION_TABLE_SINGLE_APP_LARGE=y`（1.5MB）。
- **在仓库根目录误编译**：撞根工程 freetype `.component_hash/CHECKSUMS.json` 损坏——与本 SDK 无关，必须先 cd 进工程子目录。
- **工具链 cc1 丢失**：`cannot execute 'cc1'` = 工具链目录残缺，`idf_tools.py install xtensa-esp-elf` 补装；防杀软误删。

## 4. 文档纪律（对外交付强约束）

- demo 目录及协议文档 = **给厂商的黑盒**：不得出现源码内部路径（src/model.cc、xxd 换模型流程）、编库细节、百度内部痕迹（BCLOUD/ci.yml/百度群）。
- 指引代码位置用**函数/结构体名**而非写死行号；示例数字用真机实测值（如 30ms AllocateTensors、frame=159）。
- 版本：**v0.9.0**（2026-08-26，首个交付版，用户定名）；ABI 要求写明 IDF v5.4.3 / xtensa-esp-elf 14.2.0。
- 唤醒词标签唱名 **LINGXILINGXI**（"灵犀灵犀"）——模型是单类别检测器，标签只是显示牌，但必须写对词。

## 5. 验证基线（回归用）

| 项 | 期望 |
|---|---|
| demo 4 段离线识别 | seg_001/002/003 → detected:True (max_prob≥0.73)；sample_004 → detected:False |
| seg_001 典型值 | audio_duration_ms≈2800 / outputs_windows=7 / event frame=159, score≈0.73 |
| 固件体积 | ≈1.10MB，SINGLE_APP_LARGE 1.5MB 分区剩 ~28% |
| .a 体积 | ≈379KB（strip 后），路径 `lib/esp32s3/libwake_word_detection_sdk.a` |

---
*本文件为 AI 记忆快照，供跨会话恢复上下文。*
