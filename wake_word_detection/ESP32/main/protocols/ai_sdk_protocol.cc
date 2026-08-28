/**
 * @file ai_sdk_protocol.cc
 * @brief AI SDK 协议实现
 *
 * 实现 AiSdkProtocol 类，将 ai_sdk::AsrIntelligentDialogue 封装为 Protocol 接口。
 * 使得 Application 层可以无缝切换到云端 ASR 服务器。
 *
 * 支持两种音频管理方式：
 *
 * 【方式 A — SDK 自动管理音频】(CONFIG_AI_SDK_AUTO_AUDIO=y)
 *   在构造函数中调用 initAudio(codec) 后，SDK 内部管理录音和 TTS 播放。
 *   SendAudio() 不会被调用（AudioService 未启动），录音由 SDK AudioInput 自动完成。
 *
 * 【方式 B — 手动管理音频】(CONFIG_AI_SDK_AUTO_AUDIO 未定义)
 *   AudioService 控制麦克风录音，SendAudio() 接收 Opus 数据并解码为 PCM 后发送。
 *   积累 PCM 数据到 5120 字节后发送，与 Android DealSotaOne.sendAudioMicrophoneData() 一致。
 *
 * 切换方式：在 menuconfig 中设置 "AI SDK auto-manage audio (Mode A)" 选项。
 */

#include "ai_sdk_protocol.h"

#ifdef CONFIG_AI_SDK_AUTO_AUDIO
#include "config.h"  // 板子硬件配置（AUDIO_INPUT_SAMPLE_RATE、GPIO 引脚等）
#else
#include "audio_service.h"  // OPUS_FRAME_DURATION_MS（方式 B 需要）
#endif

#include <esp_log.h>
#include <cJSON.h>
#include <cstring>

#define TAG "AiSdkProtocol"

// ============================================================================
// 构造函数和析构函数
// ============================================================================

AiSdkProtocol::AiSdkProtocol()
    : asr_(ai_sdk::AIAssistantManager::getInstance().asrIntelligentDialogueHelp()) {
    ESP_LOGI(TAG, "Initializing AI SDK Protocol");

#ifdef CONFIG_AI_SDK_AUTO_AUDIO
    // ========================================================================
    // 方式 A：SDK 自动管理音频
    // ========================================================================
    // SDK 内部管理麦克风录音（AudioInput）和 TTS 播放（TtsPlayer）。
    // 主项目无需启动 AudioService，SendAudio() 不会被调用。
    // ========================================================================
    ai_sdk::AudioConfig audio_cfg = CreateAudioConfig();
    codec_ = ai_sdk::CreateAudioCodec(audio_cfg);
    if (codec_) {
        codec_->Start();
        codec_->SetOutputVolume(5);
        asr_.initAudio(codec_);
        asr_.setAutoPlayTts(true);
        ESP_LOGI(TAG, "Mode A: SDK auto-audio enabled (AudioInput + TtsPlayer)");
    } else {
        ESP_LOGE(TAG, "Failed to create AudioCodec for Mode A!");
    }
#else
    // ========================================================================
    // 方式 B：手动管理音频（AudioService 控制麦克风，Opus 编解码）
    // ========================================================================
    // 初始化 Opus 解码器，用于将 AudioService 发来的 Opus 数据解码为 PCM
    // 参数：16kHz 采样率，单声道，20ms 帧时长
    // ========================================================================
    opus_decoder_ = std::make_unique<OpusDecoderWrapper>(16000, 1, OPUS_FRAME_DURATION_MS);
    ESP_LOGI(TAG, "Mode B: Opus decoder initialized: 16kHz, 1ch, %dms", OPUS_FRAME_DURATION_MS);
#endif

    SetupAsrCallbacks();
}

AiSdkProtocol::~AiSdkProtocol() {
    ESP_LOGI(TAG, "Destroying AI SDK Protocol");
    // 确保连接已关闭
    if (asr_.isConnected()) {
        asr_.stop();
    }
    // 方式 A：释放 SDK 创建的 AudioCodec（方式 B 时 codec_ 为 nullptr，delete 安全）
    delete codec_;
    codec_ = nullptr;
}

// ============================================================================
// 回调设置
// ============================================================================

