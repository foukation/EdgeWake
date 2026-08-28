/**
 * @file afe_wake_word.cc
 * @brief ESP32-S3/P4 唤醒词实现（带 AFE）
 * 
 * 使用 Wakenet + AFE（音频前端）进行唤醒词检测。
 */

#include "afe_wake_word.h"
#include "wake_word_sdk/wake_word_types.h"

#include <cstring>
#include <esp_log.h>
#include <esp_timer.h>
#include <opus_encoder.h>
#include <sstream>

#define DETECTION_RUNNING_EVENT 1

#define TAG "AfeWakeWord"

namespace wake_word_sdk {

AfeWakeWord::AfeWakeWord()
    : afe_data_(nullptr),
      wake_word_pcm_(),
      wake_word_opus_() {
    // 创建 FreeRTOS 事件组
    event_group_ = xEventGroupCreate();
}

AfeWakeWord::~AfeWakeWord() {
    // 清理 AFE 资源
    if (afe_data_ != nullptr) {
        afe_iface_->destroy(afe_data_);
    }

    // 清理编码任务资源
    if (wake_word_encode_task_stack_ != nullptr) {
        heap_caps_free(wake_word_encode_task_stack_);
    }

    if (wake_word_encode_task_buffer_ != nullptr) {
        heap_caps_free(wake_word_encode_task_buffer_);
    }

    // 清理模型资源
    if (models_ != nullptr) {
        esp_srmodel_deinit(models_);
    }

    // 删除事件组
    vEventGroupDelete(event_group_);
}

bool AfeWakeWord::Initialize(const WakeWordConfig& config, srmodel_list_t* models_list) {
    // 保存配置
    config_ = config;
    
    // 计算参考通道数（用于回声消除）
    int ref_num = config_.audio.has_reference ? 1 : 0;

    // 初始化模型列表
    if (models_list == nullptr) {
        models_ = esp_srmodel_init("model");
    } else {
        models_ = models_list;
    }

    // 检查模型是否有效
    if (models_ == nullptr || models_->num == -1) {
        ESP_LOGE(TAG, "初始化 Wakenet 模型失败");
        return false;
    }

    // 遍历模型列表，查找 Wakenet 模型
    for (int i = 0; i < models_->num; i++) {
        ESP_LOGI(TAG, "模型 %d: %s", i, models_->model_name[i]);
        if (strstr(models_->model_name[i], ESP_WN_PREFIX) != NULL) {
            wakenet_model_ = models_->model_name[i];
            // 获取唤醒词列表，以 ";" 分隔
            auto words = esp_srmodel_get_wake_words(models_, wakenet_model_);
            std::stringstream ss(words);
            std::string word;
            while (std::getline(ss, word, ';')) {
                wake_words_.push_back(word);
                ESP_LOGI(TAG, "唤醒词: %s", word.c_str());
            }
        }
    }

    // 构建输入格式字符串
    // 'M' 表示麦克风通道，'R' 表示参考通道（用于 AEC）
    std::string input_format;
    for (int i = 0; i < config_.audio.input_channels - ref_num; i++) {
        input_format.push_back('M');
    }
    for (int i = 0; i < ref_num; i++) {
        input_format.push_back('R');
    }
    ESP_LOGI(TAG, "输入格式: %s", input_format.c_str());

    // 初始化 AFE 配置
    afe_config_t* afe_config = afe_config_init(input_format.c_str(), models_, AFE_TYPE_SR, AFE_MODE_HIGH_PERF);
    afe_config->aec_init = config_.audio.has_reference;  // 是否启用 AEC
    afe_config->aec_mode = AEC_MODE_SR_HIGH_PERF;        // AEC 模式
    afe_config->afe_perferred_core = 1;                  // 优先使用 CPU 核心 1
    afe_config->afe_perferred_priority = 1;              // 任务优先级
    afe_config->memory_alloc_mode = AFE_MEMORY_ALLOC_MORE_PSRAM;  // 优先使用 PSRAM
    
    // 创建 AFE 实例
    afe_iface_ = esp_afe_handle_from_config(afe_config);
    afe_data_ = afe_iface_->create_from_config(afe_config);

    // 创建音频检测任务
    xTaskCreate([](void* arg) {
        auto this_ = (AfeWakeWord*)arg;
        this_->AudioDetectionTask();
        vTaskDelete(NULL);
    }, "audio_detection", 4096, this, 3, nullptr);

    ESP_LOGI(TAG, "AFE 唤醒词检测器初始化成功");
    return true;
}

void AfeWakeWord::OnWakeWordDetected(std::function<void(const std::string& wake_word)> callback) {
    wake_word_detected_callback_ = callback;
}

void AfeWakeWord::Start() {
    xEventGroupSetBits(event_group_, DETECTION_RUNNING_EVENT);
    ESP_LOGI(TAG, "唤醒词检测已启动");
}

void AfeWakeWord::Stop() {
    xEventGroupClearBits(event_group_, DETECTION_RUNNING_EVENT);
    // 重置 AFE 缓冲区
    if (afe_data_ != nullptr) {
        afe_iface_->reset_buffer(afe_data_);
    }
    ESP_LOGI(TAG, "唤醒词检测已停止");
}

void AfeWakeWord::Feed(const std::vector<int16_t>& data) {
    if (afe_data_ == nullptr) {
        return;
    }
    // 将音频数据送入 AFE 处理
    afe_iface_->feed(afe_data_, data.data());
}

size_t AfeWakeWord::GetFeedSize() {
    if (afe_data_ == nullptr) {
        return 0;
    }
    return afe_iface_->get_feed_chunksize(afe_data_);
}

void AfeWakeWord::AudioDetectionTask() {
    auto fetch_size = afe_iface_->get_fetch_chunksize(afe_data_);
    auto feed_size = afe_iface_->get_feed_chunksize(afe_data_);
    ESP_LOGI(TAG, "音频检测任务启动, feed 大小: %d, fetch 大小: %d",
        feed_size, fetch_size);

    while (true) {
        // 等待检测运行事件
        xEventGroupWaitBits(event_group_, DETECTION_RUNNING_EVENT, pdFALSE, pdTRUE, portMAX_DELAY);

        // 从 AFE 获取处理后的音频
        auto res = afe_iface_->fetch_with_delay(afe_data_, portMAX_DELAY);
        if (res == nullptr || res->ret_value == ESP_FAIL) {
            continue;
        }

        // 存储唤醒词音频数据（用于后续发送到服务器进行声纹识别等）
        StoreWakeWordData(res->data, res->data_size / sizeof(int16_t));

        // 检查是否检测到唤醒词
        if (res->wakeup_state == WAKENET_DETECTED) {
            Stop();
            last_detected_wake_word_ = wake_words_[res->wakenet_model_index - 1];

            ESP_LOGI(TAG, "检测到唤醒词: %s", last_detected_wake_word_.c_str());

            // 调用回调函数
            if (wake_word_detected_callback_) {
                wake_word_detected_callback_(last_detected_wake_word_);
            }
        }
    }
}

void AfeWakeWord::StoreWakeWordData(const int16_t* data, size_t samples) {
    // 存储音频数据到 PCM 队列
    wake_word_pcm_.emplace_back(std::vector<int16_t>(data, data + samples));
    // 保留约 2 秒的数据（采样率 16000，每块 512 样本，约 30ms）
    while (wake_word_pcm_.size() > 2000 / 30) {
        wake_word_pcm_.pop_front();
    }
}

void AfeWakeWord::EncodeWakeWordData() {
    const size_t stack_size = 4096 * 7;
    wake_word_opus_.clear();
    
    // 分配任务栈（使用 PSRAM）
    if (wake_word_encode_task_stack_ == nullptr) {
        wake_word_encode_task_stack_ = (StackType_t*)heap_caps_malloc(stack_size, MALLOC_CAP_SPIRAM);
        assert(wake_word_encode_task_stack_ != nullptr);
    }
    // 分配任务控制块（使用内部 RAM）
    if (wake_word_encode_task_buffer_ == nullptr) {
        wake_word_encode_task_buffer_ = (StaticTask_t*)heap_caps_malloc(sizeof(StaticTask_t), MALLOC_CAP_INTERNAL);
        assert(wake_word_encode_task_buffer_ != nullptr);
    }

    // 创建编码任务
    wake_word_encode_task_ = xTaskCreateStatic([](void* arg) {
        auto this_ = (AfeWakeWord*)arg;
        {
            auto start_time = esp_timer_get_time();
            // 创建 Opus 编码器（16kHz，单声道，20ms 帧）
            auto encoder = std::make_unique<OpusEncoderWrapper>(16000, 1, OPUS_FRAME_DURATION_MS);
            encoder->SetComplexity(0);  // 0 是最快的编码速度

            int packets = 0;
            // 编码所有 PCM 数据
            for (auto& pcm: this_->wake_word_pcm_) {
                encoder->Encode(std::move(pcm), [this_](std::vector<uint8_t>&& opus) {
                    std::lock_guard<std::mutex> lock(this_->wake_word_mutex_);
                    this_->wake_word_opus_.emplace_back(std::move(opus));
                    this_->wake_word_cv_.notify_all();
                });
                packets++;
            }
            this_->wake_word_pcm_.clear();

            auto end_time = esp_timer_get_time();
            ESP_LOGI(TAG, "编码唤醒词 Opus %d 包，耗时 %ld ms", packets, (long)((end_time - start_time) / 1000));

            // 添加空包表示编码结束
            std::lock_guard<std::mutex> lock(this_->wake_word_mutex_);
            this_->wake_word_opus_.push_back(std::vector<uint8_t>());
            this_->wake_word_cv_.notify_all();
        }
        vTaskDelete(NULL);
    }, "encode_wake_word", stack_size, this, 2, wake_word_encode_task_stack_, wake_word_encode_task_buffer_);
}

bool AfeWakeWord::GetWakeWordOpus(std::vector<uint8_t>& opus) {
    std::unique_lock<std::mutex> lock(wake_word_mutex_);
    // 等待 Opus 数据可用
    wake_word_cv_.wait(lock, [this]() {
        return !wake_word_opus_.empty();
    });
    // 取出数据
    opus.swap(wake_word_opus_.front());
    wake_word_opus_.pop_front();
    // 空包表示结束
    return !opus.empty();
}

}  // namespace wake_word_sdk
