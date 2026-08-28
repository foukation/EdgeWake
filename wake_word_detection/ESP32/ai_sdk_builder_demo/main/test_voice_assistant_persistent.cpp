/**
 * @file test_voice_assistant_persistent.cpp
 * @brief 语音助手持续识别测试（PCM 文件 + InsideRcChat 对话）
 *
 * 将 SpeechRecognitionPersistent（持续 ASR）与 InsideRcChat（智能对话）串联，
 * 实现无需麦克风硬件的语音助手完整链路验证：
 *
 *   test_asr.pcm（循环发送，模拟麦克风输入）
 *       ↓ sendAudio()
 *   SpeechRecognitionPersistent（持续 WebSocket，服务端 VAD 自动分割）
 *       ↓ on_result（FIN_TEXT，识别文本）
 *   InsideRcChat（HTTP 智能对话，返回 DCS 指令集）
 *       ↓ Speak directive（TTS URL）+ is_end
 *   打印 TTS URL 和回答内容（PCM 测试模式无音频输出）
 *       ↓
 *   下一轮 PCM（同一 WebSocket 连接，保留多轮对话上下文）
 *
 * 与 test_voice_assistant.cpp 的核心区别：
 * - ASR 层：AsrIntelligentDialogue（每轮重建连接）→ SpeechRecognitionPersistent（连接复用）
 * - 对话层：SDK 内置 NLU → InsideRcChat（HTTP，DCS 指令集）
 * - TTS 层：SDK 内置播放 → 仅打印 URL（PCM 测试模式无音频输出）
 * - 上下文：无 → 保留最近 kMaxHistoryTurns 轮对话历史
 *
 * 线程模型：
 * - 主任务（本文件）：发送 PCM、等待队列、调用 InsideRcChat、等待信号量
 * - WebSocket 内部任务：触发 on_result 回调，通过队列传递 ASR 文本到主任务
 * - HTTP 内部任务：触发 InsideRcChat 回调，通过 shared_ptr<ChatResult> 安全传递结果
 *
 * 入口函数：start_voice_assistant_persistent_test()
 *
 * 硬件要求：无（使用嵌入固件的 test_asr.pcm，无需 I2S 麦克风）
 */
#include "ai_sdk/ai_assistant_manager.h"
#include "ai_sdk/ai_foundation_kit.h"
#include <esp_log.h>
#include <atomic>
#include <cstring>
#include <memory>
#include <string>
#include <vector>
#include <freertos/FreeRTOS.h>
#include <freertos/task.h>
#include <freertos/queue.h>
#include <freertos/semphr.h>

static const char* TAG = "VA_PERSIST_TEST";

// ============================================================================
// PCM 参数（与 test_speech_recognition_persistent.cpp 保持一致）
// 16kHz / 16-bit / 单声道：32000 bytes/s，5120 bytes ≈ 160ms
// ============================================================================
static constexpr size_t kPcmChunkBytes   = 5120;
static constexpr int    kPcmChunkDelayMs = 160;

// ============================================================================
// 会话参数
// ============================================================================

// 每次 WebSocket 会话内，PCM 文件发送的最大轮次
// 每轮 = 发一遍 test_asr.pcm + 等待 FIN_TEXT + 一次 InsideRcChat 对话
static constexpr int kRoundsPerSession = 5;

// 会话结束后，重新建立 WebSocket 连接前的等待时间（ms）
static constexpr int kRestartDelayMs = 2000;

// 等待 ASR FIN_TEXT 结果的超时时间（ms）
// 需覆盖：PCM 发送时间（total_bytes/32000*1000 ms）+ VAD 检测（~500ms）+ 网络延迟
static constexpr int kAsrTimeoutMs = 8000;

// 等待 InsideRcChat 对话响应的超时时间（ms）
static constexpr int kChatTimeoutMs = 15000;

