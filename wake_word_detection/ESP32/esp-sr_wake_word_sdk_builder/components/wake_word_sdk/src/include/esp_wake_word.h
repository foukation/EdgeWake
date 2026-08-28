/**
 * @file esp_wake_word.h
 * @brief ESP32/C3 唤醒词实现
 * 
 * 使用 Wakenet 进行唤醒词检测，不带 AFE（音频前端）。
 * 适用于 ESP32、ESP32-C3 等不支持 AFE 的芯片。
 */

#ifndef ESP_WAKE_WORD_H
#define ESP_WAKE_WORD_H

#include <esp_wn_iface.h>
#include <esp_wn_models.h>
#include <model_path.h>

#include <string>
#include <vector>
#include <functional>
#include <atomic>

#include "wake_word_sdk/wake_word.h"

namespace wake_word_sdk {

/**
 * @brief ESP32/C3 唤醒词实现类
 * 
 * 使用 Wakenet 进行简单的唤醒词检测，不包含 AFE 处理。
 * 适用于资源受限的芯片。
 */
class EspWakeWord : public WakeWord {
public:
    EspWakeWord();
    ~EspWakeWord();

    /**
     * @brief 初始化唤醒词检测器
     * 
     * @param config 唤醒词配置（此实现中主要使用 models_list）
     * @param models_list 语音识别模型列表
     * @return 成功返回 true，失败返回 false
     */
    bool Initialize(const WakeWordConfig& config, srmodel_list_t* models_list) override;
    
    void Feed(const std::vector<int16_t>& data) override;
    void OnWakeWordDetected(std::function<void(const std::string& wake_word)> callback) override;
    void Start() override;
    void Stop() override;
    size_t GetFeedSize() override;
    void EncodeWakeWordData() override;
    bool GetWakeWordOpus(std::vector<uint8_t>& opus) override;
    const std::string& GetLastDetectedWakeWord() const override { return last_detected_wake_word_; }

private:
    esp_wn_iface_t *wakenet_iface_ = nullptr;      ///< Wakenet 接口
    model_iface_data_t *wakenet_data_ = nullptr;   ///< Wakenet 模型数据
    srmodel_list_t *wakenet_model_ = nullptr;      ///< 模型列表
    WakeWordConfig config_;                         ///< 配置（替代原来的 AudioCodec*）
    std::atomic<bool> running_ = false;            ///< 运行状态标志

    std::function<void(const std::string& wake_word)> wake_word_detected_callback_;  ///< 唤醒词检测回调
    std::string last_detected_wake_word_;          ///< 最后检测到的唤醒词
};

}  // namespace wake_word_sdk

#endif // ESP_WAKE_WORD_H
