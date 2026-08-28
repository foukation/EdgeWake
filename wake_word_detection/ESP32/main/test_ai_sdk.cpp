#include "ai_sdk/ai_assistant_manager.h"
#include "ai_sdk/ai_sdk_log.h"
#include "ai_sdk/types/language_code.h"
#include "ai_sdk/types/language_code_model.h"
#include "esp_log.h"
#include "esp_sntp.h"
#include <ctime>
#include <atomic>
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "freertos/timers.h"  // FreeRTOS 软件定时器（用于流式请求超时取消）
#include "cJSON.h"

// 语音助手模块头文件（用于ASR语音识别和对话管理）
// AsrIntelligentDialogue 通过 AIAssistantManager::asrIntelligentDialogueHelp() 获取
// 注意：ai_sdk/asr_websocket.h 是内部头文件，不对外开放
// 上层业务应使用 ai_sdk/asr_intelligent_dialogue.h（已包含在 ai_assistant_manager.h 中）

static const char* TAG = "AI_SDK_TEST";

static void print_current_time(void) {
    time_t now;
    time(&now);

    struct tm timeinfo;
    localtime_r(&now, &timeinfo);

    char time_str[32];
    strftime(time_str, sizeof(time_str), "%Y-%m-%d %H:%M:%S", &timeinfo);

    ESP_LOGI(TAG, "Current time: %s", time_str);
}

// AI SDK 管理器实例
static ai_sdk::AIAssistantManager* g_manager = nullptr;

// Embedded PCM asset symbols generated from main/test_asr.pcm.
extern const uint8_t _binary_test_asr_pcm_start[] asm("_binary_test_asr_pcm_start");
extern const uint8_t _binary_test_asr_pcm_end[] asm("_binary_test_asr_pcm_end");

// 16kHz/16-bit/mono PCM stream pacing:
// 32000 bytes/s, 5120 bytes per chunk equals 160 ms audio.
static constexpr size_t kPcmChunkBytes = 5120;
static constexpr int kPcmChunkDelayMs = 160;
static constexpr int kPcmRestartDelayMs = 1000;

// Forward declaration: this test is triggered from test_device_information().
static void test_voice_assistant_from_embedded_pcm();
static void start_pcm_loop_test();
static void pcm_loop_test_task(void* pvParameters);

// Keep a single PCM loop task instance to avoid duplicate concurrent sessions.
static TaskHandle_t g_pcm_loop_task = nullptr;

// ============================================================================
// 流式请求取消测试相关
// ============================================================================

/**
 * @brief 当前活跃的流式请求 ID
 * 
 * 用于在定时器回调中取消正在进行的流式请求。
 * 
 * 使用场景：
 * - 测试 cancelStreamRequest() 功能
 * - 模拟用户中途打断对话
 * - 超时自动取消
 * 
 * 注意：
 * - 空字符串表示没有活跃的流式请求
 * - 同一时间只跟踪一个流式请求（测试代码顺序执行）
 */
static std::string g_stream_request_id;

/**
 * @brief 初始化 AI SDK
 *
 * 按照 Android AIAssistantManager 的设计进行初始化：
 * 1. 创建配置（使用 Builder 模式）
 * 2. 初始化管理器
 * 3. 获取实例
 *
 * @return true 初始化成功，false 失败
 */
static bool initialize_ai_sdk() {
    ESP_LOGI(TAG, "Initializing AI SDK...");

    // 开启 AI SDK 内部模块的 DEBUG 日志（AsrIntelligentDialogue、AsrWebsocket 等）
    // 用于查看服务器消息解析、Start Signal 内容、音频发送状态
    ai_sdk::Log::setLevel(ai_sdk::LogLevel::DEBUG);
    // 开启项目层 AiSdkProtocol 的 DEBUG 日志（对话结果详情、directive 类型）
    esp_log_level_set("AiSdkProtocol", ESP_LOG_DEBUG);

    // 1. 创建配置（类似 Android 的 Builder）
    auto builder = std::make_unique<ai_sdk::AIAssistConfig::Builder>();
    auto config = builder->deviceNo("NNNP03900162")
                          .deviceNoType("C86")
                          .productId("1988782995351662594")
                          .productKey("mRgQQjUgfBqRPLWH")
                          // 中控配置版本：用于智能对话（inside_rc）请求
                          // 服务器根据此版本号返回对应的对话配置
                          .centralConfigVersion("9")
                          .token("sk-crwLQ3MEel44LsGW1273601f7e6b472584634f4b27C35414")
                          .deviceId("")           // 初始为空，将从云端获取
                          .deviceSecret("")       // 初始为空，将从云端获取
                          .build();

    // 2. 初始化管理器（关键！类似 Android initialize）
    // 注意：ESP32 版本不需要 Context 参数
    ai_sdk::AIAssistantManager::initialize(std::move(config));

    // 3. 获取实例
    try {
        g_manager = &ai_sdk::AIAssistantManager::getInstance();
        ESP_LOGI(TAG, "AI SDK initialized successfully");
        return true;
    } catch (const std::exception& e) {
        ESP_LOGE(TAG, "Failed to get AI SDK instance: %s", e.what());
        return false;
    }
}

/**
 * @brief 测试网关访问（手动调用）
 *
 * 测试网关配置获取功能，与Android端API保持一致。
 * 注意：新的数据结构使用嵌套的data对象来存储HTTP和WebSocket地址。
 */
