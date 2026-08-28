/**
 * @file asr_intelligent_dialogue.h
 * @brief ASR 智能对话管理类定义
 *
 * 此文件定义了 ASR（自动语音识别）智能对话的核心类 AsrIntelligentDialogue。
 * 该类负责管理与云端的 WebSocket 连接，实现实时语音识别和智能对话功能。
 *
 * 核心功能：
 * - 管理 WebSocket 连接生命周期（连接、断开、重连）
 * - 协调音频数据流（发送、缓冲）
 * - 解析 ASR 和对话响应结果
 * - 通过回调通知上层业务
 * - 内置麦克风录音（AudioInput）和 TTS 播放（TtsPlayer）
 *
 * 基础使用示例（不使用内置音频模块）：
 *
 *   #include "ai_sdk/asr_intelligent_dialogue.h"
 *   auto& asr = AIAssistantManager::getInstance().asrIntelligentDialogueHelp();
 *
 *   asr.setCallbacks(...);
 *   asr.start();
 *   asr.sendAudio(pcm_data, len);  // 厂商自行采集并发送
 *   asr.stop();
 *
 * 完整使用示例（使用内置音频模块，推荐）：
 *
 *   #include "ai_sdk/asr_intelligent_dialogue.h"
 *   #include "ai_sdk/audio/audio_config.h"
 *
 *   // 创建并启动 AudioCodec
 *   AudioConfig config;
 *   config.hardware_type = AudioHardwareType::kEs8311;
 *   // ... 填写引脚配置
 *   AudioCodec* codec = CreateAudioCodec(config);
 *   codec->Start();
 *
 *   auto& asr = AIAssistantManager::getInstance().asrIntelligentDialogueHelp();
 *
 *   asr.setCallbacks(...);
 *   asr.initAudio(codec);         // SDK 内部管理录音和 TTS
 *   asr.setAutoPlayTts(true);     // 收到 Speak 指令自动播放
 *   asr.start();
 *   asr.setRecording(true);       // 开始录音并向 ASR 发送数据
 *   // ...
 *   asr.setRecording(false);      // 暂停录音
 *   asr.stop();
 */
#pragma once

#include "ai_sdk/types/voice_assistant.h"
#include "ai_sdk/audio/audio_codec.h"
#include <functional>
#include <string>
#include <memory>

namespace ai_sdk {

// 前向声明
class AIAssistantManager;

/**
 * @brief ASR 智能对话管理类（单例模式）
 *
 * 核心功能：
 * - 管理 WebSocket 连接生命周期（连接、断开、重连）
 * - 协调音频数据流（接收、发送、缓冲）
 * - 解析 ASR 和对话响应结果
 * - 处理错误和异常情况
 *
 * 设计说明：
 * - 单例模式，通过 AIAssistantManager::asrIntelligentDialogueHelp() 获取
 * - 使用 PIMPL 模式隐藏实现细节
 * - 回调函数在独立任务上下文中执行，需注意线程安全
 */
class AsrIntelligentDialogue {
public:
    // =========================================================================
    // 回调类型定义
    // =========================================================================

    /**
     * 连接成功回调函数类型
     * 当 WebSocket 连接成功建立时调用。
     */
    using ConnectedCallback = std::function<void()>;

    /**
     * ASR 识别结果回调函数类型
     * 当收到语音识别结果时调用。
     * 通过 AsrResult.is_final 区分中间结果和最终结果。
     */
    using AsrCallback = std::function<void(const AsrResult&)>;

    /**
     * 智能对话结果回调函数类型
     * 当收到 AI 助手的对话响应时调用。
     */
    using DialogueCallback = std::function<void(const DialogueResult&)>;

    /**
     * 错误回调函数类型
     * 当发生错误时调用。
     *
     * 参数 code: 错误码
     * 参数 message: 错误描述
     */
    using ErrorCallback = std::function<void(int, const std::string&)>;

    /**
     * 识别完成回调函数类型
     * 当语音识别流程完成时调用（无论成功或失败）。
     */
    using CompleteCallback = std::function<void()>;

    // =========================================================================
    // 公开方法
    // =========================================================================

    /**
     * @brief 设置回调函数
     *
     * 必须在调用 start() 之前设置回调。
     * 回调函数将在独立的任务上下文中执行，需注意线程安全。
     *
     * @param connected_cb 连接成功回调（可为 nullptr）
     * @param asr_cb ASR 识别结果回调
     * @param dialogue_cb 对话结果回调
     * @param error_cb 错误回调
     * @param complete_cb 识别完成回调（可为 nullptr）
     */
    void setCallbacks(ConnectedCallback connected_cb,
                     AsrCallback asr_cb,
                     DialogueCallback dialogue_cb,
                     ErrorCallback error_cb,
                     CompleteCallback complete_cb);

    /**
     * @brief 启动 ASR 识别
     *
     * 启动流程：
     * 1. 构建 WebSocket URL（基础 URL + 参数 + 签名）
     * 2. 建立 WebSocket 连接
     * 3. 发送配置信息（采样率、格式等）
     * 4. 启动接收任务（处理服务器响应）
     * 5. 进入就绪状态，等待音频数据
     *
     * URL 构建：
     * - 基础 URL: ApiConfig::ASR_INTELLIGENT_DIALOGUE_API
     * - 参数: 设备信息 + 时间戳 + 签名
     *
     * 如果连接失败，将通过 error_cb 回调通知。
     *
     * @return bool 是否启动成功
     */
    bool start();

