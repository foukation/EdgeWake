/**
 * @file mp3_decoder.h
 * @brief MP3 音频解码器实现（内部头文件）
 *
 * 基于 esp_audio_codec 组件的 Simple Decoder API 实现 MP3 解码。
 * 使用 Simple Decoder 而非低级帧解码器，因为：
 * - Simple Decoder 内部处理 MP3 帧同步和解析
 * - 输入数据不需要帧对齐，可以传入任意大小的数据块
 * - 更适合 HTTP 流式下载场景（网络数据块大小不固定）
 *
 * 此文件为 SDK 内部头文件，不对外暴露。
 *
 * @version 1.0.0
 * @date 2026-03-09
 */

#ifndef AI_SDK_AUDIO_MP3_DECODER_H
#define AI_SDK_AUDIO_MP3_DECODER_H

// 内部私有头文件（在 PRIV_INCLUDE_DIRS "src/audio" 中）
#include "audio_decoder.h"

// esp_audio_codec Simple Decoder API
#include "simple_dec/esp_audio_simple_dec.h"

namespace ai_sdk {

/**
 * @brief MP3 音频解码器
 *
 * 实现 IAudioDecoder 接口，将 MP3 压缩数据解码为 PCM。
 *
 * 内部使用 esp_audio_simple_dec API：
 * - esp_audio_simple_dec_open()    → Open()
 * - esp_audio_simple_dec_process() → Decode()
 * - esp_audio_simple_dec_get_info() → GetInfo()
 * - esp_audio_simple_dec_close()   → Close()
 *
 * 典型使用流程：
 *   Mp3Decoder dec;
 *   dec.Open();
 *   while (有数据) {
 *       auto result = dec.Decode(in, in_size, &consumed, out, out_cap, &out_size);
 *       // 处理 PCM 输出...
 *   }
 *   dec.Close();
 *
 * 内存占用：
 * - MP3 解码器内部缓冲约 20-30 KB（由 esp_audio_codec 管理）
 * - Mp3Decoder 对象本身只持有句柄和状态，开销极小
 */
class Mp3Decoder : public IAudioDecoder {
public:
    Mp3Decoder();
    ~Mp3Decoder() override;

    /// @brief 打开 MP3 解码器，初始化 Simple Decoder 句柄
    bool Open() override;

    /**
     * @brief 解码 MP3 数据
     *
     * 将输入的 MP3 压缩数据解码为 PCM。
     * 输入数据不需要帧对齐，Simple Decoder 内部自动处理帧同步。
     *
     * @param[in]     in_data      MP3 压缩数据
     * @param[in]     in_size      输入数据大小（字节）
     * @param[out]    consumed     实际消费的字节数
     * @param[out]    out_data     PCM 输出缓冲区
     * @param[in]     out_capacity 输出缓冲区容量（字节）
     * @param[out]    out_size     实际输出的 PCM 字节数
     * @return DecodeResult 解码结果
     *
     * 注意：
     * - out_capacity 建议 >= 2304 字节（MP3 单帧最大 PCM 输出：
     *   1152 samples × 2 channels × 16 bit = 4608 字节；
     *   单声道最大 2304 字节）
     * - 如果 out_capacity 不足，返回 kError，
     *   可通过 esp_audio_simple_dec_out_t::needed_size 得知所需大小
     */
    DecodeResult Decode(
        const uint8_t* in_data, size_t in_size, size_t* consumed,
        uint8_t* out_data, size_t out_capacity, size_t* out_size) override;

    /// @brief 获取解码后的音频信息（采样率、声道数、位深等）
    bool GetInfo(AudioDecodeInfo* info) const override;

    /// @brief 关闭解码器，释放所有资源
    void Close() override;

    // 禁止拷贝和赋值
    Mp3Decoder(const Mp3Decoder&) = delete;
    Mp3Decoder& operator=(const Mp3Decoder&) = delete;

private:
    /// Simple Decoder 句柄（esp_audio_codec 内部管理）
    esp_audio_simple_dec_handle_t handle_;

    /// 是否已获取到音频信息（第一次解码成功后为 true）
    bool info_available_;

    /// 缓存的音频信息，避免每次 GetInfo 都查询底层
    AudioDecodeInfo cached_info_;
};

} // namespace ai_sdk

#endif // AI_SDK_AUDIO_MP3_DECODER_H
