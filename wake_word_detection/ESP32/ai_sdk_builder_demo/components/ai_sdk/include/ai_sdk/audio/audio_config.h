/**
 * @file audio_config.h
 * @brief 音频硬件配置与工厂函数
 *
 * 定义音频硬件类型枚举、音频配置结构体以及编解码器工厂函数。
 * SDK 用户通过填写 AudioConfig 结构体并调用 CreateAudioCodec() 工厂函数，
 * 即可创建与硬件匹配的音频驱动实例，无需关心底层驱动类的差异。
 *
 * 使用示例：
 * @code
 * ai_sdk::AudioConfig config;
 * config.hardware_type = ai_sdk::AudioHardwareType::kEs8311;
 * config.input_sample_rate = 16000;
 * config.output_sample_rate = 16000;
 * config.i2c_master_handle = i2c_handle;
 * config.i2c_port = I2C_NUM_0;
 * config.mclk = GPIO_NUM_2;
 * config.bclk = GPIO_NUM_17;
 * config.ws   = GPIO_NUM_47;
 * config.dout = GPIO_NUM_15;
 * config.din  = GPIO_NUM_16;
 * config.pa_pin = GPIO_NUM_46;
 * config.codec_addr = 0x18;  // 7-bit I2C address (matches datasheet)
 *
 * ai_sdk::AudioCodec* codec = ai_sdk::CreateAudioCodec(config);
 * codec->Start();
 * @endcode
 *
 * @version 1.0.0
 * @date 2026-03-09
 */

#ifndef AI_SDK_AUDIO_CONFIG_H
#define AI_SDK_AUDIO_CONFIG_H

#include <driver/gpio.h>
#include <driver/i2c_types.h>
#include <driver/i2s_std.h>

namespace ai_sdk {

// 前向声明
class AudioCodec;

/**
 * @brief 音频硬件类型枚举
 *
 * 定义 ai-sdk 支持的所有音频硬件驱动类型。
 * 用于在 AudioConfig 中指定要使用的音频驱动。
 */
enum class AudioHardwareType {
    /// ES8311 单芯片 Codec（最常用，覆盖 50%+ 开发板）
    /// 支持 ADC + DAC，I2C 控制，I2S 数据，内置功放控制
    kEs8311,

    /// ES8388 单芯片 Codec（老款开发板）
    /// 支持 ADC + DAC，创建独立的输入/输出设备
    kEs8388,

    /// ES8374 单芯片 Codec
    /// 支持 ADC + DAC，用于特定开发板
    kEs8374,

    /// ES8389 单芯片 Codec
    /// 支持 ADC + DAC，用于特定开发板
    kEs8389,

    /// ESP-BOX 系列双芯片组合（ES8311 DAC + ES7210 ADC）
    /// ES8311 负责播放，ES7210 负责 4 通道麦克风录音，使用 TDM 模式
    kBoxAudioCodec,

    /// I2S 双工直连（无编解码芯片）
    /// 使用同一组 I2S 引脚 (BCLK/WS/DOUT/DIN) 进行录音和播放
    /// 适用于 INMP441 麦克风 + MAX98357 功放等方案
    kNoCodecDuplex,

    /// I2S 单工直连（无编解码芯片）
    /// 麦克风和扬声器使用不同的 I2S 端口和引脚
    kNoCodecSimplex,

    /// PDM 麦克风 + I2S 播放（无编解码芯片）
    /// 录音使用 PDM 模式，播放使用标准 I2S 模式
    kNoCodecSimplexPdm,

    /// 空实现驱动（用于无硬件时的开发调试）
    /// Read/Write 不做任何操作，让上层逻辑可以在无硬件环境下运行测试
    kDummy,
};

/**
 * @brief 音频硬件配置结构体
 *
 * 包含创建音频驱动所需的所有配置参数。
 * 不同的硬件类型使用不同的字段子集，未使用的字段保持默认值即可。
 *
 * 各硬件类型使用的字段：
 *
 * | 类型               | I2C | 双工引脚 | 单工引脚 | codec_addr | es8311/es7210_addr | PA | 特殊标志          |
 * |--------------------|-----|----------|----------|------------|--------------------|----|-------------------|
 * | kEs8311            | Yes | Yes      | -        | Yes        | -                  | Yes| use_mclk, pa_inverted |
 * | kEs8388            | Yes | Yes      | -        | Yes        | -                  | Yes| input_reference   |
 * | kEs8374            | Yes | Yes      | -        | Yes        | -                  | Yes| use_mclk          |
 * | kEs8389            | Yes | Yes      | -        | Yes        | -                  | Yes| use_mclk          |
 * | kBoxAudioCodec     | Yes*| Yes      | -        | -          | Yes                | Yes| input_reference   |
 * | kNoCodecDuplex     | -   | Yes**    | -        | -          | -                  | -  | -                 |
 * | kNoCodecSimplex    | -   | -        | Yes      | -          | -                  | -  | slot_mask (可选)  |
 * | kNoCodecSimplexPdm | -   | -        | Yes***   | -          | -                  | -  | spk_slot_mask (可选)|
 * | kDummy             | -   | -        | -        | -          | -                  | -  | -                 |
 *
 * (*) Box 方案不使用 i2c_port 字段
 * (**) NoCodecDuplex 不使用 mclk 字段
 * (***) NoCodecSimplexPdm 不使用 mic_ws 字段
 */
struct AudioConfig {
    // ==================== 必填项 ====================

