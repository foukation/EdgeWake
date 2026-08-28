/**
 * @file audio_input.cc
 * @brief 麦克风录音模块实现
 *
 * 实现 AudioInput 类的 PIMPL 内部逻辑，包括：
 * - FreeRTOS 录音任务（从 AudioCodec 读取 PCM 数据）
 * - 重采样（硬件采样率 → 16kHz，使用 esp_audio_effects）
 * - 声道转换（多声道 → 单声道，取左声道）
 * - 数据累积与回调输出
 *
 * 内存预算（典型场景）：
 * - 录音任务栈: 3072 bytes
 * - codec_buffer_: ~640 bytes (320 samples × 2 bytes)
 * - send_buffer_: 5120 bytes
 * - 重采样器（仅按需）: ~1-2 KB
 * - 总计: ~8-10 KB
 *
 * @version 1.0.0
 * @date 2026-03-09
 */

// 内部私有头文件（在 PRIV_INCLUDE_DIRS "src/audio" 中）
#include "audio_input.h"
// 公开头文件（在 INCLUDE_DIRS "include" 中）
#include "ai_sdk/audio/audio_codec.h"

#include <freertos/FreeRTOS.h>
#include <freertos/task.h>
#include <freertos/event_groups.h>
#include <esp_log.h>
#include <esp_ae_rate_cvt.h>

#include <vector>
#include <cstring>
#include <atomic>

static const char* TAG = "AudioInput";

namespace ai_sdk {

// =============================================================================
// 常量定义
// =============================================================================

/// 目标采样率：云端 ASR 要求 16kHz
static constexpr int kTargetSampleRate = 16000;

/// 帧时长：20ms（与主项目 OPUS_FRAME_DURATION_MS 一致）
static constexpr int kFrameDurationMs = 20;

/// 每帧采样点数：16000 × 20 / 1000 = 320 samples
static constexpr int kFrameSamples = kTargetSampleRate * kFrameDurationMs / 1000;

/// 发送阈值：5120 字节（160ms @ 16kHz 16bit Mono = 2560 samples × 2 bytes）
/// 与 Android CHUNK_SIZE 和 sendAudio() 建议块大小一致
static constexpr size_t kSendThreshold = 5120;

/// 录音任务栈大小：3072 字节
/// 不含 AFE/唤醒词处理，比主项目的 4096-6144B 更精简
static constexpr uint32_t kTaskStackSize = 3072;

/// 录音任务优先级：8（高优先级，与主项目 audio_input 任务一致）
/// 确保录音不丢帧
static constexpr UBaseType_t kTaskPriority = 8;

/// EventGroup 位：录音运行标志
static constexpr EventBits_t kRecordingBit = BIT0;

// =============================================================================
// AudioInput::Impl 实现类
// =============================================================================

class AudioInput::Impl {
public:
    Impl() = default;

    ~Impl() {
        Stop();
    }

    bool Initialize(AudioCodec* codec) {
        if (!codec) {
            ESP_LOGE(TAG, "Initialize failed: codec is null");
            return false;
        }

        codec_ = codec;
        codec_sample_rate_ = codec->input_sample_rate();
        codec_channels_ = codec->input_channels();

        // 判断是否需要重采样
        need_resample_ = (codec_sample_rate_ != kTargetSampleRate);

        // 判断是否需要声道转换
        need_channel_convert_ = (codec_channels_ > 1);

        ESP_LOGI(TAG, "Initialized: codec_rate=%d, codec_channels=%d, "
                 "need_resample=%d, need_channel_convert=%d",
                 codec_sample_rate_, codec_channels_,
                 need_resample_, need_channel_convert_);

        initialized_ = true;
        return true;
    }

