/**
 * @file box_audio_codec.h
 * @brief ESP-BOX 音频编解码器驱动 (ES8311 + ES7210)
 *
 * ESP-BOX 系列开发板使用 ES8311 作为 DAC（播放），ES7210 作为 ADC（录音）。
 * ES7210 是一款 4 通道 ADC，支持麦克风阵列。
 *
 * 移植自 ESP32-RTOS-AI-SDK 项目。
 *
 * @version 1.0.0
 * @date 2026-03-05
 */

#ifndef AI_SDK_BOX_AUDIO_CODEC_H
#define AI_SDK_BOX_AUDIO_CODEC_H

#include "ai_sdk/audio/audio_codec.h"

#include <driver/gpio.h>
#include <esp_codec_dev.h>
#include <esp_codec_dev_defaults.h>
#include <mutex>

namespace ai_sdk {

/**
 * @brief ESP-BOX 音频编解码器类
 *
 * 组合 ES8311 (DAC) 和 ES7210 (ADC) 提供完整的音频输入输出功能。
 * ES8311 用于播放，ES7210 用于 4 通道麦克风录音。
 */
class BoxAudioCodec : public AudioCodec {
private:
    // esp_codec_dev 接口句柄
    const audio_codec_data_if_t* data_if_ = nullptr;
    const audio_codec_ctrl_if_t* out_ctrl_if_ = nullptr;   ///< ES8311 I2C 控制
    const audio_codec_if_t* out_codec_if_ = nullptr;       ///< ES8311 编解码器
    const audio_codec_ctrl_if_t* in_ctrl_if_ = nullptr;    ///< ES7210 I2C 控制
    const audio_codec_if_t* in_codec_if_ = nullptr;        ///< ES7210 编解码器
    const audio_codec_gpio_if_t* gpio_if_ = nullptr;

    esp_codec_dev_handle_t output_dev_ = nullptr;  ///< 输出设备 (ES8311)
    esp_codec_dev_handle_t input_dev_ = nullptr;   ///< 输入设备 (ES7210)

    std::mutex data_if_mutex_;

    /**
     * @brief 创建双工 I2S 通道
     *
     * TX 使用标准模式（播放），RX 使用 TDM 模式（4 通道录音）
     */
    void CreateDuplexChannels(gpio_num_t mclk, gpio_num_t bclk, gpio_num_t ws,
                               gpio_num_t dout, gpio_num_t din);

    virtual int Read(int16_t* dest, int samples) override;
    virtual int Write(const int16_t* data, int samples) override;

public:
    /**
     * @brief 构造函数
     *
     * @param i2c_master_handle I2C 主机句柄
     * @param input_sample_rate 输入采样率 (Hz)
     * @param output_sample_rate 输出采样率 (Hz)
     * @param mclk MCLK 引脚
     * @param bclk BCLK 引脚
     * @param ws WS (LRCK) 引脚
     * @param dout DOUT 引脚（数据输出到 ES8311 DAC）
     * @param din DIN 引脚（数据来自 ES7210 ADC）
     * @param pa_pin 功放控制引脚
     * @param es8311_addr ES8311 I2C 地址
     * @param es7210_addr ES7210 I2C 地址
     * @param input_reference 是否使用参考输入（用于 AEC）
     */
    BoxAudioCodec(void* i2c_master_handle,
                   int input_sample_rate, int output_sample_rate,
                   gpio_num_t mclk, gpio_num_t bclk, gpio_num_t ws,
                   gpio_num_t dout, gpio_num_t din,
                   gpio_num_t pa_pin, uint8_t es8311_addr, uint8_t es7210_addr,
                   bool input_reference);
    virtual ~BoxAudioCodec();

    virtual void SetOutputVolume(int volume) override;
    virtual void EnableInput(bool enable) override;
    virtual void EnableOutput(bool enable) override;
};

} // namespace ai_sdk

#endif // AI_SDK_BOX_AUDIO_CODEC_H
