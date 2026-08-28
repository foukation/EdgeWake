/**
 * @file test_voice_assistant.cpp
 * @brief 实时麦克风语音助手测试
 *
 * 使用 SDK 内置音频模块，实现实时麦克风录音 + ASR 智能对话 + TTS 自动播放。
 * 与 test_asr_pcm.cpp 的区别：
 * - test_asr_pcm.cpp：使用嵌入 PCM 文件，手动调用 sendAudio() 发送
 * - test_voice_assistant.cpp：使用实时麦克风，SDK 内部自动采集和发送
 *
 * SDK 内部工作原理：
 * - AudioInput 任务：从麦克风采集 PCM → 调用 sendAudio() → 云端 ASR
 * - TtsPlayer 任务：收到 Speak 指令 → HTTP 下载 MP3 → 解码 → 播放到扬声器
 *
 * 测试流程：
 * 1. 创建音频硬件驱动（I2S 麦克风 + I2S 扬声器）
 * 2. 绑定音频驱动到 SDK（initAudio）
 * 3. 启用 TTS 自动播放（setAutoPlayTts）
 * 4. 设置 5 个回调（connected / asr / dialogue / error / complete）
 * 5. asr.start() 建立 WebSocket 连接
 * 6. asr.setRecording(true) 开始录音
 * 7. 等待 onComplete 回调（服务器关闭 WebSocket）
 * 8. asr.stop() 清理资源
 * 9. 延迟后重复（循环测试）
 *
 * 硬件要求：
 * - bread-compact-wifi 板子（INMP441 麦克风 + MAX98357 功放）
 * - I2S Simplex 模式：麦克风和扬声器使用独立的 I2S 端口
 */
#include "ai_sdk/ai_assistant_manager.h"
#include "ai_sdk/audio/audio_config.h"
#include <driver/i2c_master.h>   // I2C 主机总线（ES8311 需要）
#include <esp_log.h>
#include <atomic>
#include <memory>
#include <freertos/FreeRTOS.h>
#include <freertos/task.h>

static const char* TAG = "VOICE_ASSISTANT_TEST";

// ============================================================================
// 音频硬件配置（bread-compact-wifi 板子）
// ============================================================================

// 采样率配置
// 麦克风输入采样率：16kHz（ASR 服务器要求）
// 扬声器输出采样率：24kHz（TTS 音频播放）
static constexpr int kInputSampleRate  = 16000;
static constexpr int kOutputSampleRate = 24000;

// I2S Simplex 模式引脚定义
// 麦克风（INMP441）：独立的 I2S RX 端口
static constexpr gpio_num_t kMicSck = GPIO_NUM_5;   // 麦克风时钟（SCK/BCLK）
static constexpr gpio_num_t kMicWs  = GPIO_NUM_4;   // 麦克风帧同步（WS/LRCLK）
static constexpr gpio_num_t kMicDin = GPIO_NUM_6;   // 麦克风数据输入（SD/DOUT）

// 扬声器（MAX98357）：独立的 I2S TX 端口
static constexpr gpio_num_t kSpkBclk = GPIO_NUM_15;  // 扬声器位时钟
static constexpr gpio_num_t kSpkLrck = GPIO_NUM_16;  // 扬声器帧同步
static constexpr gpio_num_t kSpkDout = GPIO_NUM_7;   // 扬声器数据输出

// 音量（0-100，MAX98357 功放音量较大，建议设低）
static constexpr int kOutputVolume = 10;

// ---------- ES8311 方案引脚定义（与原理图 聊天机器人.pdf 一致） ----------
// I2C 控制总线
static constexpr gpio_num_t kI2cSda = GPIO_NUM_17;   // ES_I2C_SDA  → U1 CDATA(19)
static constexpr gpio_num_t kI2cScl = GPIO_NUM_18;   // ES_I2C_CLK  → U1 CCLK(1)

// I2S 双工引脚（ES8311 共用一组 I2S）
static constexpr gpio_num_t kEs8311Mclk = GPIO_NUM_16;   // Codec_I2S0_MCLK   → U1 MCLK(2)
static constexpr gpio_num_t kEs8311Bclk = GPIO_NUM_9;    // Codec_I2S0_SCLK   → U1 SCLK(6)
static constexpr gpio_num_t kEs8311Ws   = GPIO_NUM_45;   // Codec_I2S0_LRCK   → U1 LRCK(8)
static constexpr gpio_num_t kEs8311Dout = GPIO_NUM_8;    // Codec_I2S0_DSDIN  → U1 DSDIN(9)（播放）
static constexpr gpio_num_t kEs8311Din  = GPIO_NUM_10;   // Codec_I2S0_ASDOUT ← U1 ASDOUT(7)（录音）