static void test_gateway_access() {
    ESP_LOGI(TAG, "=== Testing Gateway Access ===");

    if (!g_manager) {
        ESP_LOGE(TAG, "AI SDK not initialized!");
        return;
    }

    // 通过 gateWayHelp() 获取网关对象，然后调用 getGateWay()
    // 这与 Android 的调用方式完全一致：manager.gateWayHelp().getGateWay()
    g_manager->gateWayHelp().getGateWay(
        [](const ai_sdk::GatewayInfo& info, const std::string& message) {
            ESP_LOGI(TAG, "Gateway success:");
            ESP_LOGI(TAG, "  Token: %s", info.token.empty() ? "(empty)" : "***");
            ESP_LOGI(TAG, "  HTTP URL: %s", info.data.http.c_str());
            ESP_LOGI(TAG, "  WebSocket URL: %s", info.data.ws.c_str());
            ESP_LOGI(TAG, "  Expires in: %d seconds", info.expires);
            ESP_LOGI(TAG, "  Status: %d (1=USE, 0=NOT)", info.status);
            ESP_LOGI(TAG, "  Message: %s", message.c_str());

            // 根据状态码判断是否使用代理
            if (info.status == ai_sdk::AgentUseCode::USE) {
                ESP_LOGI(TAG, "  ✅ Agent will be used for subsequent requests");
            } else {
                ESP_LOGI(TAG, "  ❌ No agent will be used (direct connection)");
            }
        },
        [](const std::string& error) {
            ESP_LOGE(TAG, "Gateway error: %s", error.c_str());
        }
    );
}

/**
 * @brief 测试数据上报（手动调用）
 *
 * 测试设备数据上报功能，与Android端API保持一致。
 * 新的数据结构需要包含设备认证信息和参数数据。
 */
static void test_data_report() {
    ESP_LOGI(TAG, "=== Testing Data Report ===");

    if (!g_manager) {
        ESP_LOGE(TAG, "AI SDK not initialized!");
        return;
    }

    // 获取当前设备配置（包含deviceId和deviceSecret）
    const auto& config = g_manager->config();

    // 检查设备是否已认证
    if (config.deviceId.empty() || config.deviceSecret.empty()) {
        ESP_LOGE(TAG, "Device not authenticated! Need to call obtainDeviceInformation first");
        return;
    }

    // 构建上报请求（新的数据结构，与Android端一致）
    ai_sdk::DeviceReportRequest request;
    request.deviceId = config.deviceId;           // 设备唯一标识
    request.deviceSecret = config.deviceSecret;   // 设备密钥
    request.productId = config.productId;         // 产品ID
    request.productKey = config.productKey;       // 产品密钥

    // 添加设备参数（使用std::any支持不同类型）
    request.params["status"] = std::string("online");                    // 设备状态
    request.params["battery"] = std::string("85");                       // 电池电量
    request.params["temperature"] = 25.6;                                // 设备温度（double类型）
    request.params["wifi_rssi"] = -45;                                   // WiFi信号强度（int类型）
    request.params["netType"] = std::string("wifi");                     // 网络类型
    request.params["platform"] = std::string("ESP32");                   // 操作系统
    request.params["firmwareVersion"] = std::string("1.0.0");           // 固件版本

    // 可选：添加MAC地址（如果设备支持）
    // request.params["mac"] = std::string("AA:BB:CC:DD:EE:FF");

    ESP_LOGI(TAG, "Sending data report with device ID: %s", request.deviceId.c_str());
    ESP_LOGI(TAG, "Report parameters count: %zu", request.params.size());

    // 通过 gateWayHelp() 调用 dataReport()
    // 这与 Android 的调用方式完全一致：manager.gateWayHelp().dataReport()
    g_manager->gateWayHelp().dataReport(
        request,
        [](const ai_sdk::DeviceReportResponse& response) {
            ESP_LOGI(TAG, "✅ Data report success:");
            ESP_LOGI(TAG, "  Code: %d", response.code);
            ESP_LOGI(TAG, "  Success: %s", response.success ? "true" : "false");
            ESP_LOGI(TAG, "  Message: %s", response.message.c_str());
            ESP_LOGI(TAG, "  Device ID: %s", response.data.deviceId.c_str());
            ESP_LOGI(TAG, "  Protocol Type Time: %s", response.data.protocolTypeTime.c_str());

            // 检查状态码（code == 200 或 success == true 表示成功）
            if (response.code == 200 || response.success) {
                ESP_LOGI(TAG, "  ✅ Report processed successfully");
            } else {
                ESP_LOGW(TAG, "  ⚠️ Report processed with warnings (code: %d)", response.code);
            }
        },
        [](const std::string& error) {
            ESP_LOGE(TAG, "❌ Data report error: %s", error.c_str());
        }
    );
}

// ============================================================================
// AIFoundationKit 测试函数
// ============================================================================

/**
 * @brief 流式请求取消定时器回调函数
 * 
 * 当定时器到期时，自动取消当前正在进行的流式请求。
 * 用于演示和测试 AIFoundationKit::cancelStreamRequest() 功能。
 * 
 * 调用链：
 *   定时器到期 
 *   → stream_cancel_timer_callback() 
 *   → AIFoundationKit::cancelStreamRequest(requestId)
 *   → ChatbotClient::cancelRequest(requestId)
 *   → SSEClient::cancelRequest(requestId)
 *   → 设置 cancelled 标志，中断流式读取
 * 
 * @param xTimer 定时器句柄（本回调未使用）
 */
static void stream_cancel_timer_callback(TimerHandle_t xTimer) {
    (void)xTimer;  // 未使用参数，避免编译警告
    
    if (!g_stream_request_id.empty() && g_manager) {
        auto& kit = g_manager->aiFoundationKit();
        
        // 调用 SDK 的取消接口
        bool success = kit.cancelStreamRequest(g_stream_request_id);
        
        if (success) {
            ESP_LOGW(TAG, "Stream request cancelled after timeout, ID: %s", 
                     g_stream_request_id.c_str());
        } else {
            // 请求可能已经完成或不存在
            ESP_LOGI(TAG, "Stream request already completed or not found, ID: %s", 
                     g_stream_request_id.c_str());
        }
        
        // 清空请求 ID
        g_stream_request_id.clear();
    }
}

