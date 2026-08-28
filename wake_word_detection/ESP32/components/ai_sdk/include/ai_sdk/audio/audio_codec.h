/**
 * @file audio_codec.h
 * @brief 音频编解码器抽象基类
 *
 * 该文件定义了 AudioCodec 基类，为不同音频芯片提供统一的接口抽象。
 * 所有音频驱动实现（ES8311、ES8388、NoCodec 等）都继承自此类。
 *
 * 移植自 ESP32-RTOS-AI-SDK 项目，移除了对 board.h 和 settings.h 的依赖。
 *
 * @version 1.0.0
 * @date 2026-03-05
 */

#ifndef AI_SDK_AUDIO_CODEC_H
#define AI_SDK_AUDIO_CODEC_H

#include <freertos/FreeRTOS.h>
#include <freertos/event_groups.h>
#include <driver/i2s_std.h>

#include <vector>
#include <string>
#include <functional>

namespace ai_sdk {

/**
 * @brief DMA 描述符数量
 * 用于 I2S 通道配置
 */
#define AI_SDK_AUDIO_CODEC_DMA_DESC_NUM 6

/**
 * @brief DMA 帧数量
 * 用于 I2S 通道配置
 */
#define AI_SDK_AUDIO_CODEC_DMA_FRAME_NUM 240

/**
 * @brief 音频编解码器抽象基类
 *
 * 提供音频输入（麦克风）和输出（扬声器）的统一接口。
 * 子类需要实现 Read() 和 Write() 方法来完成实际的硬件操作。
 *
 * 支持的功能：
 * - 音量控制（输出）
 * - 增益控制（输入）
 * - 输入/输出独立使能
 * - 双工/单工模式
 * - 参考输入（用于回声消除）
 */
class AudioCodec {
public:
    AudioCodec();
    virtual ~AudioCodec();

    /**
     * @brief 设置输出音量
     * @param volume 音量值 (0-100)
     */
    virtual void SetOutputVolume(int volume);

    /**
     * @brief 设置输入增益
     * @param gain 增益值 (dB)
     */
    virtual void SetInputGain(float gain);

    /**
     * @brief 使能/禁用输入（麦克风）
     * @param enable true 使能，false 禁用
     */
    virtual void EnableInput(bool enable);

    /**
     * @brief 使能/禁用输出（扬声器）
     * @param enable true 使能，false 禁用
     */
    virtual void EnableOutput(bool enable);

    /**
     * @brief 输出音频数据
     * @param data PCM 数据 (16-bit)
     */
    virtual void OutputData(std::vector<int16_t>& data);

    /**
     * @brief 输入音频数据
     * @param data 用于存储读取的 PCM 数据
     * @return true 读取成功，false 读取失败
     */
    virtual bool InputData(std::vector<int16_t>& data);

    /**
     * @brief 启动音频编解码器
     *
     * 使能 I2S 通道，并打开输入/输出
     */
    virtual void Start();

    // ========== 属性访问器 ==========

    inline bool duplex() const { return duplex_; }
    inline bool input_reference() const { return input_reference_; }
    inline int input_sample_rate() const { return input_sample_rate_; }
    inline int output_sample_rate() const { return output_sample_rate_; }
    inline int input_channels() const { return input_channels_; }
    inline int output_channels() const { return output_channels_; }
    inline int output_volume() const { return output_volume_; }
    inline float input_gain() const { return input_gain_; }
    inline bool input_enabled() const { return input_enabled_; }
    inline bool output_enabled() const { return output_enabled_; }

protected:
    // I2S 通道句柄
    i2s_chan_handle_t tx_handle_ = nullptr;  ///< TX 通道（输出）
    i2s_chan_handle_t rx_handle_ = nullptr;  ///< RX 通道（输入）

    // 状态标志
    bool duplex_ = false;           ///< 是否双工模式
    bool input_reference_ = false;  ///< 是否使用参考输入（AEC）
    bool input_enabled_ = false;    ///< 输入是否使能
    bool output_enabled_ = false;   ///< 输出是否使能

    // 音频参数
    int input_sample_rate_ = 0;   ///< 输入采样率 (Hz)
    int output_sample_rate_ = 0;  ///< 输出采样率 (Hz)
    int input_channels_ = 1;      ///< 输入通道数
    int output_channels_ = 1;     ///< 输出通道数
    int output_volume_ = 20;      ///< 输出音量 (0-100)，默认 20
    float input_gain_ = 0.0;      ///< 输入增益 (dB)

    /**
     * @brief 读取音频数据（纯虚函数，子类必须实现）
     * @param dest 目标缓冲区
     * @param samples 采样点数量
     * @return 实际读取的采样点数量
     */
    virtual int Read(int16_t* dest, int samples) = 0;

    /**
     * @brief 写入音频数据（纯虚函数，子类必须实现）
     * @param data 源数据
     * @param samples 采样点数量
     * @return 实际写入的采样点数量
     */
    virtual int Write(const int16_t* data, int samples) = 0;
};

} // namespace ai_sdk

#endif // AI_SDK_AUDIO_CODEC_H