// 功放使能
static constexpr gpio_num_t kPaPin = GPIO_NUM_48;    // PA_CTRL → U6 NS4150B CTRL(1)

// ES8311 I2C 地址（7-bit 行业标准格式，与芯片手册一致）
// SDK 内部自动转换为底层驱动所需的 8-bit 格式，调用方无需关心。
static constexpr uint8_t kEs8311Addr = 0x18;

// 对话间隔（上一轮 onComplete 后等待多久再开始下一轮）
static constexpr int kRestartDelayMs = 1000;

// ============================================================================
// 全局状态
// ============================================================================

// 音频驱动实例（整个生命周期只创建一次）
static ai_sdk::AudioCodec* g_codec = nullptr;

// 语音助手循环任务句柄（防止重复创建）
static TaskHandle_t g_voice_task = nullptr;

// 前向声明
static void voice_assistant_loop_task(void* pvParameters);
static void voice_assistant_single_round();
static ai_sdk::AudioConfig create_audio_config();
static ai_sdk::AudioConfig create_audio_config_es8311();

// ============================================================================
// 音频硬件配置
// ============================================================================

/**
 * @brief 创建音频硬件配置
 *
 * 返回适配 bread-compact-wifi 板子的 AudioConfig。
 * 该板子使用 I2S Simplex 模式：
 * - 麦克风（INMP441）和扬声器（MAX98357）各自有独立的 I2S 端口
 * - 不需要编解码芯片（No Codec）
 *
 * @return ai_sdk::AudioConfig 填充好的配置结构体
 */
static ai_sdk::AudioConfig create_audio_config() {
    ai_sdk::AudioConfig cfg;

    cfg.hardware_type      = ai_sdk::AudioHardwareType::kNoCodecSimplex;
    cfg.input_sample_rate  = kInputSampleRate;
    cfg.output_sample_rate = kOutputSampleRate;

    // 扬声器 I2S 引脚（TX 通道）
    cfg.spk_bclk = kSpkBclk;
    cfg.spk_ws   = kSpkLrck;
    cfg.spk_dout = kSpkDout;

    // 麦克风 I2S 引脚（RX 通道）
    cfg.mic_sck = kMicSck;
    cfg.mic_ws  = kMicWs;
    cfg.mic_din = kMicDin;

    return cfg;
}

// ============================================================================
// ES8311 音频硬件配置
// ============================================================================

// I2C 主机总线句柄（ES8311 需要，只创建一次）
static i2c_master_bus_handle_t g_i2c_bus = nullptr;

/**
 * @brief 创建 ES8311 音频硬件配置
 *
 * 返回适配 ES8311 单芯片 Codec 开发板的 AudioConfig。
 * ES8311 使用 I2S 双工模式 + I2C 控制总线。
 * 首次调用时自动创建 I2C 主机总线。
 *
 * @return ai_sdk::AudioConfig 填充好的配置结构体
 */
static ai_sdk::AudioConfig create_audio_config_es8311() {
    // 创建 I2C 主机总线（只创建一次）
    if (g_i2c_bus == nullptr) {
        i2c_master_bus_config_t i2c_cfg = {
            .i2c_port = I2C_NUM_0,
            .sda_io_num = kI2cSda,
            .scl_io_num = kI2cScl,
            .clk_source = I2C_CLK_SRC_DEFAULT,
            .glitch_ignore_cnt = 7,
            .intr_priority = 0,
            .trans_queue_depth = 0,
            .flags = {
                .enable_internal_pullup = 1,
            },
        };
        ESP_ERROR_CHECK(i2c_new_master_bus(&i2c_cfg, &g_i2c_bus));
    }

    ai_sdk::AudioConfig cfg;
    cfg.hardware_type      = ai_sdk::AudioHardwareType::kEs8311;
    cfg.input_sample_rate  = 16000;  // ES8311 要求输入输出采样率一致
    cfg.output_sample_rate = 16000;
    cfg.i2c_master_handle  = g_i2c_bus;
    cfg.i2c_port           = I2C_NUM_0;
    cfg.mclk       = kEs8311Mclk;
    cfg.bclk       = kEs8311Bclk;
    cfg.ws         = kEs8311Ws;
    cfg.dout       = kEs8311Dout;
    cfg.din        = kEs8311Din;
    cfg.pa_pin     = kPaPin;
    cfg.codec_addr = kEs8311Addr;
    cfg.use_mclk   = true;
    return cfg;
}

// ============================================================================
// 单轮实时语音对话
// ============================================================================

