/**
 * @file tts_player.cc
 * @brief TTS 语音播放模块实现
 *
 * 实现 TtsPlayer 类的 PIMPL 内部逻辑，包括：
 * - FreeRTOS 播放任务（单任务串行处理整条流水线）
 * - URL 播放队列（支持多段 TTS 顺序播放，不丢失中间片段）
 * - HTTP 流式下载（使用 esp_http_client，分块接收）
 * - MP3 解码（使用 IAudioDecoder / Mp3Decoder）
 * - 重采样（MP3 采样率 → 硬件输出采样率，内嵌在流水线中）
 * - PCM 输出（通过 AudioCodec::OutputData() 送达扬声器）
 *
 * 流水线架构：
 *
 *   HTTP 下载 → 环形缓冲区 → MP3 解码 → 重采样 → AudioCodec 播放
 *
 *   具体流程（同一任务串行执行）：
 *   1. esp_http_client_open() 建立连接
 *   2. 循环：
 *      a. esp_http_client_read() 读取一块 MP3 数据
 *      b. IAudioDecoder::Decode() 解码为 PCM
 *      c. 如果需要重采样：esp_ae_rate_cvt_process()
 *      d. AudioCodec::OutputData() 输出到扬声器
 *   3. esp_http_client_close() 关闭连接
 *
 * 内存预算（典型场景）：
 * - 播放任务栈: 4096 bytes
 * - HTTP 接收缓冲区: 2048 bytes
 * - MP3 解码输出缓冲区: 4608 bytes（2304 samples × 2 bytes，MP3 最大帧）
 * - 重采样输出缓冲区（按需）: ~4096 bytes
 * - MP3 解码器内部: ~20-30 KB（由 esp_audio_codec 管理）
 * - 总计: ~35-40 KB（含 MP3 解码器）
 *
 * @version 1.0.0
 * @date 2026-03-10
 */

// 内部私有头文件（在 PRIV_INCLUDE_DIRS "src/audio" 中）
#include "tts_player.h"
#include "audio_decoder.h"
// 公开头文件（在 INCLUDE_DIRS "include" 中）
#include "ai_sdk/audio/audio_codec.h"

#include <freertos/FreeRTOS.h>
#include <freertos/task.h>
#include <freertos/semphr.h>
#include <esp_log.h>
#include <esp_http_client.h>
#include <esp_crt_bundle.h>
#include <esp_ae_rate_cvt.h>

#include <vector>
#include <queue>
#include <mutex>
#include <cstring>
#include <atomic>
#include <string>
#include <algorithm>

static const char* TAG = "TtsPlayer";

namespace ai_sdk {

// =============================================================================
// 常量定义
// =============================================================================

/// HTTP 接收缓冲区大小：2048 字节
/// 平衡网络吞吐量和内存占用
static constexpr size_t kHttpBufferSize = 2048;

/// MP3 解码输出缓冲区大小：4608 字节
/// MP3 帧最大输出 1152 samples × 2 channels × 2 bytes = 4608 bytes
static constexpr size_t kDecodeOutBufferSize = 4608;

/// 播放任务栈大小：8192 字节
/// HTTPS（TLS 握手）即使启用 MBEDTLS_DYNAMIC_BUFFER 仍需 4-6KB 栈空间。
/// 4096 会导致栈溢出 → 破坏 LWIP select_cb_list → Guru Meditation 崩溃。
static constexpr uint32_t kTaskStackSize = 8192;

/// 播放任务优先级：5（中等优先级）
/// 低于录音任务（优先级 8），但高于后台任务
static constexpr UBaseType_t kTaskPriority = 5;

/// HTTP 超时时间：15 秒
/// 包含连接超时和读取超时
static constexpr int kHttpTimeoutMs = 15000;

// PCM 输出帧时长（毫秒）。
static constexpr int kOutputFrameMs = 20;
// 启动播放前的预缓冲时长（毫秒）。
static constexpr int kPcmPrebufferMs = 900;
// PCM 环形缓冲区大小（字节）。
static constexpr size_t kPcmRingBufferBytes = 96 * 1024;

// PCM 输出帧时长（毫秒）。
// 启动播放前的预缓冲时长（毫秒）。
// PCM 环形缓冲区大小（字节）。

/// 重采样配置宏（与主项目 audio_service.cc 一致）
#define RATE_CVT_CFG(_src_rate, _dest_rate, _channel)        \
    (esp_ae_rate_cvt_cfg_t)                                  \
    {                                                        \
        .src_rate        = (uint32_t)(_src_rate),            \
        .dest_rate       = (uint32_t)(_dest_rate),           \
        .channel         = (uint8_t)(_channel),              \
        .bits_per_sample = 16,                               \
        .complexity      = 2,                                \
        .perf_type       = ESP_AE_RATE_CVT_PERF_TYPE_SPEED,  \
    }

// =============================================================================
// TtsPlayer::Impl 实现类
// =============================================================================

class TtsPlayer::Impl {
public:
    Impl() = default;