/**
 * @brief 测试 Chatbot 大模型闲聊功能
 *
 * 通过 aiFoundationKit() 访问 AI 功能基础工具包，调用 largeModelChatbot() 方法。
 * 这与 Android 的调用方式完全一致：manager.aiFoundationKit().largeModelChatbot()
 *
 * 功能说明：
 * - 支持流式和非流式两种模式
 * - 流式模式下，服务器会逐步返回生成的内容
 * - 非流式模式下，服务器会一次性返回完整内容
 *
 * 注意事项：
 * - 需要先完成设备认证（obtainDeviceInformation）
 * - 需要配置有效的 token（通过 AIAssistConfig::Builder::token() 设置）
 * - 流式请求可以通过 cancelStreamRequest() 取消
 *
 * @see AIFoundationKit::largeModelChatbot()
 * @see ChatbotCompletionRequest
 * @see ChatbotCompletionResponse
 */
static void test_chatbot() {
    ESP_LOGI(TAG, "=== Testing Chatbot (Large Model Chat) ===");

    if (!g_manager) {
        ESP_LOGE(TAG, "AI SDK not initialized!");
        return;
    }

    // 获取 AIFoundationKit 实例
    // 这与 Android 的调用方式完全一致：manager.aiFoundationKit()
    auto& kit = g_manager->aiFoundationKit();

    // 构建 Chatbot 请求
    ai_sdk::ChatbotCompletionRequest request;

    // 设置对话消息（支持多轮对话）
    // role: "user" 表示用户消息，"assistant" 表示助手消息，"system" 表示系统消息
    request.messages = {
        {"system", "系统提示词:您好，我是中国移动的智能助理灵犀。如果您询问我的身份，我会回答:\"您好，我是中国移动智能助理灵犀\"。我能够回答各种类型的问题。如果遇到我无法解答的问题，我会告知您:您的问题超出了我的能力范围，请尝试问其他问题。"},
        {"user", "写一篇100字的作文"}
    };

    // 设置模型名称（根据实际可用的模型配置）
    // 注意：使用 jiutian-lan 模型（与 Android 端保持一致）
    request.model = "jiutian-lan";

    // 启用流式返回（推荐，可以实时显示生成内容）
    request.stream = true;

    // 可选参数：温度（0-2，值越高回答越随机）
    request.temperature = 0.7;

    // 可选参数：top_p（0-1，核采样参数）
    request.top_p = 0.9;

    ESP_LOGI(TAG, "Sending chatbot request (stream=%s)...", request.stream ? "true" : "false");

    // 发送请求并获取请求ID（用于取消流式请求）
    std::string requestId = kit.largeModelChatbot(
        request,
        // 成功回调（流式模式下会多次调用）
        [](const ai_sdk::ChatbotCompletionResponse& response) {
            // 处理响应
            for (const auto& choice : response.choices) {
                // 流式模式：内容在 delta 中
                if (!choice.delta.content.empty()) {
                    ESP_LOGI(TAG, "Chatbot (stream): %s", choice.delta.content.c_str());
                }

                // 非流式模式：内容在 message 中
                if (!choice.message.content.empty()) {
                    ESP_LOGI(TAG, "Chatbot (complete): %s", choice.message.content.c_str());
                }

                // 检查是否完成
                if (!choice.finish_reason.empty()) {
                    ESP_LOGI(TAG, "Chatbot finished, reason: %s", choice.finish_reason.c_str());
                }
            }

            // 打印 token 使用情况（非流式模式下可用）
            if (response.usage.total_tokens > 0) {
                ESP_LOGI(TAG, "Token usage: prompt=%d, completion=%d, total=%d",
                         response.usage.prompt_tokens,
                         response.usage.completion_tokens,
                         response.usage.total_tokens);
            }

        },
        // 错误回调
        [](const std::string& error) {
            ESP_LOGE(TAG, "Chatbot error: %s", error.c_str());
        }
    );

    // ========================================================================
    // 流式请求取消演示
    // ========================================================================
    // 
    // 流式请求返回 requestId，可用于中途取消请求。
    // 非流式请求返回空字符串，无法取消。
    // 
    // 本演示：2 秒后自动取消流式请求，测试 cancelStreamRequest() 功能。
    // 实际应用场景：
    // - 用户点击"停止生成"按钮
    // - 超时保护
    // - 切换到新对话
    // ========================================================================
    
    if (!requestId.empty()) {
        ESP_LOGI(TAG, "Chatbot stream request sent, ID: %s", requestId.c_str());
        
        // 保存请求 ID 到全局变量，供定时器回调使用
        g_stream_request_id = requestId;
        
        // 定时器句柄（局部 static，避免重复创建）
        static TimerHandle_t cancel_timer = nullptr;
        
        if (cancel_timer == nullptr) {
            // 首次调用：创建定时器
            cancel_timer = xTimerCreate(
                "stream_cancel",          // 定时器名称（调试用）
                pdMS_TO_TICKS(5000),      // 超时时间：2 秒
                pdFALSE,                  // 单次触发（不自动重载）
                nullptr,                  // 定时器 ID（未使用）
                stream_cancel_timer_callback  // 回调函数
            );
            
            if (cancel_timer == nullptr) {
                ESP_LOGE(TAG, "Failed to create cancel timer");
            }
        } else {
            // 后续调用：重置定时器周期
            xTimerChangePeriod(cancel_timer, pdMS_TO_TICKS(2000), 0);
        }
        
        // 启动定时器
        if (cancel_timer != nullptr) {
            if (xTimerStart(cancel_timer, 0) == pdPASS) {
                ESP_LOGI(TAG, "Cancel timer started, will cancel request in 2 seconds");
            } else {
                ESP_LOGE(TAG, "Failed to start cancel timer");
            }
        }
    } else {
        // 非流式请求
        ESP_LOGI(TAG, "Chatbot request sent (non-stream mode)");
    }
}

