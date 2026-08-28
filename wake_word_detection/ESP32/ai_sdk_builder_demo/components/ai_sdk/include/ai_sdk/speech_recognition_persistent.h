/**
 * @file speech_recognition_persistent.h
 * @brief 持续语音识别管理类定义
 *
 * 与 AsrIntelligentDialogue 的核心区别：
 * - 功能：纯 ASR（语音转文字），无 NLU / TTS / 智能对话
 * - 端点：/app-ws/v1/long-asr（智能对话为 /app-ws/v2/asr）
 * - START 帧：只有 dev_pid / format / sample，无 client_context
 * - 持续性：服务端 VAD 分割，连接不断开，多次回调 ResultCallback
 * - 结束方式：stop()（优雅等待）或 cancel()（立即断开）
 *
 * 支持两种音频接入模式：
 *
 * 模式一：外部音频（业务方自行采集，不依赖 SDK 音频驱动）
 *
 *   auto& asr = AIAssistantManager::getInstance().speechRecognitionPersistentHelp();
 *   asr.setCallbacks(on_result, on_error, on_close);
 *   asr.start();
 *   // 业务方循环调用，传入自采集的 PCM 数据
 *   asr.sendAudio(pcm_data, len);
 *   asr.stop();
 *
 * 模式二：内置音频驱动（SDK 管理麦克风，推荐）
 *
 *   auto& asr = AIAssistantManager::getInstance().speechRecognitionPersistentHelp();
 *   asr.setCallbacks(on_result, on_error, on_close);
 *   asr.initAudio(codec);
 *   asr.start();
 *   asr.setRecording(true);
 *   // 收到足够结果后...
 *   asr.stop();   // 或 asr.cancel()
 */
#pragma once

#include "ai_sdk/types/speech_recognition_persistent_result.h"
#include "ai_sdk/audio/audio_codec.h"
#include <functional>
#include <string>
#include <memory>

namespace ai_sdk {

// 前向声明
class AIAssistantManager;

/**
 * @brief 持续语音识别管理类（单例模式）
 *
 * 通过 AIAssistantManager::speechRecognitionPersistentHelp() 获取实例。
 * 使用 PIMPL 模式隐藏实现细节。
 */
class SpeechRecognitionPersistent {
public:
    // =========================================================================
    // 回调类型定义
    // =========================================================================

    /**
     * 识别结果回调
     *
     * 服务端消息全量反序列化后透传，不过滤 type 字段。
     * 一次连接可触发多次（持续识别的核心特性）。
     * 调用方应检查 result.err_no 和 result.type。
     */
    using ResultCallback = std::function<void(const SpeechRecognitionPersistentResult&)>;

    /**
     * 错误回调
     *
     * @param code    错误码（-1=SDK 内部错误，1=连接/网络错误）
     * @param message 错误描述
     */
    using ErrorCallback = std::function<void(int code, const std::string& message)>;

    /**
     * 连接关闭回调
     *
     * 无论 stop() / cancel() / 网络异常，WebSocket 关闭后均触发。
     * 触发后 isConnected() 返回 false，可再次调用 start()。
     */
    using CloseCallback = std::function<void()>;

    // =========================================================================
    // 公开方法
    // =========================================================================

    /**
     * @brief 设置回调函数
     *
     * 必须在 start() 之前调用。
     * 回调在 WebSocket 事件上下文（ESP-IDF 内部任务）中执行，注意线程安全。
     *
     * @param on_result 识别结果回调
     * @param on_error  错误回调
     * @param on_close  连接关闭回调（可为 nullptr）
     */
    void setCallbacks(ResultCallback on_result,
                      ErrorCallback  on_error,
                      CloseCallback  on_close);

    /**
     * @brief 启动持续识别
     *
     * 启动流程：
     * 1. 用 AssistUtils::wssParameter() 构建带签名的 WebSocket URL
     * 2. 建立 WebSocket 连接（异步，信号量同步等待，30 秒超时）
     * 3. 发送 START 帧：{"type":"START","data":{"dev_pid":…,"format":"pcm","sample":16000}}
     * 4. 标记 is_recognizing_ = true，等待音频数据
     *
     * @param dev_pid 语言模型 ID，默认 15372（普通话近场）
     * @return true 启动成功，false 失败（详见 on_error 回调）
     */
    bool start(int dev_pid = 15372);

