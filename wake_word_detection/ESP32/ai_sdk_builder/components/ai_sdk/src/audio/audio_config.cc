/**
 * @file audio_config.cc
 * @brief 音频编解码器工厂函数实现
 *
 * 本文件实现了 CreateAudioCodec() 工厂函数，根据 AudioConfig 配置结构体
 * 中指定的硬件类型，创建对应的 AudioCodec 驱动实例。
 *
 * 工厂模式的核心价值：
 *   厂商只需要填写一个 AudioConfig 结构体，不需要知道具体的驱动类名称，
 *   工厂函数会根据 hardware_type 自动选择正确的驱动实现。
 *
 * 支持的硬件类型（9 种）：
 *   - 有芯片方案（4 种）：ES8311、ES8388、ES8374、ES8389
 *   - 组合芯片方案（1 种）：BoxAudioCodec (ES8311 DAC + ES7210 ADC)
 *   - 无芯片方案（3 种）：NoCodecDuplex、NoCodecSimplex、NoCodecSimplexPdm
 *   - 调试用（1 种）：DummyAudioCodec
 *
 * @version 1.0.0
 * @date 2026-03-09
 */

#include "ai_sdk/audio/audio_config.h"
#include "ai_sdk/audio/audio_codec.h"

// ============================================================================
// 引入所有编解码器驱动头文件（内部头文件，位于 src/audio/codecs/）
// 这些头文件不对外暴露，厂商通过 audio_config.h 的工厂函数间接使用
// ============================================================================
#include "es8311_audio_codec.h"   // ES8311 单芯片 Codec（最常用，50%+ 开发板）
#include "es8388_audio_codec.h"   // ES8388 单芯片 Codec（老款开发板）
#include "es8374_audio_codec.h"   // ES8374 单芯片 Codec（特定开发板）
#include "es8389_audio_codec.h"   // ES8389 单芯片 Codec（特定开发板）
#include "box_audio_codec.h"      // ESP-BOX 组合方案（ES8311 DAC + ES7210 4通道 ADC）
#include "no_audio_codec.h"       // 无芯片方案（I2S 直连：Duplex / Simplex / PDM）
#include "dummy_audio_codec.h"    // 空实现（无硬件调试用）

#include <esp_log.h>

static const char* TAG = "AudioConfig";

