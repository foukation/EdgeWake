/**
 * @file wake_word.h
 * @brief 语音唤醒抽象接口
 * 
 * 定义了语音唤醒的抽象基类，所有唤醒词实现都继承自此类。
 * 支持的实现包括：
 * - EspWakeWord: ESP32/C3 用，简单的 Wakenet 实现
 * - AfeWakeWord: ESP32-S3/P4 用，带 AFE 的 Wakenet 实现
 * - CustomWakeWord: ESP32-S3/P4 用，Multinet 自定义唤醒词实现
 */

#ifndef WAKE_WORD_H
#define WAKE_WORD_H

#include <string>
#include <vector>
#include <functional>
#include <memory>

#include <model_path.h>
#include "wake_word_types.h"

namespace wake_word_sdk {

/**
 * @brief 语音唤醒抽象基类
 * 
 * 定义了语音唤醒的通用接口，包括初始化、音频输入、唤醒检测等功能。
 */
class WakeWord {
public:
    virtual ~WakeWord() = default;
    
    /**
     * @brief 初始化唤醒词检测器
     * 
     * @param config 唤醒词配置，包含音频参数和可选的资源读取回调
     * @param models_list 语音识别模型列表
     * @return 成功返回 true，失败返回 false
     */
    virtual bool Initialize(const WakeWordConfig& config, srmodel_list_t* models_list) = 0;
    
    /**
     * @brief 输入音频数据进行唤醒词检测
     * 
     * @param data 16位 PCM 音频数据
     */
    virtual void Feed(const std::vector<int16_t>& data) = 0;
    
    /**
     * @brief 设置唤醒词检测回调
     * 
     * @param callback 检测到唤醒词时调用的回调函数，参数为检测到的唤醒词
     */
    virtual void OnWakeWordDetected(std::function<void(const std::string& wake_word)> callback) = 0;
    
    /**
     * @brief 启动唤醒词检测
     */
    virtual void Start() = 0;
    
    /**
     * @brief 停止唤醒词检测
     */
    virtual void Stop() = 0;
    
    /**
     * @brief 获取每次 Feed 需要的音频样本数
     * 
     * @return 需要的样本数
     */
    virtual size_t GetFeedSize() = 0;
    
    /**
     * @brief 编码唤醒词音频数据为 Opus 格式
     * 
     * 将检测到唤醒词时的音频数据编码为 Opus 格式，用于发送到服务器。
     */
    virtual void EncodeWakeWordData() = 0;
    
    /**
     * @brief 获取编码后的 Opus 数据包
     * 
     * @param opus 输出参数，存储 Opus 数据
     * @return 成功返回 true，无数据返回 false
     */
    virtual bool GetWakeWordOpus(std::vector<uint8_t>& opus) = 0;
    
    /**
     * @brief 获取最后检测到的唤醒词
     * 
     * @return 唤醒词字符串
     */
    virtual const std::string& GetLastDetectedWakeWord() const = 0;
};

/**
 * @brief 创建唤醒词实例的工厂函数
 * 
 * 根据目标芯片和模型列表，自动选择合适的唤醒词实现。
 * 
 * @param config 唤醒词配置
 * @param models_list 语音识别模型列表
 * @return 唤醒词实例指针，失败返回 nullptr
 */
std::unique_ptr<WakeWord> CreateWakeWord(
    const WakeWordConfig& config,
    srmodel_list_t* models_list
);

}  // namespace wake_word_sdk

#endif // WAKE_WORD_H