/**
 * @brief 测试文本翻译功能 - 机器翻译（v1 API）
 *
 * 通过 aiFoundationKit() 访问 AI 功能基础工具包，调用 textTranslate() 方法。
 * 这与 Android 的调用方式完全一致：manager.aiFoundationKit().textTranslate()
 *
 * 功能说明：
 * - 使用机器翻译引擎（v1 API）
 * - 支持 200+ 种语言
 * - 翻译速度快，适合实时翻译场景
 *
 * 语言代码：
 * - 使用 LanguageCode 命名空间中的常量
 * - 例如：LanguageCode::ZH（中文简体）、LanguageCode::EN（英语）
 *
 * @see AIFoundationKit::textTranslate()
 * @see TranslationRequest
 * @see TranslateResponse
 * @see LanguageCode
 */
static void test_text_translate() {
    ESP_LOGI(TAG, "=== Testing Text Translate (Machine Translation v1) ===");

    if (!g_manager) {
        ESP_LOGE(TAG, "AI SDK not initialized!");
        return;
    }

    // 获取 AIFoundationKit 实例
    auto& kit = g_manager->aiFoundationKit();

    // 构建翻译请求
    ai_sdk::TranslationRequest request;

    // 设置目标语言（使用 LanguageCode 命名空间中的常量）
    // 机器翻译支持 200+ 种语言
    request.targetLanguage = ai_sdk::LanguageCode::ZH;  // 翻译为中文简体

    // 设置源语言（可选，AUTO 表示自动检测）
    request.sourceLanguage = ai_sdk::LanguageCode::AUTO;

    // 设置要翻译的文本
    request.originText = "Hello, how are you today? The weather is nice.";

    ESP_LOGI(TAG, "Translating: \"%s\"", request.originText.c_str());
    ESP_LOGI(TAG, "Target language: %s", request.targetLanguage.c_str());

    // 发送翻译请求
    kit.textTranslate(
        request,
        // 成功回调
        [](const ai_sdk::TranslateResponse& response) {
            ESP_LOGI(TAG, "Translation success:");
            ESP_LOGI(TAG, "  Translated text: %s", response.data.translateText.c_str());
            ESP_LOGI(TAG, "  Source language: %s", response.data.sourceLanguage.c_str());
            ESP_LOGI(TAG, "  Target language: %s", response.data.targetLanguage.c_str());
        },
        // 错误回调
        [](const std::string& error) {
            ESP_LOGE(TAG, "Translation error: %s", error.c_str());
        }
    );

    ESP_LOGI(TAG, "Translation request sent (Machine Translation v1)");
}

/**
 * @brief 测试文本翻译功能 - 模型翻译（v2 API）
 *
 * 通过 aiFoundationKit() 访问 AI 功能基础工具包，调用 textTranslateWithModel() 方法。
 * 这与 Android 的调用方式完全一致：manager.aiFoundationKit().textTranslateWithModel()
 *
 * 功能说明：
 * - 使用大模型翻译引擎（v2 API）
 * - 支持约 90 种语言
 * - 翻译质量更高，更适合复杂文本和专业领域
 *
 * 语言代码：
 * - 使用 LanguageCodeModel 命名空间中的常量（与 LanguageCode 不同！）
 * - 例如：LanguageCodeModel::ZH（中文简体）、LanguageCodeModel::EN（英语）
 *
 * 注意事项：
 * - 模型翻译的语言代码与机器翻译不同，请使用正确的命名空间
 * - 模型翻译速度相对较慢，但翻译质量更高
 *
 * @see AIFoundationKit::textTranslateWithModel()
 * @see TranslationRequest
 * @see TranslateResponse
 * @see LanguageCodeModel
 */
static void test_text_translate_with_model() {
    ESP_LOGI(TAG, "=== Testing Text Translate (Model Translation v2) ===");

    if (!g_manager) {
        ESP_LOGE(TAG, "AI SDK not initialized!");
        return;
    }

    // 获取 AIFoundationKit 实例
    auto& kit = g_manager->aiFoundationKit();

    // 构建翻译请求
    ai_sdk::TranslationRequest request;

    // 设置目标语言（使用 LanguageCodeModel 命名空间中的常量）
    // 注意：模型翻译的语言代码与机器翻译不同！
    request.targetLanguage = ai_sdk::LanguageCodeModel::ZH;  // 翻译为中文简体

    // 设置源语言（可选，AUTO 表示自动检测）
    request.sourceLanguage = ai_sdk::LanguageCodeModel::AUTO;

    // 设置要翻译的文本（模型翻译更适合复杂文本）
    request.originText = "Artificial intelligence is transforming the way we live and work.";

    ESP_LOGI(TAG, "Translating: \"%s\"", request.originText.c_str());
    ESP_LOGI(TAG, "Target language: %s (using Model Translation)", request.targetLanguage.c_str());

    // 发送翻译请求（使用模型翻译）
    kit.textTranslateWithModel(
        request,
        // 成功回调
        [](const ai_sdk::TranslateResponse& response) {
            ESP_LOGI(TAG, "Model Translation success:");
            ESP_LOGI(TAG, "  Translated text: %s", response.data.translateText.c_str());
            ESP_LOGI(TAG, "  Source language: %s", response.data.sourceLanguage.c_str());
            ESP_LOGI(TAG, "  Target language: %s", response.data.targetLanguage.c_str());
        },
        // 错误回调
        [](const std::string& error) {
            ESP_LOGE(TAG, "Model Translation error: %s", error.c_str());
        }
    );

    ESP_LOGI(TAG, "Translation request sent (Model Translation v2)");
}

