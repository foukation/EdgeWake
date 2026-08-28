/**
 * @file test_speech_recognition_persistent.cpp
 * @brief 持续语音识别测试
 *
 * 提供两种测试模式：
 * - 模式一（PCM 文件）：复用嵌入固件的 test_asr.pcm，通过 sendAudio() 手动发送
 * - 模式二（麦克风）：使用 SDK 内置音频模块，实时录音持续识别
 *
 * 与 test_asr_pcm.cpp / test_voice_assistant.cpp 的核心区别：
 * - 使用 speechRecognitionPersistentHelp() 而非 asrIntelligentDialogueHelp()
 * - 只有 3 个回调（on_result / on_error / on_close），无 TTS / 对话回调
 * - 连接持续保持，服务端 VAD 自动分割，每段结束后触发一次 on_result
 * - stop() 发送 finish 帧，等待服务端返回最后结果后关闭；cancel() 立即断开
 *
 * 模式一（PCM 文件）说明：
 * - 每次 WebSocket 会话内，PCM 文件重复发送 kPcmRoundsPerSession 轮
 * - 每轮之间插入 kInterRoundDelayMs 静音间隔，供服务端 VAD 检测句末
 * - stop() 仅在整个会话（所有轮次）结束后调用一次
 * - 一次连接可收到多条 on_result，真正验证持续识别语义
 *
 * 入口函数：
 * - start_speech_recognition_persistent_pcm_test()   模式一：PCM 文件，无需硬件
 * - start_speech_recognition_persistent_voice_test() 模式二：实时麦克风，需要 I2S 硬件
 *
 * 硬件要求（模式二）：
 * - bread-compact-wifi 板子（INMP441 麦克风 + MAX98357 功放）
 * - I2S Simplex 模式：麦克风和扬声器使用独立的 I2S 端口
 * - 引脚配置与 test_voice_assistant.cpp 完全一致
 */
#include "ai_sdk/ai_assistant_manager.h"
#include "ai_sdk/audio/audio_config.h"
#include <esp_log.h>
#include <atomic>
#include <freertos/FreeRTOS.h>
#include <freertos/task.h>

static const char* TAG = "SR_PERSISTENT_TEST";

// ============================================================================
// 嵌入 PCM 数据（由 CMakeLists.txt EMBED_FILES 生成）
// test_asr.pcm：16kHz / 16-bit / 单声道测试语音
// ============================================================================
extern const uint8_t _binary_test_asr_pcm_start[] asm("_binary_test_asr_pcm_start");
extern const uint8_t _binary_test_asr_pcm_end[]   asm("_binary_test_asr_pcm_end");

// PCM 分块参数（与 test_asr_pcm.cpp 一致）
// 16kHz * 2 bytes/sample * 1ch = 32000 bytes/s，5120 bytes ≈ 160ms
static constexpr size_t kPcmChunkBytes      = 5120;
static constexpr int    kPcmChunkDelayMs    = 160;
static constexpr int    kRestartDelayMs     = 1000;

// 每次 WebSocket 会话内，PCM 文件重复发送的轮次
// 每轮触发一次服务端 VAD 分段，即每次连接可收到 kPcmRoundsPerSession 条 on_result
static constexpr int    kPcmRoundsPerSession = 100;

// 两次 PCM 文件之间的静音间隔（ms）
// 此期间不发送音频，服务端 VAD 检测到持续静音后触发分段并回调 on_result
static constexpr int    kInterRoundDelayMs   = 500;

// ============================================================================
// 音频硬件配置（模式二，bread-compact-wifi 板子）
// 与 test_voice_assistant.cpp 引脚配置完全一致
// ============================================================================
static constexpr int          kInputSampleRate  = 16000;
static constexpr int          kOutputSampleRate = 24000;
static constexpr gpio_num_t   kMicSck  = GPIO_NUM_5;
static constexpr gpio_num_t   kMicWs   = GPIO_NUM_4;
static constexpr gpio_num_t   kMicDin  = GPIO_NUM_6;
static constexpr gpio_num_t   kSpkBclk = GPIO_NUM_15;
static constexpr gpio_num_t   kSpkLrck = GPIO_NUM_16;
static constexpr gpio_num_t   kSpkDout = GPIO_NUM_7;

