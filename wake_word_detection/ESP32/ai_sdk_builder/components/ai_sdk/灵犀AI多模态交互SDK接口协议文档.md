# AI-SDK (ESP32 Series)

## 1. 概述

### 版本信息
- SDK 版本：1.0.0
- 发布日期：2025-12-30
- 文档更新：2026-06-22

### 系统要求
- ESP-IDF 版本：5.4.3+
- 目标芯片：ESP32 系列（ESP32/S2/S3/C2/C3/C5/C6/P4）
- C++ 标准：C++17
- C++ 异常：启用（CONFIG_COMPILER_CXX_EXCEPTIONS=y）
- C++ RTTI：启用（CONFIG_COMPILER_CXX_RTTI=y）

### 主要功能
- 设备管理（设备信息、网关配置、数据上报）
- 语音助手（ASR + NLU + TTS 全链路智能对话）
- 持续语音识别（纯 ASR，WebSocket 持久连接，服务端 VAD 分割）
- AI 基础工具包（大模型闲聊、文本链路智能问答、文本翻译、内容摘要）
- 日志控制

## 2. 快速开始

### 依赖组件

在 `idf_component.yml` 或 `CMakeLists.txt` 中添加依赖：

```cmake
REQUIRES
    esp_http_client      # HTTP/HTTPS 通信
    esp_timer            # 硬件定时器支持
    json                 # cJSON 库（JSON 数据处理）
    esp_websocket_client # WebSocket 通信（用于语音助手）
```

### 组件配置

将 `ai_sdk` 组件放入项目的 `components/` 目录：

```
project/
├── components/
│   └── ai_sdk/
│       ├── include/
│       │   └── ai_sdk/
│       │       ├── types/
│       │       ├── audio/
│       │       │   ├── audio_codec.h       # 音频抽象基类
│       │       │   └── audio_config.h      # 硬件配置 + CreateAudioCodec()
│       │       ├── ai_assistant_manager.h
│       │       ├── ai_foundation_kit.h
│       │       ├── ai_sdk_log.h
│       │       ├── asr_intelligent_dialogue.h
│       │       ├── speech_recognition_persistent.h
│       │       ├── gate_way.h
│       │       └── types.h
│       ├── lib/
│       │   ├── esp32/libai_sdk.a
│       │   ├── esp32c2/libai_sdk.a
│       │   ├── esp32c3/libai_sdk.a
│       │   ├── esp32c5/libai_sdk.a
│       │   ├── esp32c6/libai_sdk.a
│       │   ├── esp32p4/libai_sdk.a
│       │   ├── esp32s2/libai_sdk.a
│       │   └── esp32s3/libai_sdk.a
│       ├── CMakeLists.txt
└── main/
    └── main.cpp
```

### sdkconfig  配置要求

确保以下配置已启用：

```ini
# C++ 异常支持（必须）
CONFIG_COMPILER_CXX_EXCEPTIONS=y

# C++ RTTI 支持（必须）
CONFIG_COMPILER_CXX_RTTI=y

# 网络时间同步（推荐）
CONFIG_LWIP_SNTP_MAX_SERVERS=3
```

### 初始化步骤

```cpp
#include "ai_sdk/ai_assistant_manager.h"
#include "ai_sdk/ai_sdk_log.h"

void init_ai_sdk() {
    using namespace ai_sdk;

    // 1. 设置日志级别（可选，在初始化前设置）
    Log::setLevel(LogLevel::INFO);  // 生产环境可使用 LogLevel::ERROR

    // 2. 创建 TTS 配置（可选）
    TtsConfig tts(4100, 5, 5, 15);  // 音色, 语速, 音调, 音量

    // 3. 使用 Builder 构建配置
    auto builder = std::make_unique<AIAssistConfig::Builder>();
    auto config = builder->deviceNo("YOUR_DEVICE_NO")       // 设备序列号
                         ->deviceNoType("YOUR_DEVICE_NO_TYPE")               // 设备号类型
                         ->productId("YOUR_PRODUCT_ID")     // 产品 ID
                         ->productKey("YOUR_PRODUCT_KEY")   // 产品密钥
                         ->dialogueTtsConfig(tts)           // TTS 配置
                         ->token("****")                    // 大模型闲聊 token 授权
                         ->centralConfigVersion("*")        // 中控版本
                         ->build();

    // 4. 初始化管理器
    AIAssistantManager::initialize(std::move(config));

    // 5. 获取设备纳管实例
    auto& gateway = AIAssistantManager::getInstance().gateWayHelp();

    // 6. 获取语音助手实例
    auto& asr = AIAssistantManager::getInstance().asrIntelligentDialogueHelp();

    // 7. 获取 AI 基础工具包实例
    auto& aiKit = AIAssistantManager::getInstance().aiFoundationKit();
}
```

## 3. 接口定义

### 配置管理
提供以下配置选项：
- 产品 ID 配置
- 产品密钥配置
- 设备编号配置
- 设备编号类型配置
- TTS 配置（音色、语速、音调、音量）
- 中控配置版本

### 设备信息管理
设备信息管理功能：
- 路由代理地址获取
- 设备信息获取
- 设备采集信息上报

### 智能语音识别（ASrIntelligentDialogue）
语音识别、NLU 与全链路对话功能：
- 实时语音识别
- 智能对话交互（查询问答、闲聊、文生图、播放音乐等）
- 语音命令处理
- 内置音频驱动（麦克风录音 + TTS 自动播放）

### 持续语音识别（SpeechRecognitionPersistent）
纯 ASR 持久连接模式，无 NLU / TTS：
- WebSocket 持久连接，服务端 VAD 自动分割语音段
- 一次连接多次回调识别结果，持续识别直到 stop() / cancel()
- 支持外部音频（业务方自行采集 PCM）和内置音频驱动两种模式

### AI 基础工具包
AI 功能模块：
- 大模型闲聊（Chatbot）
  - 支持流式/非流式请求
  - 支持取消流式请求
- 文本链路智能问答（insideRcChat）
  - 文本输入直接进入智能对话服务，跳过 ASR 语音识别
  - 返回与语音助手相同的 DCS 指令集（Speak / RenderStreamCard / RenderCard / Nlu 等）
  - 支持流式/非流式请求
  - 支持多轮对话上下文
- 文本翻译
  - 机器翻译（200+种语言）
  - 模型翻译（~90种语言）
- 内容摘要（Content Summary）
  - 支持流式/非流式请求
  - 支持多语言输出（auto、Chinese、English 等）
  - 支持取消流式请求

### 日志控制
日志级别管理：
- 运行时动态调整日志级别
- 支持 NONE/ERROR/WARN/INFO/DEBUG/VERBOSE 六级

## 4. API 参考

### TtsConfig

TTS（文本转语音）配置类：

```cpp
struct TtsConfig {
    int voiceId;    // 音色 ID（默认：4100 活力女主播）
    int speed;      // 语速 0-15（默认：5）
    int pitch;      // 音调 0-15（默认：5）
    int volume;     // 音量 0-15（默认：15）

    TtsConfig();                                    // 默认构造
    TtsConfig(int vid, int spd, int pit, int vol);  // 带参构造
    bool isValid() const;                           // 验证参数范围
};
```

### AIAssistConfig

配置管理类，使用 Builder 模式构建：

```cpp
class AIAssistConfig {
public:
    class Builder {
    public:
        Builder& deviceId(const std::string& deviceId);
        Builder& deviceSecret(const std::string& secret);
        Builder& productId(const std::string& productId);
        Builder& productKey(const std::string& productKey);
        Builder& deviceNoType(const std::string& type);
        Builder& deviceNo(const std::string& deviceNo);
        Builder& enableGateway(bool enable);
        Builder& centralConfigVersion(const std::string& version);
        Builder& token(const std::string& t);
        Builder& dialogueTtsConfig(const TtsConfig& tts);
        Builder& clientID(const std::string& id);
        Builder& enableVoiceGain(bool enable);
        std::unique_ptr<AIAssistConfig> build();
    };

    // 配置字段
    std::string deviceId;           // 设备 ID（注册后获得）
    std::string deviceSecret;       // 设备密钥（注册后获得）
    std::string productId;          // 产品 ID
    std::string productKey;         // 产品密钥
    std::string deviceNoType;       // 设备号类型（SN/MAC/IMEI）
    std::string deviceNo;           // 设备序列号
    bool enableGateway;             // 是否启用网关代理
    TtsConfig dialogueTtsConfig;    // TTS 配置
    std::string token;              // Chatbot 授权令牌
    std::string clientID;           // 客户端类型标识
    bool enableVoiceGain;           // 是否启用 AGC
    std::string centralConfigVersion; // 中控配置版本
};
```

