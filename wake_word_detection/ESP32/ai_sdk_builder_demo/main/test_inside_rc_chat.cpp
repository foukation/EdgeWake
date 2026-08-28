/**
 * @file test_inside_rc_chat.cpp
 * @brief 文本链路智能问答（insideRcChat）功能演示
 *
 * 演示 insideRcChat 的使用方式：
 * 1. 单轮文本问答（非流式）
 * 2. 单轮文本问答（流式）
 * 3. 多轮对话（3 条 messages）
 * 4. 取消流式请求
 *
 * insideRcChat 与 largeModelChatbot 的区别：
 * - largeModelChatbot 走通用大模型，仅返回纯文本
 * - insideRcChat 走智能对话服务（DCS 协议），返回指令集
 *   （Speak/Play/Nlu/RenderStreamCard 等），与语音助手返回结构一致
 *
 * @note 需要在设备注册成功后调用（deviceId 和 deviceSecret 已获取）
 */
#include "ai_sdk/ai_assistant_manager.h"
#include "ai_sdk/ai_foundation_kit.h"
#include "esp_log.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"

static const char* TAG = "InsideRcChatTest";

// ============================================================================
// 测试 1：单轮文本问答（非流式）
// ============================================================================

static void test_non_stream_chat()
{
    ESP_LOGI(TAG, "========================================");
    ESP_LOGI(TAG, "  测试 1: 单轮文本问答（非流式）");
    ESP_LOGI(TAG, "========================================");

    auto& manager = ai_sdk::AIAssistantManager::getInstance();
    auto& kit = manager.aiFoundationKit();
    auto& config = manager.config();

    ai_sdk::InsideRcChatRequest req;
    req.qid = "test-non-stream-001";
    req.third_user_id = "demo-user";
    req.cuid = config.deviceId;
    req.messages = {{"user", "今天天气怎么样"}};
    req.stream = false;

    kit.insideRcChat(req,
        [](const ai_sdk::DialogueResult& result) {
            if (result.is_end == 1) {
                ESP_LOGI(TAG, "[非流式] 对话结束，回答: %s",
                    result.assistant_answer_content.c_str());
            } else {
                ESP_LOGI(TAG, "[非流式] 指令: %s", result.directive.c_str());
                ESP_LOGD(TAG, "  header:  %s", result.header.c_str());
                ESP_LOGD(TAG, "  payload: %s", result.payload.c_str());
            }
        },
        [](const std::string& error) {
            ESP_LOGE(TAG, "[非流式] 错误: %s", error.c_str());
        });
}

// ============================================================================
// 测试 2：单轮文本问答（流式）
// ============================================================================

static void test_stream_chat()
{
    ESP_LOGI(TAG, "========================================");
    ESP_LOGI(TAG, "  测试 2: 单轮文本问答（流式）");
    ESP_LOGI(TAG, "========================================");

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
                ESP_LOGD(TAG, "  header:  %s", result.header.c_str());
                ESP_LOGD(TAG, "  payload: %s", result.payload.c_str());
            }
        },
        [](const std::string& error) {
            ESP_LOGE(TAG, "[流式] 错误: %s", error.c_str());
        });

    ESP_LOGI(TAG, "流式请求 ID: %s", requestId.c_str());
}

// ============================================================================
// 测试 3：多轮对话
// ============================================================================

static void test_multi_turn_chat()
{
    ESP_LOGI(TAG, "========================================");
    ESP_LOGI(TAG, "  测试 3: 多轮对话");
    ESP_LOGI(TAG, "========================================");

    auto& manager = ai_sdk::AIAssistantManager::getInstance();
    auto& kit = manager.aiFoundationKit();
    auto& config = manager.config();

    ai_sdk::InsideRcChatRequest req;
    req.qid = "test-multi-001";
    req.third_user_id = "demo-user";
    req.cuid = config.deviceId;
    // messages 成员数必须为奇数，奇数位 role 为 "user"，偶数位为 "assistant"
    req.messages = {
        {"user", "今天天气怎么样"},
        {"assistant", "今天北京晴天，气温25度"},
        {"user", "那上海呢"}
    };
    req.stream = true;

    std::string requestId = kit.insideRcChat(req,
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

    ESP_LOGI(TAG, "多轮请求 ID: %s", requestId.c_str());
}

// ============================================================================
// 测试 4：取消流式请求
// ============================================================================

static void test_cancel_stream()
{
    ESP_LOGI(TAG, "========================================");
    ESP_LOGI(TAG, "  测试 4: 取消流式请求");
    ESP_LOGI(TAG, "========================================");

    auto& manager = ai_sdk::AIAssistantManager::getInstance();
    auto& kit = manager.aiFoundationKit();
    auto& config = manager.config();

    ai_sdk::InsideRcChatRequest req;
    req.qid = "test-cancel-001";
    req.third_user_id = "demo-user";
    req.cuid = config.deviceId;
    req.messages = {{"user", "给我讲一个很长的故事"}};
    req.stream = true;

    std::string requestId = kit.insideRcChat(req,
        [](const ai_sdk::DialogueResult& result) {
            ESP_LOGI(TAG, "[取消测试] 收到指令: %s", result.directive.c_str());
        },
        [](const std::string& error) {
            ESP_LOGE(TAG, "[取消测试] 错误: %s", error.c_str());
        });

    ESP_LOGI(TAG, "请求 ID: %s，2 秒后取消...", requestId.c_str());

    // 等待 2 秒后取消请求
    vTaskDelay(pdMS_TO_TICKS(2000));

    bool cancelled = kit.cancelStreamRequest(requestId);
    ESP_LOGI(TAG, "取消结果: %s", cancelled ? "成功" : "失败（请求可能已完成）");
}

// ============================================================================
// 测试任务入口
// ============================================================================

static void inside_rc_chat_test_task(void* arg)
{
    (void)arg;

    ESP_LOGI(TAG, "========================================");
    ESP_LOGI(TAG, "  insideRcChat 文本链路智能问答测试");
    ESP_LOGI(TAG, "========================================");

    // 测试 1：非流式
    test_non_stream_chat();
    vTaskDelay(pdMS_TO_TICKS(10000));

    // 测试 2：流式
    test_stream_chat();
    vTaskDelay(pdMS_TO_TICKS(10000));

    // 测试 3：多轮对话
    test_multi_turn_chat();
    vTaskDelay(pdMS_TO_TICKS(10000));

    // 测试 4：取消请求
    test_cancel_stream();

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

extern "C" void start_inside_rc_chat_test(void)
{
    ESP_LOGI(TAG, "Starting insideRcChat test ...");

    // 栈空间 12288 字节：HTTP/SSL 请求需要充足的栈空间
    xTaskCreate(inside_rc_chat_test_task, "rc_chat_test", 12288, NULL, 5, NULL);
}