/**
 * @brief 执行一轮实时语音对话
 *
 * 流程：
 * 1. 设置回调
 * 2. asr.start() 建立 WebSocket 连接
 * 3. asr.setRecording(true) 开始麦克风录音
 * 4. SDK 内部 AudioInput 自动采集 PCM 并发送到云端
 * 5. 等待 onComplete（服务器关闭连接）
 * 6. asr.stop() 清理
 *
 * 注意：音频驱动（codec）和 initAudio() 在任务启动时已完成，
 * 循环内不需要重复初始化。
 */
static void voice_assistant_single_round() {
    ESP_LOGI(TAG, "=== Voice Assistant Round Start ===");

    if (!ai_sdk::AIAssistantManager::isInitialized()) {
        ESP_LOGE(TAG, "AI SDK not initialized!");
        return;
    }

    auto& asr = ai_sdk::AIAssistantManager::getInstance().asrIntelligentDialogueHelp();

    // 状态标志
    std::atomic_bool got_final_result{false};
    std::atomic_bool got_dialogue_end{false};
    std::atomic_bool got_complete{false};
    std::atomic_bool got_error{false};
    // 单轮内状态：是否收到过 Speak，以及 TTS 是否全部播放完成
    struct TtsRoundState {
        std::atomic_bool got_speak{false};
        std::atomic_bool tts_all_done{false};
    };
    auto tts_state = std::make_shared<TtsRoundState>();

    // 单轮注册 TTS 播放回调：只更新状态，不做阻塞操作
    asr.setTtsPlaybackCallback([tts_state](const std::string& /*url*/, bool completed, bool all_done) {
        ESP_LOGI(TAG, "[VOICE][TTS] completed=%d, all_done=%d",
                 completed ? 1 : 0, all_done ? 1 : 0);
        if (all_done) {
            tts_state->tts_all_done.store(true);
        }
    });

    // 设置回调
    asr.setCallbacks(
        // onConnected：WebSocket 连接成功
        []() {
            ESP_LOGI(TAG, "[VOICE] ASR connected, listening...");
        },

        // onAsrResult：语音识别结果（中间结果 / 最终结果）
        [&](const ai_sdk::AsrResult& result) {
            if (result.is_final) {
                // 单轮会话中：首次收到 final 结果后立即停录音，
                // 避免后续 TTS 下载/播放阶段继续占用上行带宽导致卡顿。
                // 使用 exchange 做幂等保护，防止极端情况下重复 final 触发重复停录音。
                bool first_final = !got_final_result.exchange(true);
                ESP_LOGI(TAG, "[VOICE] Final ASR: %s", result.text.c_str());

                if (first_final) {
                    asr.setRecording(false);
                    ESP_LOGI(TAG, "[VOICE] Recording stopped on first final ASR");
                }

                if (!result.emotion.empty()) {
                    ESP_LOGI(TAG, "[VOICE] Emotion: %s", result.emotion.c_str());
                }
            } else {
                ESP_LOGD(TAG, "[VOICE] Partial ASR: %s", result.text.c_str());
            }
        },

        // onDialogueResult：AI 对话结果
        [&](const ai_sdk::DialogueResult& result) {
            if (result.directive == "RenderStreamCard") {
                // 流式文本回复
                ESP_LOGD(TAG, "[VOICE] Streaming text received");
            } else if (result.directive == "Speak") {
                // 收到 Speak 后，本轮需要等待 TTS 全部播放完成
                tts_state->got_speak.store(true);
                tts_state->tts_all_done.store(false);
                // TTS 播放指令（SDK 自动播放，无需手动处理）
                ESP_LOGI(TAG, "[VOICE] TTS Speak directive received (auto-playing)");
            }

            if (result.is_end == 1) {
                got_dialogue_end.store(true);
                ESP_LOGI(TAG, "[VOICE] Dialogue end, answer: %s",
                         result.assistant_answer_content.c_str());
            }
        },

        // onError：错误处理
        [&](int code, const std::string& message) {
            got_error.store(true);
            ESP_LOGE(TAG, "[VOICE] ASR error: code=%d, msg=%s", code, message.c_str());
        },

        // onComplete：会话结束（服务器关闭 WebSocket）
        [&]() {
            got_complete.store(true);
            ESP_LOGI(TAG, "[VOICE] Session complete");
        }
    );

    // 启动 ASR 会话（建立 WebSocket 连接）
    if (!asr.start()) {
        ESP_LOGE(TAG, "[VOICE] Failed to start ASR session");
        return;
    }

    // 开始麦克风录音
    // SDK 内部 AudioInput 任务自动从麦克风采集 PCM 并通过 sendAudio() 发送
    asr.setRecording(true);
    ESP_LOGI(TAG, "[VOICE] Recording started, speak now...");

    // 等待本轮结束：
    // 1) 发生错误：立即结束
    // 2) 未收到 Speak：会话 complete 后结束
    // 3) 收到 Speak：会话 complete 且 TTS 全部播放完成后结束
    while (!got_error.load() &&
           !(got_complete.load() &&
             (!tts_state->got_speak.load() || tts_state->tts_all_done.load()))) {
        vTaskDelay(pdMS_TO_TICKS(100));
    }

    // 停止录音
    asr.setRecording(false);

    // 清理会话
    asr.stop();

    ESP_LOGI(TAG,
             "[VOICE] Summary: final_result=%d, dialogue_end=%d, complete=%d, error=%d",
             got_final_result.load() ? 1 : 0,
             got_dialogue_end.load() ? 1 : 0,
             got_complete.load() ? 1 : 0,
             got_error.load() ? 1 : 0);
}

