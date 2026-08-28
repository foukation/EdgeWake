# ai-sdk 增加 TTS 音频播放功能 - 详细实现方案

## 一、当前 ai-sdk 架构分析

### 1.1 当前 ai-sdk 包含

- ASR 语音识别 (WebSocket 通信)
- 智能对话 (DialogueResult 回调)
- HTTP 客户端
- 设备管理

### 1.2 当前 ai-sdk 缺少

- TTS URL 下载
- 音频解码 (MP3/Opus -> PCM)
- 音频播放 (硬件输出)

---

## 二、TTS 播放流程

```
1. 收到 DialogueResult
   directive: "Speak"
   payload: {"url": "https://tts.xxx.com/audio.mp3", "format": "mp3"}
           ↓
2. HTTP 下载 MP3 音频
   esp_http_client 流式下载
           ↓
3. MP3 解码 -> PCM
   esp_audio_codec (esp_mp3_dec_*)
           ↓
4. 重采样 (如果需要)
   esp_audio_effects (esp_ae_rate_cvt_*)
           ↓
5. PCM -> 硬件
   esp_codec_dev / driver/i2s
```

---

## 三、ai-sdk 需要添加的依赖

```yaml
# ai_sdk_builder/main/idf_component.yml

dependencies:
  # 现有依赖
  idf:
    version: '>=5.4.0'
  espressif/esp_websocket_client: '*'
  
  # 新增依赖
  espressif/esp_audio_codec: ~2.4.0   # MP3/Opus 解码
  espressif/esp_audio_effects: ~1.2.0 # 重采样
  espressif/esp_codec_dev: ~1.5       # 硬件 Codec 驱动
```

---

## 四、ai-sdk 需要添加的文件

```
ai_sdk_builder/components/ai_sdk/
├── include/ai_sdk/
│   ├── audio_output.h         # 硬件输出抽象
│   ├── audio_config.h         # 硬件配置结构体
│   └── tts_player.h           # TTS 播放器
└── src/
    ├── tts_player.cc          # TTS 播放器实现
    ├── audio_output.cc        # 硬件输出实现
    └── codecs/                # 硬件驱动
        ├── es8311_codec.cc
        ├── es8388_codec.cc
        ├── box_codec.cc
        └── no_codec.cc
```

---

## 五、硬件配置结构体

```cpp
// include/ai_sdk/audio_config.h

#pragma once

namespace ai_sdk {

/**
 * @brief 音频硬件类型枚举
 */
enum class AudioHardwareType {
    ES8311,     // ES8311 Codec 芯片
    ES8388,     // ES8388 Codec 芯片
    BOX,        // ES8311+ES7210 组合 (ESP-BOX 系列)
    NO_CODEC,   // I2S 直连 (无 Codec 芯片)
};

/**
 * @brief 音频硬件配置结构体
 * 
 * 厂商使用此结构体配置硬件引脚和参数
 */
struct AudioConfig {
    // 硬件类型
    AudioHardwareType type = AudioHardwareType::ES8311;
    
    // I2S 引脚
    int mclk_pin = -1;      // 主时钟引脚 (部分硬件需要)
    int bclk_pin = -1;      // 位时钟引脚
    int ws_pin = -1;        // 字选择引脚 (LR Clock)
    int dout_pin = -1;      // 数据输出引脚 (播放)
    int din_pin = -1;       // 数据输入引脚 (录音)
    
    // I2C 引脚 (有 Codec 芯片时需要)
    int i2c_sda_pin = -1;   // I2C 数据引脚
    int i2c_scl_pin = -1;   // I2C 时钟引脚
    int i2c_port = 0;       // I2C 端口号
    int i2c_addr = 0x18;    // Codec 芯片 I2C 地址
    
    // 功放控制
    int pa_pin = -1;        // 功放使能引脚
    
    // 音频参数
    int sample_rate = 16000;        // 采样率
    int input_sample_rate = 16000;  // 输入采样率
    int output_sample_rate = 16000; // 输出采样率
};

} // namespace ai_sdk
```

---

## 六、TTS 播放器接口