// 保留的最大对话历史轮次（每轮 = 1 user + 1 assistant 共 2 条消息）
// 最终 messages 总数 = kMaxHistoryTurns * 2 + 1（必须为奇数，API 要求）
static constexpr int kMaxHistoryTurns = 2;

// ============================================================================
// ASR 结果队列元素
// 用于在 WebSocket 内部任务（on_result 回调）与主任务之间跨任务边界传递 ASR 文本
// 使用定长字符数组，避免 std::string 跨 FreeRTOS 队列的内存问题
// ============================================================================
static constexpr size_t kAsrTextMaxLen = 256;
struct AsrTextMsg {
    char text[kAsrTextMaxLen];
};

// ============================================================================
// InsideRcChat 对话结果
// 通过 std::shared_ptr 在 HTTP 回调（HTTP 内部任务）与主任务之间安全共享：
// - 主任务持有一份 shared_ptr，等待 done_sem
// - HTTP 回调持有一份 shared_ptr，完成后 give done_sem
// - 即使主任务因超时先返回，回调触发时仍可安全访问，不会出现悬空引用
// ============================================================================
struct ChatResult {
    SemaphoreHandle_t done_sem;          // 对话完成信号量（binary）
    std::atomic_bool  error{false};      // 是否发生错误
    std::string       answer;            // is_end=1 时的完整回答文本
    std::string       tts_url;           // Speak 指令中的 TTS 音频 URL
    std::string       tts_content;       // Speak 指令中的文本内容（供日志输出）

    ChatResult() : done_sem(xSemaphoreCreateBinary()) {}
    ~ChatResult() {
        if (done_sem) {
            vSemaphoreDelete(done_sem);
        }
    }
};

// ============================================================================
// 嵌入 PCM 数据（由 CMakeLists.txt EMBED_FILES 生成）
// test_asr.pcm：16kHz / 16-bit / 单声道测试语音
// ============================================================================
extern const uint8_t _binary_test_asr_pcm_start[] asm("_binary_test_asr_pcm_start");
extern const uint8_t _binary_test_asr_pcm_end[]   asm("_binary_test_asr_pcm_end");

// 任务句柄（幂等保护，防止重复创建任务）
static TaskHandle_t g_task = nullptr;

// ============================================================================
// 辅助函数：从 JSON 字符串中提取指定字段的字符串值
//
// 用于解析 Speak 指令 payload，避免引入 cJSON 依赖。
// payload 格式示例：
//   {"format":"AUDIO_MPEG","token":"...","url":"https://...","content":"北京今天..."}
//
// 注意：对含有复杂转义序列的 URL 字段（如 \u0026）采用逐字符匹配，
//       仅跳过 \" 转义以防止提前结束，其余原样返回。
// ============================================================================
static std::string extract_json_str_field(const std::string& json,
                                           const std::string& key) {
    const std::string search = "\"" + key + "\":\"";
    const size_t pos_start = json.find(search);
    if (pos_start == std::string::npos) {
        return "";
    }
    size_t pos = pos_start + search.size();
    size_t end = pos;
    while (end < json.size()) {
        if (json[end] == '\\') {
            end += 2;   // 跳过转义字符（如 \" \\ \n）
            continue;
        }
        if (json[end] == '"') {
            break;      // 未转义的引号 = 字段结束
        }
        ++end;
    }
    return json.substr(pos, end - pos);
}