// ============================================================================
// 语音助手循环任务
// ============================================================================

/**
 * @brief 语音助手持续循环任务
 *
 * 任务启动时：
 * 1. 创建音频驱动（只做一次）
 * 2. 绑定到 SDK（只做一次）
 *
 * 循环中：
 * 1. 执行一轮实时对话
 * 2. 等待 kRestartDelayMs 后开始下一轮
 */
static void voice_assistant_loop_task(void* pvParameters) {
    (void)pvParameters;

    // ========================================================================
    // 一次性初始化：创建音频驱动并绑定到 SDK
    // ========================================================================
    if (g_codec == nullptr) {
        // 方式 A：NoCodecSimplex（INMP441 + MAX98357，无 Codec 芯片）
        //ai_sdk::AudioConfig audio_cfg = create_audio_config();

        // 方式 B：ES8311 单芯片 Codec（需要 I2C + I2S 双工）
        ai_sdk::AudioConfig audio_cfg = create_audio_config_es8311();
        g_codec = ai_sdk::CreateAudioCodec(audio_cfg);

        if (g_codec == nullptr) {
            ESP_LOGE(TAG, "Failed to create AudioCodec!");
            g_voice_task = nullptr;
            vTaskDelete(nullptr);
            return;
        }

        g_codec->Start();
        g_codec->SetOutputVolume(kOutputVolume);
        ESP_LOGI(TAG, "AudioCodec created and started (volume=%d)", kOutputVolume);

        // 绑定音频驱动到 SDK
        auto& asr = ai_sdk::AIAssistantManager::getInstance().asrIntelligentDialogueHelp();
        if (!asr.initAudio(g_codec)) {
            ESP_LOGE(TAG, "Failed to init audio in SDK!");
            g_voice_task = nullptr;
            vTaskDelete(nullptr);
            return;
        }

        // 启用 TTS 自动播放
        // 收到 Speak 指令时，SDK 自动 HTTP 下载 MP3 → 解码 → 播放到扬声器
        asr.setAutoPlayTts(true);

        ESP_LOGI(TAG, "SDK audio initialized: initAudio + setAutoPlayTts(true)");
    }

    // ========================================================================
    // 持续对话循环
    // ========================================================================
    int round = 0;
    while (true) {
        ++round;
        ESP_LOGI(TAG, "[VOICE] Round %d begin", round);

        voice_assistant_single_round();

        ESP_LOGI(TAG, "[VOICE] Round %d complete, restart after %d ms",
                 round, kRestartDelayMs);
        vTaskDelay(pdMS_TO_TICKS(kRestartDelayMs));
    }
}

// ============================================================================
// 入口函数
// ============================================================================

/**
 * @brief 启动实时语音助手循环任务
 *
 * 幂等函数：如果任务已在运行，仅打印日志并返回。
 * 由 test_ai_sdk.cpp 在设备注册成功后调用。
 *
 * @note 与 start_pcm_loop_test() 互斥，两者不可同时运行
 *       （都使用同一个 ASR 会话实例）
 */
extern "C" void start_voice_assistant_test(void) {
    if (g_voice_task != nullptr) {
        ESP_LOGI(TAG, "[VOICE] Task already running");
        return;
    }

    BaseType_t rc = xTaskCreate(
        voice_assistant_loop_task,
        "voice_assistant",
        8192,
        nullptr,
        5,
        &g_voice_task);

    if (rc != pdPASS) {
        g_voice_task = nullptr;
        ESP_LOGE(TAG, "[VOICE] Failed to create task");
        return;
    }

    ESP_LOGI(TAG, "[VOICE] Task started");
}
