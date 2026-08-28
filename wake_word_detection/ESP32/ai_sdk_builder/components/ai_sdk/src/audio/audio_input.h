/**
 * @file audio_input.h
 * @brief 麦克风录音模块（SDK 内部头文件）
 *
 * ============================================================================
 * ⚠️ 内部头文件 - 请勿在 SDK 外部使用
 * ============================================================================
 *
 * 此文件是 AI SDK 的内部实现细节，不属于公开 API。
 * 上层厂商代码不应直接引用此头文件。
 * 录音功能由 AsrIntelligentDialogue 在内部管理，
 * 厂商通过 asr.setRecording(true/false) 间接控制。
 *
 * ============================================================================
 *
 * 提供麦克风音频数据采集功能，内部完成以下处理：
 * 1. 从 AudioCodec 读取原始 PCM 数据
 * 2. 重采样至 16kHz（如果硬件采样率不同）
 * 3. 多声道转单声道（如果硬件非单声道）
 * 4. 累积到指定字节数后，通过回调将 PCM 数据交给上层
 *
 * 输出格式：PCM 16-bit 小端序，16kHz，单声道
 *
 * @version 1.0.0
 * @date 2026-03-09
 */

#ifndef AI_SDK_AUDIO_INPUT_H
#define AI_SDK_AUDIO_INPUT_H

#include <functional>
#include <memory>

namespace ai_sdk {

// 前向声明
class AudioCodec;

/**
 * @brief 麦克风录音模块
 *
 * 从 AudioCodec 采集原始 PCM 数据，经过重采样和声道转换后，
 * 以 PCM 16-bit/16kHz/Mono 格式输出，供 ASR 发送到服务器。
 *
 * 设计说明：
 * - 使用 FreeRTOS 独立任务进行录音，不阻塞调用者
 * - 使用 EventGroup 控制录音开关，暂停时零 CPU 开销
 * - 使用 PIMPL 模式隐藏实现细节
 * - 重采样器延迟初始化，不需要时不占用内存
 * - 由 AsrIntelligentDialogue 创建和管理
 */
class AudioInput {
public:
    /**
     * @brief 音频数据回调类型
     *
     * 当累积到足够的 PCM 数据后调用此回调。
     *
     * @param data PCM 数据指针（16-bit 小端序，16kHz，单声道）
     * @param len  数据长度（字节），通常为 5120 字节（160ms）
     *
     * 注意事项：
     * - 回调在录音任务上下文中执行
     * - 不要在回调中做耗时操作（会阻塞录音）
     * - 调用 sendAudio() 是安全的（sendAudio 内部有互斥锁）
     * - data 指针仅在回调期间有效，不要保存
     */
    using AudioDataCallback = std::function<void(const uint8_t* data, size_t len)>;

    AudioInput();
    ~AudioInput();

    /**
     * @brief 初始化录音模块
     *
     * 绑定音频硬件驱动，读取采样率和声道信息，
     * 判断是否需要重采样和声道转换。
     *
     * @param codec 已创建并 Start() 的 AudioCodec 实例（不拥有所有权）
     * @return true 初始化成功，false 参数无效
     *
     * 注意：codec 的生命周期必须长于 AudioInput
     */
    bool Initialize(AudioCodec* codec);

    /**
     * @brief 启动录音任务
     *
     * 创建 FreeRTOS 任务，但不立即开始采集数据。
     * 需要调用 SetRecording(true) 才会开始采集。
     *
     * @return true 任务创建成功，false 创建失败（内存不足等）
     */
    bool Start();

    /**
     * @brief 停止录音任务
     *
     * 停止 FreeRTOS 任务并释放所有资源（重采样器、缓冲区等）。
     * 调用后可以再次 Start() 重新启动。
     */
    void Stop();

    /**
     * @brief 设置录音开关
     *
     * 控制是否采集麦克风数据：
     * - true:  开始采集，数据通过回调输出
     * - false: 暂停采集，录音任务挂起，不消耗 CPU
     *
     * 此方法不会创建或销毁任务，只是控制数据采集的开关。
     * 可以在多轮对话中反复调用。
     *
     * @param enable true 开始录音，false 暂停录音
     */
    void SetRecording(bool enable);

    /**
     * @brief 查询是否正在录音
     *
     * @return true 正在采集数据，false 已暂停或未启动
     */
    bool IsRecording() const;

    /**
     * @brief 设置音频数据回调
     *
     * 必须在 Start() 之前设置，否则采集的数据无处输出。
     *
     * @param callback 音频数据回调函数
     */
    void SetAudioDataCallback(AudioDataCallback callback);

    // 禁止拷贝和赋值
    AudioInput(const AudioInput&) = delete;
    AudioInput& operator=(const AudioInput&) = delete;

private:
    class Impl;
    std::unique_ptr<Impl> impl_;
};

} // namespace ai_sdk

#endif // AI_SDK_AUDIO_INPUT_H