// ============================================================================
// 全局状态
// ============================================================================

// PCM 模式任务句柄（防止重复创建）
static TaskHandle_t g_pcm_task  = nullptr;

// 麦克风模式任务句柄（防止重复创建）
static TaskHandle_t g_voice_task = nullptr;

// 麦克风模式音频驱动实例（整个生命周期只创建一次）
static ai_sdk::AudioCodec* g_codec = nullptr;

// 前向声明
static void sr_persistent_pcm_session();
static void sr_persistent_pcm_loop_task(void* pvParameters);
static void sr_persistent_voice_loop_task(void* pvParameters);

// ============================================================================
// 模式一：PCM 文件模式 — 单次会话（一次 WebSocket 连接）
// ============================================================================

/**
 * @brief 执行一次 PCM 文件持续语音识别会话
 *
 * 一次会话 = 一条 WebSocket 连接 + 多轮 PCM 文件发送。
 *
 * 流程：
 * 1. 设置 3 个回调（on_result / on_error / on_close）
 * 2. asr.start() 建立 WebSocket 连接并发送 START 帧（整个会话只调用一次）
 * 3. 循环 kPcmRoundsPerSession 轮：
 *    a. 按 16kHz 实时速率发送嵌入 PCM（每块 5120 字节，间隔 160ms）
 *    b. 每轮结束后等待 kInterRoundDelayMs（静音期，供服务端 VAD 检测句末）
 *    c. 服务端 VAD 触发后回调 on_result（每轮预期触发一次）
 * 4. 所有轮次完成后调用 asr.stop() 发送 FINISH 帧（整个会话只调用一次）
 * 5. 等待 on_close（服务端返回最后一条识别结果后主动关闭连接）
 *
 * 与旧版 single_round 的核心区别：
 * - 旧版：1 轮 PCM = 1 条连接（每轮都 stop() + 重连）
 * - 新版：N 轮 PCM = 1 条连接（stop() 仅在全部轮次结束后调用一次）
 * - 新版真正验证"持续识别"语义：一次连接收到多条 on_result
 */