/**
 * @brief 测试内容摘要功能
 *
 * 通过 aiFoundationKit() 访问 AI 功能基础工具包，调用 contentSummary() 方法。
 *
 * 功能说明：
 * - 对长文本进行智能摘要处理
 * - 支持流式和非流式两种模式
 * - 流式模式下，服务器会逐步返回生成的摘要内容
 *
 * 注意事项：
 * - 需要先完成设备认证（obtainDeviceInformation）
 * - 流式请求可以通过 cancelStreamRequest() 取消
 *
 * @see AIFoundationKit::contentSummary()
 * @see ContentSummaryRequest
 * @see ContentSummaryResponse
 */
static void test_content_summary() {
    ESP_LOGI(TAG, "=== Testing Content Summary ===");

    if (!g_manager) {
        ESP_LOGE(TAG, "AI SDK not initialized!");
        return;
    }

    // 获取 AIFoundationKit 实例
    auto& kit = g_manager->aiFoundationKit();

    // 构建内容摘要请求
    ai_sdk::ContentSummaryRequest request;

    // 设置需要摘要的文本内容
    request.content = "Sulaiman Ghori 在一期播客中，用了一个多小时详细讲述了他在 xAI 的经历。他说，在那里“从来没有人对我说不”，每个人都被充分信任去做正确的事；只要是好想法，当天就能落地、当天就能得到反馈。他还提到，马斯克愿意被证明是错的，只要你能拿出实验数据。";

    // 启用流式返回（推荐，可以实时显示生成内容）
    request.stream = true;

    // 设置摘要语言（可选，默认 "auto"）
    request.language = "Chinese";

    ESP_LOGI(TAG, "Sending content summary request (stream=%s)...", request.stream ? "true" : "false");
    ESP_LOGI(TAG, "Content length: %zu bytes", request.content.length());

    // 发送请求并获取请求ID（用于取消流式请求）
    std::string requestId = kit.contentSummary(
        request,
        // 成功回调（流式模式下会多次调用）
        [](const ai_sdk::ContentSummaryResponse& response) {
            // 打印摘要内容
            // 注意：content 中可能包含换行符 \n，ESP_LOGI 会将其解释为真正的换行
            // 导致日志被拆分到多行，后续行没有日志前缀，容易被覆盖
            // 这里将 \n 替换为 \\n 以便在日志中完整显示
            if (!response.data.content.empty()) {
                std::string content = response.data.content;
                // 替换换行符为可见字符，避免日志截断
                size_t pos = 0;
                while ((pos = content.find('\n', pos)) != std::string::npos) {
                    content.replace(pos, 1, "\\n");
                    pos += 2;
                }
                ESP_LOGI(TAG, "Content Summary (stream): %s", content.c_str());
            }

            // 打印状态信息
            if (response.status != 0) {
                ESP_LOGI(TAG, "Content Summary status: %d, msg: %s", 
                         response.status, response.msg.c_str());
            }

            // 打印日志ID（用于调试）
            if (!response.logId.empty()) {
                ESP_LOGD(TAG, "LogId: %s", response.logId.c_str());
            }
        },
        // 错误回调
        [](const std::string& error) {
            ESP_LOGE(TAG, "Content Summary error: %s", error.c_str());
        }
    );

    // ========================================================================
    // 流式请求取消演示
    // ========================================================================
    // 
    // 流式请求返回 requestId，可用于中途取消请求。
    // 非流式请求返回空字符串，无法取消。
    // 
    // 本演示：3 秒后自动取消流式请求，测试 cancelStreamRequest() 功能。
    // 实际应用场景：
    // - 用户点击"停止生成"按钮
    // - 超时保护
    // - 切换到新任务
    // ========================================================================
    
    if (!requestId.empty()) {
        ESP_LOGI(TAG, "Content Summary stream request sent, ID: %s", requestId.c_str());
        
        // 保存请求 ID 到全局变量，供定时器回调使用
        g_stream_request_id = requestId;
        
        // 定时器句柄（局部 static，避免重复创建）
        static TimerHandle_t content_summary_cancel_timer = nullptr;
        
        if (content_summary_cancel_timer == nullptr) {
            // 首次调用：创建定时器
            content_summary_cancel_timer = xTimerCreate(
                "content_summary_cancel",  // 定时器名称（调试用）
                pdMS_TO_TICKS(5000),       // 超时时间：3 秒
                pdFALSE,                   // 单次触发（不自动重载）
                nullptr,                   // 定时器 ID（未使用）
                stream_cancel_timer_callback  // 复用现有的回调函数
            );
            
            if (content_summary_cancel_timer == nullptr) {
                ESP_LOGE(TAG, "Failed to create content summary cancel timer");
            }
        } else {
            // 后续调用：重置定时器周期
            xTimerChangePeriod(content_summary_cancel_timer, pdMS_TO_TICKS(3000), 0);
        }
        
        // 启动定时器
        if (content_summary_cancel_timer != nullptr) {
            if (xTimerStart(content_summary_cancel_timer, 0) == pdPASS) {
                ESP_LOGI(TAG, "Cancel timer started, will cancel request in 3 seconds");
            } else {
                ESP_LOGE(TAG, "Failed to start content summary cancel timer");
            }
        }
    } else {
        // 非流式请求
        ESP_LOGI(TAG, "Content Summary request sent (non-stream mode)");
    }
}

// ============================================================================
// GateWay 测试函数
// ============================================================================

/**
 * @brief 测试设备信息获取（通过 GateWay）
 *
 * 通过 gateWayHelp() 访问网关对象，然后调用 obtainDeviceInformation()
 * 这与 Android 的调用方式完全一致：manager.gateWayHelp().obtainDeviceInformation()
 *
 * 重要说明：
 * - 不能直接调用 g_manager->obtainDeviceInformation()（该方法已在 AIAssistantManager 中删除）
 * - 必须通过 gateWayHelp() 访问 GateWay 对象
 * - 回调函数处理成功和错误情况
 *
 * 参考 Android: manager.gateWayHelp().obtainDeviceInformation(onSuccess, onError)
 */