### AIAssistantManager

SDK 核心管理类，提供以下功能：
- SDK 初始化
- 设备管理（纳管）
- 语音助手智能对话
- AI 基础工具包（大模型闲聊、文本翻译、内容摘要）
- 更新 TTS 配置

```cpp
class AIAssistantManager {
public:
    // 初始化 SDK（必须首先调用）
    static void initialize(std::unique_ptr<AIAssistConfig> config);

    // 获取单例实例
    static AIAssistantManager& getInstance();

    // 检查是否已初始化
    static bool isInitialized();

    // 销毁实例（可选）
    static void destroyInstance();

    // 获取设备管理实例
    GateWay& gateWayHelp();

    // 获取语音助手实例
    AsrIntelligentDialogue& asrIntelligentDialogueHelp();

    // 获取持续语音识别实例（v0.9.6 新增）
    SpeechRecognitionPersistent& speechRecognitionPersistentHelp();

    // 获取 AI 基础工具包实例
    AIFoundationKit& aiFoundationKit();

    // 修改 TTS 配置
    void changeTtsConfig(const TtsConfig& tts);

    // 获取配置
    const AIAssistConfig& config() const;
    AIAssistConfig& config();
};
```

### GateWay

设备管理类：
- 获取设备信息（纳管）
- 获取网关配置
- 设备数据上报（心跳）

```cpp
class GateWay {
public:
    // 回调类型定义
    using DeviceInfoSuccessCallback = std::function<void(const DeviceInfoResponse&)>;
    using DeviceInfoErrorCallback = std::function<void(const std::string&)>;
    using GatewaySuccessCallback = std::function<void(const GatewayInfo&, const std::string&)>;
    using GatewayErrorCallback = std::function<void(const std::string&)>;
    using ReportSuccessCallback = std::function<void(const DeviceReportResponse&)>;
    using ReportErrorCallback = std::function<void(const std::string&)>;

    // 获取设备信息（鉴权）
    void obtainDeviceInformation(
        DeviceInfoSuccessCallback onSuccess,
        DeviceInfoErrorCallback onError
    );

    // 获取网关配置
    void getGateWay(
        GatewaySuccessCallback onSuccess,
        GatewayErrorCallback onError
    );

    // 数据上报（心跳）
    void dataReport(
        const DeviceReportRequest& request,
        ReportSuccessCallback onSuccess,
        ReportErrorCallback onError
    );
};
```

### AsrIntelligentDialogue

ASR 智能对话类：
- 语音识别
- TTS
- 闲聊
- 查询及问答
- 文本创作
- 文生图
- 播放音乐
- 意图识别

```cpp
class AsrIntelligentDialogue {
public:
    // 回调类型定义
    using ConnectedCallback  = std::function<void()>;
    using AsrCallback        = std::function<void(const AsrResult&)>;
    using DialogueCallback   = std::function<void(const DialogueResult&)>;
    using ErrorCallback      = std::function<void(int, const std::string&)>;
    using CompleteCallback   = std::function<void()>;

    /**
     * TTS 播放回调（v0.9.5 新增）
     * 当一个 Speak URL 播放结束时触发：
     * - url：当前播放的 TTS 音频地址
     * - completed：true=正常播放完成，false=被中断或失败
     * - all_done：true=本轮所有排队 TTS 段已播完
     */
    using TtsPlaybackCallback = std::function<void(const std::string& url,
                                                   bool completed,
                                                   bool all_done)>;

    // 设置回调函数（必须在 start() 之前调用）
    void setCallbacks(
        ConnectedCallback connected_cb,
        AsrCallback asr_cb,
        DialogueCallback dialogue_cb,
        ErrorCallback error_cb,
        CompleteCallback complete_cb
    );

    // 设置 TTS 播放回调（v0.9.5 新增，与 setCallbacks() 独立）
    // 需配合 initAudio() + setAutoPlayTts(true) 才会触发
    void setTtsPlaybackCallback(TtsPlaybackCallback cb);

    // 启动 ASR 识别
    bool start();

    // 停止 ASR 识别并释放资源（可再次调用 start()）
    void stop();

    // 发送音频数据（PCM 16-bit 16kHz 单声道，建议块大小 5120 字节）
    void sendAudio(const uint8_t* data, size_t len);

    // 检查连接状态（线程安全）
    bool isConnected() const;

    // 检查识别状态（线程安全）
    bool isRecognizing() const;

    // 初始化内置音频模块，绑定已 Start() 的 AudioCodec
    // SDK 内部自动管理麦克风录音（AudioInput）和 TTS 播放（TtsPlayer）
    // codec 的生命周期须长于 AsrIntelligentDialogue
    bool initAudio(AudioCodec* codec);

    // 控制麦克风录音开关（前提：已调用 initAudio()）
    // true=开始录音并自动发送 PCM，false=暂停录音
    void setRecording(bool enable);

    // 设置是否自动播放 TTS（前提：已调用 initAudio()，默认 false）
    // true=收到 Speak 指令时 SDK 自动播放，false=仅通过 DialogueCallback 通知
    void setAutoPlayTts(bool enable);
};
```

### SpeechRecognitionPersistentResult

持续语音识别结果类型（每段语音识别完成后由服务端返回）：

```cpp
struct SpeechRecognitionPersistentResult {
    int         err_no;       // 错误码，0=成功，非0=本段识别失败
    std::string err_msg;      // 错误描述，成功时为 "OK"
    long        log_id;       // 服务端日志追踪 ID
    std::string sn;           // 本段识别序列号，格式 "uuid_ws_序号"
    std::string type;         // 消息类型，当前已知值为 "FIN_TEXT"
    std::string result;       // 识别出的文本内容
    long        start_time;   // 本段语音起始时间（ms）
    long        end_time;     // 本段语音结束时间（ms）
    int         product_id;   // 识别所用语言模型 ID，与 start() 传入的 dev_pid 对应
    std::string product_line; // 业务线标识，如 "open"
};
```

### SpeechRecognitionPersistent

持续语音识别管理类，与 `AsrIntelligentDialogue` 的核心区别：

| | SpeechRecognitionPersistent | AsrIntelligentDialogue |
|---|---|---|
| 功能 | 纯 ASR（语音转文字） | ASR + NLU + TTS 全链路 |
| 端点 | `/app-ws/v1/long-asr` | `/app-ws/v2/asr` |
| 持续性 | 连接不断开，多次回调 | 单轮对话，complete 后关闭 |
| TTS | 无 | 有 |

通过 `AIAssistantManager::speechRecognitionPersistentHelp()` 获取实例。

```cpp
class SpeechRecognitionPersistent {
public:
    // 回调类型定义
    using ResultCallback = std::function<void(const SpeechRecognitionPersistentResult&)>;
    using ErrorCallback  = std::function<void(int code, const std::string& message)>;
    using CloseCallback  = std::function<void()>;

    // 设置回调（必须在 start() 之前调用）
    // on_close 可为 nullptr
    void setCallbacks(ResultCallback on_result,
                      ErrorCallback  on_error,
                      CloseCallback  on_close);

    // 启动持续识别（建立 WebSocket 连接并发送 START 帧）
    // dev_pid：语言模型 ID，默认 15372（普通话近场）
    bool start(int dev_pid = 15372);

    // 优雅停止：发送 FINISH 帧，等待服务端返回最后一段结果后关闭
    void stop();

    // 立即取消：发送 CANCEL 帧后直接断开连接，不等待剩余结果
    void cancel();

    // 发送 PCM 音频数据（外部音频模式）
    // PCM 16-bit 小端序，16000 Hz，单声道，建议块大小 5120 字节（160ms）
    void sendAudio(const uint8_t* data, size_t len);

    bool isConnected() const;   // 检查 WebSocket 是否已连接
    bool isRecognizing() const; // 检查是否正在识别

    // ---- 内置音频驱动接口（可选，模式二专用）----

    // 初始化内置音频模块，绑定已 Start() 的 AudioCodec
    bool initAudio(AudioCodec* codec);

    // 控制麦克风录音开关（需先调用 initAudio()）
    void setRecording(bool enable);
};
```

**音频接入模式：**

