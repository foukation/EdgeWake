/**
 * @file es8311_audio_codec.h
 * @brief ES8311 音频编解码器驱动
 *
 * ES8311 是一款低功耗、高性能的音频编解码芯片，支持 ADC 和 DAC。
 * 适用于通用开发板，是最常用的音频芯片之一。
 *
 * 特点：
 * - 单编解码器，同时支持录音和播放
 * - I2C 控制接口
 * - I2S 数据接口
 * - 内置功放控制 (PA)
 *
 * 移植自 ESP32-RTOS-AI-SDK 项目。
 *
 * @version 1.0.0
 * @date 2026-03-05
 */

#ifndef AI_SDK_ES8311_AUDIO_CODEC_H
#define AI_SDK_ES8311_AUDIO_CODEC_H

#include "ai_sdk/audio/audio_codec.h"

#include <driver/i2c_master.h>
#include <driver/gpio.h>
#include <esp_codec_dev.h>
#include <esp_codec_dev_defaults.h>
#include <mutex>

namespace ai_sdk {

/**
 * @brief ES8311 音频编解码器类
 *
 * 封装 esp_codec_dev 库对 ES8311 芯片的操作，提供统一的 AudioCodec 接口。
 */
class Es8311AudioCodec : public AudioCodec {
private:
    // esp_codec_dev 接口句柄
    const audio_codec_data_if_t* data_if_ = nullptr;   ///< I2S 数据接口
    const audio_codec_ctrl_if_t* ctrl_if_ = nullptr;   ///< I2C 控制接口
    const audio_codec_if_t* codec_if_ = nullptr;       ///< 编解码器接口
    const audio_codec_gpio_if_t* gpio_if_ = nullptr;   ///< GPIO 接口

    esp_codec_dev_handle_t dev_ = nullptr;  ///< 设备句柄

    gpio_num_t pa_pin_ = GPIO_NUM_NC;  ///< 功放控制引脚
    bool pa_inverted_ = false;         ///< 功放引脚是否反转

    std::mutex data_if_mutex_;  ///< 数据接口互斥锁

    /**
     * @brief 创建双工 I2S 通道
     */
    void CreateDuplexChannels(gpio_num_t mclk, gpio_num_t bclk, gpio_num_t ws,
                               gpio_num_t dout, gpio_num_t din);

    /**
     * @brief 更新设备状态
     *
     * 根据输入/输出使能状态，打开或关闭设备
     */
    void UpdateDeviceState();

    /**
     * @brief 读取音频数据
     */
    virtual int Read(int16_t* dest, int samples) override;

    /**
     * @brief 写入音频数据
     */
    virtual int Write(const int16_t* data, int samples) override;

public:
    /**
     * @brief 构造函数
     *
     * @param i2c_master_handle I2C 主机句柄
     * @param i2c_port I2C 端口号
     * @param input_sample_rate 输入采样率 (Hz)
     * @param output_sample_rate 输出采样率 (Hz)
     * @param mclk MCLK 引脚
     * @param bclk BCLK 引脚
     * @param ws WS (LRCK) 引脚
     * @param dout DOUT 引脚（数据输出到 DAC）
     * @param din DIN 引脚（数据来自 ADC）
     * @param pa_pin 功放控制引脚 (GPIO_NUM_NC 表示不使用)
     * @param es8311_addr ES8311 I2C 地址 (通常为 0x18)
     * @param use_mclk 是否使用 MCLK
     * @param pa_inverted 功放引脚是否反转（高电平关闭）
     */
    Es8311AudioCodec(void* i2c_master_handle, i2c_port_t i2c_port,
                      int input_sample_rate, int output_sample_rate,
                      gpio_num_t mclk, gpio_num_t bclk, gpio_num_t ws,
                      gpio_num_t dout, gpio_num_t din,
                      gpio_num_t pa_pin, uint8_t es8311_addr,
                      bool use_mclk = true, bool pa_inverted = false);

    virtual ~Es8311AudioCodec();

    virtual void SetOutputVolume(int volume) override;
    virtual void EnableInput(bool enable) override;
    virtual void EnableOutput(bool enable) override;
};

} // namespace ai_sdk

#endif // AI_SDK_ES8311_AUDIO_CODEC_H