static void sr_persistent_pcm_session() {
    ESP_LOGI(TAG, "=== SR Persistent PCM Session Start (rounds=%d) ===",
             kPcmRoundsPerSession);

    if (!ai_sdk::AIAssistantManager::isInitialized()) {
        ESP_LOGE(TAG, "AI SDK not initialized!");
        return;
    }

    auto& asr = ai_sdk::AIAssistantManager::getInstance()
                    .speechRecognitionPersistentHelp();

    // 会话级原子标志：生命周期覆盖整个会话（含 stop() 后等待 on_close 阶段）
    std::atomic_bool got_close{false};
    std::atomic_bool got_error{false};
    std::atomic_int  result_count{0};

    asr.setCallbacks(
        // on_result：服务端推送的所有消息统一进入此回调（含 HEARTBEAT / MID_TEXT / FIN_TEXT）
        [&](const ai_sdk::SpeechRecognitionPersistentResult& r) {
            int count = result_count.fetch_add(1) + 1;
            ESP_LOGI(TAG, "[PCM][%d] err_no=%d | err_msg=%s | log_id=%ld | sn=%s | type=%s | result=%s | start_time=%ld | end_time=%ld | product_id=%d | product_line=%s",
                     count, r.err_no, r.err_msg.c_str(), r.log_id, r.sn.c_str(),
                     r.type.c_str(), r.result.c_str(), r.start_time, r.end_time,
                     r.product_id, r.product_line.c_str());
        },
        // on_error：连接建立失败或运行期间网络错误
        [&](int code, const std::string& msg) {
            got_error.store(true);
            ESP_LOGE(TAG, "[PCM] 错误 code=%d: %s", code, msg.c_str());
        },
        // on_close：WebSocket 连接关闭（stop() / cancel() / 网络异常后触发）
        [&]() {
            got_close.store(true);
            ESP_LOGI(TAG, "[PCM] 连接已关闭，本次会话共收到 %d 条识别结果",
                     result_count.load());
        }
    );

    // 校验嵌入 PCM 数据
    const uint8_t* pcm_start   = _binary_test_asr_pcm_start;
    const uint8_t* pcm_end     = _binary_test_asr_pcm_end;
    const size_t   total_bytes = static_cast<size_t>(pcm_end - pcm_start);
    if (total_bytes == 0) {
        ESP_LOGE(TAG, "[PCM] 嵌入 PCM 文件为空");
        return;
    }

    const int total_chunks = static_cast<int>(
        (total_bytes + kPcmChunkBytes - 1) / kPcmChunkBytes);
    ESP_LOGI(TAG, "[PCM] PCM 大小=%u 字节，每轮分 %d 块发送",
             (unsigned)total_bytes, total_chunks);

    // 建立 WebSocket 连接并发送 START 帧（整个会话只建立一次连接）
    if (!asr.start()) {
        ESP_LOGE(TAG, "[PCM] start() 失败");
        return;
    }

    // =========================================================================
    // 多轮 PCM 发送：在同一条连接内循环发送 kPcmRoundsPerSession 轮
    // 每轮发完后等待静音间隔，服务端 VAD 触发分段并回调 on_result
    // =========================================================================
    for (int round = 1; round <= kPcmRoundsPerSession; ++round) {
        if (got_error.load()) {
            ESP_LOGW(TAG, "[PCM] Round %d/%d: 检测到错误，中止剩余轮次",
                     round, kPcmRoundsPerSession);
            break;
        }

        ESP_LOGI(TAG, "[PCM] Round %d/%d: 开始发送 PCM", round, kPcmRoundsPerSession);

        // 按实时速率（160ms/块）发送 PCM 数据，模拟实时音频流
        size_t offset      = 0;
        int    chunk_index = 0;
        while (offset < total_bytes && !got_error.load()) {
            const size_t remaining = total_bytes - offset;
            const size_t chunk_len = (remaining > kPcmChunkBytes) ? kPcmChunkBytes : remaining;

            asr.sendAudio(pcm_start + offset, chunk_len);
            offset      += chunk_len;
            chunk_index += 1;

            ESP_LOGD(TAG, "[PCM] Round %d/%d 块 %d/%d, len=%u",
                     round, kPcmRoundsPerSession, chunk_index, total_chunks, (unsigned)chunk_len);
            vTaskDelay(pdMS_TO_TICKS(kPcmChunkDelayMs));
        }

        if (got_error.load()) {
            break;
        }

        ESP_LOGI(TAG, "[PCM] Round %d/%d: PCM 发送完毕，等待 %d ms 静音（VAD 分段）",
                 round, kPcmRoundsPerSession, kInterRoundDelayMs);

        // 轮次间静音期：不发送音频，服务端 VAD 检测到持续静音后触发 on_result
        // 最后一轮无需等待，直接进入 stop() 流程
        if (round < kPcmRoundsPerSession) {
            vTaskDelay(pdMS_TO_TICKS(kInterRoundDelayMs));
        }
    }

    // =========================================================================
    // 所有轮次完成（或发生错误），发送 FINISH 帧
    // 整个会话只调用一次 stop()，服务端返回最后一段识别结果后主动关闭连接
    // =========================================================================
    ESP_LOGI(TAG, "[PCM] 所有轮次完成（error=%d），发送 FINISH 帧",
             got_error.load() ? 1 : 0);
    asr.stop();

    // 等待 on_close 确认连接已关闭，再开始下一次会话
    while (!got_close.load() && !got_error.load()) {
        vTaskDelay(pdMS_TO_TICKS(100));
    }

    ESP_LOGI(TAG, "[PCM] 会话结束: result_count=%d, error=%d",
             result_count.load(), got_error.load() ? 1 : 0);
}

// ============================================================================
// 模式一：PCM 文件模式 — 会话循环任务
// ============================================================================

/**
 * @brief PCM 持续语音识别会话循环任务
 *
 * 每次会话在同一条 WebSocket 连接内发送 kPcmRoundsPerSession 轮 PCM 文件，
 * 等待 on_close 后延迟 kRestartDelayMs 再开始下一次会话（重新建立连接）。
 */