static void test_device_information() {
    ESP_LOGI(TAG, "=== Testing Device Information ===");

    if (!g_manager) {
        ESP_LOGE(TAG, "AI SDK not initialized!");
        return;
    }

    // 通过 gateWayHelp() 调用 obtainDeviceInformation()
    // 这与 Android 的调用方式完全一致
    g_manager->gateWayHelp().obtainDeviceInformation(
        [](const ai_sdk::DeviceInfoResponse& response) {
            ESP_LOGI(TAG, "Device info success:");
            ESP_LOGI(TAG, "  Status: %d", response.code);  // 修改为code字段
            ESP_LOGI(TAG, "  Device ID: %s", response.data.deviceId.c_str());
            ESP_LOGI(TAG, "  Device Secret: %s", response.data.deviceSecret.c_str());

        // 测试 2: 网关访问（手动调用）
        ESP_LOGI(TAG, "Test 2: Gateway Access");
        // test_gateway_access();
        vTaskDelay(pdMS_TO_TICKS(3000));

        // 测试 3: 数据上报（手动调用）
        ESP_LOGI(TAG, "Test 3: Data Report");
        // test_data_report();
        vTaskDelay(pdMS_TO_TICKS(3000));

        // ============================================
        // 测试 4: 语音助手连接（暂时禁用）
        // 启用 CONFIG_USE_AI_SDK_PROTOCOL 后，AiSdkProtocol 会使用 AsrIntelligentDialogue 单例
        // 如果同时运行此测试，会导致重复连接冲突
        // ============================================
        ESP_LOGI(TAG, "Test 4: Embedded PCM Voice Assistant");
        //start_pcm_loop_test();

        // ============================================
        // 测试 5: Chatbot 大模型闲聊（AIFoundationKit）
        // ============================================
        ESP_LOGI(TAG, "Test 5: Chatbot");
        // test_chatbot();
        // vTaskDelay(pdMS_TO_TICKS(5000));

        // ============================================
        // 测试 6: 文本翻译 - 机器翻译（AIFoundationKit）
        // ============================================
        ESP_LOGI(TAG, "Test 6: Text Translate (Machine)");
        // test_text_translate();
        vTaskDelay(pdMS_TO_TICKS(3000));

        // ============================================
        // 测试 7: 文本翻译 - 模型翻译（AIFoundationKit）
        // ============================================
        ESP_LOGI(TAG, "Test 7: Text Translate (Model)");
        // test_text_translate_with_model();
        vTaskDelay(pdMS_TO_TICKS(3000));

        // ============================================
        // 测试 8: 内容摘要（AIFoundationKit）
        // ============================================
        ESP_LOGI(TAG, "Test 8: Content Summary");
        // test_content_summary();
        vTaskDelay(pdMS_TO_TICKS(5000));

        },
        [](const std::string& error) {
            ESP_LOGE(TAG, "Device info error: %s", error.c_str());
        }
    );

    ESP_LOGI(TAG, "Device information request sent, check logs for results");
}


/**
 * @brief 定时打印当前时间的任务
 *
 * @param pvParameters 任务参数（未使用）
 *
 * @details
 * - 每10秒打印一次当前系统时间
 * - 使用 FreeRTOS 延时实现精确的时间间隔
 * - 任务会一直运行，直到手动删除
 * - 适合长时间运行的监控和日志记录
 */
static void time_print_task(void *pvParameters)
{
    ESP_LOGI(TAG, "Time print task started");

    while (1) {
        // 打印当前时间
        print_current_time();

        // 等待10秒（pdMS_TO_TICKS将毫秒转换为FreeRTOS时钟节拍）
        vTaskDelay(pdMS_TO_TICKS(10000));
    }
}

/**
 * @brief AI SDK 测试任务（核心测试流程）
 *
 * 该任务演示了如何使用新的 AI SDK（与 Android 设计完全一致）：
 *
 * 测试步骤：
 * 1. 等待时间同步（ESP32 需要准确的时间戳）
 * 2. 初始化 AI SDK（使用 AIAssistantManager::initialize）
 * 3. 测试完整的设备启动流程（调用 obtainDeviceInformation）
 * 4. 可选择测试单独的功能
 *
 * 重要说明：
 * - 本任务完全遵循 Android AIAssistantManager 的使用模式
 * - 所有操作都通过 AIAssistantManager 实例完成
 * - 网关操作统一通过 gateWayHelp() 访问
 * - 这是与 Android 项目保持一致的示例代码
 *
 * 与旧版的区别：
 * - ❌ 旧版：直接创建 GatewayClient、DeviceClient、ReportClient
 * - ✅ 新版：通过 AIAssistantManager::gateWayHelp() 统一访问
 *
 * @param arg 任务参数（未使用）
 */
/**
 * @brief Stream embedded PCM data to ASR for end-to-end session verification.
 */
/**
 * @brief Start the PCM loop task once.
 *
 * This function is idempotent. If the loop task is already running,
 * it will only print an info log and return.
 */
static void start_pcm_loop_test() {
    if (g_pcm_loop_task != nullptr) {
        ESP_LOGI(TAG, "[PCM_TEST] Loop task already running");
        return;
    }

    BaseType_t rc = xTaskCreate(
        pcm_loop_test_task,
        "pcm_loop_test",
        8192,
        nullptr,
        5,
        &g_pcm_loop_task);

    if (rc != pdPASS) {
        g_pcm_loop_task = nullptr;
        ESP_LOGE(TAG, "[PCM_TEST] Failed to create PCM loop task");
        return;
    }

    ESP_LOGI(TAG, "[PCM_TEST] Loop task started");
}

/**
 * @brief Continuous PCM regression loop.
 *
 * Each round starts a new single-turn ASR session. The next round is
 * started 1 second after the previous round receives onComplete.
 */