    ~Impl() {
        Stop();
    }

    // =========================================================================
    // 公开方法
    // =========================================================================

    bool Initialize(AudioCodec* codec) {
        if (!codec) {
            ESP_LOGE(TAG, "Initialize failed: codec is null");
            return false;
        }

        codec_ = codec;
        hw_sample_rate_ = codec->output_sample_rate();
        hw_channels_ = codec->output_channels();

        ESP_LOGI(TAG, "Initialized: hw_sample_rate=%d, hw_channels=%d",
                 hw_sample_rate_, hw_channels_);

        initialized_ = true;
        return true;
    }

    bool Play(const std::string& url) {
        if (!initialized_) {
            ESP_LOGE(TAG, "Play failed: not initialized");
            return false;
        }
        if (url.empty()) {
            ESP_LOGE(TAG, "Play failed: URL is empty");
            return false;
        }

        // 如果当前正在播放，将新 URL 入队等待
        // 云端对长文本会发送多个 Speak 指令，每个包含独立 MP3 URL，
        // 入队保证所有片段按顺序完整播放，不丢失中间片段。
        if (task_handle_) {
            std::lock_guard<std::mutex> lock(queue_mutex_);
            url_queue_.push(url);
            // newlib nano printf 在部分配置下对 %zu 支持不稳定，使用 %u + 显式转换避免日志崩溃。
            ESP_LOGI(TAG, "Play queued (queue_size=%u): %s",
                     static_cast<unsigned>(url_queue_.size()), url.c_str());
            return true;
        }

        // 当前空闲，直接开始播放
        return PlayInternal(url);
    }

    void Stop() {
        StopInternal();
    }

    TtsPlayState GetState() const {
        return state_;
    }

    bool IsPlaying() const {
        return state_ == TtsPlayState::kPlaying;
    }

    void SetStateCallback(TtsPlayer::StateCallback callback) {
        state_callback_ = std::move(callback);
    }

    void SetCompletionCallback(TtsPlayer::CompletionCallback callback) {
        completion_callback_ = std::move(callback);
    }

    void SetVolume(int volume) {
        if (codec_) {
            codec_->SetOutputVolume(volume);
        }
    }

private:
    // =========================================================================
    // 内部播放启动（创建 FreeRTOS 任务）
    // =========================================================================

    /**
     * @brief 立即启动播放指定 URL
     *
     * 设置 current_url_、重置状态标志、创建 FreeRTOS 播放任务。
     * 调用前必须确保当前没有正在运行的播放任务（task_handle_ == nullptr）。
     *
     * @param url 要播放的 MP3 URL
     * @return true 任务创建成功，false 失败
     */
    bool PlayInternal(const std::string& url) {
        // 保存播放 URL
        current_url_ = url;

        // 重置状态
        should_stop_ = false;
        play_completed_ = false;

        // 创建播放任务
        BaseType_t ret = xTaskCreate(
            TaskEntry,       // 任务入口函数
            "ai_tts_play",   // 任务名称
            kTaskStackSize,  // 栈大小
            this,            // 任务参数
            kTaskPriority,   // 优先级
            &task_handle_    // 任务句柄
        );

        if (ret != pdPASS) {
            ESP_LOGE(TAG, "PlayInternal failed: cannot create task (ret=%d)", ret);
            SetState(TtsPlayState::kError);
            return false;
        }

        ESP_LOGI(TAG, "Play started: %s", url.c_str());
        return true;
    }

