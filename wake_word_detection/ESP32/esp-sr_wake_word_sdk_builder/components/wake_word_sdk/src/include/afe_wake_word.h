/**
 * @file afe_wake_word.h
 * @brief ESP32-S3/P4 唤醒词实现（带 AFE）
 * 
 * 使用 Wakenet + AFE（音频前端）进行唤醒词检测。
 * AFE 提供回声消除（AEC）、噪声抑制（NS）等功能。
 * 适用于 ESP32-S3、ESP32-P4 等支持 AFE 的芯片。
 */

#ifndef AFE_WAKE_WORD_H
#define AFE_WAKE_WORD_H

#include <freertos/FreeRTOS.h>
#include <freertos/task.h>
#include <freertos/event_groups.h>

#include <esp_afe_sr_models.h>
#include <esp_nsn_models.h>
#include <model_path.h>

#include <deque>
#include <string>
#include <vector>
#include <functional>
#include <mutex>
#include <condition_variable>

#include "wake_word_sdk/wake_word.h"

namespace wake_word_sdk {

/**
 * @brief ESP32-S3/P4 唤醒词实现类（带 AFE）
 * 
 * 使用 Wakenet + AFE 进行唤醒词检测，支持：
 * - 回声消除（AEC）：消除扬声器回声
 * - 噪声抑制（NS）：降低环境噪声
 * - 波束成形（BF）：多麦克风阵列增强
 */
class AfeWakeWord : public WakeWord {
public:
    AfeWakeWord();
    ~AfeWakeWord();

    /**
     * @brief 初始化唤醒词检测器
     * 
     * @param config 唤醒词配置，包含音频参数
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
    srmodel_list_t *models_ = nullptr;                  ///< 模型列表
    const esp_afe_sr_iface_t* afe_iface_ = nullptr;     ///< AFE 接口
    esp_afe_sr_data_t* afe_data_ = nullptr;             ///< AFE 数据
    char* wakenet_model_ = NULL;                        ///< Wakenet 模型名称
    std::vector<std::string> wake_words_;               ///< 支持的唤醒词列表
    EventGroupHandle_t event_group_;                    ///< FreeRTOS 事件组
    std::function<void(const std::string& wake_word)> wake_word_detected_callback_;  ///< 唤醒词检测回调
    WakeWordConfig config_;                             ///< 配置（替代原来的 AudioCodec*）
    std::string last_detected_wake_word_;               ///< 最后检测到的唤醒词

    // Opus 编码相关
    TaskHandle_t wake_word_encode_task_ = nullptr;      ///< 编码任务句柄
    StaticTask_t* wake_word_encode_task_buffer_ = nullptr;  ///< 任务控制块
    StackType_t* wake_word_encode_task_stack_ = nullptr;    ///< 任务栈
    std::deque<std::vector<int16_t>> wake_word_pcm_;    ///< PCM 数据队列
    std::deque<std::vector<uint8_t>> wake_word_opus_;   ///< Opus 数据队列
    std::mutex wake_word_mutex_;                        ///< 互斥锁
    std::condition_variable wake_word_cv_;              ///< 条件变量

    /**
     * @brief 存储唤醒词音频数据
     * 
     * @param data 音频数据指针
     * @param size 样本数
     */
    void StoreWakeWordData(const int16_t* data, size_t size);
    
    /**
     * @brief 音频检测任务
     * 
     * 在独立任务中运行，从 AFE 获取处理后的音频并检测唤醒词。
     */
    void AudioDetectionTask();
};

}  // namespace wake_word_sdk

#endif // AFE_WAKE_WORD_H