**模式一：外部音频**（业务方自行采集）
```cpp
auto& asr = AIAssistantManager::getInstance().speechRecognitionPersistentHelp();
asr.setCallbacks(on_result, on_error, nullptr);
asr.start();
asr.sendAudio(pcm_data, len);  // 循环发送
asr.stop();
```

**模式二：内置音频驱动**（SDK 管理麦克风，推荐）
```cpp
auto& asr = AIAssistantManager::getInstance().speechRecognitionPersistentHelp();
asr.setCallbacks(on_result, on_error, nullptr);
asr.initAudio(codec);
asr.start();
asr.setRecording(true);
// 收到足够结果后...
asr.stop();  // 或 asr.cancel()
```

### AudioHardwareType

音频硬件类型枚举，用于 `AudioConfig.hardware_type`：

| 枚举值 | 描述 |
|---|---|
| `kEs8311` | ES8311 单芯片 Codec，支持 ADC + DAC，最常用（50%+ 开发板） |
| `kEs8388` | ES8388 单芯片 Codec，适用于老款开发板 |
| `kEs8374` | ES8374 单芯片 Codec |
| `kEs8389` | ES8389 单芯片 Codec |
| `kBoxAudioCodec` | ESP-BOX 系列双芯片（ES8311 DAC + ES7210 ADC，TDM 模式） |
| `kNoCodecDuplex` | I2S 双工直连，同一组引脚录音 + 播放（无编解码芯片） |
| `kNoCodecSimplex` | I2S 单工直连，麦克风和扬声器使用不同 I2S 端口 |
| `kNoCodecSimplexPdm` | PDM 麦克风 + I2S 播放（无编解码芯片） |
| `kDummy` | 空实现，无硬件时调试用 |

### AudioConfig

音频硬件配置结构体，填写后通过 `CreateAudioCodec()` 工厂函数创建驱动实例：

```cpp
struct AudioConfig {
    AudioHardwareType hardware_type;       // 硬件类型（必填）
    int input_sample_rate  = 16000;        // 麦克风采样率（Hz）
    int output_sample_rate = 16000;        // 扬声器采样率（Hz）

    // I2C 配置（有芯片方案必填）
    void*      i2c_master_handle = nullptr; // i2c_new_master_bus() 返回的句柄
    i2c_port_t i2c_port = I2C_NUM_0;

    // I2S 引脚 - 双工模式（有芯片 + kNoCodecDuplex）
    gpio_num_t mclk = GPIO_NUM_NC;         // 主时钟（部分芯片不需要）
    gpio_num_t bclk = GPIO_NUM_NC;         // 位时钟
    gpio_num_t ws   = GPIO_NUM_NC;         // 字选择
    gpio_num_t dout = GPIO_NUM_NC;         // 数据输出（ESP32 → DAC/扬声器）
    gpio_num_t din  = GPIO_NUM_NC;         // 数据输入（麦克风/ADC → ESP32）

    // I2S 引脚 - 单工模式（kNoCodecSimplex / kNoCodecSimplexPdm）
    gpio_num_t spk_bclk = GPIO_NUM_NC;
    gpio_num_t spk_ws   = GPIO_NUM_NC;
    gpio_num_t spk_dout = GPIO_NUM_NC;
    gpio_num_t mic_sck  = GPIO_NUM_NC;
    gpio_num_t mic_ws   = GPIO_NUM_NC;     // kNoCodecSimplexPdm 不使用
    gpio_num_t mic_din  = GPIO_NUM_NC;

    // 编解码器 I2C 地址（7-bit 行业标准格式，与芯片手册一致）
    // SDK 内部自动转换为底层驱动所需的 8-bit 格式，调用方无需关心
    uint8_t codec_addr  = 0x18;            // 单芯片方案（ES8311 默认 0x18）
    uint8_t es8311_addr = 0x18;            // Box 方案 ES8311（7-bit）
    uint8_t es7210_addr = 0x40;            // Box 方案 ES7210（7-bit）

    // 功放控制
    gpio_num_t pa_pin      = GPIO_NUM_NC;  // 功放使能引脚
    bool       pa_inverted = false;        // 低电平使能时设 true（仅 kEs8311）

    // 可选参数
    bool use_mclk       = true;            // kEs8311/Es8374/Es8389 使用
    bool input_reference = false;          // 回声消除参考通道（kEs8388/Box）
};
```

### CreateAudioCodec()

工厂函数，根据 `AudioConfig` 创建对应 `AudioCodec` 子类实例：

```cpp
// 头文件：ai_sdk/audio/audio_config.h
AudioCodec* CreateAudioCodec(const AudioConfig& config);
```

- 调用方负责管理返回指针的生命周期
- 创建后**必须**调用 `codec->Start()` 启动音频设备
- 创建失败时返回 `nullptr`

### Log


日志控制类：

```cpp
enum class LogLevel {
    NONE,       // 禁用所有日志
    ERROR,      // 仅错误
    WARN,       // 警告和错误
    INFO,       // 信息、警告和错误（默认）
    DEBUG,      // 调试及以上
    VERBOSE     // 所有日志
};

class Log {
public:
    static void setLevel(LogLevel level);  // 设置日志级别
    static LogLevel getLevel();            // 获取当前级别
    static void disable();                 // 禁用所有日志
    static void reset();                   // 恢复默认级别（INFO）
};
```

### AIFoundationKit

AI 基础工具包：
- 大模型闲聊（Chatbot）
- 文本链路智能问答（insideRcChat）
- 文本翻译
- 内容摘要（Content Summary）

```cpp
class AIFoundationKit {
public:
    // 回调类型定义
    using ChatbotSuccessCallback = std::function<void(const ChatbotCompletionResponse&)>;
    using ChatbotErrorCallback = std::function<void(const std::string&)>;
    using TranslateSuccessCallback = std::function<void(const TranslateResponse&)>;
    using TranslateErrorCallback = std::function<void(const std::string&)>;
    using ContentSummarySuccessCallback = std::function<void(const ContentSummaryResponse&)>;
    using ContentSummaryErrorCallback = std::function<void(const std::string&)>;
    using InsideRcChatSuccessCallback = std::function<void(const DialogueResult&)>;
    using InsideRcChatErrorCallback = std::function<void(const std::string&)>;

    // 大模型闲聊
    std::string largeModelChatbot(
        const ChatbotCompletionRequest& request,
        ChatbotSuccessCallback onSuccess,
        ChatbotErrorCallback onError);

    // 取消流式请求（支持 Chatbot、内容摘要、insideRcChat）
    bool cancelStreamRequest(const std::string& requestId);

    // 文本翻译（机器翻译 v1）
    void textTranslate(
        const TranslationRequest& request,
        TranslateSuccessCallback onSuccess,
        TranslateErrorCallback onError);

    // 文本翻译（模型翻译 v2）
    void textTranslateWithModel(
        const TranslationRequest& request,
        TranslateSuccessCallback onSuccess,
        TranslateErrorCallback onError);

    // 内容摘要
    std::string contentSummary(
        const ContentSummaryRequest& request,
        ContentSummarySuccessCallback onSuccess,
        ContentSummaryErrorCallback onError);

    // 文本链路智能问答（走智能对话服务，返回 DCS 指令集，跳过 ASR）
    // 支持流式（stream=true）和非流式（stream=false）两种模式
    // 支持多轮对话（messages 携带历史上下文）
    std::string insideRcChat(
        const InsideRcChatRequest& request,
        InsideRcChatSuccessCallback onSuccess,
        InsideRcChatErrorCallback onError);
};
```

### ContentSummaryRequest

内容摘要请求参数：

```cpp
struct ContentSummaryRequest {
    std::string content;            // 需要摘要的文本内容（必选）
    bool stream = true;             // 是否启用流式响应（默认 true）
    std::string language = "auto";  // 摘要语言（auto/Chinese/English/French 等）
};
```

### ContentSummaryResponse

内容摘要响应结果：

```cpp
struct ContentData {
    std::string content;            // 摘要内容（流式模式为增量，非流式为完整内容）
};

struct ContentSummaryResponse {
    std::string msg;                // 消息信息
    ContentData data;               // 内容数据
    std::string logId;              // 日志标识符（用于跟踪和调试）
    int status = 0;                 // 状态码（0 表示成功）
};
```

### InsideRcChatMessage

对话消息结构：

```cpp
struct InsideRcChatMessage {
    std::string role;     // "user" 或 "assistant"
    std::string content;  // 消息内容，不能为空

    InsideRcChatMessage() = default;
    InsideRcChatMessage(const std::string& role, const std::string& content);
};
```

### InsideRcChatRequest

