/**
 * @file no_audio_codec.h
 * @brief 无芯片音频驱动
 *
 * 提供三种无芯片方案：
 * - NoAudioCodecDuplex: I2S 双工模式（同一组引脚用于输入和输出）
 * - NoAudioCodecSimplex: I2S 单工模式（分离的输入输出引脚）
 * - NoAudioCodecSimplexPdm: I2S 播放 + PDM 麦克风
 *
 * 移植自 ESP32-RTOS-AI-SDK 项目。
 *
 * @version 1.0.0
 * @date 2026-03-05
 */

#ifndef AI_SDK_NO_AUDIO_CODEC_H
#define AI_SDK_NO_AUDIO_CODEC_H

#include "ai_sdk/audio/audio_codec.h"

#include <driver/gpio.h>
#include <driver/i2s_pdm.h>
#include <mutex>

namespace ai_sdk {

/**
 * @brief 无芯片音频驱动基类
 *
 * 直接使用 I2S 接口，不依赖音频编解码芯片。
 * 通过软件实现音量控制和增益控制。
 */
class NoAudioCodec : public AudioCodec {
protected:
    std::mutex data_if_mutex_;

    virtual int Write(const int16_t* data, int samples) override;
    virtual int Read(int16_t* dest, int samples) override;

public:
    virtual ~NoAudioCodec();
};

/**
 * @brief I2S 双工模式驱动
 *
 * 使用同一组 I2S 引脚 (BCLK, WS, DOUT  DIN) 进行输入和输出。
 * 适用于简单的音频板，无需编解码芯片。
 */
class NoAudioCodecDuplex : public NoAudioCodec {
public:
    /**
     * @brief 构造函数
     *
     * @param input_sample_rate 输入采样率
     * @param output_sample_rate 输出采样率
     * @param bclk BCLK 引脚
     * @param ws WS (LRCK) 引脚
     * @param dout DOUT 引脚（数据输出到扬声器）
     * @param din DIN 引脚（数据来自麦克风）
     */
    NoAudioCodecDuplex(int input_sample_rate, int output_sample_rate,
                       gpio_num_t bclk, gpio_num_t ws,
                       gpio_num_t dout, gpio_num_t din);
};

/**
 * @brief I2S 单工模式驱动
 *
 * 使用分离的 I2S 端口进行输入和输出。
 * 适用于需要独立录音和播放通道的场景。
 */
class NoAudioCodecSimplex : public NoAudioCodec {
public:
    /**
     * @brief 构造函数
     *
     * @param input_sample_rate 输入采样率
     * @param output_sample_rate 输出采样率
     * @param spk_bclk 扬声器 BCLK 引脚
     * @param spk_ws 扬声器 WS 引脚
     * @param spk_dout 扬声器 DOUT 引脚
     * @param mic_sck 麦克风 SCK 引脚
     * @param mic_ws 麦克风 WS 引脚
     * @param mic_din 麦克风 DIN 引脚
     */
    NoAudioCodecSimplex(int input_sample_rate, int output_sample_rate,
                        gpio_num_t spk_bclk, gpio_num_t spk_ws, gpio_num_t spk_dout,
                        gpio_num_t mic_sck, gpio_num_t mic_ws, gpio_num_t mic_din);

    /**
     * @brief 构造函数（支持 slot mask 配置）
     */
    NoAudioCodecSimplex(int input_sample_rate, int output_sample_rate,
                        gpio_num_t spk_bclk, gpio_num_t spk_ws, gpio_num_t spk_dout,
                        i2s_std_slot_mask_t spk_slot_mask,
                        gpio_num_t mic_sck, gpio_num_t mic_ws, gpio_num_t din,
                        i2s_std_slot_mask_t mic_slot_mask);
};

/**
 * @brief I2S 播放 + PDM 麦克风驱动
 *
 * 使用标准 I2S 进行播放，使用 PDM 模式进行录音。
 * PDM 麦克风直接输出数字信号，无需额外 ADC。
 */
class NoAudioCodecSimplexPdm : public NoAudioCodec {
public:
    /**
     * @brief 构造函数（使用默认 slot mask）
     */
    NoAudioCodecSimplexPdm(int input_sample_rate, int output_sample_rate,
                          gpio_num_t spk_bclk, gpio_num_t spk_ws, gpio_num_t spk_dout,
                          gpio_num_t mic_sck, gpio_num_t mic_din);

    /**
     * @brief 构造函数（支持 slot mask 配置）
     */
    NoAudioCodecSimplexPdm(int input_sample_rate, int output_sample_rate,
                          gpio_num_t spk_bclk, gpio_num_t spk_ws, gpio_num_t spk_dout,
                          i2s_std_slot_mask_t spk_slot_mask,
                          gpio_num_t mic_sck, gpio_num_t mic_din);

    virtual int Read(int16_t* dest, int samples) override;
};

} // namespace ai_sdk

#endif // AI_SDK_NO_AUDIO_CODEC_H