    bool Start() {
        if (!initialized_) {
            ESP_LOGE(TAG, "Start failed: not initialized");
            return false;
        }
        if (task_handle_) {
            ESP_LOGW(TAG, "Start: task already running");
            return true;
        }

        // 创建 EventGroup
        event_group_ = xEventGroupCreate();
        if (!event_group_) {
            ESP_LOGE(TAG, "Start failed: cannot create event group");
            return false;
        }

        // 初始化重采样器（如果需要）
        if (need_resample_ && !InitResampler()) {
            vEventGroupDelete(event_group_);
            event_group_ = nullptr;
            return false;
        }

        // 预分配缓冲区
        AllocateBuffers();

        // 重置退出标志
        task_should_exit_ = false;

        // 创建录音任务
        BaseType_t ret = xTaskCreate(
            TaskEntry,       // 任务入口函数
            "ai_audio_in",   // 任务名称
            kTaskStackSize,  // 栈大小
            this,            // 任务参数
            kTaskPriority,   // 优先级
            &task_handle_    // 任务句柄
        );

        if (ret != pdPASS) {
            ESP_LOGE(TAG, "Start failed: cannot create task (ret=%d)", ret);
            CleanupResources();
            return false;
        }

        ESP_LOGI(TAG, "Recording task started");
        return true;
    }

    void Stop() {
        if (!task_handle_) {
            return;
        }

        // 通知任务退出
        task_should_exit_ = true;

        // 如果任务正在 WaitBits 挂起，设置 RECORDING 位让它唤醒以检查退出标志
        if (event_group_) {
            xEventGroupSetBits(event_group_, kRecordingBit);
        }

        // 等待任务结束
        // 给任务一段时间来响应退出标志
        vTaskDelay(pdMS_TO_TICKS(100));

        // 如果任务还没退出，强制删除
        if (task_handle_) {
            vTaskDelete(task_handle_);
            task_handle_ = nullptr;
            ESP_LOGW(TAG, "Recording task force deleted");
        }

        // 释放资源
        CleanupResources();

        ESP_LOGI(TAG, "Recording task stopped");
    }

    void SetRecording(bool enable) {
        if (!event_group_) {
            return;
        }

        if (enable) {
            xEventGroupSetBits(event_group_, kRecordingBit);
            ESP_LOGI(TAG, "Recording enabled");
        } else {
            xEventGroupClearBits(event_group_, kRecordingBit);
            // 清空累积缓冲区（暂停时不发送残留数据）
            send_buffer_pos_ = 0;
            ESP_LOGI(TAG, "Recording disabled");
        }
    }

    bool IsRecording() const {
        if (!event_group_) {
            return false;
        }
        return (xEventGroupGetBits(event_group_) & kRecordingBit) != 0;
    }

    void SetAudioDataCallback(AudioInput::AudioDataCallback callback) {
        callback_ = std::move(callback);
    }

private:
    // =========================================================================
    // 重采样器初始化
    // =========================================================================

    bool InitResampler() {
        esp_ae_rate_cvt_cfg_t cfg = {
            .src_rate = (uint32_t)codec_sample_rate_,
            .dest_rate = (uint32_t)kTargetSampleRate,
            .channel = (uint8_t)codec_channels_,
            .bits_per_sample = 16,  // PCM 16-bit
            .complexity = 2,        // 中等质量（平衡 CPU 和质量）
            .perf_type = ESP_AE_RATE_CVT_PERF_TYPE_SPEED,
        };

        esp_ae_err_t err = esp_ae_rate_cvt_open(&cfg, &resampler_);
        if (err != ESP_AE_ERR_OK || !resampler_) {
            ESP_LOGE(TAG, "Failed to create resampler: %d", err);
            return false;
        }

        // 计算输入帧大小（硬件采样率下 20ms 的采样点数）
        resample_in_samples_ = codec_sample_rate_ * kFrameDurationMs / 1000;

        // 查询输出缓冲区大小
        uint32_t max_out = 0;
        esp_ae_rate_cvt_get_max_out_sample_num(
            resampler_, resample_in_samples_, &max_out);
        resample_out_samples_ = max_out;

        // 预分配重采样输出缓冲区
        resample_out_buffer_.resize(resample_out_samples_ * codec_channels_);

        ESP_LOGI(TAG, "Resampler created: %d->%d Hz, in=%d out_max=%d samples",
                 codec_sample_rate_, kTargetSampleRate,
                 resample_in_samples_, (int)resample_out_samples_);
        return true;
    }