```cpp
// include/ai_sdk/tts_player.h

#pragma once

#include "audio_config.h"
#include <string>
#include <memory>

namespace ai_sdk {

/**
 * @brief TTS 播放器类
 * 
 * 负责下载、解码和播放 TTS 音频
 */
class TtsPlayer {
public:
    TtsPlayer();
    ~TtsPlayer();
    
    /**
     * @brief 初始化硬件
     * 
     * @param config 硬件配置
     * @return true 初始化成功
     * @return false 初始化失败
     */
    bool init(const AudioConfig& config);
    
    /**
     * @brief 播放 TTS URL
     * 
     * @param url TTS 音频 URL
     * @param format 音频格式 ("mp3", "opus", "wav")
     * @return true 开始播放
     * @return false 播放失败
     */
    bool playUrl(const std::string& url, const std::string& format = "mp3");
    
    /**
     * @brief 停止播放
     */
    void stop();
    
    /**
     * @brief 暂停播放
     */
    void pause();
    
    /**
     * @brief 恢复播放
     */
    void resume();
    
    /**
     * @brief 设置音量
     * 
     * @param volume 音量 (0-100)
     */
    void setVolume(int volume);
    
    /**
     * @brief 获取当前音量
     * 
     * @return int 当前音量 (0-100)
     */
    int getVolume() const;
    
    /**
     * @brief 检查是否正在播放
     * 
     * @return true 正在播放
     * @return false 未播放
     */
    bool isPlaying() const;
    
    /**
     * @brief 检查是否已初始化
     * 
     * @return true 已初始化
     * @return false 未初始化
     */
    bool isInitialized() const;
    
private:
    class Impl;
    std::unique_ptr<Impl> impl_;
};

} // namespace ai_sdk
```

---

## 七、集成到 AsrIntelligentDialogue

```cpp
// asr_intelligent_dialogue.h 新增

class AsrIntelligentDialogue {
public:
    // 现有方法...
    void setCallbacks(...);
    bool start();
    void stop();
    void sendAudio(const uint8_t* data, size_t len);
    bool isConnected() const;
    bool isRecognizing() const;
    
    // ========== 新增方法 ==========
    
    /**
     * @brief 初始化音频播放
     * 
     * 必须在 start() 之前调用
     * 
     * @param config 硬件配置
     * @return true 初始化成功
     * @return false 初始化失败
     */
    bool initAudio(const AudioConfig& config);
    
    /**
     * @brief 获取 TTS 播放器
     * 
     * 可用于手动控制播放
     * 
     * @return TtsPlayer& TTS 播放器引用
     */
    TtsPlayer& getTtsPlayer();
    
    /**
     * @brief 设置是否自动播放 TTS
     * 
     * 默认为 true
     * 
     * @param enabled true 自动播放, false 手动控制
     */
    void setAutoPlayTts(bool enabled);
};
```

---

## 八、厂商使用方式

```cpp
// 厂商代码示例

#include "ai_sdk/ai_assistant_manager.h"
#include "ai_sdk/audio_config.h"

void app_main() {
    // 1. 配置硬件
    ai_sdk::AudioConfig audio_config = {
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
    
    // 2. 获取 ASR 实例
    auto& asr = ai_sdk::AIAssistantManager::getInstance().asrIntelligentDialogueHelp();
    
    // 3. 初始化音频播放
    if (!asr.initAudio(audio_config)) {
        ESP_LOGE(TAG, "Failed to init audio");
        return;
    }
    
    // 4. 设置回调 (可选，TTS 会自动播放)
    asr.setCallbacks(
        []() { 
            ESP_LOGI(TAG, "Connected"); 
        },
        [](const ai_sdk::AsrResult& result) { 
            ESP_LOGI(TAG, "ASR: %s", result.text.c_str()); 
        },
        [](const ai_sdk::DialogueResult& result) {
            // TTS 会自动播放，此回调可用于显示文本
            if (result.directive == "Speak") {
                ESP_LOGI(TAG, "TTS playing...");
            }
            if (!result.assistant_answer_content.empty()) {
                ESP_LOGI(TAG, "Answer: %s", result.assistant_answer_content.c_str());
            }
        },
        [](int code, const std::string& msg) { 
            ESP_LOGE(TAG, "Error: %d - %s", code, msg.c_str()); 
        },
        []() { 
            ESP_LOGI(TAG, "Complete"); 
        }
    );
    
    // 5. 启动对话
    asr.start();
    
    // 6. 发送音频数据
    asr.sendAudio(pcm_data, len);
    // TTS 自动播放！
}
```