    /**
     * @brief 停止 ASR 识别并释放资源
     *
     * 停止流程：
     * 1. 发送结束信号（type: "finish"）
     * 2. 等待服务器响应（最多 2 秒）
     * 3. 关闭 WebSocket 连接
     * 4. 释放所有资源
     * 5. 停止接收任务
     *
     * 调用后对象回到初始状态，可再次调用 start()。
     */
    void stop();

    /**
     * @brief 发送音频数据
     *
     * 音频要求：
     * - 格式：PCM 16-bit 小端序
     * - 采样率：16000 Hz
     * - 通道数：单声道
     * - 建议块大小：5120 字节（160ms 音频）
     *
     * 内部处理：
     * - 数据缓存和分包
     * - 自动添加时间戳和序列号
     * - 发送到 WebSocket
     *
     * 线程安全：可以从任意任务调用，内部有互斥锁保护。
     *
     * @param data PCM 音频数据缓冲区
     * @param len 数据长度（字节）
     */
    void sendAudio(const uint8_t* data, size_t len);

    /**
     * @brief 检查是否已连接
     *
     * 线程安全：可以从任意任务调用。
     *
     * @return bool true 已连接，false 未连接
     */
    bool isConnected() const;

    /**
     * @brief 检查是否正在识别
     *
     * 线程安全：可以从任意任务调用。
     *
     * @return bool true 正在识别，false 未识别
     */
    bool isRecognizing() const;

    // =========================================================================
    // 音频模块接口（任务 24-25：ASR 集成层）
    // =========================================================================

    /**
     * @brief 初始化内置音频模块
     *
     * 绑定 AudioCodec，在 SDK 内部创建并管理：
     * - AudioInput：麦克风录音模块（录音 → sendAudio）
     * - TtsPlayer：TTS 播放模块（Speak 指令 → MP3 播放）
     *
     * 必须在 start() 之前调用（也可以在 start() 之后调用，但推荐之前）。
     * 调用后通过 setRecording(true) 开始录音，
     * 通过 setAutoPlayTts(true) 开启 TTS 自动播放。
     *
     * @param codec 已创建并 Start() 的 AudioCodec 实例（不拥有所有权）
     *              codec 的生命周期须长于 AsrIntelligentDialogue
     * @return true 初始化成功，false 失败（codec 为 null 或内部错误）
     */
    bool initAudio(AudioCodec* codec);

    /**
     * @brief 控制麦克风录音开关
     *
     * 控制 AudioInput 模块是否采集并发送麦克风数据：
     * - true：开始录音，PCM 数据自动通过 sendAudio() 发送到云端 ASR
     * - false：暂停录音（如 TTS 播放期间）
     *
     * 前提：已调用 initAudio() 初始化音频模块。
     * 如果未初始化，此方法无操作并打印警告日志。
     *
     * 可以在多轮对话中反复调用，不会重建任务。
     *
     * @param enable true 开始录音，false 暂停录音
     */
    void setRecording(bool enable);

    /**
     * @brief 设置是否自动播放 TTS
     *
     * 控制收到 Speak 指令时是否自动触发 TTS 播放：
     * - true：收到 Speak 指令时，SDK 自动调用 TtsPlayer::Play(url)
     * - false：收到 Speak 指令时，SDK 只通过 DialogueCallback 通知，不自动播放
     *
     * 前提：已调用 initAudio() 初始化音频模块。
     * 如果未初始化，此方法无操作并打印警告日志。
     *
     * 默认值：false（需要主动开启）
     *
     * @param enable true 自动播放，false 手动控制
     */
    void setAutoPlayTts(bool enable);

    // 禁止拷贝和赋值（单例模式）
    AsrIntelligentDialogue(const AsrIntelligentDialogue&) = delete;
    AsrIntelligentDialogue& operator=(const AsrIntelligentDialogue&) = delete;

private:
    // =========================================================================
    // 私有方法（单例模式）
    // =========================================================================

    /**
     * 获取单例实例
     *
     * 私有方法：只能通过 AIAssistantManager::asrIntelligentDialogueHelp() 访问。
     *
     * @return AsrIntelligentDialogue& 单例引用
     */
    static AsrIntelligentDialogue& getInstance();

    /**
     * 私有构造函数（单例模式）
     */
    AsrIntelligentDialogue();

    /**
     * 析构函数
     */
    ~AsrIntelligentDialogue();

    // =========================================================================
    // PIMPL 实现
    // =========================================================================

    /**
     * PIMPL 模式实现细节
     * 隐藏实现细节，减少编译依赖，提高二进制兼容性
     */
    class Impl;
    std::unique_ptr<Impl> impl_;

    // 允许 AIAssistantManager 访问私有的 getInstance() 方法
    friend class AIAssistantManager;
};

}  // namespace ai_sdk
