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

## 6. 后续方向（2026-09-28 起草 / 2026-09-29 定案）

- **需求：语音唤醒 SDK 内置"音频驱动"**，让 `.a` 库能自己驱动麦克风采集做唤醒，不只被动收 PCM。**厂商可选择用或不用**（SDK 交付给众多厂商、设备各异，驱动不能绑死单一芯片）。
- **两种用法并存（厂商二选一）**：
  1. **用内置音频驱动**：厂商填 `AudioConfig`（选驱动类型 + 传引脚/地址）→ SDK 自采集 16k/16bit/单声道 → 特征 → Invoke → 命中回调。
  2. **不用**：走原 `AcceptWaveform(int16*, len)` 自己喂 PCM（永久兜底，任何硬件通用）。
- **✅ 定案（2026-09-29）：直接把 `ai_sdk_builder` 已适配的整套音频栈移植进唤醒 SDK**——`AudioHardwareType` 全部 **9 种驱动** + `AudioCodec` 基类 + `AudioInput` 采集层 + `CreateAudioCodec()` 工厂 + `AudioConfig`。第一版**全部 9 种都移植**（含 Dummy）。
  - 9 种：`kEs8311`(最常用) / `kEs8388`(AEC) / `kEs8374` / `kEs8389` / `kBoxAudioCodec`(ES8311+ES7210 TDM 麦阵列) / `kNoCodecDuplex` / `kNoCodecSimplex`(如 INMP441) / `kNoCodecSimplexPdm`(PDM 麦) / `kDummy`(空实现占位/调试，不产音频、不会真唤醒，30 行零依赖，留着无害)。
  - 覆盖总线：I2S 标准 / I2S TDM / I2S PDM；有 Codec(需 I2C 1~5) / 无 Codec 直连(6~8)。
  - 源位置：`ai_sdk_builder/components/ai_sdk/`：`include/ai_sdk/audio/audio_config.h`(枚举+AudioConfig+工厂声明) / `src/audio/audio_config.cc`(工厂) / `src/audio/codecs/*.{h,cc}`(各驱动) / `src/audio/audio_input.{h,cc}`(采集层, 重采样→16k+转单声道, 按块 5120B/160ms 回调) / `include/.../audio/audio_codec.h`(基类, 虚函数 `Read(int16_t*,samples)`/`Write`)。
- **设计约束**：引脚/地址**不可写死进库**，必须由 `AudioConfig` 参数传入。
- **连带改动**：组件 `CMakeLists.txt` 的 `REQUIRES`（现仅 `esp-tflite-micro`）需加 `esp_codec_dev` + `esp_codec_dev_defaults` + `driver`(i2s_std/i2s_pdm/i2s_tdm/i2c_master)；重编库 → `.a` 变大；demo 改调新 API。ESP-IDF v5.0+（PDM 需芯片 `SOC_I2S_SUPPORTS_PDM_RX`，S3 支持）。
- 流式判决参数：`RecognizeCommands(avg_window=1000, threshold=0.5, suppression=2000, min_count=1)`；`ProcessLatestResults` 的 `current_time_ms` 必须单调递增。
- **状态：方向+范围已定案（全 9 种移植），尚未动手。用户明确"先不要修改文件"，详细实施方案待定稿→用户批准后再改。**

---
*本文件为 AI 记忆快照，供跨会话恢复上下文。*
