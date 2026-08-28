/**
 * @file dummy_audio_codec.h
 * @brief 空实现音频驱动
 *
 * 用于测试目的，不进行实际的音频输入输出。
 *
 * 移植自 ESP32-RTOS-AI-SDK 项目。
 *
 * @version 1.0.0
 * @date 2026-03-05
 */

#ifndef AI_SDK_DUMMY_AUDIO_CODEC_H
#define AI_SDK_DUMMY_AUDIO_CODEC_H

#include "ai_sdk/audio/audio_codec.h"

namespace ai_sdk {

/**
 * @brief 空实现音频编解码器
 *
 * 所有读写操作都返回 0，不进行实际的硬件操作。
 * 适用于测试和开发阶段。
 */
class DummyAudioCodec : public AudioCodec {
private:
    virtual int Read(int16_t* dest, int samples) override;
    virtual int Write(const int16_t* data, int samples) override;

public:
    /**
     * @brief 构造函数
     *
     * @param input_sample_rate 输入采样率
     * @param output_sample_rate 输出采样率
     */
    DummyAudioCodec(int input_sample_rate, int output_sample_rate);
    virtual ~DummyAudioCodec();
};

} // namespace ai_sdk

#endif // AI_SDK_DUMMY_AUDIO_CODEC_H