void AiSdkProtocol::SetupAsrCallbacks() {
    asr_.setCallbacks(
        // ========================================
        // onConnected 回调
        // 对应 Android: listener?.onConnected()
        // 转换为 Protocol: on_audio_channel_opened_
        // ========================================
        [this]() {
            ESP_LOGI(TAG, "ASR connected, audio channel opened");
            
            // 更新时间戳（防止超时判断）
            last_incoming_time_ = std::chrono::steady_clock::now();
            
            // 通知 Application 音频通道已打开
            if (on_audio_channel_opened_) {
                on_audio_channel_opened_();
            }
        },

        // ========================================
        // onAsrResult 回调
        // 对应 Android: listener?.onAsrMidResult() / onAsrFinalResult()
        // 转换为 Protocol: on_incoming_json_ (type: "stt")
        // ========================================
        [this](const ai_sdk::AsrResult& result) {
            last_incoming_time_ = std::chrono::steady_clock::now();

            // 转换为现有的 JSON 格式：{"type":"stt", "text":"..."}
            // 这样 Application::OnIncomingJson 可以直接处理
            cJSON* root = cJSON_CreateObject();
            if (!root) {
                ESP_LOGE(TAG, "Failed to create JSON for ASR result");
                return;
            }

            cJSON_AddStringToObject(root, "type", "stt");
            cJSON_AddStringToObject(root, "text", result.text.c_str());

            if (on_incoming_json_) {
                on_incoming_json_(root);
            }

            cJSON_Delete(root);
        },

        // ========================================
        // onDialogueResult 回调
        // 对应 Android: listener?.onDialogueResult()
        // 根据 directive 类型转换为不同的 Protocol 回调
        // ========================================
        [this](const ai_sdk::DialogueResult& result) {
            last_incoming_time_ = std::chrono::steady_clock::now();

            ESP_LOGD(TAG, "Dialogue result: directive=%s, is_end=%d",
                     result.directive.c_str(), result.is_end);

            // 根据 directive 类型处理
            if (result.directive == "RenderStreamCard") {
                // 流式文本 → 转换为 tts sentence_start
                // 让 Application 显示助手回复
                cJSON* payload = cJSON_Parse(result.payload.c_str());
                if (payload) {
                    cJSON* answer = cJSON_GetObjectItem(payload, "answer");
                    if (answer && cJSON_IsString(answer)) {
                        cJSON* root = cJSON_CreateObject();
                        if (root) {
                            cJSON_AddStringToObject(root, "type", "tts");
                            cJSON_AddStringToObject(root, "state", "sentence_start");
                            cJSON_AddStringToObject(root, "text", answer->valuestring);

                            if (on_incoming_json_) {
                                on_incoming_json_(root);
                            }
                            cJSON_Delete(root);
                        }
                    }
                    cJSON_Delete(payload);
                }
            } else if (result.directive == "Speak") {
                // TTS 播放指令
                // 方式 A：SDK 内部 TtsPlayer 自动下载 MP3 并播放（setAutoPlayTts(true) 已启用时）
                // 方式 B：SDK 不自动播放，仅通知 Application 更新 UI 状态
                ESP_LOGI(TAG, "Speak directive received");

                // 通知 Application 进入 TTS 状态（两种方式通用）
                cJSON* root = cJSON_CreateObject();
                if (root) {
                    cJSON_AddStringToObject(root, "type", "tts");
                    cJSON_AddStringToObject(root, "state", "start");
                    if (on_incoming_json_) {
                        on_incoming_json_(root);
                    }
                    cJSON_Delete(root);
                }
            } else if (result.directive == "Play") {
                // 音乐播放指令
                ESP_LOGD(TAG, "Play directive received");
            }

            // is_end=1 时表示对话结束
            if (result.is_end == 1 && !result.assistant_answer_content.empty()) {
                ESP_LOGI(TAG, "Dialogue completed, answer: %s",
                         result.assistant_answer_content.c_str());
            }
        },

        // ========================================
        // onError 回调
        // 对应 Android: listener?.onError()
        // 转换为 Protocol: SetError() → on_network_error_
        // ========================================
        [this](int code, const std::string& message) {
            ESP_LOGE(TAG, "ASR error: code=%d, message=%s", code, message.c_str());
            SetError(message);
        },

        // ========================================
        // onComplete 回调
        // 对应 Android: listener?.onComplete()
        // 转换为 Protocol: on_audio_channel_closed_
        // ========================================
        [this]() {
            ESP_LOGI(TAG, "ASR session completed, audio channel closed");

            if (on_audio_channel_closed_) {
                on_audio_channel_closed_();
            }
        }
    );

    ESP_LOGI(TAG, "ASR callbacks configured");
}

// ============================================================================
// Protocol 接口实现
// ============================================================================

bool AiSdkProtocol::Start() {
    // ai_sdk 不需要预启动，连接在 OpenAudioChannel() 中建立
    ESP_LOGI(TAG, "Protocol started (no pre-connection needed)");
    return true;
}

