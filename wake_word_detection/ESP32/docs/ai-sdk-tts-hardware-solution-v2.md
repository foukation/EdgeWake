# ai-sdk TTS 播放功能 + 硬件适配方案

## 一、需求概述

**目标**：ai-sdk 静态库 (libai_sdk.a) 需要支持 TTS 音频播放，并适配多种硬件。

**使用场景**：ai-sdk 作为静态库发布给厂商使用，厂商只需配置引脚即可使用 TTS 播放功能。

---

## 二、方案对比

### 方案 A：一个静态库包含所有驱动（推荐）

```
libai_sdk.a 包含：
- TTS 播放器
- ES8311 驱动
- ES8388 驱动
- ES8374 驱动
- I2S 直连驱动
- ...
```

| 优点 | 缺点 |
|------|------|
| 厂商适配成本最低 | 静态库体积稍大 |
| 只需配置，不需要写代码 | |
| 维护简单，只有一个版本 | |
| 开箱即用 | |

### 方案 B：多个静态库版本

```
libai_sdk_es8311.a  - 只含 ES8311 驱动
libai_sdk_es8388.a  - 只含 ES8388 驱动
libai_sdk_i2s.a     - 只含 I2S 直连
...
```

| 优点 | 缺点 |
|------|------|
| 体积小 | 需要维护多个版本 |
| | 发布复杂 |
| | 厂商需要选择正确版本 |

### 方案 C：接口抽象（用户实现硬件层）

```
libai_sdk.a 包含：
- TTS 播放器
- IAudioOutput 接口（用户实现）
```

| 优点 | 缺点 |
|------|------|
| 体积最小 | 厂商需要写硬件代码 |
| 用户自由度最高 | 适配成本高 |

---

## 三、推荐方案 A 详细设计

### 3.1 体积分析

**Q**: 包含所有驱动，体积会很大吗？

**A**: 不会太大。原因：

1. **esp_codec_dev 组件已经包含常见硬件驱动**
   - ES8311, ES8388, ES7210, AW88298 等
   - 这个组件本身就是一个库

2. **链接器会优化**
   - 没使用的驱动代码不会被链接进最终固件
   - 只有用户实际调用的驱动会被包含

3. **预估增加体积**
   - TTS 播放器逻辑：~10KB
   - esp_codec_dev 驱动：~30KB
   - esp_audio_codec 解码器：~50KB
   - 总计：~90KB

### 3.2 支持的硬件类型

| 硬件类型 | 芯片 | 依赖组件 |
|----------|------|----------|
| Codec | ES8311 | esp_codec_dev |
| Codec | ES8388 | esp_codec_dev |
| Codec | ES8374 | esp_codec_dev |
| Codec | ES8389 | esp_codec_dev |
| 功放 | AW88298 | esp_codec_dev |
| 无芯片 | I2S 直连 | driver/i2s |
| 无芯片 | PDM | driver/i2s |

### 3.3 厂商使用方式

```cpp
// 厂商代码

// 1. 配置硬件类型和引脚
ai_sdk::AudioHardwareConfig config = {
    .type = ai_sdk::AudioHardwareType::ES8311,
    .mclk_pin = 0,
    .bclk_pin = 45,
    .ws_pin = 46,
    .dout_pin = 47,
    .din_pin = 48,
    .i2c_sda_pin = 1,
    .i2c_scl_pin = 2,
    .i2c_addr = 0x18,
    .pa_pin = 38,
    .sample_rate = 16000,
};

// 2. 初始化音频
asr.initAudio(config);

// 3. 启动对话
asr.start();
// TTS 自动播放
```

---

## 四、ai-sdk 需要添加的依赖

```yaml
# ai_sdk_builder/main/idf_component.yml
dependencies:
  # 现有依赖
  idf:
    version: '>=5.4.0'
  espressif/esp_websocket_client: '*'
  
  # 新增：TTS 播放相关
  espressif/esp_audio_codec: ~2.4.0   # MP3/Opus 解码
  espressif/esp_audio_effects: ~1.2.0 # 重采样
  espressif/esp_codec_dev: ~1.5       # 硬件驱动
```

---

## 五、ai-sdk 需要添加的文件

```
ai_sdk_builder/components/ai_sdk/
├── include/ai_sdk/
│   ├── audio_hardware_config.h   # 硬件配置结构体
│   ├── tts_player.h              # TTS 播放器
│   └── audio_hal.h               # 硬件抽象层
└── src/
    ├── tts_player.cc             # TTS 播放器实现
    ├── audio_hal.cc              # 硬件抽象层实现
    └── drivers/
        ├── es8311_driver.cc
        ├── es8388_driver.cc
        ├── es8374_driver.cc
        ├── es8389_driver.cc
        ├── aw88298_driver.cc
        ├── no_codec_driver.cc    # I2S 直连
        └── pdm_driver.cc
```

---

## 六、硬件配置结构体设计