static void pcm_loop_test_task(void* pvParameters) {
    (void)pvParameters;

    int round = 0;
    while (true) {
        ++round;
        ESP_LOGI(TAG, "[PCM_TEST] Round %d begin", round);
        test_voice_assistant_from_embedded_pcm();
        ESP_LOGI(TAG, "[PCM_TEST] Round %d complete, restart after %d ms",
                 round, kPcmRestartDelayMs);
        vTaskDelay(pdMS_TO_TICKS(kPcmRestartDelayMs));
    }
}

static void test_voice_assistant_from_embedded_pcm() {
    ESP_LOGI(TAG, "=== Testing Voice Assistant (Embedded PCM Stream) ===");

    if (!g_manager) {
        ESP_LOGE(TAG, "AI SDK not initialized!");
        return;
    }

    auto& asr = g_manager->asrIntelligentDialogueHelp();

    std::atomic_bool got_final_result{false};
    std::atomic_bool got_dialogue_end{false};
    std::atomic_bool got_complete{false};
    std::atomic_bool got_error{false};

    asr.setCallbacks(
        []() {
            ESP_LOGI(TAG, "[PCM_TEST] ASR connected");
        },
        [&](const ai_sdk::AsrResult& result) {
            if (result.is_final) {
                got_final_result.store(true);
                ESP_LOGI(TAG, "[PCM_TEST] Final ASR result: %s", result.text.c_str());
                // Log emotion label returned by server (empty if not provided)
                ESP_LOGI(TAG, "[PCM_TEST] Emotion: %s",
                         result.emotion.empty() ? "(none)" : result.emotion.c_str());
            }
        },
        [&](const ai_sdk::DialogueResult& result) {
            if (result.is_end == 1) {
                got_dialogue_end.store(true);
                ESP_LOGI(TAG, "[PCM_TEST] Dialogue end received, answer: %s",
                         result.assistant_answer_content.c_str());
            }
        },
        [&](int code, const std::string& message) {
            got_error.store(true);
            ESP_LOGE(TAG, "[PCM_TEST] ASR error: code=%d, message=%s", code, message.c_str());
        },
        [&]() {
            got_complete.store(true);
            ESP_LOGI(TAG, "[PCM_TEST] Session complete callback received");
        }
    );

    const uint8_t* pcm_start = _binary_test_asr_pcm_start;
    const uint8_t* pcm_end = _binary_test_asr_pcm_end;
    const size_t total_bytes = static_cast<size_t>(pcm_end - pcm_start);
    if (total_bytes == 0) {
        ESP_LOGE(TAG, "[PCM_TEST] Embedded PCM asset is empty");
        return;
    }
    if ((total_bytes & 0x1U) != 0U) {
        ESP_LOGW(TAG, "[PCM_TEST] PCM size is odd (%u), expected 16-bit alignment",
                 (unsigned)total_bytes);
    }

    const int total_chunks = static_cast<int>((total_bytes + kPcmChunkBytes - 1) / kPcmChunkBytes);
    ESP_LOGI(TAG, "[PCM_TEST] Asset size=%u bytes, chunk=%u bytes, chunks=%d",
             (unsigned)total_bytes, (unsigned)kPcmChunkBytes, total_chunks);

    if (!asr.start()) {
        ESP_LOGE(TAG, "[PCM_TEST] Failed to start ASR session");
        return;
    }

    const int64_t send_begin_us = esp_timer_get_time();
    size_t offset = 0;
    int chunk_index = 0;
    while (offset < total_bytes) {
        const size_t remaining = total_bytes - offset;
        const size_t chunk_len = (remaining > kPcmChunkBytes) ? kPcmChunkBytes : remaining;

        asr.sendAudio(pcm_start + offset, chunk_len);
        offset += chunk_len;
        ++chunk_index;

        ESP_LOGI(TAG, "[PCM_TEST] Sent chunk %d/%d, len=%u, progress=%u/%u",
                 chunk_index, total_chunks, (unsigned)chunk_len, (unsigned)offset, (unsigned)total_bytes);

        if (got_error.load()) {
            ESP_LOGE(TAG, "[PCM_TEST] Stop sending due to ASR error");
            break;
        }

        vTaskDelay(pdMS_TO_TICKS(kPcmChunkDelayMs));
    }

    const int64_t send_elapsed_ms = (esp_timer_get_time() - send_begin_us) / 1000;
    ESP_LOGI(TAG, "[PCM_TEST] Send phase elapsed=%lld ms", (long long)send_elapsed_ms);

    // No timeout here by design: restart is strictly driven by onComplete.
    while (!got_complete.load()) {
        vTaskDelay(pdMS_TO_TICKS(100));
    }

    asr.stop();

    ESP_LOGI(TAG,
             "[PCM_TEST] Summary: final_result=%d, dialogue_end=%d, complete=%d, error=%d",
             got_final_result.load() ? 1 : 0,
             got_dialogue_end.load() ? 1 : 0,
             got_complete.load() ? 1 : 0,
             got_error.load() ? 1 : 0);
}