bool AiSdkProtocol::OpenAudioChannel() {
    ESP_LOGI(TAG, "Opening audio channel...");

    // 重置错误状态，允许新会话正常工作
    // 与 WebsocketProtocol、MqttProtocol 的 OpenAudioChannel() 保持一致
    error_occurred_ = false;

#ifndef CONFIG_AI_SDK_AUTO_AUDIO
    // 方式 B：清空 PCM 缓冲区
    audio_buffer_.clear();
#endif

    // 启动 ai_sdk 连接
    // 内部会建立 WebSocket 连接并发送 Start Signal
    if (!asr_.start()) {
        ESP_LOGE(TAG, "Failed to start ASR connection");
        return false;
    }

    // 通知 SDK 开始录音
    // 方式 A：SDK 内部 AudioInput 开始采集麦克风数据并通过 sendAudio() 发送
    // 方式 B：initAudio() 未调用，SDK 内部忽略此调用，不影响现有逻辑
    asr_.setRecording(true);

    ESP_LOGI(TAG, "Audio channel opened successfully");
    return true;
}

void AiSdkProtocol::CloseAudioChannel() {
    ESP_LOGI(TAG, "Closing audio channel...");

    // 通知 SDK 停止录音
    // 方式 A：SDK 内部 AudioInput 停止采集麦克风数据
    // 方式 B：initAudio() 未调用，SDK 内部忽略此调用，不影响现有逻辑
    asr_.setRecording(false);

#ifndef CONFIG_AI_SDK_AUTO_AUDIO
    // 方式 B：发送剩余缓冲区数据（会话结束前）
    // 这些是 PCM 数据，只是还没达到发送阈值
    if (!audio_buffer_.empty()) {
        ESP_LOGD(TAG, "Sending remaining PCM buffer: %zu bytes", audio_buffer_.size());
        asr_.sendAudio(audio_buffer_.data(), audio_buffer_.size());
        audio_buffer_.clear();
    }
#endif

    // 停止 ai_sdk 连接
    // 内部会发送 Finish Signal 并断开 WebSocket
    asr_.stop();

    ESP_LOGI(TAG, "Audio channel closed");
}

bool AiSdkProtocol::IsAudioChannelOpened() const {
    return asr_.isConnected() && !error_occurred_ && !IsTimeout();
}

bool AiSdkProtocol::SendAudio(std::unique_ptr<AudioStreamPacket> packet) {
#ifdef CONFIG_AI_SDK_AUTO_AUDIO
    // ========================================
    // 方式 A：空操作
    // ========================================
    // SDK 内部 AudioInput 已在录音任务中自动采集和发送音频，
    // 主项目的 AudioService 未启动，此方法不会被频繁调用。
    // ========================================
    return true;
#else
    // ========================================
    // 方式 B：Opus 解码为 PCM，积累后发送
    // ========================================
    //
    // 问题背景：
    // - ESP32 的 Opus 编码帧时长（20ms）与服务器期望的帧时长（10ms）不匹配
    // - 导致服务器解码失败：opus: buffer too small / corrupted stream
    //
    // 解决方案：
    // - 将 Opus 数据解码回 PCM 后发送
    // - 与 Android DealSotaOne.sendAudioMicrophoneData() 模式一致
    //
    // 处理流程：
    // 1. 接收 Opus 编码的音频包（来自 AudioService）
    // 2. 使用 OpusDecoderWrapper 解码为 PCM（int16_t 数组）
    // 3. 将 PCM 样本转换为字节（little-endian）并加入缓冲区
    // 4. 缓冲区达到 5120 字节时发送（160ms @ 16kHz 16bit mono）
    //
    // 与 Android 对应：
    // - Android CHUNK_SIZE = 5120（来自 hht_ctx4.conf）
    // - 每次读取 5120 字节 PCM → 直接发送 WebSocket
    // ========================================

    // 检查连接状态
    if (!asr_.isRecognizing()) {
        return false;
    }

    // 第1步：Opus 解码为 PCM
    std::vector<int16_t> pcm_samples;
    if (!opus_decoder_->Decode(std::move(packet->payload), pcm_samples)) {
        ESP_LOGW(TAG, "Failed to decode Opus packet");
        return false;
    }

    // 第2步：将 PCM 样本转换为字节（little-endian）
    // 每个样本 2 字节，低字节在前（little-endian）
    const uint8_t* pcm_bytes = reinterpret_cast<const uint8_t*>(pcm_samples.data());
    size_t pcm_byte_count = pcm_samples.size() * sizeof(int16_t);
    audio_buffer_.insert(audio_buffer_.end(), pcm_bytes, pcm_bytes + pcm_byte_count);

    // 第3步：达到阈值时发送
    // 阈值 5120 字节 = 160ms @ 16kHz 16bit mono
    if (audio_buffer_.size() >= SEND_THRESHOLD) {
        asr_.sendAudio(audio_buffer_.data(), audio_buffer_.size());
        audio_buffer_.clear();
    }

    return true;
#endif  // CONFIG_AI_SDK_AUTO_AUDIO
}