```cpp
// audio_hardware_config.h

namespace ai_sdk {

enum class AudioHardwareType {
    ES8311,
    ES8388,
    ES8374,
    ES8389,
    AW88298,
    NO_CODEC,  // I2S 直连
    PDM,
};

struct AudioHardwareConfig {
    // 硬件类型
    AudioHardwareType type;
    
    // I2S 引脚
    int mclk_pin;
    int bclk_pin;
    int ws_pin;
    int dout_pin;
    int din_pin;
    
    // I2C 引脚（Codec 芯片需要）
    int i2c_sda_pin;
    int i2c_scl_pin;
    int i2c_port;
    uint8_t i2c_addr;
    
    // 功放控制引脚
    int pa_pin;
    
    // 音频参数
    int sample_rate;
    int input_sample_rate;
    int output_sample_rate;
};

} // namespace ai_sdk
```

---

## 七、TTS 播放流程

```
1. 收到 TTS URL
   └─ AsrIntelligentDialogue::DialogueCallback
   
2. HTTP 下载音频
   └─ esp_http_client
   
3. 解码 (MP3/Opus -> PCM)
   └─ esp_audio_codec
   
4. 重采样 (如果需要)
   └─ esp_audio_effects
   
5. 播放 PCM
   └─ esp_codec_dev / driver/i2s
```

---

## 八、当前项目的引脚配置机制参考

### 8.1 文件结构

```
main/boards/lichuang-c3-dev/
├── config.h                    # 引脚宏定义
├── lichuang_c3_dev_board.cc    # 板级代码
└── config.json                 # 编译配置
```

### 8.2 config.h 内容

```c
// 采样率
#define AUDIO_INPUT_SAMPLE_RATE  24000
#define AUDIO_OUTPUT_SAMPLE_RATE 24000

// I2S 引脚
#define AUDIO_I2S_GPIO_MCLK GPIO_NUM_10
#define AUDIO_I2S_GPIO_WS   GPIO_NUM_12
#define AUDIO_I2S_GPIO_BCLK GPIO_NUM_8
#define AUDIO_I2S_GPIO_DIN  GPIO_NUM_7
#define AUDIO_I2S_GPIO_DOUT GPIO_NUM_11

// I2C 引脚
#define AUDIO_CODEC_PA_PIN       GPIO_NUM_13
#define AUDIO_CODEC_I2C_SDA_PIN  GPIO_NUM_0
#define AUDIO_CODEC_I2C_SCL_PIN  GPIO_NUM_1
#define AUDIO_CODEC_ES8311_ADDR  ES8311_CODEC_DEFAULT_ADDR
```

### 8.3 板级代码如何使用引脚

```cpp
// lichuang_c3_dev_board.cc

#include "config.h"  // 包含引脚宏定义

class LichuangC3DevBoard : public WifiBoard {
    AudioCodec* GetAudioCodec() override {
        // 使用 config.h 中的宏定义创建 AudioCodec
        static Es8311AudioCodec audio_codec(
            codec_i2c_bus_,
            I2C_NUM_0,
            AUDIO_INPUT_SAMPLE_RATE,   // 来自 config.h
            AUDIO_OUTPUT_SAMPLE_RATE,  // 来自 config.h
            AUDIO_I2S_GPIO_MCLK,       // 来自 config.h
            AUDIO_I2S_GPIO_BCLK,       // 来自 config.h
            AUDIO_I2S_GPIO_WS,         // 来自 config.h
            AUDIO_I2S_GPIO_DOUT,       // 来自 config.h
            AUDIO_I2S_GPIO_DIN,        // 来自 config.h
            AUDIO_CODEC_PA_PIN,        // 来自 config.h
            AUDIO_CODEC_ES8311_ADDR    // 来自 config.h
        );
        return &audio_codec;
    }
};

DECLARE_BOARD(LichuangC3DevBoard);
```

### 8.4 配置流程

```
1. 用户通过 menuconfig 选择开发板类型
   → CONFIG_BOARD_TYPE_LICHUANG_DEV_C3=y

2. CMakeLists.txt 根据 CONFIG 设置 BOARD_TYPE
   → set(BOARD_TYPE "lichuang-c3-dev")

3. CMakeLists.txt 加载对应目录的源文件
   → boards/lichuang-c3-dev/*.cc

4. 板级代码包含 config.h，读取引脚宏定义
   → #include "config.h"

5. 板级代码创建 AudioCodec 实例
   → Es8311AudioCodec(AUDIO_I2S_GPIO_MCLK, ...)
```

---

## 九、方案可行性确认

**方案 100% 可行！**

原因：

1. **当前项目已经在使用这些组件**
   - esp_audio_codec：已使用
   - esp_audio_effects：已使用
   - esp_codec_dev：已使用

2. **所有 API 已经验证可用**
   - `esp_codec_dev_write()` - 播放 PCM
   - `esp_mp3_dec_process()` - MP3 解码
   - `esp_ae_rate_cvt_process()` - 重采样

3. **ai-sdk 只需要复制相同的模式**
   - 参考当前项目的 AudioCodec 实现
   - 参考当前项目的 AudioService 实现