    // =========================================================================
    // 停止播放（内部方法）
    // =========================================================================

    void StopInternal() {
        if (!task_handle_ && !output_task_handle_) {
            return;
        }

        // 先清空队列，防止任务在 should_stop_ 之前取到新 URL
        {
            std::lock_guard<std::mutex> lock(queue_mutex_);
            std::queue<std::string>().swap(url_queue_);
        }

        // 设置停止标志
        should_stop_ = true;

        // 等待任务结束
        for (int i = 0; i < 50; i++) {  // 最多等待 5 秒
            if (!task_handle_ && !output_task_handle_) {
                break;
            }
            vTaskDelay(pdMS_TO_TICKS(100));
        }

        // 如果任务还没退出，强制删除
        if (task_handle_) {
            vTaskDelete(task_handle_);
            task_handle_ = nullptr;
            ESP_LOGW(TAG, "Play task force deleted");
        }
        if (output_task_handle_) {
            vTaskDelete(output_task_handle_);
            output_task_handle_ = nullptr;
            ESP_LOGW(TAG, "Output task force deleted");
        }

        // 清理重采样器
        CleanupResampler();

        SetState(TtsPlayState::kIdle);

        ESP_LOGI(TAG, "Play stopped");
    }

    // =========================================================================
    // 状态管理
    // =========================================================================

    void SetState(TtsPlayState new_state) {
        if (state_ != new_state) {
            state_ = new_state;
            if (state_callback_) {
                state_callback_(new_state);
            }
        }
    }

    // =========================================================================
    // 重采样器管理
    // =========================================================================

    /**
     * @brief 初始化重采样器
     *
     * 当 MP3 解码后的采样率与硬件输出采样率不同时调用。
     * 参考主项目 audio_service.cc:476-489 的实现。
     *
     * @param src_rate MP3 解码输出的采样率
     * @param channels 声道数
     * @return true 初始化成功，false 失败
     */
    bool InitResampler(int src_rate, int channels) {
        // 先清理已有的重采样器
        CleanupResampler();

        esp_ae_rate_cvt_cfg_t cfg = RATE_CVT_CFG(src_rate, hw_sample_rate_, channels);
        esp_ae_err_t err = esp_ae_rate_cvt_open(&cfg, &resampler_);
        if (err != ESP_AE_ERR_OK || !resampler_) {
            ESP_LOGE(TAG, "Failed to create resampler: %d", err);
            return false;
        }

        resampler_src_rate_ = src_rate;
        resampler_channels_ = channels;

        ESP_LOGI(TAG, "Resampler created: %d -> %d Hz, %d ch",
                 src_rate, hw_sample_rate_, channels);
        return true;
    }

    void CleanupResampler() {
        if (resampler_) {
            esp_ae_rate_cvt_close(resampler_);
            resampler_ = nullptr;
        }
        resampler_src_rate_ = 0;
        resampler_channels_ = 0;
    }

    /**
     * @brief 对 PCM 数据执行重采样
     *
     * 参考主项目 audio_service.cc:378-386 的实现。
     *
     * @param pcm 输入/输出 PCM 数据，重采样后的数据会替换原数据
     * @return true 重采样成功，false 失败
     */
    bool ResamplePcm(std::vector<int16_t>& pcm) {
        if (!resampler_ || pcm.empty()) {
            return false;
        }

        uint32_t in_samples = pcm.size() / resampler_channels_;
        uint32_t max_out_samples = 0;
        esp_ae_rate_cvt_get_max_out_sample_num(resampler_, in_samples, &max_out_samples);

        resample_temp_.resize(max_out_samples * resampler_channels_);
        uint32_t actual_out = max_out_samples;

        esp_ae_err_t err = esp_ae_rate_cvt_process(
            resampler_,
            (esp_ae_sample_t)pcm.data(), in_samples,
            (esp_ae_sample_t)resample_temp_.data(), &actual_out
        );

        if (err != ESP_AE_ERR_OK) {
            ESP_LOGW(TAG, "Resample failed: %d", err);
            return false;
        }

        resample_temp_.resize(actual_out * resampler_channels_);
        pcm.assign(resample_temp_.begin(), resample_temp_.end());
        return true;
    }

