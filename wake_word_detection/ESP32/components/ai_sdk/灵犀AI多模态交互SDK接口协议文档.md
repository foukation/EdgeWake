# AI-SDK (ESP32 Series)

## 1. 概述

### 版本信息
- SDK 版本：0.9.4
- 发布日期：2025-12-30
- 文档更新：2026-03-03

### 系统要求
- ESP-IDF 版本：5.4.3+
- 目标芯片：ESP32 系列（ESP32/S2/S3/C2/C3/C5/C6/P4）
- C++ 标准：C++17
- C++ 异常：启用（CONFIG_COMPILER_CXX_EXCEPTIONS=y）
- C++ RTTI：启用（CONFIG_COMPILER_CXX_RTTI=y）

### 主要功能
- 设备管理（设备信息、网关配置、数据上报）
- 语音助手
- AI 基础工具包（大模型闲聊、文本翻译、内容摘要）
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
│       │       ├── ai_assistant_manager.h
│       │       ├── ai_foundation_kit.h
│       │       ├── ai_sdk_log.h
│       │       ├── asr_intelligent_dialogue.h
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

### 智能语音识别
语音识别和对话功能：
- 实时语音识别
- 智能对话交互
- 语音命令处理

### AI 基础工具包
AI 功能模块：
- 大模型闲聊（Chatbot）
  - 支持流式/非流式请求
  - 支持取消流式请求
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
    using ConnectedCallback = std::function<void()>;
    using AsrCallback = std::function<void(const AsrResult&)>;
    using DialogueCallback = std::function<void(const DialogueResult&)>;
    using ErrorCallback = std::function<void(int, const std::string&)>;
    using CompleteCallback = std::function<void()>;

    // 设置回调函数
    void setCallbacks(
        ConnectedCallback connected_cb,
        AsrCallback asr_cb,
        DialogueCallback dialogue_cb,
        ErrorCallback error_cb,
        CompleteCallback complete_cb
    );

    // 启动 ASR 识别
    bool start();

    // 停止 ASR 识别
    void stop();

    // 发送音频数据（PCM 16-bit 16kHz 单声道）
    void sendAudio(const uint8_t* data, size_t len);

    // 检查连接状态
    bool isConnected() const;

    // 检查识别状态
    bool isRecognizing() const;
};
```

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

    // 大模型闲聊
    std::string largeModelChatbot(
        const ChatbotCompletionRequest& request,
        ChatbotSuccessCallback onSuccess,
        ChatbotErrorCallback onError);

    // 取消流式请求（支持 Chatbot 和内容摘要）
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
        if (asr.start()) {
            ESP_LOGI(TAG, "ASR started");

            // 发送音频数据（示例）
            // asr.sendAudio(pcm_data, pcm_len);

            // 停止识别
            // asr.stop();
        }
    }
};
```

### 音频数据发送

```cpp
#include "ai_sdk/ai_assistant_manager.h"

// 音频要求：
// - 格式：PCM 16-bit 小端序
// - 采样率：16000 Hz
// - 通道数：单声道
// - 建议块大小：5120 字节（160ms 音频）

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

## 10. 更新日志

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