文本链路智能问答请求参数：

```cpp
struct InsideRcChatRequest {
    std::string qid;            // 请求唯一标识（建议每次生成唯一值）
    std::string third_user_id;  // 第三方用户 ID
    std::string cuid;           // 设备 ID（通常使用 config.deviceId）
    std::vector<InsideRcChatMessage> messages;  // 对话消息列表
    bool stream = true;         // 是否启用流式响应（默认 true）
};
```

> **messages 规则**：
> - 成员数必须为**奇数**
> - 奇数位（第 1、3、5 条…）的 `role` 必须为 `"user"`
> - 偶数位（第 2、4 条…）的 `role` 必须为 `"assistant"`
> - 最后一条为当前用户问题，前面的为历史对话

> **流式 vs 非流式回调差异**：
> - `stream=true`：多次回调，`directive` 为 `RenderStreamCard`，`payload` 中 `answer` 字段为累积文本，最后一次为完整内容；最终 `is_end=1` 触发结束
> - `stream=false`：单次回调，`directive` 为 `RenderCard`，`payload` 中 `content` 字段为完整文本

## 5. 示例代码

### 基础示例

```cpp
#include "ai_sdk/ai_assistant_manager.h"
#include "ai_sdk/ai_sdk_log.h"
#include "esp_log.h"

static const char* TAG = "AI_SDK_DEMO";

void init_ai_sdk() {
    using namespace ai_sdk;

    // 配置日志级别
    Log::setLevel(LogLevel::INFO);

    // 构建配置
    auto builder = std::make_unique<AIAssistConfig::Builder>();
    auto config = builder->deviceNo("YOUR_DEVICE_NO")
                         ->deviceNoType("YOUR_DEVICE_NO_TYPE")
                         ->productId("YOUR_PRODUCT_ID")
                         ->productKey("YOUR_PRODUCT_KEY")
                         ->centralConfigVersion("*")
                         ->build();

    // 初始化 SDK
    AIAssistantManager::initialize(std::move(config));

    ESP_LOGI(TAG, "AI SDK initialized");
}
```

### 业务流程

```cpp
#include "ai_sdk/ai_assistant_manager.h"
#include "ai_sdk/ai_sdk_log.h"
#include "esp_log.h"

static const char* TAG = "AI_EXAMPLE";

class AIAssistExample {
public:
    void initializeSDK() {
        using namespace ai_sdk;

        // 1. 设置日志级别
        Log::setLevel(LogLevel::INFO);

        // 2. 创建 TTS 配置
        TtsConfig tts(4100, 5, 5, 15);

        // 3. 构建配置
        auto builder = std::make_unique<AIAssistConfig::Builder>();
        auto config = builder->deviceNo("YOUR_DEVICE_NO")
                             ->deviceNoType("YOUR_DEVICE_NO_TYPE")
                             ->productId("YOUR_PRODUCT_ID")
                             ->productKey("YOUR_PRODUCT_KEY")
                             ->dialogueTtsConfig(tts)
                             ->centralConfigVersion("*")
                             ->build();

        // 4. 初始化 SDK
        AIAssistantManager::initialize(std::move(config));

        // 5. 获取服务实例
        auto& manager = AIAssistantManager::getInstance();

        // 获取设备管理
        auto& gateway = manager.gateWayHelp();

        // 获取语音助手
        auto& voiceAssistant = manager.asrIntelligentDialogueHelp();

        // 获取 AI 基础工具包
        auto& aiKit = manager.aiFoundationKit();
    }

    /**
     * 获取设备信息
     *
     * 设备通过产品信息及设备号从云端获取设备信息。
     * 返回：deviceId, deviceNo, productId, deviceSecret
     * 这些信息用于后续 API 调用的认证。
     */
    void obtainDeviceInformation() {
        using namespace ai_sdk;

        auto& gateway = AIAssistantManager::getInstance().gateWayHelp();

        gateway.obtainDeviceInformation(
            [](const DeviceInfoResponse& response) {
                ESP_LOGI(TAG, "Device registered successfully");
                ESP_LOGI(TAG, "Device ID: %s", response.data.deviceId.c_str());
                ESP_LOGI(TAG, "Device Secret: %s", response.data.deviceSecret.c_str());

                // 设备注册成功后，可以继续其他操作
                // getGateWay();  // 获取网关配置（可选）
                // dataReport();  // 数据上报
            },
            [](const std::string& error) {
                ESP_LOGE(TAG, "Device registration failed: %s", error.c_str());
            }
        );
    }

    /**
     * 获取网关信息
     *
     * 获取代理网关配置，包含：
     * - token：网关验证令牌
     * - expires：代理有效期（秒）
     * - status：1=使用代理，0=不使用代理
     * - data.http：HTTP 代理地址
     * - data.ws：WebSocket 代理地址
     */
    void getGateWay() {
        using namespace ai_sdk;

        auto& gateway = AIAssistantManager::getInstance().gateWayHelp();

        gateway.getGateWay(
            [](const GatewayInfo& info, const std::string&) {
                ESP_LOGI(TAG, "Gateway info received");
                ESP_LOGI(TAG, "Status: %d", info.status);
                if (info.status == AgentUseCode::USE) {
                    ESP_LOGI(TAG, "HTTP Proxy: %s", info.data.http.c_str());
                    ESP_LOGI(TAG, "WS Proxy: %s", info.data.ws.c_str());
                }
            },
            [](const std::string& error) {
                ESP_LOGE(TAG, "Gateway request failed: %s", error.c_str());
            }
        );
    }

    /**
     * 执行设备参数采集上报
     *
     * 心跳接口：设备每24小时至少上报一次。
     *
     * 请求策略（参考）：
     * 1. 设备首次启动时上报
     * 2. 每隔12小时上报一次
     * 3. 添加 ±15 分钟随机偏移，避免服务器压力
     */
    void dataReport() {
        using namespace ai_sdk;

        auto& manager = AIAssistantManager::getInstance();
        auto& config = manager.config();
        auto& gateway = manager.gateWayHelp();

        DeviceReportRequest request;
        request.deviceId = config.deviceId;
        request.deviceSecret = config.deviceSecret;
        request.productId = config.productId;
        request.productKey = config.productKey;

        // 添加上报参数
        // 注意：sdkVersion 由 SDK 内部自动添加，无需手动设置
        request.params["netType"] = std::string("WiFi");

        gateway.dataReport(
            request,
            [](const DeviceReportResponse& response) {
                ESP_LOGI(TAG, "Data report success: code=%d", response.code);
            },
            [](const std::string& error) {
                ESP_LOGE(TAG, "Data report failed: %s", error.c_str());
            }
        );
    }

    /**
     * 语音助手智能对话
     */
    void intelligentDialogue() {
        using namespace ai_sdk;

        auto& voiceAssistant = AIAssistantManager::getInstance().asrIntelligentDialogueHelp();

        voiceAssistant.setCallbacks(
            // 连接成功回调
            []() {
                ESP_LOGI(TAG, "ASR connected");
            },

            // ASR 识别结果回调
            [](const AsrResult& result) {
                if (result.is_final) {
                    ESP_LOGI(TAG, "ASR Final: %s", result.text.c_str());
                } else {
                    ESP_LOGD(TAG, "ASR Mid: %s", result.text.c_str());
                }
            },

            // 智能对话结果回调
            [](const DialogueResult& result) {
                // 返回数据包的具体 id
                const char* qid = result.qid.c_str();

                // 本次智能对话结束标志
                int isEnd = result.is_end;

                // 智能对话完整回复内容
                const char* content = result.assistant_answer_content.c_str();

                if (isEnd == 1) {
                    // 一次智能对话过程，返回的最后一个包
                    ESP_LOGI(TAG, "Dialogue complete: %s", content);
                }

                // 取出具体指令
                const char* directive = result.directive.c_str();

                // 图片渲染进度指令
                if (strcmp(directive, "RenderProcessing") == 0) {
                    ESP_LOGI(TAG, "RenderProcessing: %s", result.payload.c_str());
                }
                // 意图指令
                else if (strcmp(directive, "Nlu") == 0) {
                    ESP_LOGI(TAG, "Nlu: %s", result.payload.c_str());
                }
                // 意图标签指令
                else if (strcmp(directive, "NluTag") == 0) {
                    ESP_LOGI(TAG, "NluTag: %s", result.payload.c_str());
                }
                // 文生图结果指令
                else if (strcmp(directive, "RenderMultiImageCard") == 0) {
                    ESP_LOGI(TAG, "RenderMultiImageCard: %s", result.payload.c_str());
                }
                // 音乐播放指令
                else if (strcmp(directive, "Play") == 0) {
                    ESP_LOGI(TAG, "Play: %s", result.payload.c_str());
                }
                // 查询及问答、闲聊、文本创作流式输出指令
                else if (strcmp(directive, "RenderStreamCard") == 0) {
                    ESP_LOGD(TAG, "RenderStreamCard: %s", result.payload.c_str());
                }
                // 语音播放指令
                else if (strcmp(directive, "Speak") == 0) {
                    ESP_LOGI(TAG, "Speak: %s", result.payload.c_str());
                }
            },

            // 错误回调
            [](int code, const std::string& message) {
                ESP_LOGE(TAG, "ASR error [%d]: %s", code, message.c_str());
            },

            // 完成回调
            []() {
                ESP_LOGI(TAG, "ASR complete");
            }
        );

        // 启动语音识别
        if (voiceAssistant.start()) {
            ESP_LOGI(TAG, "ASR started");

            // 发送音频数据（示例）
            // asr.sendAudio(pcm_data, pcm_len);

            // 停止识别
            // asr.stop();
        }
    }
};
```