// ============================================================================
// 可选覆盖方法
// ============================================================================

void AiSdkProtocol::SendStartListening(ListeningMode mode) {
    // ai_sdk 在 start() 时自动发送 Start Signal
    // 此方法不需要额外操作
    ESP_LOGD(TAG, "SendStartListening called (no-op for AI SDK)");
}

void AiSdkProtocol::SendStopListening() {
    // 停止监听 = 关闭音频通道
    ESP_LOGI(TAG, "SendStopListening called, closing audio channel");
    CloseAudioChannel();
}

bool AiSdkProtocol::SendText(const std::string& text) {
    // ai_sdk 不需要发送 JSON 文本消息
    // 现有 Protocol 的 SendWakeWordDetected、SendAbortSpeaking 等方法
    // 会调用此函数，但 ai_sdk 不需要这些消息
    ESP_LOGD(TAG, "SendText called (no-op for AI SDK): %s", text.c_str());
    return true;
}

// ============================================================================
// 辅助方法
// ============================================================================

bool AiSdkProtocol::IsTimeout() const {
    // 计算自上次收到服务器消息以来的时间
    auto now = std::chrono::steady_clock::now();
    auto elapsed = std::chrono::duration_cast<std::chrono::milliseconds>(now - last_incoming_time_);
    return elapsed.count() > TIMEOUT_MS;
}

// ============================================================================
// 方式 A：音频硬件配置
// ============================================================================

#ifdef CONFIG_AI_SDK_AUTO_AUDIO
ai_sdk::AudioConfig AiSdkProtocol::CreateAudioConfig() {
    ai_sdk::AudioConfig cfg;

    // 采样率（来自板子 config.h）
    cfg.input_sample_rate  = AUDIO_INPUT_SAMPLE_RATE;
    cfg.output_sample_rate = AUDIO_OUTPUT_SAMPLE_RATE;

#ifdef AUDIO_I2S_METHOD_SIMPLEX
    // ========================================================================
    // Simplex 模式：麦克风和扬声器使用独立的 I2S 端口
    // ========================================================================
    // 适用于无芯片方案（如 INMP441 麦克风 + MAX98357 功放）
    // 麦克风和扬声器各有独立的时钟线、帧同步线和数据线
    // ========================================================================
    cfg.hardware_type = ai_sdk::AudioHardwareType::kNoCodecSimplex;

    // 扬声器 I2S 引脚（TX 通道）
    cfg.spk_bclk = AUDIO_I2S_SPK_GPIO_BCLK;
    cfg.spk_ws   = AUDIO_I2S_SPK_GPIO_LRCK;
    cfg.spk_dout = AUDIO_I2S_SPK_GPIO_DOUT;

    // 麦克风 I2S 引脚（RX 通道）
    cfg.mic_sck  = AUDIO_I2S_MIC_GPIO_SCK;
    cfg.mic_ws   = AUDIO_I2S_MIC_GPIO_WS;
    cfg.mic_din  = AUDIO_I2S_MIC_GPIO_DIN;
#else
    // ========================================================================
    // Duplex 模式：麦克风和扬声器共用同一组 I2S 引脚
    // ========================================================================
    // 适用于无芯片双工方案，麦克风和扬声器共享时钟线和帧同步线，
    // 数据线分别为 DIN（输入）和 DOUT（输出）
    // ========================================================================
    cfg.hardware_type = ai_sdk::AudioHardwareType::kNoCodecDuplex;

    cfg.bclk = AUDIO_I2S_GPIO_BCLK;
    cfg.ws   = AUDIO_I2S_GPIO_WS;
    cfg.dout = AUDIO_I2S_GPIO_DOUT;
    cfg.din  = AUDIO_I2S_GPIO_DIN;
#endif

    // 注意：当前配置仅适用于无芯片板子（bread-compact-wifi 等）。
    // 如果切换到有芯片的板子（如 esp-box-3 使用 ES8311），
    // 需要额外配置以下字段：
    //   cfg.hardware_type = ai_sdk::AudioHardwareType::kEs8311;
    //   cfg.i2c_master_handle = /* 板子 I2C 主机句柄 */;
    //   cfg.i2c_port = /* I2C 端口号 */;
    //   cfg.mclk / bclk / ws / dout / din = /* I2S 引脚 */;
    //   cfg.codec_addr = /* 芯片 I2C 地址 */;
    //   cfg.pa_pin = /* 功放使能引脚 */;

    return cfg;
}
#endif  // CONFIG_AI_SDK_AUTO_AUDIO

