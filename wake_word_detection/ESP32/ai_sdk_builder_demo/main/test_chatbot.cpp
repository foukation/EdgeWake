/**
 * @file test_chatbot.cpp
 * @brief 大模型闲聊（largeModelChatbot）功能演示
 *
 * 测试方式对齐 Android FullyCompatibleSDK 的 largeModelChatbot()：
 * - 单次调用（不拆多用例）
 * - messages = system 人设 + user 提问("今天天气怎么样？")
 * - model = "jiutian-lan"，temperature = 0.1，top_p = 0.1，stream = true
 * - 流式请求返回 requestId，可用 cancelStreamRequest() 取消（示例见注释）
 *
 * @note 需要在设备注册成功后调用（deviceId 和 deviceSecret 已获取）
 * @note Chatbot 需要在 demo_config.h 中配置 DEMO_TOKEN（Bearer 授权令牌）
 */
#include "ai_sdk/ai_assistant_manager.h"
#include "ai_sdk/ai_foundation_kit.h"
#include "esp_log.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"

static const char* TAG = "ChatbotTest";

// ============================================================================
// 大模型闲聊（对齐 Android largeModelChatbot()）
// ============================================================================

static void test_chatbot()
{
    ESP_LOGI(TAG, "========================================");
    ESP_LOGI(TAG, "  largeModelChatbot 大模型闲聊测试");
    ESP_LOGI(TAG, "========================================");

    auto& manager = ai_sdk::AIAssistantManager::getInstance();
    auto& kit = manager.aiFoundationKit();

    // messages：第一条 system 人设，第二条 user 提问（与 Android 一致）
    ai_sdk::ChatbotCompletionRequest req;
    req.messages = {
        {"system", "您好，我是中国移动的智能助理灵犀。如果您询问我的身份，我会回答:您好，"
                   "我是中国移动智能助理灵犀。回答用户非人设或身份问题时，不要重复你的人设设定。"},
        {"user", "今天天气怎么样？"}
    };
    req.model = "jiutian-lan";   // 默认模型名（与 Android 一致）
    req.temperature = 0.1;
    req.top_p = 0.1;
    req.stream = false;

    std::string requestId = kit.largeModelChatbot(req,
        [](const ai_sdk::ChatbotCompletionResponse& resp) {
            // Chatbot 响应与 OpenAI 兼容：流式增量在 choices[].delta.content
            for (const auto& choice : resp.choices) {
                if (!choice.delta.content.empty()) {
                    ESP_LOGI(TAG, "response(delta): %s", choice.delta.content.c_str());
                }
                if (!choice.message.content.empty()) {
                    ESP_LOGI(TAG, "response(message): %s", choice.message.content.c_str());
                }
            }
        },
        [](const std::string& error) {
            ESP_LOGE(TAG, "error: %s", error.c_str());
        });

    ESP_LOGI(TAG, "流式请求 ID: %s", requestId.c_str());

    // 如需取消（对齐 Android 中注释掉的 postDelayed cancelStreamRequest）：
    // vTaskDelay(pdMS_TO_TICKS(3000));
    // kit.cancelStreamRequest(requestId);
    (void)requestId;
}

// ============================================================================
// 测试任务入口
// ============================================================================

static void chatbot_test_task(void* arg)
{
    (void)arg;

    test_chatbot();

    // 保持任务运行，等待异步回调完成
    while (true) {
        vTaskDelay(pdMS_TO_TICKS(10000));
    }
}

// ============================================================================
// 外部入口（由 test_ai_sdk.cpp 调用）
// ============================================================================

extern "C" void start_chatbot_test(void)
{
    ESP_LOGI(TAG, "Starting largeModelChatbot test ...");

    // 栈空间 12288 字节：HTTP/SSL 请求需要充足的栈空间
    xTaskCreate(chatbot_test_task, "chatbot_test", 12288, NULL, 5, NULL);
}