    // PCM 环形缓冲：减少 producer/consumer 间拷贝和突发抖动影响。
    void ResetPcmRingBuffer() {
        std::lock_guard<std::mutex> lock(pcm_ring_mutex_);
        pcm_ring_.assign(kPcmRingBufferBytes, 0);
        pcm_ring_read_pos_ = 0;
        pcm_ring_write_pos_ = 0;
        pcm_ring_used_bytes_ = 0;
    }

    size_t GetPcmRingUsedBytes() {
        std::lock_guard<std::mutex> lock(pcm_ring_mutex_);
        return pcm_ring_used_bytes_;
    }

    bool PushPcmToRing(const int16_t* samples, size_t sample_count) {
        const uint8_t* src = reinterpret_cast<const uint8_t*>(samples);
        size_t total_bytes = sample_count * sizeof(int16_t);
        size_t offset = 0;

        while (offset < total_bytes && !should_stop_) {
            size_t wrote = 0;
            {
                std::lock_guard<std::mutex> lock(pcm_ring_mutex_);
                size_t free_bytes = pcm_ring_.size() - pcm_ring_used_bytes_;
                if (free_bytes > 0) {
                    size_t chunk = std::min(free_bytes, total_bytes - offset);
                    size_t first = std::min(chunk, pcm_ring_.size() - pcm_ring_write_pos_);
                    std::memcpy(&pcm_ring_[pcm_ring_write_pos_], src + offset, first);
                    pcm_ring_write_pos_ = (pcm_ring_write_pos_ + first) % pcm_ring_.size();
                    pcm_ring_used_bytes_ += first;
                    offset += first;
                    wrote += first;

                    size_t remain = chunk - first;
                    if (remain > 0) {
                        std::memcpy(&pcm_ring_[pcm_ring_write_pos_], src + offset, remain);
                        pcm_ring_write_pos_ = (pcm_ring_write_pos_ + remain) % pcm_ring_.size();
                        pcm_ring_used_bytes_ += remain;
                        offset += remain;
                        wrote += remain;
                    }
                }
            }
            if (wrote == 0) {
                vTaskDelay(pdMS_TO_TICKS(2));
            }
        }

        return (offset == total_bytes) && !should_stop_;
    }

    size_t PopPcmFromRing(uint8_t* dst, size_t bytes) {
        std::lock_guard<std::mutex> lock(pcm_ring_mutex_);
        if (pcm_ring_used_bytes_ == 0) {
            return 0;
        }

        size_t read_bytes = std::min(bytes, pcm_ring_used_bytes_);
        size_t first = std::min(read_bytes, pcm_ring_.size() - pcm_ring_read_pos_);
        std::memcpy(dst, &pcm_ring_[pcm_ring_read_pos_], first);
        pcm_ring_read_pos_ = (pcm_ring_read_pos_ + first) % pcm_ring_.size();
        pcm_ring_used_bytes_ -= first;

        size_t remain = read_bytes - first;
        if (remain > 0) {
            std::memcpy(dst + first, &pcm_ring_[pcm_ring_read_pos_], remain);
            pcm_ring_read_pos_ = (pcm_ring_read_pos_ + remain) % pcm_ring_.size();
            pcm_ring_used_bytes_ -= remain;
        }

        return read_bytes;
    }

    static void OutputTaskEntry(void* arg) {
        auto* self = static_cast<Impl*>(arg);
        self->OutputTask();
        self->output_task_handle_ = nullptr;
        vTaskDelete(nullptr);
    }

    bool StartOutputTask() {
        if (output_task_handle_) {
            return true;
        }

        BaseType_t ret = xTaskCreate(
            OutputTaskEntry,
            "ai_tts_out",
            4096,
            this,
            kTaskPriority + 1,
            &output_task_handle_);
        if (ret != pdPASS) {
            output_task_handle_ = nullptr;
            ESP_LOGE(TAG, "Failed to create output task");
            return false;
        }
        return true;
    }