---

## 九、内部实现细节

### 9.1 DialogueCallback 处理 TTS

```cpp
// src/asr_intelligent_dialogue.cc 内部

void AsrIntelligentDialogue::Impl::handleDialogueResult(const DialogueResult& result) {
    // 检查是否是 TTS 指令
    if (result.directive == "Speak" && auto_play_tts_) {
        // 解析 payload
        // {"url": "https://tts.xxx.com/audio.mp3", "format": "mp3"}
        cJSON* json = cJSON_Parse(result.payload.c_str());
        if (json) {
            const char* url = cJSON_GetStringValue(cJSON_GetObjectItem(json, "url"));
            const char* format = cJSON_GetStringValue(cJSON_GetObjectItem(json, "format"));
            
            // 播放 TTS
            if (url) {
                tts_player_.playUrl(url, format ? format : "mp3");
            }
            cJSON_Delete(json);
        }
    }
    
    // 调用用户回调
    if (dialogue_callback_) {
        dialogue_callback_(result);
    }
}
```

### 9.2 TtsPlayer 实现

```cpp
// src/tts_player.cc

#include "ai_sdk/tts_player.h"
#include <esp_http_client.h>
#include <esp_audio_dec.h>
#include <esp_mp3_dec.h>
#include <esp_ae_rate_cvt.h>
#include <esp_codec_dev.h>

namespace ai_sdk {

class TtsPlayer::Impl {
public:
    AudioConfig config_;
    esp_codec_dev_handle_t codec_dev_ = nullptr;
    int volume_ = 70;
    bool playing_ = false;
    bool initialized_ = false;
    
    bool init(const AudioConfig& config) {
        config_ = config;
        
        // 根据硬件类型初始化
        switch (config.type) {
            case AudioHardwareType::ES8311:
                return initEs8311();
            case AudioHardwareType::ES8388:
                return initEs8388();
            case AudioHardwareType::BOX:
                return initBox();
            case AudioHardwareType::NO_CODEC:
                return initNoCodec();
            default:
                return false;
        }
    }
    
    bool initEs8311() {
        // 初始化 I2C
        i2c_master_bus_handle_t i2c_bus = nullptr;
        i2c_master_bus_config_t i2c_cfg = {
            .i2c_port = (i2c_port_t)config_.i2c_port,
            .sda_io_num = (gpio_num_t)config_.i2c_sda_pin,
            .scl_io_num = (gpio_num_t)config_.i2c_scl_pin,
            .clk_source = I2C_CLK_SRC_DEFAULT,
            .glitch_ignore_cnt = 7,
        };
        ESP_ERROR_CHECK(i2c_new_master_bus(&i2c_cfg, &i2c_bus));
        
        // 初始化 ES8311
        audio_codec_i2c_cfg_t i2c_codec_cfg = {
            .port = (i2c_port_t)config_.i2c_port,
            .addr = (uint8_t)config_.i2c_addr,
            .bus_handle = i2c_bus,
        };
        const audio_codec_ctrl_if_t* ctrl_if = audio_codec_new_i2c_ctrl(&i2c_codec_cfg);
        
        es8311_codec_cfg_t es8311_cfg = {};
        es8311_cfg.ctrl_if = ctrl_if;
        es8311_cfg.codec_mode = ESP_CODEC_DEV_WORK_MODE_BOTH;
        es8311_cfg.pa_pin = (gpio_num_t)config_.pa_pin;
        const audio_codec_if_t* codec_if = es8311_codec_new(&es8311_cfg);
        
        // 创建 Codec 设备
        esp_codec_dev_cfg_t dev_cfg = {
            .dev_type = ESP_CODEC_DEV_TYPE_IN_OUT,
            .codec_if = codec_if,
            .data_if = createI2sDataIf(),
        };
        codec_dev_ = esp_codec_dev_new(&dev_cfg);
        
        initialized_ = (codec_dev_ != nullptr);
        return initialized_;
    }
    
    bool playUrl(const std::string& url, const std::string& format) {
        if (!initialized_) return false;
        
        playing_ = true;
        
        // 1. HTTP 流式下载
        esp_http_client_config_t http_config = {
            .url = url.c_str(),
        };
        esp_http_client_handle_t client = esp_http_client_init(&http_config);
        esp_http_client_open(client, 0);
        
        // 2. 创建解码器
        esp_audio_dec_handle_t decoder = nullptr;
        if (format == "mp3") {
            esp_mp3_dec_cfg_t dec_cfg = ESP_MP3_DEC_CONFIG_DEFAULT();
            esp_mp3_dec_open(&dec_cfg, &decoder);
        }
        
        // 3. 打开 Codec 设备
        esp_codec_dev_sample_info_t fs = {
            .bits_per_sample = 16,
            .channel = 1,
            .sample_rate = (uint32_t)config_.sample_rate,
        };
        esp_codec_dev_open(codec_dev_, &fs);
        esp_codec_dev_set_out_vol(codec_dev_, volume_);
        
        // 4. 流式处理
        uint8_t http_buffer[1024];
        int16_t pcm_buffer[4096];
        
        while (playing_) {
            // 读取 HTTP 数据
            int read_len = esp_http_client_read(client, (char*)http_buffer, sizeof(http_buffer));
            if (read_len <= 0) break;
            
            // 解码
            esp_audio_dec_info_t dec_info = {};
            int pcm_len = 0;
            esp_audio_dec_process(decoder, http_buffer, read_len, pcm_buffer, &pcm_len, &dec_info);
            
            // 播放
            if (pcm_len > 0) {
                esp_codec_dev_write(codec_dev_, pcm_buffer, pcm_len);
            }
        }
        
        // 5. 清理
        esp_codec_dev_close(codec_dev_);
        esp_audio_dec_close(decoder);
        esp_http_client_cleanup(client);
        
        playing_ = false;
        return true;
    }
    
    void stop() {
        playing_ = false;
    }
    
    void setVolume(int volume) {
        volume_ = volume;
        if (codec_dev_) {
            esp_codec_dev_set_out_vol(codec_dev_, volume);
        }
    }
    
private:
    const audio_codec_data_if_t* createI2sDataIf() {
        // 创建 I2S 数据接口
        // ... 根据 config_ 配置 I2S
        return nullptr; // 实际实现
    }
    
    bool initEs8388() { /* 类似 initEs8311 */ return true; }
    bool initBox() { /* ES8311+ES7210 */ return true; }
    bool initNoCodec() { /* I2S 直连 */ return true; }
};

TtsPlayer::TtsPlayer() : impl_(std::make_unique<Impl>()) {}
TtsPlayer::~TtsPlayer() = default;

bool TtsPlayer::init(const AudioConfig& config) { return impl_->init(config); }
bool TtsPlayer::playUrl(const std::string& url, const std::string& format) { return impl_->playUrl(url, format); }
void TtsPlayer::stop() { impl_->stop(); }
void TtsPlayer::setVolume(int volume) { impl_->setVolume(volume); }
int TtsPlayer::getVolume() const { return impl_->volume_; }
bool TtsPlayer::isPlaying() const { return impl_->playing_; }
bool TtsPlayer::isInitialized() const { return impl_->initialized_; }

} // namespace ai_sdk
```

