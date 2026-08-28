/**
 * @file test_ai_sdk.cpp
 * @brief AI SDK 功能演示
 *
 * 演示 AI SDK 的初始化和设备接入完整流程：
 * 1. 设置日志级别
 * 2. 构建 SDK 配置（凭证从 demo_config.h 读取）
 * 3. 初始化 AIAssistantManager 单例
 * 4. 调用 obtainDeviceInformation 获取设备信息
 * 5. 打印返回的设备 ID、设备密钥等信息
 * 6. 调用 dataReport 完成设备启动上报（心跳，fire-and-forget）
 * 7. 启动业务逻辑
 */
#include "ai_sdk/ai_assistant_manager.h"
#include "ai_sdk/ai_sdk_log.h"
#include "demo_config.h"
#include "esp_log.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"

// PCM 流式 ASR 循环测试（定义在 test_asr_pcm.cpp）
extern "C" void start_pcm_loop_test(void);

// 实时麦克风语音助手循环测试（定义在 test_voice_assistant.cpp）
extern "C" void start_voice_assistant_test(void);

// 持续语音识别 PCM 文件测试（定义在 test_speech_recognition_persistent.cpp）
extern "C" void start_speech_recognition_persistent_pcm_test(void);

// 持续语音识别实时麦克风测试（定义在 test_speech_recognition_persistent.cpp）
extern "C" void start_speech_recognition_persistent_voice_test(void);

// 文本链路智能问答测试（定义在 test_inside_rc_chat.cpp）
extern "C" void start_inside_rc_chat_test(void);

// 语音助手持续识别测试（定义在 test_voice_assistant_persistent.cpp）
// SpeechRecognitionPersistent（持续 ASR）+ InsideRcChat（对话），PCM 文件驱动，无需硬件
extern "C" void start_voice_assistant_persistent_test(void);

// 大模型闲聊测试（定义在 test_chatbot.cpp，HTTPS，无需硬件）
extern "C" void start_chatbot_test(void);

// 文本翻译测试（定义在 test_translate.cpp，HTTPS，无需硬件）
extern "C" void start_translate_test(void);

// 内容摘要测试（定义在 test_content_summary.cpp，HTTPS，无需硬件）
extern "C" void start_content_summary_test(void);

static const char* TAG = "AI_SDK_DEMO";

// ============================================================================
// 设备启动数据上报（心跳）
// ============================================================================

/**
 * @brief 向平台上报设备状态（启动心跳）
 *
 * 规范要求：设备至少每 24 小时上报一次，建议启动时立即调用。
 * 此函数为 fire-and-forget，异步执行，不阻塞调用方。
 *
 * 前提：必须在 obtainDeviceInformation 成功后调用，
 * 此时 SDK 内部 config 中的 deviceId / deviceSecret 已由云端写入。
 */
static void do_data_report()
{
    auto& mgr = ai_sdk::AIAssistantManager::getInstance();
    const auto& cfg = mgr.config();

    ai_sdk::DeviceReportRequest req;
    req.deviceId     = cfg.deviceId;
    req.deviceSecret = cfg.deviceSecret;
    req.productId    = cfg.productId;
    req.productKey   = cfg.productKey;

    // 上报基础设备信息，供平台监控设备状态
    req.params["netType"]         = std::string("wifi");
    req.params["platform"]        = std::string("ESP32-S3");
    req.params["firmwareVersion"] = std::string("1.0.0");

    mgr.gateWayHelp().dataReport(
        req,
        // 上报成功：仅记录日志，不驱动业务流程
        [](const ai_sdk::DeviceReportResponse& resp) {
            ESP_LOGI(TAG, "Data report success: code=%d, %s",
                     resp.code, resp.message.c_str());
        },
        // 上报失败：仅记录日志，不影响业务
        [](const std::string& err) {
            ESP_LOGW(TAG, "Data report failed (non-fatal): %s", err.c_str());
        }
    );
}

// ============================================================================
// AI SDK Demo Task
// ============================================================================