static void sr_persistent_pcm_loop_task(void* pvParameters) {
    (void)pvParameters;

    int session = 0;
    while (true) {
        ++session;
        ESP_LOGI(TAG, "[PCM] Session %d begin", session);

        sr_persistent_pcm_session();

        ESP_LOGI(TAG, "[PCM] Session %d complete, restart after %d ms",
                 session, kRestartDelayMs);
        vTaskDelay(pdMS_TO_TICKS(kRestartDelayMs));
    }
}

// ============================================================================
// 模式二：麦克风模式 — 循环任务
// ============================================================================

/**
 * @brief 实时麦克风持续语音识别循环任务
 *
 * 与 test_voice_assistant.cpp 使用相同的硬件配置，区别：
 * - 无 TTS 播放（SpeechRecognitionPersistent 不包含 TtsPlayer）
 * - 持续录音，服务端 VAD 自动分割，每段语音触发一次 on_result
 * - 连接保持直到网络异常或主动 stop() / cancel()，异常后自动重连
 *
 * 初始化流程（只执行一次）：
 * 1. 创建 AudioCodec（I2S Simplex 模式）
 * 2. asr.initAudio(codec)：SDK 内部创建 AudioInput（无 TtsPlayer）
 *
 * 每轮流程：
 * 1. asr.setCallbacks(on_result, on_error, on_close)
 * 2. asr.start()：建立 WebSocket，发送 START 帧
 * 3. asr.setRecording(true)：AudioInput 开始采集并自动 sendAudio()
 * 4. 持续运行，直到网络异常触发 on_close / on_error
 * 5. asr.setRecording(false) + 延迟后重连
 */
static void sr_persistent_voice_loop_task(void* pvParameters) {
    (void)pvParameters;

    // ========================================================================
    // 一次性初始化：创建音频驱动并绑定到 SDK
    // SpeechRecognitionPersistent 只创建 AudioInput（无 TtsPlayer）
    // ========================================================================
    if (g_codec == nullptr) {
        ai_sdk::AudioConfig cfg;
        cfg.hardware_type      = ai_sdk::AudioHardwareType::kNoCodecSimplex;
        cfg.input_sample_rate  = kInputSampleRate;
        cfg.output_sample_rate = kOutputSampleRate;
        cfg.spk_bclk = kSpkBclk;
        cfg.spk_ws   = kSpkLrck;
        cfg.spk_dout = kSpkDout;
        cfg.mic_sck  = kMicSck;
        cfg.mic_ws   = kMicWs;
        cfg.mic_din  = kMicDin;

        g_codec = ai_sdk::CreateAudioCodec(cfg);
        if (g_codec == nullptr) {
            ESP_LOGE(TAG, "[VOICE] 创建 AudioCodec 失败");
            g_voice_task = nullptr;
            vTaskDelete(nullptr);
            return;
        }

        g_codec->Start();
        ESP_LOGI(TAG, "[VOICE] AudioCodec 已启动");

        // 绑定音频驱动到持续语音识别
        // 内部只创建 AudioInput（麦克风 → sendAudio），不创建 TtsPlayer
        auto& asr = ai_sdk::AIAssistantManager::getInstance()
                        .speechRecognitionPersistentHelp();
        if (!asr.initAudio(g_codec)) {
            ESP_LOGE(TAG, "[VOICE] initAudio 失败");
            g_voice_task = nullptr;
            vTaskDelete(nullptr);
            return;
        }

        ESP_LOGI(TAG, "[VOICE] SDK 音频初始化完成（AudioInput only，无 TTS）");
    }

    // ========================================================================
    // 持续识别循环：连接断开后自动重连
    // ========================================================================
    auto& asr = ai_sdk::AIAssistantManager::getInstance()
                    .speechRecognitionPersistentHelp();

    int round = 0;
    while (true) {
        ++round;
        ESP_LOGI(TAG, "[VOICE] Round %d begin", round);

        std::atomic_bool got_close{false};
        std::atomic_bool got_error{false};
        std::atomic_int  result_count{0};

        asr.setCallbacks(
            // on_result：服务端推送的所有消息统一进入此回调（含 HEARTBEAT / MID_TEXT / FIN_TEXT）
            [&](const ai_sdk::SpeechRecognitionPersistentResult& r) {
                int count = result_count.fetch_add(1) + 1;
                ESP_LOGI(TAG, "[VOICE][%d] err_no=%d | err_msg=%s | log_id=%ld | sn=%s | type=%s | result=%s | start_time=%ld | end_time=%ld | product_id=%d | product_line=%s",
                         count, r.err_no, r.err_msg.c_str(), r.log_id, r.sn.c_str(),
                         r.type.c_str(), r.result.c_str(), r.start_time, r.end_time,
                         r.product_id, r.product_line.c_str());
            },
            // on_error：连接失败或网络中断
            [&](int code, const std::string& msg) {
                got_error.store(true);
                ESP_LOGE(TAG, "[VOICE] 错误 code=%d: %s", code, msg.c_str());
            },
            // on_close：WebSocket 连接关闭，需要重连
            [&]() {
                got_close.store(true);
                ESP_LOGI(TAG, "[VOICE] 连接已关闭，共收到 %d 条识别结果", result_count.load());
            }
        );

        if (!asr.start()) {
            ESP_LOGE(TAG, "[VOICE] start() 失败，%d ms 后重试", kRestartDelayMs);
            vTaskDelay(pdMS_TO_TICKS(kRestartDelayMs));
            continue;
        }

        // 开始录音：SDK 内部 AudioInput 自动采集 PCM 并调用 sendAudio()
        asr.setRecording(true);
        ESP_LOGI(TAG, "[VOICE] 开始持续录音，请说话...");

        // 等待连接关闭或发生错误
        // 正常情况下连接长期保持，每段语音后回调 on_result，不会关闭
        // 网络异常时触发 on_error / on_close，退出循环并重连
        while (!got_close.load() && !got_error.load()) {
            vTaskDelay(pdMS_TO_TICKS(200));
        }

        // 确保录音停止
        asr.setRecording(false);

        ESP_LOGI(TAG, "[VOICE] Round %d 结束: result_count=%d, error=%d，%d ms 后重连",
                 round, result_count.load(), got_error.load() ? 1 : 0, kRestartDelayMs);
        vTaskDelay(pdMS_TO_TICKS(kRestartDelayMs));
    }
}

