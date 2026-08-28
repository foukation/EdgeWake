/**
 * @file test_translate.cpp
 * @brief 文本翻译（textTranslate / textTranslateWithModel）功能演示
 *
 * 演示两种翻译方式（均为非流式，翻译能力不支持流式）：
 * 1. 机器翻译 v1（textTranslate）：使用机器翻译引擎，支持 200+ 种语言
 * 2. 模型翻译 v2（textTranslateWithModel）：使用大模型翻译，质量更高，支持约 90 种语言
 *
 * 语言代码：
 * - 机器翻译 v1 使用 LanguageCode 中的常量
 * - 模型翻译 v2 使用 LanguageCodeModel 中的常量
 *
 * @note 需要在设备注册成功后调用（deviceId 和 deviceSecret 已获取）
 */
#include "ai_sdk/ai_assistant_manager.h"
#include "ai_sdk/ai_foundation_kit.h"
#include "ai_sdk/types/language_code.h"
#include "ai_sdk/types/language_code_model.h"
#include "esp_log.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"

static const char* TAG = "TranslateTest";

// 测试例句（对齐 Android 测试）
static const char* kSampleText =
    "春节期间，社交媒体上的一些外国人,对中国这一传统小吃赞不绝口。"
    "如今，肉夹馍也成为文化交流的使者。向世界展现一个富足、美味的中国";

// ============================================================================
// 辅助函数：打印翻译结果
// ============================================================================
static void log_translate_response(const char* tag, const ai_sdk::TranslateResponse& resp)
{
    ESP_LOGI(TAG, "%s code=%d, msg=%s", tag, resp.code, resp.msg.c_str());
    ESP_LOGI(TAG, "%s 译文: %s", tag, resp.data.translateText.c_str());
    ESP_LOGI(TAG, "%s 源语言: %s -> 目标语言: %s",
        tag, resp.data.sourceLanguage.c_str(), resp.data.targetLanguage.c_str());
}

// ============================================================================
// 测试 1：机器翻译 v1（textTranslate）中译英
// ============================================================================

static void test_machine_translate()
{
    ESP_LOGI(TAG, "========================================");
    ESP_LOGI(TAG, "  测试 1: 机器翻译 v1（中 -> 英）");
    ESP_LOGI(TAG, "========================================");
    auto& manager = ai_sdk::AIAssistantManager::getInstance();
    auto& kit = manager.aiFoundationKit();

    ai_sdk::TranslationRequest req;
    req.originText = kSampleText;
    req.targetLanguage = ai_sdk::LanguageCode::EN;   // 目标语言：英语
    req.sourceLanguage = ai_sdk::LanguageCode::ZH;   // 源语言：中文

    kit.textTranslate(req,
        [](const ai_sdk::TranslateResponse& resp) {
            log_translate_response("[机器翻译]", resp);
        },
        [](const std::string& error) {
            ESP_LOGE(TAG, "[机器翻译] 错误: %s", error.c_str());
        });
}

// ============================================================================
// 测试 2：模型翻译 v2（textTranslateWithModel）中译英
// ============================================================================

static void test_model_translate()
{
    ESP_LOGI(TAG, "========================================");
    ESP_LOGI(TAG, "  测试 2: 模型翻译 v2（中 -> 英）");
    ESP_LOGI(TAG, "========================================");

    auto& manager = ai_sdk::AIAssistantManager::getInstance();
    auto& kit = manager.aiFoundationKit();

    ai_sdk::TranslationRequest req;
    req.originText = kSampleText;
    req.targetLanguage = ai_sdk::LanguageCodeModel::EN;   // 目标语言：英语
    req.sourceLanguage = ai_sdk::LanguageCodeModel::ZH;   // 源语言：中文

    kit.textTranslateWithModel(req,
        [](const ai_sdk::TranslateResponse& resp) {
            log_translate_response("[模型翻译]", resp);
        },
        [](const std::string& error) {
            ESP_LOGE(TAG, "[模型翻译] 错误: %s", error.c_str());
        });
}

// ============================================================================
// 测试任务入口
// ============================================================================

static void translate_test_task(void* arg)
{
    (void)arg;

    ESP_LOGI(TAG, "========================================");
    ESP_LOGI(TAG, "  文本翻译测试（v1 机器 + v2 模型）");
    ESP_LOGI(TAG, "========================================");

    // 测试 1：机器翻译 v1
    test_machine_translate();
    vTaskDelay(pdMS_TO_TICKS(10000));

    // 测试 2：模型翻译 v2
    test_model_translate();

    ESP_LOGI(TAG, "========================================");
    ESP_LOGI(TAG, "  所有测试已启动（异步执行中）");
    ESP_LOGI(TAG, "========================================");

    // 保持任务运行，等待异步回调完成
    while (true) {
        vTaskDelay(pdMS_TO_TICKS(10000));
    }
}

// ============================================================================
// 外部入口（由 test_ai_sdk.cpp 调用）
// ============================================================================

extern "C" void start_translate_test(void)
{
    ESP_LOGI(TAG, "Starting textTranslate test ...");

    // 栈空间 12288 字节：HTTP/SSL 请求需要充足的栈空间
    xTaskCreate(translate_test_task, "translate_test", 12288, NULL, 5, NULL);
}
