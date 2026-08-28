/**
 * @file custom_wake_word.h
 * @brief ESP32-S3/P4 自定义唤醒词实现
 * 
 * 使用 Multinet 进行自定义唤醒词/命令词检测。
 * 支持用户自定义唤醒词，通过 index.json 配置文件或 Kconfig 配置。
 * 适用于 ESP32-S3、ESP32-P4 等支持 Multinet 的芯片。
 */

#ifndef CUSTOM_WAKE_WORD_H
#define CUSTOM_WAKE_WORD_H

#include <esp_attr.h>
#include <esp_mn_iface.h>
#include <esp_mn_models.h>
#include <model_path.h>

#include <deque>
#include <string>
#include <vector>
#include <functional>
#include <mutex>
#include <condition_variable>
#include <atomic>

#include "wake_word_sdk/wake_word.h"

namespace wake_word_sdk {

/**
 * @brief ESP32-S3/P4 自定义唤醒词实现类
 * 
 * 使用 Multinet 进行自定义唤醒词检测，支持：
 * - 自定义唤醒词配置
 * - 多个命令词识别
 * - 可配置的检测阈值和超时时间
 */
class CustomWakeWord : public WakeWord {
public:
    CustomWakeWord();
    ~CustomWakeWord();

    /**
     * @brief 初始化唤醒词检测器
     * 
     * @param config 唤醒词配置，包含音频参数和资源读取回调
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
    /**
     * @brief 命令词结构体
     */
    struct Command {
        std::string command;    ///< 命令词（用于识别）
        std::string text;       ///< 显示文本
        std::string action;     ///< 动作类型（如 "wake"）
    };

    // Multinet 相关成员变量
    esp_mn_iface_t* multinet_ = nullptr;            ///< Multinet 接口
    model_iface_data_t* multinet_model_data_ = nullptr;  ///< Multinet 模型数据
    srmodel_list_t *models_ = nullptr;              ///< 模型列表
    char* mn_name_ = nullptr;                       ///< Multinet 模型名称
    std::string language_ = "cn";                   ///< 语言（默认中文）
    int duration_ = 3000;                           ///< 检测超时时间（毫秒）
    float threshold_ = 0.2;                         ///< 检测阈值
    std::deque<Command> commands_;                  ///< 命令词列表
 
    std::function<void(const std::string& wake_word)> wake_word_detected_callback_;  ///< 唤醒词检测回调
    WakeWordConfig config_;                         ///< 配置（替代原来的 AudioCodec*）
    std::string last_detected_wake_word_;           ///< 最后检测到的唤醒词
    std::atomic<bool> running_ = false;             ///< 运行状态标志

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
     * @param data 音频数据
     */
    void StoreWakeWordData(const std::vector<int16_t>& data);
    
    /**
     * @brief 解析唤醒词模型配置
     * 
     * 从 index.json 文件读取自定义唤醒词配置。
     */
    void ParseWakenetModelConfig();
};

}  // namespace wake_word_sdk

#endif // CUSTOM_WAKE_WORD_H