### 语音助手（含音频硬件 + TTS 回调）

适用于内置麦克风 + 扬声器的完整单轮对话场景（v0.9.5 推荐用法）：

```cpp
#include "ai_sdk/ai_assistant_manager.h"
#include "ai_sdk/audio/audio_config.h"
#include "esp_log.h"
#include <atomic>
#include <memory>

static const char* TAG = "VOICE_ASSISTANT";

// 用于跨任务安全追踪 TTS 状态的结构体
struct TtsRoundState {
    std::atomic_bool got_speak{false};   // 本轮是否收到 Speak 指令
    std::atomic_bool tts_all_done{false}; // 本轮所有 TTS 是否播放完成
};

void voice_assistant_example() {
    using namespace ai_sdk;

    // -------------------------------------------------------
    // 1. 创建并启动音频硬件（以无芯片单工方案为例）
    // -------------------------------------------------------
    AudioConfig audio_cfg;
    audio_cfg.hardware_type    = AudioHardwareType::kNoCodecSimplex;
    audio_cfg.input_sample_rate  = 16000;
    audio_cfg.output_sample_rate = 16000;
    // 麦克风引脚（根据实际硬件填写）
    audio_cfg.mic_sck  = GPIO_NUM_42;
    audio_cfg.mic_ws   = GPIO_NUM_41;
    audio_cfg.mic_din  = GPIO_NUM_2;
    // 扬声器引脚
    audio_cfg.spk_bclk = GPIO_NUM_17;
    audio_cfg.spk_ws   = GPIO_NUM_47;
    audio_cfg.spk_dout = GPIO_NUM_15;

    AudioCodec* codec = CreateAudioCodec(audio_cfg);
    codec->Start();
    codec->SetOutputVolume(80);

    // -------------------------------------------------------
    // 2. 获取 ASR 实例并绑定音频硬件
    // -------------------------------------------------------
    auto& asr = AIAssistantManager::getInstance().asrIntelligentDialogueHelp();
    asr.initAudio(codec);
    asr.setAutoPlayTts(true);  // 收到 Speak 指令时自动播放 TTS

    // -------------------------------------------------------
    // 3. 单轮对话循环
    // -------------------------------------------------------
    while (true) {
        auto tts_state = std::make_shared<TtsRoundState>();
        std::atomic_bool got_final_asr{false};
        std::atomic_bool got_complete{false};
        std::atomic_bool got_error{false};

        // 设置 TTS 播放回调（v0.9.5 新增）
        asr.setTtsPlaybackCallback(
            [tts_state](const std::string& /*url*/, bool /*completed*/, bool all_done) {
                if (all_done) {
                    tts_state->tts_all_done.store(true);
                }
            });

        // 设置 ASR + 对话回调
        asr.setCallbacks(
            nullptr,  // connected_cb

            // ASR 识别结果
            [&](const AsrResult& result) {
                if (result.is_final) {
                    // 首个最终结果后立即停止录音，释放上行带宽给 TTS 下载
                    if (!got_final_asr.exchange(true)) {
                        asr.setRecording(false);
                        ESP_LOGI(TAG, "ASR: %s", result.text.c_str());
                    }
                }
            },

            // 智能对话结果
            [&, tts_state](const DialogueResult& result) {
                if (result.directive == "Speak") {
                    tts_state->got_speak.store(true);
                    tts_state->tts_all_done.store(false);  // 重置，准备接收新一批
                } else if (result.directive == "RenderStreamCard") {
                    ESP_LOGD(TAG, "Text: %s", result.payload.c_str());
                }
                if (result.is_end == 1) {
                    ESP_LOGI(TAG, "Answer: %s", result.assistant_answer_content.c_str());
                }
            },

            // 错误回调
            [&](int code, const std::string& msg) {
                ESP_LOGE(TAG, "Error [%d]: %s", code, msg.c_str());
                got_error.store(true);
            },

            // 完成回调
            [&]() {
                got_complete.store(true);
            });

        // -------------------------------------------------------
        // 4. 启动本轮识别并开始录音
        // -------------------------------------------------------
        if (!asr.start()) {
            ESP_LOGE(TAG, "ASR start failed");
            vTaskDelay(pdMS_TO_TICKS(2000));
            continue;
        }
        asr.setRecording(true);

        // -------------------------------------------------------
        // 5. 等待本轮结束（ASR 完成 + TTS 播完）
        // -------------------------------------------------------
        while (!got_error) {
            bool asr_done = got_complete.load();
            bool tts_done = !tts_state->got_speak.load() || tts_state->tts_all_done.load();
            if (asr_done && tts_done) break;
            vTaskDelay(pdMS_TO_TICKS(100));
        }

        asr.stop();
        vTaskDelay(pdMS_TO_TICKS(200));  // 两轮对话之间短暂间隔
    }
}
```

### 持续语音识别（内置麦克风模式）

适用于需要持续识别多段语音（如字幕、会议记录）的场景（v0.9.6 新增）：

```cpp
#include "ai_sdk/ai_assistant_manager.h"
#include "ai_sdk/audio/audio_config.h"
#include "esp_log.h"

static const char* TAG = "PERSISTENT_ASR";

void persistent_asr_example() {
    using namespace ai_sdk;

    // 1. 创建并启动音频硬件（与语音助手用法相同）
    AudioConfig audio_cfg;
    audio_cfg.hardware_type      = AudioHardwareType::kNoCodecSimplex;
    audio_cfg.input_sample_rate  = 16000;
    audio_cfg.output_sample_rate = 16000;
    audio_cfg.mic_sck  = GPIO_NUM_42;
    audio_cfg.mic_ws   = GPIO_NUM_41;
    audio_cfg.mic_din  = GPIO_NUM_2;
    AudioCodec* codec = CreateAudioCodec(audio_cfg);
    codec->Start();

    // 2. 获取持续语音识别实例并初始化音频
    auto& asr = AIAssistantManager::getInstance().speechRecognitionPersistentHelp();
    asr.initAudio(codec);

    // 3. 设置回调
    asr.setCallbacks(
        // 识别结果回调（每段语音结束后触发一次）
        [](const SpeechRecognitionPersistentResult& result) {
            if (result.err_no == 0) {
                ESP_LOGI(TAG, "[%ldms - %ldms] %s",
                         result.start_time, result.end_time,
                         result.result.c_str());
            } else {
                ESP_LOGW(TAG, "识别失败 err_no=%d: %s",
                         result.err_no, result.err_msg.c_str());
            }
        },
        // 错误回调
        [](int code, const std::string& msg) {
            ESP_LOGE(TAG, "Error [%d]: %s", code, msg.c_str());
        },
        // 连接关闭回调
        []() {
            ESP_LOGI(TAG, "ASR connection closed");
        });

    // 4. 启动识别并开始录音
    if (asr.start()) {
        asr.setRecording(true);
        ESP_LOGI(TAG, "Persistent ASR started");
    }

    // 5. 业务逻辑决定何时停止
    // asr.stop();    // 优雅停止：等待最后一段结果
    // asr.cancel();  // 立即取消：不等待剩余结果
}
```

### 音频数据发送