    // 先预缓冲，再按固定帧长输出，降低网络抖动导致的卡顿。
    void OutputTask() {
        const size_t frame_bytes = static_cast<size_t>(
            (hw_sample_rate_ * hw_channels_ * sizeof(int16_t) * kOutputFrameMs) / 1000);
        const size_t prebuffer_bytes = static_cast<size_t>(
            (hw_sample_rate_ * hw_channels_ * sizeof(int16_t) * kPcmPrebufferMs) / 1000);
        TickType_t frame_ticks = pdMS_TO_TICKS(kOutputFrameMs);
        if (frame_ticks == 0) {
            frame_ticks = 1;
        }
        const TickType_t wait_prebuffer_ticks =
            (pdMS_TO_TICKS(5) == 0) ? 1 : pdMS_TO_TICKS(5);
        const TickType_t wait_short_ticks =
            (pdMS_TO_TICKS(2) == 0) ? 1 : pdMS_TO_TICKS(2);
        TickType_t last_wake_tick = xTaskGetTickCount();

        std::vector<uint8_t> byte_frame(frame_bytes);
        std::vector<int16_t> pcm_frame(frame_bytes / sizeof(int16_t));
        bool prebuffer_ready = false;

        while (!should_stop_) {
            size_t used = GetPcmRingUsedBytes();
            if (!prebuffer_ready) {
                if (used >= prebuffer_bytes || (producer_done_ && used > 0)) {
                    prebuffer_ready = true;
                } else if (producer_done_ && used == 0) {
                    break;
                } else {
                    vTaskDelay(wait_prebuffer_ticks);
                    continue;
                }
            }

            size_t request_bytes = frame_bytes;
            if (used < frame_bytes) {
                if (!producer_done_) {
                    vTaskDelay(wait_short_ticks);
                    continue;
                }
                if (used == 0) {
                    break;
                }
                request_bytes = used;
            }

            size_t got = PopPcmFromRing(byte_frame.data(), request_bytes);
            if (got == 0) {
                if (producer_done_) {
                    break;
                }
                vTaskDelay(wait_short_ticks);
                continue;
            }

            size_t samples = got / sizeof(int16_t);
            pcm_frame.resize(samples);
            std::memcpy(pcm_frame.data(), byte_frame.data(), samples * sizeof(int16_t));
            codec_->OutputData(pcm_frame);
            vTaskDelayUntil(&last_wake_tick, frame_ticks);
        }
    }

    // =========================================================================
    // FreeRTOS 播放任务
    // =========================================================================

    static void TaskEntry(void* arg) {
        auto* self = static_cast<Impl*>(arg);

        // 播放当前 URL 及队列中的所有后续 URL
        // 云端对长文本拆分为多个 Speak 指令，任务在同一上下文中依次播放，
        // 避免频繁创建/销毁 FreeRTOS 任务。
        do {
            self->PlayTask();

            // 被主动停止时，不再处理队列（StopInternal 已清空队列）
            if (self->should_stop_) {
                break;
            }

            // 检查队列中是否有下一条 URL
            std::string next_url;
            {
                std::lock_guard<std::mutex> lock(self->queue_mutex_);
                if (self->url_queue_.empty()) {
                    break;
                }
                next_url = std::move(self->url_queue_.front());
                self->url_queue_.pop();
            }

            // 准备播放下一条
            self->current_url_ = std::move(next_url);
            self->should_stop_ = false;
            self->play_completed_ = false;

            ESP_LOGI(TAG, "Playing next queued URL: %s", self->current_url_.c_str());

        } while (true);

        // 任务正常退出时，清除句柄
        self->task_handle_ = nullptr;
        vTaskDelete(nullptr);
    }

