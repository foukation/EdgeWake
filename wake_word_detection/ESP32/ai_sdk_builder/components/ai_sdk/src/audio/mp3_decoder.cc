/**
 * @file mp3_decoder.cc
 * @brief MP3 音频解码器实现
 *
 * 基于 esp_audio_codec 组件的 Simple Decoder API 实现 IAudioDecoder 接口。
 *
 * Simple Decoder 使用流程：
 * 1. esp_audio_simple_dec_open()    分配解码器资源
 * 2. esp_audio_simple_dec_process() 输入 MP3 数据，输出 PCM
 *    - 内部自动处理 MP3 帧同步、ID3 标签跳过
 *    - 输入不需要帧对齐，可以传入任意大小的数据块
 *    - consumed 字段返回实际消费的字节数
 * 3. esp_audio_simple_dec_get_info() 获取音频参数（首次解码成功后可用）
 * 4. esp_audio_simple_dec_close()   释放资源
 *
 * 错误码映射（esp_audio_err_t → DecodeResult）：
 * - ESP_AUDIO_ERR_OK              → kOk（解码成功）
 * - ESP_AUDIO_ERR_BUFF_NOT_ENOUGH → kNeedMore（输入不足，需要更多数据）
 * - 其他                          → kError（解码错误）
 *
 * @version 1.0.0
 * @date 2026-03-09
 */

#include "mp3_decoder.h"

#include <cstring>
#include "esp_log.h"

// MP3 解码器注册（需要先注册才能使用 Simple Decoder）
#include "decoder/impl/esp_mp3_dec.h"

static const char* TAG = "Mp3Decoder";