---

## 十、支持的硬件类型

| 硬件类型 | AudioHardwareType | 使用组件 | 说明 |
|----------|-------------------|----------|------|
| ES8311 | `ES8311` | esp_codec_dev | 最常用 Codec |
| ES8388 | `ES8388` | esp_codec_dev | 老款 Codec |
| ES8311+ES7210 | `BOX` | esp_codec_dev | ESP-BOX 系列 |
| I2S 直连 | `NO_CODEC` | driver/i2s | 无 Codec 芯片 |

---

## 十一、预估体积增加

| 组件 | 预估增加 |
|------|----------|
| TTS 播放器逻辑 | ~10KB |
| esp_codec_dev | ~30KB |
| esp_audio_codec (MP3 解码) | ~50KB |
| esp_audio_effects (重采样) | ~10KB |
| **总计** | **~100KB** |

---

## 十二、实现步骤

1. **添加依赖** - 修改 `idf_component.yml`
2. **添加配置结构体** - `audio_config.h`
3. **实现硬件初始化** - `codecs/*.cc`
4. **实现 TTS 播放器** - `tts_player.cc`
5. **集成到 AsrIntelligentDialogue** - `asr_intelligent_dialogue.cc`
6. **测试** - 验证各硬件类型
7. **更新文档** - 厂商使用指南
