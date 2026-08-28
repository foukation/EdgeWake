/**
 * @file ai_sdk_protocol.h
 * @brief AI SDK 协议实现 - 连接云端 ASR 服务器
 *
 * 本文件实现了 Protocol 接口，用于与云端 ASR 服务器通信。
 * 内部封装 ai_sdk::AsrIntelligentDialogue 模块。
 *
 * 与现有协议的区别：
 * - WebsocketProtocol/MqttProtocol：连接小智私有服务器（Opus 编码）
 * - AiSdkProtocol：连接云端 ASR 服务器（PCM 格式）
 *
 * 支持两种音频管理方式（通过 Kconfig CONFIG_AI_SDK_AUTO_AUDIO 切换）：
 *
 * 【方式 A — SDK 自动管理音频】(CONFIG_AI_SDK_AUTO_AUDIO=y)
 *   调用 initAudio(codec) 后，SDK 内部管理麦克风录音（AudioInput）和 TTS 播放（TtsPlayer）。
 *   录音数据自动通过 sendAudio() 发送到云端，Speak 指令自动下载 MP3 播放。
 *   数据流：麦克风 → AudioCodec → AudioInput → sendAudio() → 云端 ASR
 *          云端 Speak → TtsPlayer → MP3 解码 → AudioCodec → 扬声器
 *
 * 【方式 B — 手动管理音频】(CONFIG_AI_SDK_AUTO_AUDIO 未定义)
 *   主项目的 AudioService 控制麦克风录音，音频通过 SendAudio() 发送。
 *   数据流：AudioProcessor → Opus 编码 → SendAudio() → Opus 解码 → PCM → sendAudio() → 云端 ASR
 *
 * 切换方式：在 menuconfig 中设置 "AI SDK auto-manage audio (Mode A)" 选项。
 *
 * @note 需要在 InitializeProtocol() 中根据配置选择使用此协议
 */

#ifndef AI_SDK_PROTOCOL_H
#define AI_SDK_PROTOCOL_H

#include "protocol.h"
#include "ai_sdk/ai_assistant_manager.h"
#include "ai_sdk/audio/audio_config.h"

#ifndef CONFIG_AI_SDK_AUTO_AUDIO
#include <opus_decoder.h>  // 方式 B：Opus 解码器（手动管理音频时使用）
#endif

#include <vector>
#include <cstdint>
#include <memory>
#include <chrono>

/**
 * @class AiSdkProtocol
 * @brief AI SDK 协议实现类
 *
 * 继承自 Protocol 基类，实现与云端 ASR 服务器的通信。
 * 将 ai_sdk::AsrDialogue 的回调转换为 Protocol 的回调格式，
 * 使得 Application 层代码无需修改即可使用不同的后端服务器。
 */
class AiSdkProtocol : public Protocol {
public:
    /**
     * @brief 构造函数
     *
     * 初始化 ai_sdk 回调，将 ASR 结果转换为 Protocol 回调格式
     */
    AiSdkProtocol();

    /**
     * @brief 析构函数
     */
    ~AiSdkProtocol() override;

    // ========================================
    // Protocol 接口实现
    // ========================================

    /**
     * @brief 启动协议
     * @return 始终返回 true（ai_sdk 不需要预启动）
     */
    bool Start() override;

    /**
     * @brief 打开音频通道
     *
     * 调用 ai_sdk::AsrDialogue::start() 建立 WebSocket 连接
     *
     * @return true 连接成功，false 连接失败
     */
    bool OpenAudioChannel() override;

    /**
     * @brief 关闭音频通道
     *
     * 发送剩余缓冲区数据，然后调用 ai_sdk::AsrDialogue::stop()
     */
    void CloseAudioChannel() override;

    /**
     * @brief 检查音频通道是否打开
     * @return true 已连接，false 未连接
     */
    bool IsAudioChannelOpened() const override;

    /**
     * @brief 发送音频数据
     *
     * 接收 Opus 编码的音频数据，解码为 PCM 后发送到云端 ASR 服务器。
     * PCM 数据会先积累到缓冲区，达到 SEND_THRESHOLD (5120) 后发送。
     *
     * 处理流程：
     * 1. 接收 Opus 编码的音频包（来自 AudioService）
     * 2. 使用 OpusDecoderWrapper 解码为 PCM（int16_t）
     * 3. 将 PCM 数据转换为字节并加入缓冲区
     * 4. 缓冲区达到阈值时发送
     *
     * @param packet Opus 编码的音频数据包
     * @return true 发送成功，false 未连接或发送失败
     */
    bool SendAudio(std::unique_ptr<AudioStreamPacket> packet) override;