static void ai_sdk_demo_task(void* arg)
{
    (void)arg;

    // ========================================================================
    // 1. 设置 AI SDK 日志级别为 DEBUG（便于调试）
    // ========================================================================
    ai_sdk::Log::setLevel(ai_sdk::LogLevel::DEBUG);

    // ========================================================================
    // 2. 构建 SDK 配置（Builder 模式）
    //    凭证从 demo_config.h 中的宏定义读取
    // ========================================================================
    auto config = std::make_unique<ai_sdk::AIAssistConfig::Builder>()
        ->deviceNo(DEMO_DEVICE_NO)
        .deviceNoType(DEMO_DEVICE_NO_TYPE)
        .productId(DEMO_PRODUCT_ID)
        .productKey(DEMO_PRODUCT_KEY)
        .centralConfigVersion(DEMO_CENTRAL_CONFIG_VER)
        .token(DEMO_TOKEN)
        .deviceId("")          // 初始为空，由 obtainDeviceInformation 从云端获取
        .deviceSecret("")      // 初始为空，由 obtainDeviceInformation 从云端获取
        .build();

    // ========================================================================
    // 3. 初始化 AIAssistantManager 单例
    //    全局只需调用一次，后续通过 getInstance() 获取实例
    // ========================================================================
    ai_sdk::AIAssistantManager::initialize(std::move(config));
    auto& manager = ai_sdk::AIAssistantManager::getInstance();
    ESP_LOGI(TAG, "AI SDK initialized successfully");

    // ========================================================================
    // 4. 获取设备信息（设备注册）
    //    前提：设备号已在云端平台录入
    //    成功后 SDK 会自动更新内部的 deviceId 和 deviceSecret
    // ========================================================================
    ESP_LOGI(TAG, "Requesting device information from cloud ...");
    ESP_LOGI(TAG, "  deviceNo:     %s", DEMO_DEVICE_NO);
    ESP_LOGI(TAG, "  deviceNoType: %s", DEMO_DEVICE_NO_TYPE);
    ESP_LOGI(TAG, "  productId:    %s", DEMO_PRODUCT_ID);

    manager.gateWayHelp().obtainDeviceInformation(
        // 成功回调
        [](const ai_sdk::DeviceInfoResponse& resp) {
            ESP_LOGI(TAG, "========================================");
            ESP_LOGI(TAG, "  Device registration success!");
            ESP_LOGI(TAG, "========================================");
            ESP_LOGI(TAG, "  code:         %d", resp.code);
            ESP_LOGI(TAG, "  message:      %s", resp.message.c_str());
            ESP_LOGI(TAG, "  deviceId:     %s", resp.data.deviceId.c_str());
            ESP_LOGI(TAG, "  deviceNo:     %s", resp.data.deviceNo.c_str());
            ESP_LOGI(TAG, "  productId:    %s", resp.data.productId.c_str());
            ESP_LOGI(TAG, "  deviceSecret: %s", resp.data.deviceSecret.c_str());
            ESP_LOGI(TAG, "========================================");

            // 启动数据上报（fire-and-forget，不阻塞后续业务）
            do_data_report();

            // ========================================
            // 启动业务逻辑
            // ========================================
            // 方式 1：嵌入 PCM 文件流式 ASR 测试（无需硬件）
            // start_pcm_loop_test();
            //
            // 方式 2：实时麦克风语音助手（需要 I2S 麦克风 + 扬声器）
            //start_voice_assistant_test();
            //
            // 方式 3：持续语音识别 PCM 文件测试（无需硬件）
            start_speech_recognition_persistent_pcm_test();
            //
            // 方式 4：持续语音识别实时麦克风（需要 I2S 麦克风）
            //start_speech_recognition_persistent_voice_test();
            //
            // 方式 5：文本链路智能问答（insideRcChat，无需硬件）
            // start_inside_rc_chat_test();
            //
            // 方式 6：语音助手持续识别（SpeechRecognitionPersistent + InsideRcChat，无需硬件）
            // start_voice_assistant_persistent_test();
            //
            // 方式 7：大模型闲聊（largeModelChatbot，无需硬件）
            // start_chatbot_test();
            //
            // 方式 8：文本翻译（textTranslate v1 + textTranslateWithModel v2，无需硬件）
            // start_translate_test();
            //
            // 方式 9：内容摘要（contentSummary，无需硬件）
            // start_content_summary_test();
        },
        // 错误回调
        [](const std::string& err) {
            ESP_LOGE(TAG, "========================================");
            ESP_LOGE(TAG, "  Device registration FAILED!");
            ESP_LOGE(TAG, "  Error: %s", err.c_str());
            ESP_LOGE(TAG, "========================================");
            ESP_LOGE(TAG, "Please check:");
            ESP_LOGE(TAG, "  1. demo_config.h credentials are correct");
            ESP_LOGE(TAG, "  2. Device is registered on cloud platform");
            ESP_LOGE(TAG, "  3. Network connection is stable");
        }
    );

    // 保持任务运行，等待异步回调触发
    while (true) {
        vTaskDelay(pdMS_TO_TICKS(5000));
    }
}

// ============================================================================
// 入口函数（由 app_main 调用）
// ============================================================================

extern "C" void test_ai_sdk_functions(void)
{
    ESP_LOGI(TAG, "Starting AI SDK Demo ...");

    // 任务栈 12288 字节：SSL 握手 + JSON 解析需要充足的栈空间
    xTaskCreate(ai_sdk_demo_task, "ai_sdk_demo", 12288, NULL, 5, NULL);
}