    /**
     * @brief 播放任务主循环
     *
     * 完整的播放流水线：
     * 1. 初始化 MP3 解码器
     * 2. 建立 HTTP 连接
     * 3. 循环：读取 HTTP 数据 → 解码 → 重采样 → 播放
     * 4. 清理资源
     */
    void PlayTask() {
        SetState(TtsPlayState::kPlaying);

        bool success = false;

        // 第 1 步：创建 MP3 解码器
        IAudioDecoder* decoder = CreateAudioDecoder(AudioFormatType::kMp3);
        if (!decoder) {
            ESP_LOGE(TAG, "Failed to create MP3 decoder");
            SetState(TtsPlayState::kError);
            NotifyCompletion(false);
            return;
        }

        if (!decoder->Open()) {
            ESP_LOGE(TAG, "Failed to open MP3 decoder");
            delete decoder;
            SetState(TtsPlayState::kError);
            NotifyCompletion(false);
            return;
        }

        // 第 2 步：建立 HTTP 连接
        esp_http_client_config_t http_config = {};
        http_config.url = current_url_.c_str();
        http_config.timeout_ms = kHttpTimeoutMs;
        http_config.buffer_size = kHttpBufferSize;
        http_config.crt_bundle_attach = esp_crt_bundle_attach;

        esp_http_client_handle_t http_client = esp_http_client_init(&http_config);
        if (!http_client) {
            ESP_LOGE(TAG, "Failed to init HTTP client");
            decoder->Close();
            delete decoder;
            SetState(TtsPlayState::kError);
            NotifyCompletion(false);
            return;
        }

        // 打开 HTTP 连接（流式读取）
        esp_err_t err = esp_http_client_open(http_client, 0);
        if (err != ESP_OK) {
            ESP_LOGE(TAG, "Failed to open HTTP connection: %s", esp_err_to_name(err));
            esp_http_client_cleanup(http_client);
            decoder->Close();
            delete decoder;
            SetState(TtsPlayState::kError);
            NotifyCompletion(false);
            return;
        }

        // 获取 HTTP 内容长度（可能为 -1，表示分块传输）
        int content_length = esp_http_client_fetch_headers(http_client);
        int status_code = esp_http_client_get_status_code(http_client);
        ESP_LOGI(TAG, "HTTP connected: status=%d, content_length=%d", status_code, content_length);

        if (status_code != 200) {
            ESP_LOGE(TAG, "HTTP error: status=%d", status_code);
            esp_http_client_close(http_client);
            esp_http_client_cleanup(http_client);
            decoder->Close();
            delete decoder;
            SetState(TtsPlayState::kError);
            NotifyCompletion(false);
            return;
        }

        // 确保输出使能
        if (!codec_->output_enabled()) {
            codec_->EnableOutput(true);
        }

        // 第 3 步：启动输出线程（consumer），并初始化 PCM 环形缓冲。
        ResetPcmRingBuffer();
        producer_done_ = false;
        if (!StartOutputTask()) {
            esp_http_client_close(http_client);
            esp_http_client_cleanup(http_client);
            decoder->Close();
            delete decoder;
            CleanupResampler();
            SetState(TtsPlayState::kError);
            NotifyCompletion(false);
            return;
        }

        // 第 4 步：下载+解码+重采样（producer）。
        success = StreamDecodeAndPlay(http_client, decoder);
        producer_done_ = true;

        // 第 5 步：等待输出线程消耗完缓冲 PCM。
        for (int i = 0; i < 500 && output_task_handle_ != nullptr; ++i) {
            if (should_stop_) {
                break;
            }
            vTaskDelay(pdMS_TO_TICKS(10));
        }
        if (output_task_handle_ != nullptr) {
            success = false;
        }

        // 第 6 步：清理资源
        esp_http_client_close(http_client);
        esp_http_client_cleanup(http_client);
        decoder->Close();
        delete decoder;
        CleanupResampler();

        if (should_stop_) {
            // 被主动停止
            SetState(TtsPlayState::kIdle);
            NotifyCompletion(false);
        } else if (success) {
            // 正常播放完成
            SetState(TtsPlayState::kIdle);
            NotifyCompletion(true);
        } else {
            // 播放出错
            SetState(TtsPlayState::kError);
            NotifyCompletion(false);
        }
    }