```cpp
void send_audio_example() {
    using namespace ai_sdk;

    auto& voiceAssistant = AIAssistantManager::getInstance().asrIntelligentDialogueHelp();

    // 假设 audio_buffer 包含录音数据
    uint8_t audio_buffer[5120];
    size_t audio_len = sizeof(audio_buffer);

    // 发送音频数据
    voiceAssistant.sendAudio(audio_buffer, audio_len);
}
```

### 错误处理

```cpp
#include "ai_sdk/ai_assistant_manager.h"
#include "esp_log.h"

static const char* TAG = "AI_ERROR";

void error_handling_example() {
    using namespace ai_sdk;

    // 检查是否已初始化
    if (!AIAssistantManager::isInitialized()) {
        ESP_LOGE(TAG, "AI SDK not initialized");
        return;
    }

    try {
        auto& manager = AIAssistantManager::getInstance();
        // 使用 SDK...
    } catch (const std::exception& e) {
        ESP_LOGE(TAG, "Exception: %s", e.what());
    }
}
```

### 大模型闲聊

```cpp
#include "ai_sdk/ai_assistant_manager.h"
#include "ai_sdk/ai_foundation_kit.h"
#include "esp_log.h"

static const char* TAG = "CHATBOT_EXAMPLE";

void chatbot_example() {
    using namespace ai_sdk;

    auto& aiKit = AIAssistantManager::getInstance().aiFoundationKit();

    // 构建请求
    ChatbotCompletionRequest request;
    request.messages = {{"user", "你好，介绍一下你自己"}};
    request.stream = true;  // 流式输出

    // 发送请求
    std::string requestId = aiKit.largeModelChatbot(request,
        [](const ChatbotCompletionResponse& resp) {
            for (const auto& choice : resp.choices) {
                ESP_LOGI(TAG, "Response: %s", choice.delta.content.c_str());
            }
        },
        [](const std::string& error) {
            ESP_LOGE(TAG, "Error: %s", error.c_str());
        });

    // 取消请求（如果需要）
    // aiKit.cancelStreamRequest(requestId);
}
```

### 文本翻译

```cpp
#include "ai_sdk/ai_assistant_manager.h"
#include "ai_sdk/ai_foundation_kit.h"
#include "ai_sdk/types/language_code.h"
#include "esp_log.h"

static const char* TAG = "TRANSLATE_EXAMPLE";

void translate_example() {
    using namespace ai_sdk;

    auto& aiKit = AIAssistantManager::getInstance().aiFoundationKit();

    // 构建请求
    TranslationRequest request;
    request.targetLanguage = LanguageCode::ZH;  // 翻译为中文
    request.originText = "Hello, world!";

    // 机器翻译 (v1)
    aiKit.textTranslate(request,
        [](const TranslateResponse& resp) {
            ESP_LOGI(TAG, "翻译结果: %s", resp.data.translateText.c_str());
        },
        [](const std::string& error) {
            ESP_LOGE(TAG, "翻译失败: %s", error.c_str());
        });

    // 模型翻译 (v2)
    request.targetLanguage = LanguageCodeModel::ZH;
    aiKit.textTranslateWithModel(request,
        [](const TranslateResponse& resp) {
            ESP_LOGI(TAG, "模型翻译结果: %s", resp.data.translateText.c_str());
        },
        [](const std::string& error) {
            ESP_LOGE(TAG, "模型翻译失败: %s", error.c_str());
        });
}
```

### 内容摘要

```cpp
#include "ai_sdk/ai_assistant_manager.h"
#include "ai_sdk/ai_foundation_kit.h"
#include "esp_log.h"

static const char* TAG = "SUMMARY_EXAMPLE";

void content_summary_example() {
    using namespace ai_sdk;

    auto& aiKit = AIAssistantManager::getInstance().aiFoundationKit();

    // 构建请求
    ContentSummaryRequest request;
    request.content = "这是一段很长的会议记录，包含了多个议题的讨论内容...";
    request.stream = true;       // 流式输出
    request.language = "Chinese"; // 中文摘要

    // 发送请求
    std::string requestId = aiKit.contentSummary(request,
        [](const ContentSummaryResponse& resp) {
            if (resp.status == 0) {
                ESP_LOGI(TAG, "摘要: %s", resp.data.content.c_str());
            }
        },
        [](const std::string& error) {
            ESP_LOGE(TAG, "摘要失败: %s", error.c_str());
        });

    // 取消请求（如果需要）
    // aiKit.cancelStreamRequest(requestId);
}
```

### 文本链路智能问答（insideRcChat）

#### 单轮流式

```cpp
void example_inside_rc_chat_stream() {
    auto& manager = ai_sdk::AIAssistantManager::getInstance();
    auto& kit = manager.aiFoundationKit();
    auto& config = manager.config();

    ai_sdk::InsideRcChatRequest req;
    req.qid = "test-stream-001";
    req.third_user_id = "demo-user";
    req.cuid = config.deviceId;
    req.messages = {{"user", "讲一个笑话"}};
    req.stream = true;

    std::string requestId = kit.insideRcChat(req,
        [](const ai_sdk::DialogueResult& result) {
            if (result.is_end == 1) {
                ESP_LOGI(TAG, "[流式] 对话结束，回答: %s",
                    result.assistant_answer_content.c_str());
            } else {
                ESP_LOGI(TAG, "[流式] 指令: %s", result.directive.c_str());
            }
        },
        [](const std::string& error) {
            ESP_LOGE(TAG, "[流式] 错误: %s", error.c_str());
        });
}
```

#### 多轮对话

```cpp
void example_inside_rc_chat_multi_turn() {
    auto& manager = ai_sdk::AIAssistantManager::getInstance();
    auto& kit = manager.aiFoundationKit();
    auto& config = manager.config();

    ai_sdk::InsideRcChatRequest req;
    req.qid = "test-multi-001";
    req.third_user_id = "demo-user";
    req.cuid = config.deviceId;
    // messages 成员数必须为奇数，奇数位 role 为 "user"，偶数位为 "assistant"
    req.messages = {
        {"user",      "今天天气怎么样"},
        {"assistant", "今天北京晴天，气温25度"},
        {"user",      "那上海呢"}
    };
    req.stream = true;

    kit.insideRcChat(req,
        [](const ai_sdk::DialogueResult& result) {
            if (result.is_end == 1) {
                ESP_LOGI(TAG, "[多轮] 对话结束，回答: %s",
                    result.assistant_answer_content.c_str());
            } else {
                ESP_LOGI(TAG, "[多轮] 指令: %s", result.directive.c_str());
            }
        },
        [](const std::string& error) {
            ESP_LOGE(TAG, "[多轮] 错误: %s", error.c_str());
        });
}
```

## 6. 常见问题

### 初始化问题

**Q: SDK 初始化失败怎么办？**

A: 检查以下几点：
1. 确保 ESP-IDF 版本为 5.4.3+
2. 确保 C++ 异常和 RTTI 已启用
3. 检查配置参数是否完整（productId, productKey, deviceNo）
4. 查看日志中的具体错误信息

### 网络问题

**Q: 设备注册失败如何处理？**

A: 检查以下几点：
1. 确认 WiFi 已连接
2. 确认设备号已在云端平台录入
3. 确认 productId 和 productKey 正确
4. 检查网络防火墙设置

### 时间同步问题

**Q: 签名验证失败怎么办？**

A: SDK 使用时间戳进行签名，需要确保设备时间正确：
1. 启用 SNTP 时间同步
2. 在 SDK 初始化前完成时间同步
3. 示例代码：

```cpp
#include "esp_sntp.h"

void init_sntp() {
    esp_sntp_setoperatingmode(SNTP_OPMODE_POLL);
    esp_sntp_setservername(0, "pool.ntp.org");
    esp_sntp_init();

    // 等待时间同步完成
    time_t now = 0;
    struct tm timeinfo = { 0 };
    int retry = 0;
    while (timeinfo.tm_year < (2020 - 1900) && ++retry < 10) {
        vTaskDelay(pdMS_TO_TICKS(1000));
        time(&now);
        localtime_r(&now, &timeinfo);
    }
}
```

### 设备信息上报问题

**Q: sdkVersion 字段如何获取？**

A: SDK 内部自动维护 sdkVersion 字段的上报，厂商无需关注。

**Q: CMEI 码如何获取？**

A: CMEI 码为 15 位，规则如下：

| 编码 | 11 | 123456 | 123456 | 1 |
|------|-----|--------|--------|---|
| 位数 | 2位 | 6位 | 6位 | 1位 |
| 含义 | 非蜂窝智能硬件标识，固定为 11 | 随机码，区分产品品牌与型号 | 序列号，标识产品唯一性 | 校验位 |