namespace ai_sdk {

/**
 * @brief 创建音频编解码器实例（工厂函数）
 *
 * 根据 config.hardware_type 选择对应的驱动子类，使用 config 中的参数构造实例。
 * 不同硬件类型使用 AudioConfig 中不同的字段子集（见 audio_config.h 中的表格）。
 *
 * @param config 音频硬件配置结构体（由厂商填写）
 * @return 创建成功返回 AudioCodec* 指针，失败或未知类型返回 nullptr
 *
 * @note 调用方负责生命周期管理（delete）
 * @note 创建后需调用 codec->Start() 启动 I2S 通道
 */
AudioCodec* CreateAudioCodec(const AudioConfig& config) {
    switch (config.hardware_type) {

        // ====================================================================
        // 有芯片方案：ES8311（最常用，覆盖 50%+ 开发板）
        // ====================================================================
        // 特点：单芯片集成 ADC + DAC，全双工，I2C 控制 + I2S 数据
        // 要求：input_sample_rate == output_sample_rate（芯片限制）
        // 需要：I2C 总线句柄 + I2S 引脚(mclk/bclk/ws/dout/din) + 芯片地址
        // ====================================================================
        case AudioHardwareType::kEs8311:
            return new Es8311AudioCodec(
                config.i2c_master_handle,    // I2C 主机总线句柄（由 i2c_new_master_bus 创建）
                config.i2c_port,             // I2C 端口号（通常 I2C_NUM_0）
                config.input_sample_rate,    // 麦克风采样率（如 16000 或 24000）
                config.output_sample_rate,   // 扬声器采样率（必须等于输入采样率）
                config.mclk, config.bclk, config.ws,   // I2S 时钟引脚
                config.dout, config.din,     // I2S 数据引脚（输出/输入）
                config.pa_pin,               // 功放使能引脚（GPIO_NUM_NC 表示不使用）
                config.codec_addr,           // ES8311 I2C 地址（默认 0x18）
                config.use_mclk,             // 是否使用 MCLK（部分板子省略 MCLK）
                config.pa_inverted           // 功放引脚逻辑是否反转
            );

        // ====================================================================
        // 有芯片方案：ES8388（老款 Codec）
        // ====================================================================
        // 特点：创建独立的输入设备(input_dev_)和输出设备(output_dev_)
        // 与 ES8311 的区别：支持 input_reference（参考通道，用于 AEC 回声消除）
        // ====================================================================
        case AudioHardwareType::kEs8388:
            return new Es8388AudioCodec(
                config.i2c_master_handle,
                config.i2c_port,
                config.input_sample_rate,
                config.output_sample_rate,
                config.mclk, config.bclk, config.ws,
                config.dout, config.din,
                config.pa_pin,
                config.codec_addr,           // ES8388 I2C 地址
                config.input_reference       // 是否启用参考通道（AEC 用）
            );

        // ====================================================================
        // 有芯片方案：ES8374
        // ====================================================================
        // 特点：与 ES8311 类似，单芯片 ADC + DAC
        // 与 ES8311 的区别：支持 use_mclk 但不支持 pa_inverted
        // ====================================================================
        case AudioHardwareType::kEs8374:
            return new Es8374AudioCodec(
                config.i2c_master_handle,
                config.i2c_port,
                config.input_sample_rate,
                config.output_sample_rate,
                config.mclk, config.bclk, config.ws,
                config.dout, config.din,
                config.pa_pin,
                config.codec_addr,           // ES8374 I2C 地址
                config.use_mclk              // 是否使用 MCLK
            );

        // ====================================================================
        // 有芯片方案：ES8389
        // ====================================================================
        // 特点：与 ES8374 参数完全一致
        // ====================================================================
        case AudioHardwareType::kEs8389:
            return new Es8389AudioCodec(
                config.i2c_master_handle,
                config.i2c_port,
                config.input_sample_rate,
                config.output_sample_rate,
                config.mclk, config.bclk, config.ws,
                config.dout, config.din,
                config.pa_pin,
                config.codec_addr,           // ES8389 I2C 地址
                config.use_mclk              // 是否使用 MCLK
            );

        // ====================================================================
        // 组合芯片方案：BoxAudioCodec（ES8311 DAC + ES7210 4通道 ADC）
        // ====================================================================
        // 特点：ES8311 只做播放（DAC 模式），ES7210 做 4 通道麦克风录音（TDM 模式）
        // 适用于 ESP-BOX、ESP-BOX-3 等带麦克风阵列的开发板
        // 注意：使用 es8311_addr/es7210_addr 而非 codec_addr
        // 注意：不使用 i2c_port 字段（内部通过 i2c_master_handle 直接操作）
        // ====================================================================
        case AudioHardwareType::kBoxAudioCodec:
            return new BoxAudioCodec(
                config.i2c_master_handle,    // I2C 主机总线句柄
                config.input_sample_rate,    // 录音采样率
                config.output_sample_rate,   // 播放采样率
                config.mclk, config.bclk, config.ws,   // I2S 引脚
                config.dout, config.din,
                config.pa_pin,               // 功放使能引脚
                config.es8311_addr,          // ES8311 (DAC) 的 I2C 地址（默认 0x18）
                config.es7210_addr,          // ES7210 (ADC) 的 I2C 地址（默认 0x40）
                config.input_reference       // 是否启用参考通道
            );

        // ====================================================================
        // 无芯片方案：I2S 双工直连（NoCodecDuplex）
        // ====================================================================
        // 特点：麦克风和扬声器共用同一组 I2S 引脚（bclk/ws/dout/din）
        // 不需要 I2C 控制，不需要 MCLK
        // 数据格式：32-bit（需要内部做位移转换）
        // 音量控制：软件计算（无硬件音量寄存器）
        // ====================================================================
        case AudioHardwareType::kNoCodecDuplex:
            return new NoAudioCodecDuplex(
                config.input_sample_rate,    // 麦克风采样率
                config.output_sample_rate,   // 扬声器采样率
                config.bclk, config.ws,      // 共用时钟引脚
                config.dout, config.din      // 数据引脚（输出/输入）
            );

        // ====================================================================
        // 无芯片方案：I2S 单工直连（NoCodecSimplex）
        // ====================================================================
        // 特点：麦克风和扬声器使用独立的 I2S 端口和引脚
        // 适用于 INMP441 麦克风 + MAX98357 功放等典型组合
        // 使用单工引脚组：spk_bclk/spk_ws/spk_dout + mic_sck/mic_ws/mic_din
        // 可选：use_slot_mask=true 时自定义声道映射（左声道/右声道/双声道）
        // ====================================================================
        case AudioHardwareType::kNoCodecSimplex:
            if (config.use_slot_mask) {
                // 带自定义声道掩码的版本（如只需要左声道）
                return new NoAudioCodecSimplex(
                    config.input_sample_rate,
                    config.output_sample_rate,
                    config.spk_bclk, config.spk_ws, config.spk_dout,  // 扬声器 I2S
                    config.spk_slot_mask,      // 扬声器声道掩码
                    config.mic_sck, config.mic_ws, config.mic_din,     // 麦克风 I2S
                    config.mic_slot_mask       // 麦克风声道掩码
                );
            } else {
                // 默认版本（双声道，I2S_STD_SLOT_BOTH）
                return new NoAudioCodecSimplex(
                    config.input_sample_rate,
                    config.output_sample_rate,
                    config.spk_bclk, config.spk_ws, config.spk_dout,  // 扬声器 I2S
                    config.mic_sck, config.mic_ws, config.mic_din      // 麦克风 I2S
                );
            }

        // ====================================================================
        // 无芯片方案：PDM 麦克风 + I2S 播放（NoCodecSimplexPdm）
        // ====================================================================
        // 特点：录音使用 PDM 模式（数字麦克风），播放使用标准 I2S 模式
        // PDM 麦克风输出 16-bit 数据，无需位移转换
        // 注意：不使用 mic_ws 字段（PDM 只需要 CLK + DATA）
        // ====================================================================
        case AudioHardwareType::kNoCodecSimplexPdm:
            if (config.use_slot_mask) {
                // 带自定义扬声器声道掩码的版本
                return new NoAudioCodecSimplexPdm(
                    config.input_sample_rate,
                    config.output_sample_rate,
                    config.spk_bclk, config.spk_ws, config.spk_dout,  // 扬声器 I2S
                    config.spk_slot_mask,      // 扬声器声道掩码
                    config.mic_sck, config.mic_din  // PDM 麦克风（CLK + DATA，无 WS）
                );
            } else {
                // 默认版本
                return new NoAudioCodecSimplexPdm(
                    config.input_sample_rate,
                    config.output_sample_rate,
                    config.spk_bclk, config.spk_ws, config.spk_dout,  // 扬声器 I2S
                    config.mic_sck, config.mic_din  // PDM 麦克风（CLK + DATA）
                );
            }

        // ====================================================================
        // 空实现驱动：DummyAudioCodec
        // ====================================================================
        // 特点：Read/Write 不做任何操作
        // 用途：在无硬件环境下进行开发调试，让上层逻辑可以正常运行
        // ====================================================================
        case AudioHardwareType::kDummy:
            return new DummyAudioCodec(
                config.input_sample_rate,    // 采样率（仅用于属性查询）
                config.output_sample_rate
            );

        // ====================================================================
        // 未知类型：返回 nullptr
        // ====================================================================
        default:
            ESP_LOGE(TAG, "Unknown audio hardware type: %d",
                     static_cast<int>(config.hardware_type));
            return nullptr;
    }
}

} // namespace ai_sdk