    /**
     * @brief 流式解码播放核心循环
     *
     * 从 HTTP 连接读取 MP3 数据，解码为 PCM，重采样后输出到扬声器。
     *
     * @param http_client HTTP 客户端句柄
     * @param decoder     MP3 解码器
     * @return true 正常完成，false 出错
     */
    bool StreamDecodeAndPlay(esp_http_client_handle_t http_client, IAudioDecoder* decoder) {
        // 分配缓冲区
        std::vector<uint8_t> http_buffer(kHttpBufferSize);
        std::vector<uint8_t> decode_out_buffer(kDecodeOutBufferSize);

        // 未消费的 MP3 数据缓冲区
        std::vector<uint8_t> mp3_buffer;
        mp3_buffer.reserve(kHttpBufferSize * 4);
        size_t mp3_offset = 0;

        bool http_done = false;       // HTTP 数据是否已全部读取
        bool resampler_inited = false; // 重采样器是否已初始化
        bool need_resample = false;    // 是否需要重采样

        while (!should_stop_) {
            // 从 HTTP 读取数据（如果还有）
            if (!http_done) {
                int read_len = esp_http_client_read(
                    http_client, (char*)http_buffer.data(), http_buffer.size());

                if (read_len > 0) {
                    // 追加到 MP3 缓冲区
                    mp3_buffer.insert(mp3_buffer.end(),
                                     http_buffer.data(), http_buffer.data() + read_len);
                } else if (read_len == 0) {
                    // HTTP 数据读取完毕
                    http_done = true;
                    ESP_LOGI(TAG, "HTTP download complete");
                } else {
                    // 读取错误
                    ESP_LOGE(TAG, "HTTP read error: %d", read_len);
                    return false;
                }
            }

            // 如果没有数据可解码，退出
            if ((mp3_buffer.size() - mp3_offset) == 0 && http_done) {
                break;
            }

            // 如果缓冲区为空但 HTTP 未完成，继续读取
            if ((mp3_buffer.size() - mp3_offset) == 0) {
                continue;
            }

            // 解码 MP3 数据
            size_t consumed = 0;
            size_t out_size = 0;
            DecodeResult result = decoder->Decode(
                mp3_buffer.data() + mp3_offset, mp3_buffer.size() - mp3_offset, &consumed,
                decode_out_buffer.data(), decode_out_buffer.size(), &out_size
            );

            // 滑动消费窗口：降低频繁 erase 的内存搬运成本。
            if (consumed > 0) {
                mp3_offset += consumed;
                if (mp3_offset >= kHttpBufferSize &&
                    mp3_offset >= (mp3_buffer.size() / 2)) {
                    mp3_buffer.erase(mp3_buffer.begin(), mp3_buffer.begin() + mp3_offset);
                    mp3_offset = 0;
                }
            }

            switch (result) {
                case DecodeResult::kOk: {
                    // 解码成功，有 PCM 输出

                    // 首次解码成功后，初始化重采样器（如果需要）
                    if (!resampler_inited) {
                        resampler_inited = true;
                        AudioDecodeInfo info;
                        if (decoder->GetInfo(&info) && info.sample_rate > 0) {
                            if ((int)info.sample_rate != hw_sample_rate_) {
                                need_resample = true;
                                if (!InitResampler(info.sample_rate, info.channels)) {
                                    ESP_LOGE(TAG, "Failed to init resampler");
                                    return false;
                                }
                            }
                            ESP_LOGI(TAG, "MP3 info: %d Hz, %d ch, resample=%d",
                                     (int)info.sample_rate, (int)info.channels,
                                     need_resample);
                        }
                    }

                    // 将字节数组转换为 int16_t vector
                    size_t pcm_samples = out_size / sizeof(int16_t);
                    std::vector<int16_t> pcm(pcm_samples);
                    std::memcpy(pcm.data(), decode_out_buffer.data(), out_size);

                    // 重采样（如果需要）
                    if (need_resample && resampler_) {
                        if (!ResamplePcm(pcm)) {
                            ESP_LOGW(TAG, "Resample failed, skipping frame");
                            continue;
                        }
                    }

                    // 写入 PCM 环形缓冲，由输出线程稳定消费。
                    if (!PushPcmToRing(pcm.data(), pcm.size())) {
                        ESP_LOGW(TAG, "Push PCM to ring failed");
                        return false;
                    }
                    break;
                }

                case DecodeResult::kNeedMore:
                    // 需要更多数据
                    if (http_done && ((mp3_buffer.size() - mp3_offset) == 0)) {
                        // HTTP 已结束且没有更多数据，播放完成
                        ESP_LOGI(TAG, "All data decoded");
                        return true;
                    }
                    // 继续读取更多 HTTP 数据
                    break;

                case DecodeResult::kError:
                    ESP_LOGE(TAG, "Decode error");
                    return false;
            }
        }

        return !should_stop_;
    }