    // ========================================
    // Protocol 可选覆盖方法
    // ========================================

    /**
     * @brief 发送开始监听命令
     *
     * ai_sdk 在 start() 时自动发送 Start Signal，此方法为空实现
     *
     * @param mode 监听模式（未使用）
     */
    void SendStartListening(ListeningMode mode) override;

    /**
     * @brief 发送停止监听命令
     *
     * 调用 CloseAudioChannel() 关闭连接
     */
    void SendStopListening() override;

protected:
    /**
     * @brief 发送文本消息
     *
     * ai_sdk 不需要发送 JSON 文本消息，此方法始终返回 true
     *
     * @param text 文本内容（未使用）
     * @return 始终返回 true
     */
    bool SendText(const std::string& text) override;

private:
    // AI SDK ASR 智能对话单例引用
    // 通过 AIAssistantManager::asrIntelligentDialogueHelp() 获取
    // 对应 Android: ASRIntelligentDialogue
    ai_sdk::AsrIntelligentDialogue& asr_;

    // ========================================
    // Opus 解码器 + PCM 缓冲（方式 B：手动管理音频时使用）
    // ========================================
#ifndef CONFIG_AI_SDK_AUTO_AUDIO
    //
    // 由于 ESP32 的 Opus 编码帧时长（20ms）与服务器期望的帧时长（10ms）不匹配，
    // 导致服务器解码失败。改为发送 PCM 数据可避免此问题。
    //
    // 处理流程（方式 B）：
    // 1. AudioService 使用 OpusEncoderWrapper 编码音频为 Opus
    // 2. AiSdkProtocol 收到 Opus 数据后，使用此解码器解码回 PCM
    // 3. 将 PCM 数据发送到服务器
    //
    std::unique_ptr<OpusDecoderWrapper> opus_decoder_;

    //
    // PCM 音频缓冲策略：
    // 与 Android DealSotaOne.sendAudioMicrophoneData() 模式一致：
    // - 积累 PCM 数据到 CHUNK_SIZE (5120 字节) 后发送
    // - 5120 字节 = 160ms @ 16kHz 16bit mono
    //
    std::vector<uint8_t> audio_buffer_;

    // 发送阈值：与 Android CHUNK_SIZE 保持一致
    // 5120 字节 = 160ms @ 16kHz 16bit mono = 2560 samples × 2 bytes/sample
    static constexpr size_t SEND_THRESHOLD = 5120;
#endif  // !CONFIG_AI_SDK_AUTO_AUDIO

    // ========================================
    // SDK 音频驱动实例（方式 A：SDK 自动管理音频时使用）
    // ========================================
    //
    // 由 CreateAudioCodec() 工厂函数创建，用于 SDK 内部的 AudioInput 和 TtsPlayer。
    // 调用 asr_.initAudio(codec_) 后，SDK 内部自动管理录音和 TTS 播放。
    // 方式 B 时为 nullptr，不影响现有逻辑。
    // ========================================
    ai_sdk::AudioCodec* codec_ = nullptr;

    // ========================================
    // 超时和错误状态管理
    // ========================================

    // 上次收到服务器消息的时间（用于超时判断）
    mutable std::chrono::steady_clock::time_point last_incoming_time_;

    // 是否发生过错误
    bool error_occurred_ = false;

    // 超时阈值（毫秒）
    static constexpr int TIMEOUT_MS = 15000;

    /**
     * @brief 检查是否超时
     * @return true 超时，false 未超时
     */
    bool IsTimeout() const;

    /**
     * @brief 设置 ai_sdk 回调
     *
     * 将 ai_sdk 的回调转换为 Protocol 的回调格式：
     * - onConnected → on_audio_channel_opened_
     * - onAsrResult → on_incoming_json_ (type: "stt")
     * - onDialogueResult → on_incoming_json_ (根据 directive 类型)
     * - onError → on_network_error_
     * - onComplete → on_audio_channel_closed_
     */
    void SetupAsrCallbacks();

#ifdef CONFIG_AI_SDK_AUTO_AUDIO
    /**
     * @brief 创建音频硬件配置
     *
     * 从板子的 config.h 宏定义转换为 SDK 的 AudioConfig。
     * 包含引脚配置、采样率、硬件类型、I2C 句柄等。
     *
     * @return ai_sdk::AudioConfig 填充好的音频配置结构体
     *
     * @note TODO: 根据实际板子硬件填写配置，当前为空壳实现
     */
    ai_sdk::AudioConfig CreateAudioConfig();
#endif
};

#endif // AI_SDK_PROTOCOL_H