// ============================================================================
// 入口函数（由 test_ai_sdk.cpp 在设备注册成功后调用）
// ============================================================================

/**
 * @brief 启动持续语音识别 PCM 文件测试（循环）
 *
 * 幂等函数：如果任务已在运行，仅打印日志并返回。
 * 使用嵌入固件的 test_asr.pcm 文件，无需麦克风硬件，适合功能验证。
 *
 * @note 与其他 ASR 测试互斥，不可同时运行
 */
extern "C" void start_speech_recognition_persistent_pcm_test(void) {
    if (g_pcm_task != nullptr) {
        ESP_LOGI(TAG, "[PCM] 任务已在运行");
        return;
    }

    BaseType_t rc = xTaskCreate(
        sr_persistent_pcm_loop_task,
        "sr_persist_pcm",
        8192,
        nullptr,
        5,
        &g_pcm_task);

    if (rc != pdPASS) {
        g_pcm_task = nullptr;
        ESP_LOGE(TAG, "[PCM] 创建任务失败");
        return;
    }

    ESP_LOGI(TAG, "[PCM] 任务已启动");
}

/**
 * @brief 启动实时麦克风持续语音识别测试（循环）
 *
 * 幂等函数：如果任务已在运行，仅打印日志并返回。
 * 需要 I2S 麦克风硬件（bread-compact-wifi 板子），断线后自动重连。
 *
 * @note 与其他 ASR 测试互斥，不可同时运行
 */
extern "C" void start_speech_recognition_persistent_voice_test(void) {
    if (g_voice_task != nullptr) {
        ESP_LOGI(TAG, "[VOICE] 任务已在运行");
        return;
    }

    BaseType_t rc = xTaskCreate(
        sr_persistent_voice_loop_task,
        "sr_persist_voice",
        8192,
        nullptr,
        5,
        &g_voice_task);

    if (rc != pdPASS) {
        g_voice_task = nullptr;
        ESP_LOGE(TAG, "[VOICE] 创建任务失败");
        return;
    }

    ESP_LOGI(TAG, "[VOICE] 任务已启动");
}
