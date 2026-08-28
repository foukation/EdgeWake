/**
 * @file test_asr_pcm.cpp
 * @brief PCM 流式 ASR 测试
 *
 * 使用嵌入在固件中的 PCM 音频文件，通过 AsrIntelligentDialogue 接口
 * 进行端到端的语音识别和智能对话测试。
 *
 * 测试流程：
 * 1. 设置 5 个 ASR 回调（connected / asr / dialogue / error / complete）
 * 2. asr.start() 建立 WebSocket 连接
 * 3. 按 16kHz 实时速率发送嵌入 PCM 数据（每次 5120 字节，间隔 160ms）
 * 4. 等待 onComplete 回调（服务器关闭 WebSocket）
 * 5. asr.stop() 清理资源
 * 6. 延迟后重复（循环测试）
 *
 * 迁移自 main/test_ai_sdk.cpp 的语音助手测试部分。
 */
#include "ai_sdk/ai_assistant_manager.h"
#include <esp_log.h>
#include <esp_timer.h>
#include <atomic>
#include <freertos/FreeRTOS.h>
#include <freertos/task.h>

static const char* TAG = "ASR_PCM_TEST";

// ============================================================================
// 嵌入 PCM 数据（由 CMakeLists.txt EMBED_FILES 生成）
// ============================================================================
// 第 1 段测试音频：test_asr.pcm
extern const uint8_t _binary_test_asr_pcm_start[] asm("_binary_test_asr_pcm_start");
extern const uint8_t _binary_test_asr_pcm_end[] asm("_binary_test_asr_pcm_end");
// 第 2 段测试音频：test_asr2.pcm
extern const uint8_t _binary_test_asr2_pcm_start[] asm("_binary_test_asr2_pcm_start");
extern const uint8_t _binary_test_asr2_pcm_end[] asm("_binary_test_asr2_pcm_end");

// PCM 测试选择开关：
//   1 = 使用 test_asr.pcm（默认）
//   2 = 使用 test_asr2.pcm
// 修改本宏后重新编译即可切换测试所用的 PCM 音频源。
#define PCM_TEST_SELECT 2

// 16kHz/16-bit/mono PCM stream pacing:
// 32000 bytes/s, 5120 bytes per chunk equals 160 ms audio.
static constexpr size_t kPcmChunkBytes = 5120;
static constexpr int kPcmChunkDelayMs = 160;
static constexpr int kPcmRestartDelayMs = 1000;

// Keep a single PCM loop task instance to avoid duplicate concurrent sessions.
static TaskHandle_t g_pcm_loop_task = nullptr;

// Forward declarations
static void test_voice_assistant_from_embedded_pcm();
static void pcm_loop_test_task(void* pvParameters);

// ============================================================================
// 单轮 PCM 流式 ASR 测试
// ============================================================================

/**
 * @brief Stream embedded PCM data to ASR for end-to-end session verification.
 */
static void test_voice_assistant_from_embedded_pcm() {
    ESP_LOGI(TAG, "=== Testing Voice Assistant (Embedded PCM Stream) ===");

    if (!ai_sdk::AIAssistantManager::isInitialized()) {
        ESP_LOGE(TAG, "AI SDK not initialized!");
        return;
    }

    auto& asr = ai_sdk::AIAssistantManager::getInstance().asrIntelligentDialogueHelp();

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

    // 根据 PCM_TEST_SELECT 选择嵌入的 PCM 音频源（1=test_asr.pcm，2=test_asr2.pcm）
#if PCM_TEST_SELECT == 2
    const uint8_t* pcm_start = _binary_test_asr2_pcm_start;
    const uint8_t* pcm_end = _binary_test_asr2_pcm_end;
#else
    const uint8_t* pcm_start = _binary_test_asr_pcm_start;
    const uint8_t* pcm_end = _binary_test_asr_pcm_end;
#endif
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

// ============================================================================
// PCM 循环测试任务
// ============================================================================

/**
 * @brief Continuous PCM regression loop.
 *
 * Each round starts a new single-turn ASR session. The next round is
 * started after the previous round receives onComplete.
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

// ============================================================================
// 入口函数
// ============================================================================

/**
 * @brief Start the PCM loop task once.
 *
 * This function is idempotent. If the loop task is already running,
 * it will only print an info log and return.
 */
extern "C" void start_pcm_loop_test(void) {
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