    // =========================================================================
    // 播放完成通知
    // =========================================================================

    void NotifyCompletion(bool completed) {
        play_completed_ = completed;
        bool all_done = false;
        {
            // Queue is the source of truth for pending Speak URLs.
            // If it is empty when current segment finishes, this callback marks batch completion.
            std::lock_guard<std::mutex> lock(queue_mutex_);
            all_done = url_queue_.empty();
        }
        if (completion_callback_) {
            completion_callback_(current_url_, completed, all_done);
        }
    }

    // =========================================================================
    // 成员变量
    // =========================================================================

    // 外部依赖（不拥有所有权）
    AudioCodec* codec_ = nullptr;

    // 初始化状态
    bool initialized_ = false;

    // 硬件参数
    int hw_sample_rate_ = 0;    ///< 硬件输出采样率
    int hw_channels_ = 1;       ///< 硬件输出声道数

    // FreeRTOS 任务
    TaskHandle_t task_handle_ = nullptr;
    TaskHandle_t output_task_handle_ = nullptr;
    std::atomic<bool> should_stop_{false};
    std::atomic<bool> producer_done_{false};

    // 播放状态
    std::atomic<TtsPlayState> state_{TtsPlayState::kIdle};
    bool play_completed_ = false;

    // 当前播放 URL
    std::string current_url_;

    // URL 播放队列
    // 云端对长文本会拆分为多个 Speak 指令，每个指令包含独立的 MP3 URL。
    // 当前正在播放时，后续 URL 入队等待；当前播放完成后自动出队播放下一条。
    std::queue<std::string> url_queue_;
    std::mutex queue_mutex_;  ///< 保护 url_queue_（Play 在调用者线程，NotifyCompletion 在播放任务线程）

    // 重采样器
    esp_ae_rate_cvt_handle_t resampler_ = nullptr;
    int resampler_src_rate_ = 0;   ///< 重采样器源采样率
    int resampler_channels_ = 0;   ///< 重采样器声道数
    std::vector<int16_t> resample_temp_;

    // PCM 环形缓冲（下载/解码写，播放线程读）
    std::vector<uint8_t> pcm_ring_;
    size_t pcm_ring_read_pos_ = 0;
    size_t pcm_ring_write_pos_ = 0;
    size_t pcm_ring_used_bytes_ = 0;
    std::mutex pcm_ring_mutex_;

    // 回调
    TtsPlayer::StateCallback state_callback_;
    TtsPlayer::CompletionCallback completion_callback_;
};

// =============================================================================
// TtsPlayer 公开方法（委托给 Impl）
// =============================================================================

TtsPlayer::TtsPlayer() : impl_(std::make_unique<Impl>()) {}

TtsPlayer::~TtsPlayer() = default;

bool TtsPlayer::Initialize(AudioCodec* codec) {
    return impl_->Initialize(codec);
}

bool TtsPlayer::Play(const std::string& url) {
    return impl_->Play(url);
}

void TtsPlayer::Stop() {
    impl_->Stop();
}

TtsPlayState TtsPlayer::GetState() const {
    return impl_->GetState();
}

bool TtsPlayer::IsPlaying() const {
    return impl_->IsPlaying();
}

void TtsPlayer::SetStateCallback(StateCallback callback) {
    impl_->SetStateCallback(std::move(callback));
}

void TtsPlayer::SetCompletionCallback(CompletionCallback callback) {
    impl_->SetCompletionCallback(std::move(callback));
}

void TtsPlayer::SetVolume(int volume) {
    impl_->SetVolume(volume);
}

} // namespace ai_sdk
