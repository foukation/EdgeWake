/**
 * @file test_content_summary.cpp
 * @brief 内容摘要（contentSummary）功能演示
 *
 * 测试方式对齐 Android FullyCompatibleSDK 的 contentSummary()：
 * - 单次调用（不拆多用例）
 * - content = 待摘要长文本，language = "English"
 * - stream 使用默认值 true（流式返回增量摘要）
 * - 流式请求返回 requestId，可用 cancelStreamRequest() 取消（示例见注释）
 *
 * @note 需要在设备注册成功后调用（deviceId 和 deviceSecret 已获取）
 */
#include "ai_sdk/ai_assistant_manager.h"
#include "ai_sdk/ai_foundation_kit.h"
#include "esp_log.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"

static const char* TAG = "ContentSummaryTest";

// 用于测试的长文本内容（对齐 Android 测试：量子计算新闻）
static const char* kSampleContent =
    "近日，中国科学院宣布在量子计算领域取得重大突破。研究团队成功研发出新型量子计算芯片，"
    "实现了72个量子比特的稳定控制，大幅提升了量子计算的处理能力。该成果发表在国际顶级期刊"
    "《Nature》上，引起国际科学界广泛关注。专家表示，这一突破将加速量子计算在密码学、"
    "新材料开发、药物研发等领域的应用进程。中国科学院量子信息与量子科技创新研究院院长"
    "潘建伟教授指出，团队计划在未来三年内，进一步提升量子比特数量至100个以上，"
    "并着手解决量子计算实用化过程中的关键技术难题。";

// ============================================================================
// 内容摘要（对齐 Android contentSummary()）
// ============================================================================

static void test_content_summary()
{
    ESP_LOGI(TAG, "========================================");
    ESP_LOGI(TAG, "  contentSummary 内容摘要测试");
    ESP_LOGI(TAG, "========================================");

    auto& manager = ai_sdk::AIAssistantManager::getInstance();
    auto& kit = manager.aiFoundationKit();

    ai_sdk::ContentSummaryRequest req;
    req.content = kSampleContent;
    req.language = "English";  // 摘要语言：英文（与 Android 一致）
    req.stream = false;
    // stream 使用默认值 true

    std::string requestId = kit.contentSummary(req,
        [](const ai_sdk::ContentSummaryResponse& resp) {
            // 流式模式下 data.content 为增量内容
            ESP_LOGI(TAG, "response: status=%d, %s",
                resp.status, resp.data.content.c_str());
        },
        [](const std::string& error) {
            ESP_LOGE(TAG, "error: %s", error.c_str());
        });

    ESP_LOGI(TAG, "流式请求 ID: %s", requestId.c_str());

    // 如需取消（对齐 Android 中注释掉的 postDelayed cancelStreamRequest）：
    // vTaskDelay(pdMS_TO_TICKS(2000));
    // kit.cancelStreamRequest(requestId);
    (void)requestId;
}

// ============================================================================
// 测试任务入口
// ============================================================================

static void content_summary_test_task(void* arg)
{
    (void)arg;

    test_content_summary();

    // 保持任务运行，等待异步回调完成
    while (true) {
        vTaskDelay(pdMS_TO_TICKS(10000));
    }
}

// ============================================================================
// 外部入口（由 test_ai_sdk.cpp 调用）
// ============================================================================

extern "C" void start_content_summary_test(void)
{
    ESP_LOGI(TAG, "Starting contentSummary test ...");

    // 栈空间 12288 字节：HTTP/SSL 请求需要充足的栈空间
    xTaskCreate(content_summary_test_task, "content_summary_test", 12288, NULL, 5, NULL);
}