## 7. 代理网关使用说明

### 不使用代理的情况
- 响应状态码 status = 0
- HTTP 状态码不等于 200
- 接口网络请求异常

### 代理请求规则

URL 转换示例：

```
原始请求：
https://aqua-digital.aipaas.com/smart-channel-aggregation-hubtest/smartChannel/asr

代理请求：
https://域名:端口/p/http/smart-channel-aggregation-hubtest/smartChannel/asr

新增Header：
X-AI-PROXY-PASS: https://aqua-digital.aipaas.com
```

必需的请求头：

| Header名 | 值 | 是否必须 | 说明 |
|----------|-----|----------|------|
| X-AI-PROXY-PASS | 原始请求URL | 是 | 完整原始请求地址 |
| X-AI-UID | xxxx | 是 | 设备ID（注册下发） |
| X-AI-VID | xxxx | 是 | 产品标识 |

## 8. 设备信息上报说明

### 请求参数

| 字段 | 说明 | 值类型 | 是否必填 |
|------|------|--------|----------|
| deviceId | 设备ID | string | 是 |
| deviceSecret | 设备密钥 | string | 是 |
| productId | 产品ID | string | 是 |
| productKey | 产品密钥 | string | 是 |
| params | 上报参数 | map | 是 |
| +innerIp | 内网IP | List\<string\> | 是 |
| +netSpeed | 网络分级 | string | 是 |
| +netType | 网络类型 | string | 是 |
| +platform | 操作系统 | string | 是 |
| +sdkVersion | SDK版本 | string | 是 |
| +firmwareVersion | 固件版本 | string | 是 |
| +imei | IMEI | string | 是* |
| +cmei | CMEI | string | 是* |
| +mac | MAC地址 | string | 是 |

*注：获取不到传空字符串

### 请求示例

```cpp
DeviceReportRequest request;
request.deviceId = "123456789";
request.deviceSecret = "xxxxx";
request.productId = "1234567890123";
request.productKey = "xxxxxxx";

// 添加参数
std::vector<std::string> innerIps = {"192.168.1.100"};
request.params["innerIp"] = innerIps;
request.params["netSpeed"] = std::string("0Mbps");
request.params["netType"] = std::string("WiFi");
request.params["platform"] = std::string("esp32s3");
request.params["firmwareVersion"] = std::string("1.0.0");
request.params["imei"] = std::string("");
request.params["cmei"] = std::string("");
request.params["mac"] = std::string("AA:BB:CC:DD:EE:FF");
```

## 9. ABI 兼容性注意事项

使用预编译静态库时，必须满足以下条件：

| 项目 | 要求 |
|------|------|
| ESP-IDF 版本 | 5.4.3（或以上版本） |
| 目标芯片 | ESP32 系列（ESP32/S2/S3/C2/C3/C5/C6/P4） |
| C++ 异常 | 启用 |
| C++ RTTI | 启用 |
| 优化级别 | Size (-Os)（建议一致） |

### IDF v5.5.1 已知问题

IDF v5.5.1 将 `CONFIG_ESP_TLS_USE_DS_PERIPHERAL` 的默认值改为 `y`（ESP32-S3），
导致 `esp_websocket_client_config_t` 结构体中出现条件编译字段，引起 ABI 不匹配。

**症状**：`LoadProhibited` 崩溃，`EXCVADDR=0x0000001c`（偏移量为 28，即一个指针字段）

**解决方案**：在 `sdkconfig.defaults` 中显式锁定该配置为 `n`：

```ini
# 锁定 DS 外设配置，避免 IDF v5.5.1 默认值变更导致 ABI 不匹配
CONFIG_ESP_TLS_USE_DS_PERIPHERAL=n
```

`ai_sdk_builder` 和 `ai_sdk_builder_demo` 的 `sdkconfig.defaults` 均已包含此配置（v0.9.6 修复）。

## 10. 更新日志

### [1.0.0] - 2026-06-22

> **里程碑**：首个正式稳定版本（GA）。接口与预编译库 ABI 自此进入稳定承诺期。
> **平台说明**：本版本预编译库（`libai_sdk.a`）更新了 **ESP32-S3** 平台。
> **兼容性**：从 0.9.x 升级**无破坏性接口变更**，可直接替换头文件与 `libai_sdk.a`。

#### Bug 修复

**修复大消息被拆帧导致的 `JSON parse failed`**

- **现象**：服务端返回较长回复（如带 Markdown 表格的长文本，超过 WebSocket
  `buffer_size=8192`）时，偶发 `Error parsing message: JSON parse failed`、
  `Mobile AI SDK error [-1]`，单轮对话被中断。
- **根因**：一条消息可能被两层拆分——① 协议层多帧（`fin` 标志，续帧 `op_code=0`）；
  ② 单帧超 buffer（同一帧分多次事件，`payload_offset` 递增）。旧逻辑拿到第一个
  分片就当完整 JSON 解析，导致截断失败。
- **修复**：重写 `WEBSOCKET_EVENT_DATA` 的消息重组逻辑，**两层拆分统一累积**进
  `message_buffer_`，仅当「当前帧收齐 且 是整条消息末帧（`fin`）」时才回调上层解析；
  首帧缓存 `op_code` 作为整条消息类型。

#### 健壮性增强

- **解析失败不再中断会话**：畸形/残缺分片改为「仅告警并丢弃」，不再抛错打断当前对话。
- **内存优化**：消息重组阶段预分配缓冲、减少不必要拷贝，降低长消息场景的内存抖动。
- **向后兼容**：以上改动对调用方接口透明，无需修改业务代码。

#### 平台兼容性

- 当前版本预编译库支持 **IDF 5.4.3** 。

---

### [0.9.9] - 2026-05-21

> **平台说明**：本版本预编译库（`libai_sdk.a`）仅更新了 **ESP32-S3** 平台。

#### Bug 修复

**修复 `SpeechRecognitionPersistent` AB-BA 死锁**

- **现象**：持续语音识别（持久 WebSocket）在断开/重连时序下整个服务挂起
- **根因**：持锁调用阻塞 I/O —— 持有 `state_mutex_` 时调用 `websocket_.disconnect()`，
  内部 `esp_websocket_client_destroy()` 等待客户端任务退出；而客户端任务收到
  `DISCONNECTED` 事件时反过来要获取同一把 `state_mutex_`
- **修复**：拆分“状态检查”与“断开调用”
  - 锁内只读写共享状态
  - 释放锁后再执行可能阻塞的 I/O
  - 调整错误回调顺序，避免引入新的死锁链

#### 平台兼容性

- 预编译库同时支持 **IDF 5.4.3** 和 **IDF 5.5.1**

---

### [0.9.8] - 2026-04-23

> **平台说明**：本版本预编译库（`libai_sdk.a`）仅更新了 **ESP32-S3** 平台。

#### 新增功能

**文本链路智能问答（insideRcChat）**
- 新增 `AIFoundationKit::insideRcChat()` 接口
- 文本直接发送智能对话后端，跳过 ASR 语音识别环节
- 返回与语音助手相同的 DCS 指令集（Speak / RenderStreamCard / RenderCard / Nlu 等）
- 支持流式和非流式两种模式
- 支持多轮对话（messages 历史上下文）
- 新增类型：`InsideRcChatMessage`、`InsideRcChatRequest`

**语音助手持续识别 Demo**
- 新增 `test_voice_assistant_persistent.cpp`，演示 `SpeechRecognitionPersistent` + `InsideRcChat` 完整联动链路
- PCM 文件驱动，无需硬件麦克风，支持多轮对话上下文保留

#### 平台兼容性

- 预编译库同时支持 IDF 5.4.3 和 IDF 5.5.1

### [0.9.7] - 2026-04-17

> **平台说明**：本版本预编译库（`libai_sdk.a`）仅更新了 **ESP32-S3** 平台。

#### 功能改进

**I2C 地址格式统一**

`AudioConfig` 的 I2C 地址字段（`codec_addr`、`es8311_addr`、`es7210_addr`）统一使用 **7-bit 行业标准格式**，与芯片手册一致。SDK 内部自动完成 7-bit → 8-bit 的格式转换，调用方无需关心底层驱动的地址约定。

受影响的编解码器驱动：ES8311、ES8388、ES8374、ES8389、BoxAudioCodec（ES8311 + ES7210）。

> **注意**：如果你之前使用 8-bit 地址（如 ES8311 传 `0x30`），升级后需改为 7-bit 地址（`0x18`）。
> 使用默认值的用户不受影响。

