/**
 * @file audio_codec.cc
 * @brief 音频编解码器抽象基类实现
 *
 * 移植自 ESP32-RTOS-AI-SDK 项目，移除了对 board.h 和 settings.h 的依赖。
 * 音量设置不再持久化存储，使用默认值。
 *
 * @version 1.0.0
 * @date 2026-03-05
 */

#include "ai_sdk/audio/audio_codec.h"

#include <esp_log.h>
#include <cstring>
#include <driver/i2s_common.h>

namespace ai_sdk {

static const char* TAG = "AudioCodec";

AudioCodec::AudioCodec() {
}

AudioCodec::~AudioCodec() {
}

void AudioCodec::OutputData(std::vector<int16_t>& data) {
    Write(data.data(), data.size());
}

bool AudioCodec::InputData(std::vector<int16_t>& data) {
    int samples = Read(data.data(), data.size());
    if (samples > 0) {
        return true;
    }
    return false;
}

void AudioCodec::Start() {
    // 使用默认音量值（不再从 settings 加载）
    // output_volume_ 已在头文件中初始化为 20
    if (output_volume_ <= 0) {
        ESP_LOGW(TAG, "Output volume value (%d) is too small, setting to default (10)", output_volume_);
        output_volume_ = 10;
    }

    // 使能 TX 通道
    if (tx_handle_ != nullptr) {
        ESP_ERROR_CHECK(i2s_channel_enable(tx_handle_));
    }

    // 使能 RX 通道
    if (rx_handle_ != nullptr) {
        ESP_ERROR_CHECK(i2s_channel_enable(rx_handle_));
    }

    // 打开输入/输出
    EnableInput(true);
    EnableOutput(true);
    ESP_LOGI(TAG, "Audio codec started");
}

void AudioCodec::SetOutputVolume(int volume) {
    output_volume_ = volume;
    ESP_LOGI(TAG, "Set output volume to %d", output_volume_);
    // 不再保存到 settings（ai-sdk 无 settings 模块）
}

void AudioCodec::SetInputGain(float gain) {
    input_gain_ = gain;
    ESP_LOGI(TAG, "Set input gain to %.1f", input_gain_);
}

void AudioCodec::EnableInput(bool enable) {
    if (enable == input_enabled_) {
        return;
    }
    input_enabled_ = enable;
    ESP_LOGI(TAG, "Set input enable to %s", enable ? "true" : "false");
}

void AudioCodec::EnableOutput(bool enable) {
    if (enable == output_enabled_) {
        return;
    }
    output_enabled_ = enable;
    ESP_LOGI(TAG, "Set output enable to %s", enable ? "true" : "false");
}

} // namespace ai_sdk
