/**
 * @file tts_player.h
 * @brief TTS 语音播放模块（SDK 内部头文件）
 *
 * ============================================================================
 * ⚠️ 内部头文件 - 请勿在 SDK 外部使用
 * ============================================================================
 *
 * 此文件是 AI SDK 的内部实现细节，不属于公开 API。
 * 上层厂商代码不应直接引用此头文件。
 * TTS 播放由 AsrIntelligentDialogue 在内部管理：
 * - 解析 DialogueResult 中的 Speak 指令
 * - 自动提取 URL 并调用 TtsPlayer::Play()
 * - 厂商通过 asr.setAutoPlayTts(true/false) 控制是否自动播放
 *
 * ============================================================================
 *
 * 提供完整的 TTS 播放功能，从 HTTP URL 下载 MP3 音频并实时播放。
 * 内部完成以下处理流水线：
 * 1. HTTP 流式下载 MP3 数据
 * 2. MP3 解码为 PCM（使用 IAudioDecoder）
 * 3. 重采样至硬件输出采样率（如果需要）
 * 4. 通过 AudioCodec 输出到扬声器
 *
 * 设计说明：
 * - 每次 Play() 调用会中断上一次播放并开始新的播放
 * - 使用独立的 FreeRTOS 任务执行下载和播放
 * - HTTP 下载和 MP3 解码在同一任务中串行处理（减少内存占用）
 * - 重采样逻辑内嵌在播放流水线中（方案 A，不单独封装）
 * - 使用 PIMPL 模式隐藏实现细节
 * - 由 AsrIntelligentDialogue 创建和管理
 *
 * @version 1.0.0
 * @date 2026-03-10
 */

#ifndef AI_SDK_TTS_PLAYER_H
#define AI_SDK_TTS_PLAYER_H

#include <functional>
#include <memory>
#include <string>

namespace ai_sdk {

// 前向声明
class AudioCodec;

/**
 * @brief TTS 播放状态
 */
enum class TtsPlayState {
    kIdle,       ///< 空闲，未播放
    kPlaying,    ///< 正在播放
    kError,      ///< 播放出错
};

/**
 * @brief TTS 语音播放模块
 *
 * 从 HTTP URL 下载 MP3 音频，解码并播放到扬声器。
 *
 * 设计说明：
 * - 使用 FreeRTOS 独立任务执行播放流水线
 * - 单任务串行处理：HTTP 下载 → MP3 解码 → 重采样 → 输出
 * - 使用 PIMPL 模式隐藏实现细节
 * - Play() 是非阻塞的，立即返回
 * - 由 AsrIntelligentDialogue 创建和管理
 */
class TtsPlayer {
public:
    /**
     * @brief 播放状态变更回调类型
     *
     * 当播放状态发生变化时调用此回调。
     *
     * @param state 新的播放状态
     *
     * 注意事项：
     * - 回调在播放任务上下文中执行
     * - 不要在回调中做耗时操作
     */
    using StateCallback = std::function<void(TtsPlayState state)>;

    /**
     * @brief 播放完成回调类型
     *
     * 当一个 URL 片段播放完成（正常结束或被中断）时调用。
     *
     * @param url       当前完成的 URL
     * @param completed true 正常播完，false 被中断或出错
     * @param all_done  true 表示当前批次所有已排队 URL 均已结束；
     *                  false 表示队列中仍有后续 URL 待播
     */
    using CompletionCallback = std::function<void(const std::string& url,
                                                  bool completed,
                                                  bool all_done)>;

    TtsPlayer();
    ~TtsPlayer();

    /**
     * @brief 初始化 TTS 播放器
     *
     * 绑定音频硬件驱动，读取输出采样率信息。
     *
     * @param codec 已创建并 Start() 的 AudioCodec 实例（不拥有所有权）
     * @return true 初始化成功，false 参数无效
     *
     * 注意：codec 的生命周期必须长于 TtsPlayer
     */
    bool Initialize(AudioCodec* codec);

    /**
     * @brief 播放 TTS 音频
     *
     * 从指定 URL 下载 MP3 音频并播放。
     * 如果当前正在播放，会先中断上一次播放。
     * 此方法是非阻塞的，立即返回。
     *
     * @param url MP3 音频的 HTTP/HTTPS URL
     * @return true 播放任务启动成功，false 未初始化或 URL 为空
     */
    bool Play(const std::string& url);

    /**
     * @brief 停止当前播放
     *
     * 中断正在进行的 HTTP 下载和音频播放。
     * 如果当前未在播放，此方法无操作。
     */
    void Stop();

    /**
     * @brief 查询当前播放状态
     *
     * @return 当前播放状态
     */
    TtsPlayState GetState() const;

    /**
     * @brief 查询是否正在播放
     *
     * @return true 正在播放，false 空闲或出错
     */
    bool IsPlaying() const;

    /**
     * @brief 设置播放状态变更回调
     *
     * @param callback 状态变更回调函数
     */
    void SetStateCallback(StateCallback callback);

    /**
     * @brief 设置播放完成回调
     *
     * @param callback 播放完成回调函数
     */
    void SetCompletionCallback(CompletionCallback callback);

    /**
     * @brief 设置输出音量
     *
     * 通过 AudioCodec 设置扬声器音量。
     *
     * @param volume 音量值 (0-100)
     */
    void SetVolume(int volume);

    // 禁止拷贝和赋值
    TtsPlayer(const TtsPlayer&) = delete;
    TtsPlayer& operator=(const TtsPlayer&) = delete;

private:
    class Impl;
    std::unique_ptr<Impl> impl_;
};

} // namespace ai_sdk

#endif // AI_SDK_TTS_PLAYER_H
