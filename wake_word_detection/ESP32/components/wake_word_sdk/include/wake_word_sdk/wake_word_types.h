/**
 * @file wake_word_types.h
 * @brief 语音唤醒 SDK 类型定义
 * 
 * 本文件定义了语音唤醒 SDK 所需的配置结构体和类型。
 * 这些类型用于替代原项目中对 AudioCodec 和 Assets 的依赖。
 */

#ifndef WAKE_WORD_TYPES_H
#define WAKE_WORD_TYPES_H

#include <functional>
#include <cstddef>

namespace wake_word_sdk {

/**
 * @brief 音频配置结构体
 * 
 * 用于替代原来的 AudioCodec* 参数，只包含唤醒词模块需要的音频配置信息。
 */
struct WakeWordAudioConfig {
    int sample_rate = 16000;        ///< 采样率，默认 16kHz
    int input_channels = 1;         ///< 输入声道数，默认单声道
    bool has_reference = false;     ///< 是否有参考通道（用于回声消除 AEC）
};

/**
 * @brief 读取资源文件的回调函数类型
 * 
 * 用于替代原来的 Assets::GetInstance().GetAssetData() 调用。
 * 调用者需要提供此回调函数来读取配置文件（如 index.json）。
 * 
 * @param path 资源文件路径
 * @param data 输出参数，指向读取到的数据
 * @param size 输出参数，数据大小
 * @return 成功返回 true，失败返回 false
 */
using ReadAssetFunc = std::function<bool(const char* path, void** data, size_t* size)>;

/**
 * @brief 唤醒词完整配置结构体
 * 
 * 包含音频配置和可选的资源读取回调。
 */
struct WakeWordConfig {
    WakeWordAudioConfig audio;      ///< 音频配置
    ReadAssetFunc read_asset;       ///< 读取资源的回调函数（可选，用于读取 index.json）
};

/**
 * @brief Opus 帧时长常量（毫秒）
 * 
 * 原来定义在 audio_service.h 中，现在移到 SDK 内部。
 * 用于唤醒词音频的 Opus 编码。
 */
constexpr int OPUS_FRAME_DURATION_MS = 20;

}  // namespace wake_word_sdk

#endif // WAKE_WORD_TYPES_H