    /// 硬件类型（必填）
    AudioHardwareType hardware_type;

    /// 输入（麦克风）采样率，单位 Hz，常用值: 16000, 24000
    int input_sample_rate = 16000;

    /// 输出（扬声器）采样率，单位 Hz，常用值: 16000, 24000
    int output_sample_rate = 16000;

    // ==================== I2C 配置 ====================
    // 有芯片方案（kEs8311 / kEs8388 / kEs8374 / kEs8389 / kBoxAudioCodec）需要填写

    /// I2C 主机句柄，通过 i2c_new_master_bus() 创建
    void* i2c_master_handle = nullptr;

    /// I2C 端口号（kBoxAudioCodec 不使用此字段）
    i2c_port_t i2c_port = I2C_NUM_0;

    // ==================== I2S 引脚 - 双工模式 ====================
    // 有芯片方案 + kNoCodecDuplex 使用这组引脚

    /// 主时钟引脚，部分芯片不需要时设为 GPIO_NUM_NC
    gpio_num_t mclk = GPIO_NUM_NC;

    /// 位时钟引脚
    gpio_num_t bclk = GPIO_NUM_NC;

    /// 字选择（左右声道切换）引脚
    gpio_num_t ws = GPIO_NUM_NC;

    /// 数据输出引脚（ESP32 → 扬声器/DAC）
    gpio_num_t dout = GPIO_NUM_NC;

    /// 数据输入引脚（麦克风/ADC → ESP32）
    gpio_num_t din = GPIO_NUM_NC;

    // ==================== I2S 引脚 - 单工模式 ====================
    // kNoCodecSimplex / kNoCodecSimplexPdm 使用这组引脚

    /// 扬声器位时钟引脚
    gpio_num_t spk_bclk = GPIO_NUM_NC;

    /// 扬声器字选择引脚
    gpio_num_t spk_ws = GPIO_NUM_NC;

    /// 扬声器数据输出引脚
    gpio_num_t spk_dout = GPIO_NUM_NC;

    /// 麦克风时钟引脚
    gpio_num_t mic_sck = GPIO_NUM_NC;

    /// 麦克风字选择引脚（kNoCodecSimplexPdm 不使用此字段）
    gpio_num_t mic_ws = GPIO_NUM_NC;

    /// 麦克风数据输入引脚
    gpio_num_t mic_din = GPIO_NUM_NC;

    // ==================== 编解码器 I2C 地址 ====================
    // 所有地址均使用 I2C 行业标准 7-bit 格式（与芯片手册一致）。
    // SDK 内部负责转换为底层驱动所需的 8-bit 格式，调用方无需关心。

    /// 单芯片方案的 I2C 地址（kEs8311 / kEs8388 / kEs8374 / kEs8389 使用）
    /// ES8311 默认 0x18, ES8388/ES8374/ES8389 默认 0x10
    uint8_t codec_addr = 0x18;

    /// Box 方案: ES8311 (DAC) 的 I2C 地址（7-bit，默认 0x18）
    uint8_t es8311_addr = 0x18;

    /// Box 方案: ES7210 (ADC) 的 I2C 地址（7-bit，默认 0x40）
    uint8_t es7210_addr = 0x40;

    // ==================== 功放控制 ====================

    /// 功放使能引脚，GPIO_NUM_NC 表示不使用
    gpio_num_t pa_pin = GPIO_NUM_NC;

    /// 功放引脚逻辑是否反转（true 表示低电平使能），仅 kEs8311 使用
    bool pa_inverted = false;

    // ==================== 可选参数 ====================

    /// 是否使用 MCLK（kEs8311 / kEs8374 / kEs8389 使用，默认 true）
    bool use_mclk = true;

    /// 是否使用参考通道用于回声消除（kEs8388 / kBoxAudioCodec 使用，默认 false）
    bool input_reference = false;

    // ==================== Slot Mask 配置 ====================
    // kNoCodecSimplex / kNoCodecSimplexPdm 在需要自定义声道映射时使用

    /// 扬声器声道掩码
    i2s_std_slot_mask_t spk_slot_mask = I2S_STD_SLOT_BOTH;

    /// 麦克风声道掩码（仅 kNoCodecSimplex 使用）
    i2s_std_slot_mask_t mic_slot_mask = I2S_STD_SLOT_BOTH;

    /// 是否启用自定义 slot mask（默认 false，使用默认的 I2S_STD_SLOT_BOTH）
    bool use_slot_mask = false;
};

/**
 * @brief 创建音频编解码器实例
 *
 * 根据 AudioConfig 中指定的硬件类型，创建对应的 AudioCodec 子类实例。
 * 调用方负责管理返回指针的生命周期（使用完毕后 delete）。
 *
 * @param config 音频硬件配置
 * @return 音频编解码器实例指针，创建失败时返回 nullptr
 *
 * @note 对于有芯片方案，需要确保 i2c_master_handle 已通过 i2c_new_master_bus() 创建
 * @note 调用 CreateAudioCodec() 后需要调用 codec->Start() 启动音频设备
 */
AudioCodec* CreateAudioCodec(const AudioConfig& config);

} // namespace ai_sdk

#endif // AI_SDK_AUDIO_CONFIG_H