    // =========================================================================
    // 缓冲区分配
    // =========================================================================

    void AllocateBuffers() {
        // codec 读取缓冲区：每帧的采样点数
        int codec_frame_samples;
        if (need_resample_) {
            // 重采样时，按硬件采样率计算每帧采样点数
            codec_frame_samples = resample_in_samples_ * codec_channels_;
        } else {
            // 不需要重采样，直接按 16kHz 计算
            codec_frame_samples = kFrameSamples * codec_channels_;
        }
        codec_buffer_.resize(codec_frame_samples);

        // 发送缓冲区：固定 kSendThreshold 字节
        send_buffer_.resize(kSendThreshold);
        send_buffer_pos_ = 0;

        ESP_LOGD(TAG, "Buffers allocated: codec_buffer=%zu samples, send_buffer=%zu bytes",
                 codec_buffer_.size(), send_buffer_.size());
    }

    // =========================================================================
    // 资源清理
    // =========================================================================

    void CleanupResources() {
        // 释放重采样器
        if (resampler_) {
            esp_ae_rate_cvt_close(resampler_);
            resampler_ = nullptr;
        }

        // 释放缓冲区内存
        codec_buffer_.clear();
        codec_buffer_.shrink_to_fit();
        resample_out_buffer_.clear();
        resample_out_buffer_.shrink_to_fit();
        send_buffer_.clear();
        send_buffer_.shrink_to_fit();
        send_buffer_pos_ = 0;

        // 释放 EventGroup
        if (event_group_) {
            vEventGroupDelete(event_group_);
            event_group_ = nullptr;
        }
    }

    // =========================================================================
    // FreeRTOS 录音任务
    // =========================================================================

    static void TaskEntry(void* arg) {
        auto* self = static_cast<Impl*>(arg);
        self->TaskLoop();
        // 任务正常退出时，清除句柄
        self->task_handle_ = nullptr;
        vTaskDelete(nullptr);
    }

    void TaskLoop() {
        ESP_LOGI(TAG, "Recording task loop started");

        while (!task_should_exit_) {
            // 等待录音开关被打开
            // 使用 pdFALSE 不自动清除位，pdTRUE 需要所有位都设置
            EventBits_t bits = xEventGroupWaitBits(
                event_group_,
                kRecordingBit,
                pdFALSE,        // 不自动清除位（保持录音状态）
                pdTRUE,         // 等待所有位（只有一个位）
                pdMS_TO_TICKS(100)  // 100ms 超时，定期检查退出标志
            );

            if (task_should_exit_) {
                break;
            }

            if (!(bits & kRecordingBit)) {
                // 录音未开启，继续等待
                continue;
            }

            // 从 AudioCodec 读取一帧 PCM 数据
            if (!codec_->InputData(codec_buffer_)) {
                // 读取失败（可能硬件未就绪），短暂等待后重试
                vTaskDelay(pdMS_TO_TICKS(10));
                continue;
            }

            // 处理数据：重采样 → 声道转换 → 累积发送
            ProcessFrame();
        }

        ESP_LOGI(TAG, "Recording task loop exited");
    }

    // =========================================================================
    // 数据处理流水线
    // =========================================================================

