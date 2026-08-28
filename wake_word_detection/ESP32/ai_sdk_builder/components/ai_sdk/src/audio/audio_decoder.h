/**
 * @file audio_decoder.h
 * @brief 音频解码器抽象接口（SDK 内部头文件）
 *
 * ============================================================================
 * ⚠️ 内部头文件 - 请勿在 SDK 外部使用
 * ============================================================================
 *
 * 此文件是 AI SDK 的内部实现细节，不属于公开 API。
 * 上层厂商代码不应直接引用此头文件。
 * TTS 播放器通过此接口解码服务器返回的 MP3 音频数据。
 *
 * ============================================================================
 *
 * 定义音频解码器的统一接口，用于将压缩音频数据（如 MP3）解码为 PCM。
 *
 * 设计说明：
 * - 流式解码：输入数据不需要帧对齐，解码器内部处理帧解析
 * - 输出格式由解码后的音频内容决定（采样率、声道数等）
 * - 通过 GetInfo() 获取解码后的音频参数
 * - 使用工厂函数 CreateAudioDecoder() 创建具体实现
 *
 * @version 1.0.0
 * @date 2026-03-09
 */

#ifndef AI_SDK_AUDIO_DECODER_H
#define AI_SDK_AUDIO_DECODER_H

#include <cstdint>
#include <cstddef>

namespace ai_sdk {

/**
 * @brief 解码后的音频信息
 *
 * 在第一次成功解码后可用，描述解码输出的 PCM 格式。
 */
struct AudioDecodeInfo {
    uint32_t sample_rate = 0;      ///< 采样率，如 44100、48000
    uint8_t  bits_per_sample = 16; ///< 位深，通常 16
    uint8_t  channels = 0;         ///< 声道数，1=Mono, 2=Stereo
    uint32_t bitrate = 0;          ///< 比特率 (bps)
};

/**
 * @brief 解码结果枚举
 */
enum class DecodeResult {
    kOk,              ///< 解码成功，有 PCM 输出
    kNeedMore,        ///< 输入数据不足，需要更多数据
    kError,           ///< 解码错误
};

/**
 * @brief 音频解码器抽象接口
 *
 * 所有解码器实现（MP3、Opus 等）都继承此接口。
 * 使用流式解码模式：
 * 1. 调用 Open() 初始化
 * 2. 循环调用 Decode() 输入压缩数据，获取 PCM 输出
 * 3. 调用 GetInfo() 获取解码后的音频参数
 * 4. 调用 Close() 释放资源
 */
class IAudioDecoder {
public:
    virtual ~IAudioDecoder() = default;

    /**
     * @brief 打开解码器
     *
     * 初始化解码器内部状态，分配必要的资源。
     *
     * @return true 初始化成功，false 失败
     */
    virtual bool Open() = 0;

    /**
     * @brief 解码音频数据
     *
     * 输入压缩数据，输出解码后的 PCM 数据。
     * 输入数据不需要帧对齐，解码器内部处理帧解析。
     *
     * @param[in]     in_data      输入压缩数据指针
     * @param[in]     in_size      输入数据大小（字节）
     * @param[out]    consumed     实际消费的输入字节数
     * @param[out]    out_data     输出 PCM 数据缓冲区
     * @param[in]     out_capacity 输出缓冲区容量（字节）
     * @param[out]    out_size     实际输出的 PCM 字节数
     * @return DecodeResult 解码结果
     *
     * 使用模式：
     * - 返回 kOk：out_data 中有 out_size 字节的 PCM 数据
     * - 返回 kNeedMore：输入数据不足，需要继续 Feed 更多数据
     * - 返回 kError：解码错误，应调用 Close() 重置
     */
    virtual DecodeResult Decode(
        const uint8_t* in_data, size_t in_size, size_t* consumed,
        uint8_t* out_data, size_t out_capacity, size_t* out_size) = 0;

    /**
     * @brief 获取解码后的音频信息
     *
     * 在第一次成功解码后调用，获取 PCM 输出的格式参数。
     *
     * @param[out] info 解码信息
     * @return true 信息可用，false 尚未解码
     */
    virtual bool GetInfo(AudioDecodeInfo* info) const = 0;

    /**
     * @brief 关闭解码器，释放所有资源
     */
    virtual void Close() = 0;
};

/**
 * @brief 音频格式类型
 */
enum class AudioFormatType {
    kMp3,   ///< MP3 格式
};

/**
 * @brief 创建音频解码器（工厂函数）
 *
 * 根据音频格式创建对应的解码器实例。
 * 调用者拥有返回对象的所有权，需要在使用完毕后 delete。
 *
 * @param format 音频格式类型
 * @return 解码器实例指针，失败返回 nullptr
 */
IAudioDecoder* CreateAudioDecoder(AudioFormatType format);

} // namespace ai_sdk

#endif // AI_SDK_AUDIO_DECODER_H