**ES8311 开发板语音助手示例**

`ai_sdk_builder_demo` 新增完整的 ES8311 硬件适配示例，包含：
- 根据原理图校准的 GPIO 引脚配置（I2C + I2S 双工 + PA 使能）

---

### [0.9.6] - 2026-04-08

> **平台说明**：本版本预编译库（`libai_sdk.a`）仅更新了 **ESP32-S3** 平台。

#### 新增功能

**持续语音识别（SpeechRecognitionPersistent）**

新增独立的持续语音识别模块，与 `AsrIntelligentDialogue` 并列：
- 纯 ASR 功能（无 NLU / TTS），适合字幕、会议记录等持续识别场景
- 端点：`/app-ws/v1/long-asr`，WebSocket 持久连接不断开
- 服务端 VAD 自动分割语音段，每段结束回调一次 `ResultCallback`
- 支持两种音频接入模式：
  - 模式一：外部音频（业务方自行采集，调用 `sendAudio()`）
  - 模式二：内置音频驱动（`initAudio(codec)` + `setRecording(true)`）
- 新增类型：`SpeechRecognitionPersistentResult`（含 `result`、`start_time`、`end_time`、`err_no` 等字段）
- `AIAssistantManager` 新增访问方法：`speechRecognitionPersistentHelp()`

#### Bug 修复

- 修复 IDF v5.5.1 下 `esp_websocket_client_config_t` 结构布局 ABI 不匹配
  导致的 `LoadProhibited` 崩溃（`EXCVADDR=0x0000001c`）。
  原因：`CONFIG_ESP_TLS_USE_DS_PERIPHERAL` 在 IDF v5.5.1 中对 ESP32-S3 默认值由 `n` 改为 `y`，
  引入条件编译字段，导致构建库与应用层结构体偏移不一致。
  修复：`sdkconfig.defaults` 显式锁定 `CONFIG_ESP_TLS_USE_DS_PERIPHERAL=n`。

---

### [0.9.5] - 2026-03-30

> **平台说明**：本版本预编译库（`libai_sdk.a`）仅更新了 **ESP32-S3** 平台。
> 其他芯片支持的功能，截止到 v0.9.4 版本。

#### 新增功能

**内置音频支持**

新增 `ai_sdk/audio/audio_config.h`，支持直接绑定硬件麦克风和扬声器：
- `AudioHardwareType` + `AudioConfig`：统一描述音频硬件，
  涵盖 ES8311/ES8374/ES8388/ES8389、ESP-BOX、I2S 直连等主流方案
- `CreateAudioCodec(config)`：工厂函数，按配置创建对应硬件驱动实例

`AsrIntelligentDialogue` 新增 4 个方法：
- `initAudio(codec)`：绑定音频硬件后，SDK 自动接管录音和 TTS 播放
- `setRecording(enable)`：控制麦克风采集开关
- `setAutoPlayTts(enable)`：控制收到 Speak 指令时是否自动播放 TTS（默认关闭）
- `setTtsPlaybackCallback(cb)`：设置 TTS 播放状态回调
  - `completed`：当前段是否正常播完
  - `all_done`：本轮全部 TTS 段是否全部结束

**情绪识别**

`AsrResult` 新增 `emotion` 字段，返回说话人情绪标签，仅最终识别结果（`is_final=true`）携带：
- 支持 12 种情绪：`happy`、`angry`、`dejected`、`wronged`、`thingking`、
  `terrified`、`smirk`、`confused`、`bored`、`dizzy`、`chaos`、`wink`
- 为空字符串表示服务器未返回情绪标签

#### 功能改进

- **TTS 多段顺序播放**：`Play()` 改为队列模式，多个 Speak 指令的音频片段按序播完，
  不再相互中断导致中间片段丢失
- **TTS 播放架构重构**：引入 PCM 环形缓冲区 + 独立输出任务（生产者/消费者解耦），
  有效抵抗网络抖动，消除播放卡顿
- **PCM 预缓冲时长**：400ms → 900ms，弱网环境下启播更稳定
- **PCM 帧输出精度**：使用 `vTaskDelayUntil` 保障 20ms 精确帧间隔，减少输出抖动
- **WebSocket 缓冲区**：4KB → 8KB，避免内部分片引发的 `transport_poll_write` 超时
- **ASR 发送超时**：1s → 1.5s，弱网下降低无效重连频率
- **ASR 录音自动暂停**：连接断开或会话未就绪时自动停止麦克风采集，减少无效 CPU 消耗

#### Bug 修复

- 修复 WebSocket 任务栈溢出崩溃（4096 → 8192 字节）
  — 调用链超 15 层（JSON 解析 → TTS Play → ESP_LOGI），实测需约 3700 字节
- 修复 TTS 播放任务 HTTPS/TLS 握手导致的 Guru Meditation 崩溃（4096 → 8192 字节）
- 修复 TTS 播放器日志中 `%zu` 在 newlib nano 下不稳定引发的崩溃（改用 `%u`）
- 修复生产环境 API 端点配置错误（曾误用测试环境标识符）

### [0.9.4] - 2026-03-03

#### Bug 修复
- 修复 ASR 语音识别完成后（fin_result）仍发送音频导致 transport_poll_write 错误
- 修复 ASR 智能对话中会话结束（dcs_decide end=1）时的音频发送问题
- 修复 WebSocket 客户端重连时的内存泄漏问题
- 修复 WebSocket 客户端重连时的资源清理问题
- 禁用 ESP-IDF 内部自动重连，避免与应用层重连逻辑冲突
- 修复音频通道开启时的错误状态重置问题

#### 安全增强
- 所有 HTTP/WebSocket 客户端启用系统证书包校验 (esp_crt_bundle_attach)
- 启用 TLS 主机名校验，防止证书与域名不匹配

### [0.9.3] - 2026-01-23

#### 新增功能
- 新增 **内容摘要（Content Summary）** 功能
  - `contentSummary()` - 对长文本进行智能摘要处理
  - 支持流式/非流式请求
  - 支持多语言输出（auto、Chinese、English、French 等）
- 新增数据类型：
  - `ContentSummaryRequest`, `ContentSummaryResponse`, `ContentData`
  - `ContentSummarySuccessCallback`, `ContentSummaryErrorCallback`

#### 功能改进
- `cancelStreamRequest()` 现支持取消内容摘要的流式请求
- 内容摘要客户端：实现内存安全改造（静态函数解析响应）

### [0.9.2] - 2026-01-22

#### 新增功能
- 新增 **AIFoundationKit** 模块，提供大模型闲聊和文本翻译功能
  - `largeModelChatbot()` - 大模型闲聊，支持流式/非流式请求
  - `textTranslate()` - 机器翻译（200+种语言）
  - `textTranslateWithModel()` - 模型翻译（~90种语言）
  - `cancelStreamRequest()` - 取消流式请求
- 新增数据类型：
  - `ChatbotMessage`, `ChatbotCompletionRequest`, `ChatbotCompletionResponse`
  - `TranslationRequest`, `TranslateResponse`
  - `LanguageCode`, `LanguageCodeModel` 语言代码常量

#### 接口变更
- **破坏性变更**: `aiFoundationKit()` 返回类型从 `void*` 改为 `AIFoundationKit&`

#### 功能改进
- HTTP 客户端：实现内存安全改造和异步非阻塞请求
- SSE 客户端：改进取消机制并完善错误处理回调
- Chatbot 客户端：新增服务端错误响应处理机制
- 翻译客户端：新增服务端错误响应处理机制
- 新增 ESP32 多芯片平台支持库（所有芯片预编译库更新）

#### Bug 修复
- 修复错误处理逻辑，返回完整响应数据
- SSE 客户端添加未处理数据检测和错误处理
- SSE 客户端日志修复

### [0.9.1] - 2026-1-6
- 支持 ESP32 系列全部芯片（ESP32/S2/S3/C2/C3/C5/C6/P4）
- 优化文档结构

### [0.9.0] - 2025-12-30
- 初始版本
- 支持设备注册、网关配置、数据上报
- 支持 ASR 语音识别与智能对话
- 新增 TtsConfig 配置
- 新增 token, clientID, enableVoiceGain 字段
- 新增日志控制接口（`ai_sdk::Log`）
- 支持运行时动态调整日志级别
- 优化内部日志级别（约 80 条 INFO 降级为 DEBUG）
- 默认 INFO 级别下，日志输出更简洁

