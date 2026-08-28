/**
 * @file esp_wake_word.cc
 * @brief ESP32/C3 唤醒词实现
 * 
 * 使用 Wakenet 进行唤醒词检测，不带 AFE（音频前端）。
 */

#include "esp_wake_word.h"
#include <esp_log.h>

#define TAG "EspWakeWord"

namespace wake_word_sdk {

EspWakeWord::EspWakeWord() {
}

EspWakeWord::~EspWakeWord() {
    // 清理 Wakenet 资源
    if (wakenet_data_ != nullptr) {
        wakenet_iface_->destroy(wakenet_data_);
        esp_srmodel_deinit(wakenet_model_);
    }
}

bool EspWakeWord::Initialize(const WakeWordConfig& config, srmodel_list_t* models_list) {
    // 保存配置
    config_ = config;

    // 初始化模型列表
    if (models_list == nullptr) {
        wakenet_model_ = esp_srmodel_init("model");
    } else {
        wakenet_model_ = models_list;
    }

    // 检查模型是否有效
    if (wakenet_model_ == nullptr || wakenet_model_->num == -1) {
        ESP_LOGE(TAG, "初始化 Wakenet 模型失败");
        return false;
    }
    if (wakenet_model_->num > 1) {
        ESP_LOGW(TAG, "发现多个模型，使用第一个");
    } else if (wakenet_model_->num == 0) {
        ESP_LOGE(TAG, "未找到模型");
        return false;
    }

    // 获取模型名称并创建 Wakenet 实例
    char *model_name = wakenet_model_->model_name[0];
    wakenet_iface_ = (esp_wn_iface_t*)esp_wn_handle_from_name(model_name);
    wakenet_data_ = wakenet_iface_->create(model_name, DET_MODE_95);

    // 打印模型信息
    int frequency = wakenet_iface_->get_samp_rate(wakenet_data_);
    int audio_chunksize = wakenet_iface_->get_samp_chunksize(wakenet_data_);
    ESP_LOGI(TAG, "唤醒词(%s), 采样率: %d, 块大小: %d", model_name, frequency, audio_chunksize);

    return true;
}

void EspWakeWord::OnWakeWordDetected(std::function<void(const std::string& wake_word)> callback) {
    wake_word_detected_callback_ = callback;
}

void EspWakeWord::Start() {
    running_ = true;
    ESP_LOGI(TAG, "唤醒词检测已启动");
}

void EspWakeWord::Stop() {
    running_ = false;
    ESP_LOGI(TAG, "唤醒词检测已停止");
}

void EspWakeWord::Feed(const std::vector<int16_t>& data) {
    // 检查是否可以处理
    if (wakenet_data_ == nullptr || !running_) {
        return;
    }

    // 进行唤醒词检测
    int res = wakenet_iface_->detect(wakenet_data_, (int16_t *)data.data());
    if (res > 0) {
        // 检测到唤醒词
        last_detected_wake_word_ = wakenet_iface_->get_word_name(wakenet_data_, res);
        running_ = false;

        ESP_LOGI(TAG, "检测到唤醒词: %s", last_detected_wake_word_.c_str());

        // 调用回调函数
        if (wake_word_detected_callback_) {
            wake_word_detected_callback_(last_detected_wake_word_);
        }
    }
}

size_t EspWakeWord::GetFeedSize() {
    if (wakenet_data_ == nullptr) {
        return 0;
    }
    return wakenet_iface_->get_samp_chunksize(wakenet_data_);
}

void EspWakeWord::EncodeWakeWordData() {
    // ESP32/C3 版本不支持唤醒词音频编码
    // 因为资源有限，不存储唤醒词音频
}

bool EspWakeWord::GetWakeWordOpus(std::vector<uint8_t>& opus) {
    // ESP32/C3 版本不支持唤醒词音频编码
    return false;
}

}  // namespace wake_word_sdk