    /**
     * @brief 优雅停止识别
     *
     * 发送 FINISH 帧：{"type":"finish"}
     * 服务端返回最后一段识别结果后主动关闭连接，触发 on_close 回调。
     * 如果已初始化音频模块，同时暂停麦克风录音。
     *
     * 使用场景：用户正常结束，希望获取最后一段识别结果。
     */
    void stop();

    /**
     * @brief 立即取消识别
     *
     * 发送 CANCEL 帧：{"type":"CANCEL"} 后主动断开 WebSocket 连接。
     * 不等待服务端返回，触发 on_close 回调。
     * 如果已初始化音频模块，同时暂停麦克风录音。
     *
     * 使用场景：用户中途放弃，不需要剩余识别结果。
     */
    void cancel();

    /**
     * @brief 发送 PCM 音频数据（外部音频模式）
     *
     * 音频要求：
     * - 格式：PCM 16-bit 小端序
     * - 采样率：16000 Hz
     * - 通道：单声道
     * - 建议块大小：5120 字节（= 160ms 音频）
     *
     * 未调用 initAudio() 时的唯一音频输入方式。
     * 已调用 initAudio() 时，SDK 内部自动调用此方法，无需手动调用。
     *
     * 线程安全：持有 state_mutex_，可从任意任务调用。
     *
     * @param data PCM 数据指针
     * @param len  数据长度（字节）
     */
    void sendAudio(const uint8_t* data, size_t len);

    /**
     * @brief 检查 WebSocket 是否已连接
     * @return true 已连接，false 未连接
     */
    bool isConnected() const;

    /**
     * @brief 检查是否正在识别（已连接且已发送 START 帧）
     * @return true 正在识别，false 未识别
     */
    bool isRecognizing() const;

    // =========================================================================
    // 内置音频驱动接口（可选，模式二专用）
    // =========================================================================

    /**
     * @brief 初始化内置音频模块（可选）
     *
     * 绑定 AudioCodec，在 SDK 内部创建 AudioInput（麦克风 → sendAudio）。
     * 调用后通过 setRecording(true/false) 控制录音开关。
     *
     * 不调用此方法时，业务方需自行采集 PCM 并调用 sendAudio()。
     *
     * @param codec 已 Start() 的 AudioCodec 实例，生命周期须长于本对象
     * @return true 成功，false 失败（codec 为 null 或内部错误）
     */
    bool initAudio(AudioCodec* codec);

    /**
     * @brief 控制麦克风录音开关（内置音频模式专用）
     *
     * 前提：已调用 initAudio()，否则打印警告并忽略。
     *
     * @param enable true 开始录音，false 暂停录音
     */
    void setRecording(bool enable);

    // 禁止拷贝和赋值（单例模式）
    SpeechRecognitionPersistent(const SpeechRecognitionPersistent&) = delete;
    SpeechRecognitionPersistent& operator=(const SpeechRecognitionPersistent&) = delete;

private:
    // =========================================================================
    // 私有方法（单例模式）
    // =========================================================================

    /**
     * 获取单例实例
     *
     * 私有方法：只能通过 AIAssistantManager::speechRecognitionPersistentHelp() 访问。
     *
     * @return SpeechRecognitionPersistent& 单例引用
     */
    static SpeechRecognitionPersistent& getInstance();

    /**
     * 私有构造函数（单例模式）
     */
    SpeechRecognitionPersistent();

    /**
     * 析构函数
     */
    ~SpeechRecognitionPersistent();

    // =========================================================================
    // PIMPL 实现
    // =========================================================================

    class Impl;
    std::unique_ptr<Impl> impl_;

    // 允许 AIAssistantManager 访问私有的 getInstance() 方法
    friend class AIAssistantManager;
};

} // namespace ai_sdk
