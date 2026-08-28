/**
 * @file audio_decoder.cc
 * @brief 音频解码器工厂函数实现
 *
 * 实现 CreateAudioDecoder() 工厂函数，根据音频格式类型
 * 创建对应的解码器实例。
 *
 * 当前支持的格式：
 * - AudioFormatType::kMp3 → Mp3Decoder（基于 esp_audio_codec）
 *
 * @version 1.0.0
 * @date 2026-03-09
 */

// 内部私有头文件（在 PRIV_INCLUDE_DIRS "src/audio" 中）
#include "audio_decoder.h"
#include "mp3_decoder.h"

#include <new>
#include "esp_log.h"

static const char* TAG = "AudioDecoder";

namespace ai_sdk {

IAudioDecoder* CreateAudioDecoder(AudioFormatType format)
{
    switch (format) {
        case AudioFormatType::kMp3:
            return new (std::nothrow) Mp3Decoder();

        default:
            ESP_LOGE(TAG, "Unsupported audio format: %d", (int)format);
            return nullptr;
    }
}

} // namespace ai_sdk