    void ProcessFrame() {
        int16_t* pcm_data = codec_buffer_.data();
        int pcm_samples = codec_buffer_.size() / codec_channels_;
        int channels = codec_channels_;

        // 第 1 步：重采样（如果需要）
        if (need_resample_) {
            uint32_t out_samples = resample_out_samples_;
            esp_ae_err_t err = esp_ae_rate_cvt_process(
                resampler_,
                (esp_ae_sample_t)codec_buffer_.data(),
                pcm_samples,
                (esp_ae_sample_t)resample_out_buffer_.data(),
                &out_samples
            );

            if (err != ESP_AE_ERR_OK) {
                ESP_LOGW(TAG, "Resample failed: %d", err);
                return;
            }

            pcm_data = resample_out_buffer_.data();
            pcm_samples = out_samples;
        }

        // 第 2 步：声道转换（如果需要）
        // 多声道 → 单声道：取左声道（第一个声道）
        if (need_channel_convert_) {
            for (int i = 0; i < pcm_samples; i++) {
                pcm_data[i] = pcm_data[i * channels];
            }
        }

        // 第 3 步：累积到发送缓冲区
        const uint8_t* src = reinterpret_cast<const uint8_t*>(pcm_data);
        size_t src_bytes = pcm_samples * sizeof(int16_t);
        size_t src_offset = 0;

        while (src_offset < src_bytes) {
            // 计算本次可以拷贝的字节数
            size_t space = kSendThreshold - send_buffer_pos_;
            size_t copy_len = src_bytes - src_offset;
            if (copy_len > space) {
                copy_len = space;
            }

            // 拷贝到发送缓冲区
            std::memcpy(send_buffer_.data() + send_buffer_pos_,
                        src + src_offset, copy_len);
            send_buffer_pos_ += copy_len;
            src_offset += copy_len;

            // 达到阈值，触发回调
            if (send_buffer_pos_ >= kSendThreshold) {
                if (callback_) {
                    callback_(send_buffer_.data(), send_buffer_pos_);
                }
                send_buffer_pos_ = 0;
            }
        }
    }

    // =========================================================================
    // 成员变量
    // =========================================================================

    // 外部依赖（不拥有所有权）
    AudioCodec* codec_ = nullptr;

    // 初始化状态
    bool initialized_ = false;

    // 音频参数
    int codec_sample_rate_ = 0;   ///< 硬件采样率
    int codec_channels_ = 1;      ///< 硬件声道数
    bool need_resample_ = false;  ///< 是否需要重采样
    bool need_channel_convert_ = false;  ///< 是否需要声道转换

    // FreeRTOS 任务
    TaskHandle_t task_handle_ = nullptr;
    EventGroupHandle_t event_group_ = nullptr;
    std::atomic<bool> task_should_exit_{false};

    // 重采样器
    esp_ae_rate_cvt_handle_t resampler_ = nullptr;
    int resample_in_samples_ = 0;      ///< 重采样输入帧采样点数
    uint32_t resample_out_samples_ = 0; ///< 重采样输出帧采样点数上限

    // 缓冲区
    std::vector<int16_t> codec_buffer_;         ///< AudioCodec 读取缓冲区
    std::vector<int16_t> resample_out_buffer_;  ///< 重采样输出缓冲区
    std::vector<uint8_t> send_buffer_;          ///< 发送累积缓冲区
    size_t send_buffer_pos_ = 0;                ///< 发送缓冲区当前写入位置

    // 回调
    AudioInput::AudioDataCallback callback_;
};

// =============================================================================
// AudioInput 公开方法（委托给 Impl）
// =============================================================================

AudioInput::AudioInput() : impl_(std::make_unique<Impl>()) {}

AudioInput::~AudioInput() = default;

bool AudioInput::Initialize(AudioCodec* codec) {
    return impl_->Initialize(codec);
}

bool AudioInput::Start() {
    return impl_->Start();
}

void AudioInput::Stop() {
    impl_->Stop();
}

void AudioInput::SetRecording(bool enable) {
    impl_->SetRecording(enable);
}

bool AudioInput::IsRecording() const {
    return impl_->IsRecording();
}

void AudioInput::SetAudioDataCallback(AudioDataCallback callback) {
    impl_->SetAudioDataCallback(std::move(callback));
}

} // namespace ai_sdk