// ============================================================================
// 单次会话：一条 WebSocket 连接 + kRoundsPerSession 轮 ASR 对话
//
// 完整流程：
//   1. 注册 ASR 回调（on_result / on_error / on_close）
//   2. asr.start()：建立 WebSocket，发送 START 帧（整个会话只调用一次）
//   3. 循环 kRoundsPerSession 轮：
//      a. 按实时速率发送 test_asr.pcm（每块 5120 bytes，间隔 160ms）
//      b. 等待 FIN_TEXT 结果（服务端 VAD 检测到静音后触发）
//      c. 调用 InsideRcChat，等待对话响应
//      d. 打印 TTS URL 和回答内容
//      e. 更新多轮对话历史
//   4. asr.stop()：发送 FINISH 帧，等待 on_close
// ============================================================================
static void va_persistent_session() {
    if (!ai_sdk::AIAssistantManager::isInitialized()) {
        ESP_LOGE(TAG, "AI SDK 未初始化");
        return;
    }

    auto& asr    = ai_sdk::AIAssistantManager::getInstance()
                       .speechRecognitionPersistentHelp();
    auto& kit    = ai_sdk::AIAssistantManager::getInstance()
                       .aiFoundationKit();
    auto& config = ai_sdk::AIAssistantManager::getInstance().config();

    // ========================================================================
    // 会话级状态
    // ========================================================================

    // ASR 结果队列：深度 1，WebSocket 回调写入，主任务读取
    QueueHandle_t    asr_queue = xQueueCreate(1, sizeof(AsrTextMsg));
    std::atomic_bool got_close{false};
    std::atomic_bool got_error{false};
    std::atomic_int  result_count{0};

    // 多轮对话历史：[{"user","..."}, {"assistant","..."}, ...]
    // 保留最近 kMaxHistoryTurns 轮，每次对话时拼接到 messages 头部
    std::vector<ai_sdk::InsideRcChatMessage> history;

    // ========================================================================
    // ASR 回调设置
    // 注意：所有回调在 WebSocket 内部任务上下文中执行，不可调用阻塞 API
    // ========================================================================
    asr.setCallbacks(
        // on_result：服务端推送识别消息（含 HEARTBEAT / MID_TEXT / FIN_TEXT）
        [asr_queue, &result_count](
                const ai_sdk::SpeechRecognitionPersistentResult& r) {
            int count = result_count.fetch_add(1) + 1;
            ESP_LOGD(TAG, "[ASR][%d] type=%s err_no=%d result=%s",
                     count, r.type.c_str(), r.err_no, r.result.c_str());

            // 只处理最终识别结果：type=FIN_TEXT，无错误，文本非空
            if (r.type != "FIN_TEXT" || r.err_no != 0 || r.result.empty()) {
                return;
            }

            AsrTextMsg msg{};
            strncpy(msg.text, r.result.c_str(), kAsrTextMaxLen - 1);
            msg.text[kAsrTextMaxLen - 1] = '\0';

            // 非阻塞发送：队列深度 1，满则丢弃（主任务正在处理上一条时不应堆积）
            if (xQueueSend(asr_queue, &msg, 0) != pdTRUE) {
                ESP_LOGW(TAG, "[ASR] 队列已满，丢弃结果: %s", msg.text);
            }
        },
        // on_error：连接失败或网络中断
        [&got_error](int code, const std::string& msg) {
            got_error.store(true);
            ESP_LOGE(TAG, "[ASR] 错误 code=%d: %s", code, msg.c_str());
        },
        // on_close：WebSocket 连接关闭（stop() / cancel() / 网络异常后触发）
        [&got_close, &result_count]() {
            got_close.store(true);
            ESP_LOGI(TAG, "[ASR] 连接已关闭，本次会话共收到 %d 条识别结果",
                     result_count.load());
        }
    );

    // ========================================================================
    // 建立 WebSocket 连接（整个会话只建立一次）
    // ========================================================================
    if (!asr.start()) {
        ESP_LOGE(TAG, "asr.start() 失败");
        vQueueDelete(asr_queue);
        return;
    }

    // 校验嵌入 PCM 数据
    const uint8_t* pcm_start   = _binary_test_asr_pcm_start;
    const uint8_t* pcm_end     = _binary_test_asr_pcm_end;
    const size_t   total_bytes = static_cast<size_t>(pcm_end - pcm_start);
    if (total_bytes == 0) {
        ESP_LOGE(TAG, "嵌入 PCM 文件为空");
        asr.cancel();
        vQueueDelete(asr_queue);
        return;
    }

    const int total_chunks = static_cast<int>(
        (total_bytes + kPcmChunkBytes - 1) / kPcmChunkBytes);
    ESP_LOGI(TAG, "PCM 大小=%u bytes，每轮分 %d 块，间隔 %d ms",
             (unsigned)total_bytes, total_chunks, kPcmChunkDelayMs);

    // ========================================================================
    // 主循环：发送 PCM → 等待 ASR → 对话 → 更新历史 → 下一轮
    // ========================================================================
    for (int round = 1; round <= kRoundsPerSession; ++round) {
        if (got_error.load()) {
            ESP_LOGW(TAG, "检测到 ASR 错误，中止会话");
            break;
        }

        ESP_LOGI(TAG, "==============================");
        ESP_LOGI(TAG, "  Round %d / %d", round, kRoundsPerSession);
        ESP_LOGI(TAG, "==============================");

        // --------------------------------------------------------------------
        // Step 1：按实时速率发送 PCM 数据，模拟真实音频流
        // 发送完毕后不再发送音频，服务端 VAD 检测到持续静音后触发 FIN_TEXT
        // --------------------------------------------------------------------
        size_t offset      = 0;
        int    chunk_index = 0;
        while (offset < total_bytes && !got_error.load()) {
            const size_t remaining = total_bytes - offset;
            const size_t chunk_len = (remaining > kPcmChunkBytes)
                                         ? kPcmChunkBytes
                                         : remaining;
            asr.sendAudio(pcm_start + offset, chunk_len);
            offset      += chunk_len;
            chunk_index += 1;

            ESP_LOGD(TAG, "[PCM] Round %d 块 %d/%d len=%u",
                     round, chunk_index, total_chunks, (unsigned)chunk_len);
            vTaskDelay(pdMS_TO_TICKS(kPcmChunkDelayMs));
        }

        if (got_error.load()) {
            break;
        }

        ESP_LOGI(TAG, "[Round %d] PCM 发送完毕，等待 ASR FIN_TEXT（超时 %d ms）...",
                 round, kAsrTimeoutMs);

        // --------------------------------------------------------------------
        // Step 2：等待 ASR FIN_TEXT 结果
        // PCM 发完后停止发送音频 → 服务端 VAD 检测到静音 → 推送 FIN_TEXT
        // --------------------------------------------------------------------
        AsrTextMsg asr_msg{};
        if (xQueueReceive(asr_queue, &asr_msg, pdMS_TO_TICKS(kAsrTimeoutMs))
                != pdTRUE) {
            ESP_LOGW(TAG, "[Round %d] 等待 ASR 结果超时（%d ms），跳过本轮对话",
                     round, kAsrTimeoutMs);
            continue;
        }

        const std::string user_text = asr_msg.text;
        ESP_LOGI(TAG, "[Round %d] 识别结果: \"%s\"", round, user_text.c_str());

        // --------------------------------------------------------------------
        // Step 3：构建 InsideRcChat 请求
        //
        // messages 格式（API 要求总数为奇数）：
        //   [历史 turn1 user, 历史 turn1 assistant, ..., 当前 user]
        // history 最多保留 kMaxHistoryTurns * 2 条，加上当前 user 共奇数条
        // --------------------------------------------------------------------
        std::vector<ai_sdk::InsideRcChatMessage> messages = history;
        messages.push_back({"user", user_text});

        ai_sdk::InsideRcChatRequest req;
        req.qid           = "va-persist-r" + std::to_string(round);
        req.third_user_id = "demo-user";
        req.cuid          = config.deviceId;
        req.messages      = messages;
        req.stream        = true;    // 流式：服务端边生成边推，减少等待时间，降低静音帧发送量

        ESP_LOGI(TAG, "[Round %d] 发起对话请求（历史 %d 轮，messages %d 条）",
                 round, (int)history.size() / 2, (int)messages.size());

        // --------------------------------------------------------------------
        // Step 4：调用 InsideRcChat，通过 shared_ptr<ChatResult> 安全共享回调结果
        //
        // 为何使用 shared_ptr 而非栈变量：
        //   InsideRcChat 回调在 HTTP 内部任务中异步触发。如果主任务因超时先返回，
        //   栈上的局部变量会被释放，回调访问时产生悬空引用。
        //   shared_ptr 确保两侧都持有引用时，数据才会被销毁。
        // --------------------------------------------------------------------
        auto chat_result = std::make_shared<ChatResult>();

        kit.insideRcChat(req,
            // 成功回调：在 HTTP 内部任务上下文执行
            [chat_result](const ai_sdk::DialogueResult& result) {
                // RenderStreamCard（流式）：answer 字段为累积文本，持续覆盖，最后一次为完整内容
                // RenderCard（非流式）：content 字段为完整文本
                if (result.directive == "RenderStreamCard" && !result.payload.empty()) {
                    std::string answer =
                        extract_json_str_field(result.payload, "answer");
                    if (!answer.empty()) {
                        chat_result->tts_content = answer;
                    }
                } else if (result.directive == "RenderCard" && !result.payload.empty()) {
                    chat_result->tts_content =
                        extract_json_str_field(result.payload, "content");
                // Speak 指令：提取 TTS URL，并打印已从 RenderStreamCard/RenderCard 取到的文本
                } else if (result.directive == "Speak" && !result.payload.empty()) {
                    chat_result->tts_url =
                        extract_json_str_field(result.payload, "url");
                    ESP_LOGI(TAG, "[Chat] TTS 内容: %s",
                             chat_result->tts_content.c_str());
                    ESP_LOGI(TAG, "[Chat] TTS URL: %s",
                             chat_result->tts_url.c_str());
                } else if (!result.directive.empty()) {
                    ESP_LOGD(TAG, "[Chat] 指令: %s payload: %s",
                             result.directive.c_str(), result.payload.c_str());
                }

                // is_end=1：所有指令已下发，对话完成，通知主任务
                if (result.is_end == 1) {
                    chat_result->answer = result.assistant_answer_content;
                    ESP_LOGI(TAG, "[Chat] 对话完成，回答: %s",
                             chat_result->answer.c_str());
                    xSemaphoreGive(chat_result->done_sem);
                }
            },
            // 错误回调
            [chat_result](const std::string& error) {
                chat_result->error.store(true);
                ESP_LOGE(TAG, "[Chat] 错误: %s", error.c_str());
                xSemaphoreGive(chat_result->done_sem);
            }
        );

        // --------------------------------------------------------------------
        // Step 5：等待对话完成，同时持续向 WebSocket 发送静音帧
        //
        // 问题背景：
        //   insideRcChat 发起 HTTP 请求后，原实现用 xSemaphoreTake 死等，
        //   等待期间（约 5 秒）没有任何音频推送到 WebSocket，服务端触发
        //   空闲超时后主动关闭连接，导致后续轮次全部 "Not ready"。
        //
        // 修复方案：
        //   将死等改为轮询：每 kPcmChunkDelayMs（160ms）非阻塞检查一次
        //   信号量，若 chat 尚未完成则向 WebSocket 推送一块全零 PCM
        //   （静音帧），保持服务端认为音频流仍在进行，避免触发空闲超时。
        //
        // 静音帧说明：
        //   全零的 16-bit PCM 在服务端 VAD 看来是静音，不会误触发
        //   新一轮语音识别；on_result 回调已过滤 result.empty()，
        //   即使服务端推送静音 FIN_TEXT 也不会进入对话流程。
        //
        // chat_result 内存安全：
        //   shared_ptr 保证超时退出后回调仍能安全访问，不会悬空引用。
        // --------------------------------------------------------------------
        static const uint8_t silence[kPcmChunkBytes] = {0};
        const TickType_t chat_deadline =
            xTaskGetTickCount() + pdMS_TO_TICKS(kChatTimeoutMs);
        bool chat_done = false;

        while (xTaskGetTickCount() < chat_deadline) {
            if (xSemaphoreTake(chat_result->done_sem, 0) == pdTRUE) {
                chat_done = true;
                break;
            }
            // chat 尚未完成，发静音帧维持 WebSocket 活跃
            asr.sendAudio(silence, sizeof(silence));
            vTaskDelay(pdMS_TO_TICKS(kPcmChunkDelayMs));
        }

        if (!chat_done) {
            ESP_LOGW(TAG, "[Round %d] 等待对话响应超时（%d ms），跳过上下文更新",
                     round, kChatTimeoutMs);
            continue;
        }

        if (chat_result->error.load()) {
            ESP_LOGW(TAG, "[Round %d] 对话请求失败，跳过上下文更新", round);
            continue;
        }

        // --------------------------------------------------------------------
        // Step 6：更新多轮对话历史
        //
        // 追加本轮 user + assistant 各一条；
        // 超出 kMaxHistoryTurns 限制时，从头部删除最早的一轮（2 条）
        // --------------------------------------------------------------------
        if (!chat_result->answer.empty()) {
            history.push_back({"user",      user_text});
            history.push_back({"assistant", chat_result->answer});

            while ((int)history.size() > kMaxHistoryTurns * 2) {
                history.erase(history.begin(), history.begin() + 2);
            }

            ESP_LOGI(TAG, "[Round %d] 上下文已更新（当前历史 %d 轮）",
                     round, (int)history.size() / 2);
        }
    }

    // ========================================================================
    // 所有轮次完成（或发生错误），优雅停止：发送 FINISH 帧，等待服务端关闭连接
    // ========================================================================
    ESP_LOGI(TAG, "所有轮次完成（error=%d），发送 FINISH 帧...",
             got_error.load() ? 1 : 0);
    asr.stop();

    while (!got_close.load() && !got_error.load()) {
        vTaskDelay(pdMS_TO_TICKS(100));
    }

    ESP_LOGI(TAG, "会话结束");
    vQueueDelete(asr_queue);
}