namespace ai_sdk {

// ============================================================================
// Mp3Decoder 构造与析构
// ============================================================================

Mp3Decoder::Mp3Decoder()
    : handle_(nullptr)
    , info_available_(false)
    , cached_info_{}
{
}

Mp3Decoder::~Mp3Decoder()
{
    // 确保资源被释放
    Close();
}

// ============================================================================
// Open - 打开 MP3 解码器
// ============================================================================

bool Mp3Decoder::Open()
{
    // 防止重复打开
    if (handle_ != nullptr) {
        ESP_LOGW(TAG, "Decoder already opened, closing first");
        Close();
    }

    // 注册 MP3 解码器到 Simple Decoder 框架
    // 每次调用都是安全的，内部会检查是否已注册
    esp_audio_err_t ret = esp_mp3_dec_register();
    if (ret != ESP_AUDIO_ERR_OK) {
        ESP_LOGE(TAG, "Failed to register MP3 decoder: %d", (int)ret);
        return false;
    }

    // 配置 Simple Decoder
    // use_frame_dec = false: 使用解析器模式，输入不需要帧对齐
    // dec_cfg = nullptr:     MP3 解码器不需要额外配置
    esp_audio_simple_dec_cfg_t cfg = {};
    cfg.dec_type = ESP_AUDIO_SIMPLE_DEC_TYPE_MP3;
    cfg.dec_cfg = nullptr;
    cfg.cfg_size = 0;
    cfg.use_frame_dec = false;  // 使用解析器模式，支持任意大小输入

    // 打开解码器
    ret = esp_audio_simple_dec_open(&cfg, &handle_);
    if (ret != ESP_AUDIO_ERR_OK || handle_ == nullptr) {
        ESP_LOGE(TAG, "Failed to open simple decoder: %d", (int)ret);
        handle_ = nullptr;
        return false;
    }

    // 重置状态
    info_available_ = false;
    memset(&cached_info_, 0, sizeof(cached_info_));

    ESP_LOGI(TAG, "MP3 decoder opened successfully");
    return true;
}

// ============================================================================
// Decode - 解码 MP3 数据为 PCM
// ============================================================================

DecodeResult Mp3Decoder::Decode(
    const uint8_t* in_data, size_t in_size, size_t* consumed,
    uint8_t* out_data, size_t out_capacity, size_t* out_size)
{
    // 参数校验
    if (handle_ == nullptr) {
        ESP_LOGE(TAG, "Decoder not opened");
        return DecodeResult::kError;
    }
    if (consumed == nullptr || out_size == nullptr) {
        ESP_LOGE(TAG, "Output parameters cannot be null");
        return DecodeResult::kError;
    }

    // 初始化输出
    *consumed = 0;
    *out_size = 0;

    // 准备输入结构体
    // 注意：Simple Decoder API 使用非 const 指针，需要强制转换
    esp_audio_simple_dec_raw_t raw = {};
    raw.buffer = const_cast<uint8_t*>(in_data);
    raw.len = static_cast<uint32_t>(in_size);
    raw.eos = false;  // 流式解码，由调用者在最后一次传入时设置
    raw.consumed = 0;
    raw.frame_recover = ESP_AUDIO_SIMPLE_DEC_RECOVERY_NONE;

    // 准备输出结构体
    esp_audio_simple_dec_out_t frame = {};
    frame.buffer = out_data;
    frame.len = static_cast<uint32_t>(out_capacity);
    frame.needed_size = 0;
    frame.decoded_size = 0;

    // 执行解码
    esp_audio_err_t ret = esp_audio_simple_dec_process(handle_, &raw, &frame);

    // 返回实际消费的字节数（无论解码是否成功都要更新）
    *consumed = raw.consumed;

    // 根据返回值映射到 DecodeResult
    switch (ret) {
        case ESP_AUDIO_ERR_OK:
            // 解码成功，有 PCM 输出
            *out_size = frame.decoded_size;

            // 首次解码成功后，缓存音频信息
            if (!info_available_ && frame.decoded_size > 0) {
                esp_audio_simple_dec_info_t dec_info = {};
                esp_audio_err_t info_ret = esp_audio_simple_dec_get_info(handle_, &dec_info);
                if (info_ret == ESP_AUDIO_ERR_OK && dec_info.sample_rate > 0) {
                    cached_info_.sample_rate = dec_info.sample_rate;
                    cached_info_.bits_per_sample = dec_info.bits_per_sample;
                    cached_info_.channels = dec_info.channel;
                    cached_info_.bitrate = dec_info.bitrate;
                    info_available_ = true;
                    ESP_LOGI(TAG, "Audio info: %d Hz, %d-bit, %d ch, %d bps",
                             (int)cached_info_.sample_rate,
                             (int)cached_info_.bits_per_sample,
                             (int)cached_info_.channels,
                             (int)cached_info_.bitrate);
                }
            }
            return DecodeResult::kOk;

        case ESP_AUDIO_ERR_BUFF_NOT_ENOUGH:
            // 输入数据不足，需要更多数据才能完成一帧解码
            // 或者输出缓冲区不够大
            if (frame.needed_size > 0 && frame.needed_size > out_capacity) {
                // 输出缓冲区不够大
                ESP_LOGW(TAG, "Output buffer too small: need %d, have %d",
                         (int)frame.needed_size, (int)out_capacity);
            }
            return DecodeResult::kNeedMore;

        default:
            // 其他错误
            ESP_LOGE(TAG, "Decode error: %d", (int)ret);
            return DecodeResult::kError;
    }
}

// ============================================================================
// GetInfo - 获取解码后的音频信息
// ============================================================================

bool Mp3Decoder::GetInfo(AudioDecodeInfo* info) const
{
    if (info == nullptr) {
        return false;
    }

    if (!info_available_) {
        return false;
    }

    *info = cached_info_;
    return true;
}

// ============================================================================
// Close - 关闭解码器
// ============================================================================

void Mp3Decoder::Close()
{
    if (handle_ != nullptr) {
        esp_audio_simple_dec_close(handle_);
        handle_ = nullptr;
        ESP_LOGI(TAG, "MP3 decoder closed");
    }

    info_available_ = false;
    memset(&cached_info_, 0, sizeof(cached_info_));
}

} // namespace ai_sdk