void ai_sdk_test_task(void* arg) {
    ESP_LOGI(TAG, "AI SDK test task started");

    // ============================================
    // 步骤 1: 等待时间同步完成
    // ============================================
    // ESP32 需要准确的时间戳，因为：
    // 1. SSL/TLS 证书验证需要准确时间
    // 2. 上报数据的时间戳需要准确
    // 3. 定时任务调度需要准确时间
    ESP_LOGI(TAG, "Waiting for time synchronization...");
    int wait_seconds = 0;
    while (wait_seconds < 20) {
        time_t now;
        time(&now);

        // 检查时间是否已同步（如果时间 > 2021年，说明已同步）
        if (now > 1609459200) {
            ESP_LOGI(TAG, "Time synchronization completed!");
            break;
        }

        ESP_LOGI(TAG, "Waiting... %d seconds", wait_seconds);
        vTaskDelay(pdMS_TO_TICKS(1000));
        wait_seconds++;
    }

    // 处理时间同步超时的情况
    if (wait_seconds >= 20) {
        ESP_LOGW(TAG, "SNTP sync timeout, but continuing test (timestamps may be inaccurate)");
    }

    // 打印当前时间（用于验证时间同步）
    print_current_time();
    ESP_LOGI(TAG, "Time sync step completed");

    // ============================================
    // 步骤 2: 初始化 AI SDK
    // ============================================
    // 这是关键改变点！现在使用 AIAssistantManager 统一管理
    // 类似 Android: AIAssistantManager.initialize(context, config)
    if (!initialize_ai_sdk()) {
        ESP_LOGE(TAG, "AI SDK initialization failed! Cannot proceed with tests.");
        vTaskDelete(NULL);
        return;
    }
    ESP_LOGI(TAG, "AI SDK initialization completed");

    // 等待一小段时间，确保初始化完成
    vTaskDelay(pdMS_TO_TICKS(1000));

    // ============================================
    // 步骤 3: 测试 GateWay 功能
    // ============================================
    // 这些功能都通过 gateWayHelp() 访问，与 Android 完全一致
    // 注意：AIAssistantManager 不再直接提供这些方法
    // 必须通过 manager.gateWayHelp().xxx() 调用
    ESP_LOGI(TAG, "Testing GateWay features...");

    // 测试 1: 设备信息获取（通过 gateWayHelp）
    ESP_LOGI(TAG, "Test 1: Device Information");
    test_device_information();
    vTaskDelay(pdMS_TO_TICKS(3000));

    // 这些功能也可以手动调用，用于调试或特殊场景
    // 注释掉的部分可以根据需要启用
    ESP_LOGI(TAG, "Optional individual feature tests (commented out)");

    // ============================================
    // 测试完成
    // ============================================
    ESP_LOGI(TAG, "All test steps completed! Check logs above for results.");
    vTaskDelete(NULL);
}

/**
 * @brief AI-SDK 测试入口函数（主入口）
 *
 * 这是 AI SDK 测试的入口函数，演示了如何像 Android 一样使用 AI SDK。
 *
 * 核心特点：
 * - 完全遵循 Android AIAssistantManager 的设计模式
 * - 统一通过 AIAssistantManager 管理所有 AI 功能
 * - 网关操作统一通过 gateWayHelp() 访问
 * - 初始化流程：initialize() -> getInstance() -> 调用功能
 *
 * 调用示例：
 * // 1. 创建配置
 * auto builder = std::make_unique<AIAssistConfig::Builder>();
 * auto config = builder->deviceNo("device-001")
 *                      ->productId("PROD001")
 *                      ->productKey("KEY001")
 *                      ->build();
 *
 * // 2. 初始化管理器（类似 Android：AIAssistantManager.initialize(config)）
 * // 注意：ESP32 版本不需要 Context 参数
 * AIAssistantManager::initialize(std::move(config));
 *
 * // 3. 获取实例并调用方法
 * auto& manager = AIAssistantManager::getInstance();
 * manager.obtainDeviceInformation();  // 会自动调用 getGateWay() -> dataReport()
 *
 * 日志输出：
 * - 启动日志（表明测试开始）
 * - 时间同步日志
 * - SDK 初始化日志
 * - 设备注册日志
 * - 网关配置日志
 * - 数据上报日志
 * - 定时时间输出（每10秒）
 *
 * 注意事项：
 * - 在实际设备上运行前，需要配置正确的 productId 和 productKey
 * - 需要先连接 WiFi（时间同步和网络通信需要）
 * - 栈大小设置为 8192，确保有足够的内存用于证书和网络操作
 */
extern "C" void test_ai_sdk_functions(void)
{
    ESP_LOGI(TAG, "========================================");
    ESP_LOGI(TAG, "Starting AI-SDK Test (Android Compatible)");
    ESP_LOGI(TAG, "========================================");

    // 打印当前时间（用于验证时间同步）
    print_current_time();
    ESP_LOGI(TAG, "Current time printed");

    // ========================================
    // 创建测试任务
    // ========================================
    // 栈大小设置为 8192 字节的原因：
    // 1. SSL/TLS 握手需要大量内存
    // 2. JSON 解析需要额外栈空间
    // 3. 网络缓冲区分配
    // 4. 回调函数调用链
    ESP_LOGI(TAG, "Creating AI SDK test task (stack: 8192 bytes, priority: 5)");
    xTaskCreate(ai_sdk_test_task, "ai_sdk_test", 8192, NULL, 5, NULL);
    ESP_LOGI(TAG, "AI SDK test task created successfully");

    // ========================================
    // 创建时间打印任务（可选，用于监控）
    // ========================================
    // 每 10 秒打印一次当前时间，用于：
    // 1. 验证时间同步是否正常
    // 2. 检查系统是否正常运行
    // 3. 监控任务调度
    ESP_LOGI(TAG, "Creating time monitor task (stack: 2048 bytes, priority: 3)");
    xTaskCreate(time_print_task, "time_print", 2048, NULL, 3, NULL);
    ESP_LOGI(TAG, "Time monitor task created successfully");

    // ========================================
    // 测试启动完成
    // ========================================
    ESP_LOGI(TAG, "Test framework initialized. Check logs above for results.");
    ESP_LOGI(TAG, "Expected log sequence:");
    ESP_LOGI(TAG, "  1. Time synchronization");
    ESP_LOGI(TAG, "  2. AI SDK initialization");
    ESP_LOGI(TAG, "  3. Device registration (obtainDeviceInformation)");
    ESP_LOGI(TAG, "  4. Gateway configuration (getGateWay)");
    ESP_LOGI(TAG, "  5. Initial data report (dataReport)");
    ESP_LOGI(TAG, "  6. Periodic time output (every 10 seconds)");
}