// ============================================================================
// 持续会话循环任务：会话结束后延迟重启，实现持续识别
// ============================================================================
static void va_persistent_loop_task(void* pvParameters) {
    (void)pvParameters;

    int session = 0;
    while (true) {
        ++session;
        ESP_LOGI(TAG, "========================================");
        ESP_LOGI(TAG, "  语音助手持续识别 Session %d 开始", session);
        ESP_LOGI(TAG, "========================================");

        va_persistent_session();

        ESP_LOGI(TAG, "Session %d 结束，%d ms 后重启...", session, kRestartDelayMs);
        vTaskDelay(pdMS_TO_TICKS(kRestartDelayMs));
    }
}

// ============================================================================
// 入口函数（由 test_ai_sdk.cpp 在设备注册成功后调用）
// ============================================================================

/**
 * @brief 启动语音助手持续识别测试（PCM 文件 + InsideRcChat 对话循环）
 *
 * 幂等函数：如果任务已在运行，仅打印日志并返回。
 * 使用嵌入固件的 test_asr.pcm，无需麦克风硬件，适合完整链路功能验证。
 *
 * @note 与其他 ASR 测试互斥，不可同时运行
 */
extern "C" void start_voice_assistant_persistent_test(void) {
    if (g_task != nullptr) {
        ESP_LOGI(TAG, "任务已在运行");
        return;
    }

    BaseType_t rc = xTaskCreate(
        va_persistent_loop_task,
        "va_persist",
        12288,    // 12KB 栈：HTTP/SSL + JSON 解析 + 对话历史向量需要充足栈空间
        nullptr,
        5,
        &g_task);

    if (rc != pdPASS) {
        g_task = nullptr;
        ESP_LOGE(TAG, "创建任务失败");
        return;
    }

    ESP_LOGI(TAG, "语音助手持续识别任务已启动");
}
