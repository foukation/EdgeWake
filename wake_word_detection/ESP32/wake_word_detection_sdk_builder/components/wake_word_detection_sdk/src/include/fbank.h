/* Copyright (c) 2017 Personal (Binbin Zhang), adapted for ESP-IDF
 * Fbank implementation based on wekws/runtime/core/frontend/fbank.h
 * Embedded-friendly: no std::vector, fixed buffers
 */

// =============================================================================
// fbank.h —— 内部私有头（非对外 API）
// -----------------------------------------------------------------------------
// 作用：将 int16 PCM 提取为 log-Mel filterbank（fbank）特征，作为模型输入的
//       前端特征提取器。实现对齐 wekws 及其 Python FbankExtractor，保证端侧
//       特征与训练侧一致。
// 链路位置：PCM → Fbank(本文件) → TFLite 模型 Invoke → 逐帧概率 → 判决 → 命中
// 参数来源：帧长/帧移/滤波器数等全部取自 micro_model_settings.h（单一事实来源）。
// =============================================================================

#ifndef FBANK_H_
#define FBANK_H_

#include <cstddef>
#include <cstdint>

// kFeatureSize / kFeatureCount / kMaxAudioSampleSize / kAudioSampleFrequency /
// kFeatureDurationMs / kFeatureStrideMs / kFbankDither / kFbankPreemphCoeff /
// kFbankLogEpsilon 等参数常量的单一事实来源。
#include "micro_model_settings.h"

// wekws 风格 filter bank，输出 log-Mel 特征（float，不量化）。
// 注：下方注释中的 num_bins=40 / frame_length=25ms / frame_shift=10ms /
// sample_rate=16000 为「当前 settings 配置下的取值」，实际均由
// micro_model_settings.h 常量在构造时算出，改配置以 settings 为准。
class Fbank {
 public:
  // 构造即预计算（较重，只需一次）：FFT 正弦表/位反转表、Hamming 窗、
  // Mel 三角滤波器组权重。之后可对多段音频反复调用 Compute()。
  Fbank();
  ~Fbank() = default;  // 无动态资源

  // 从 int16 PCM 计算 fbank 特征。
  //   wave        : 输入 PCM（16kHz/16bit/单声道）
  //   num_samples : 采样点数；不足一帧(frame_length)或空指针 → 返回 0
  //   feat_output : 行主序输出 [frame][bin]，容量须 >= num_frames * kFeatureSize
  //   max_frames  : 输出帧数上限（默认 kFeatureCount）
  // 返回：实际产生的帧数 = min(1 + (num_samples-frame_length)/frame_shift, max_frames)
  // 每帧流水：(可选dither) → 去直流 → 预加重 → Hamming → FFT → 功率谱
  //           → Mel 滤波累加 → log(max(energy, ε))
  int Compute(const int16_t* wave, size_t num_samples,
              float* feat_output, int max_frames = kFeatureCount);

 private:
  static int UpperPowerOfTwo(int n);        // ≥n 的最小 2 的幂（定 FFT 长度）
  static float MelScale(float freq);        // Hz→Mel：1127*ln(1+f/700)
  static float InverseMelScale(float mel_freq);  // Mel→Hz 逆变换
  static float GenerateGaussianNoise();     // Box-Muller，用于 dither
  void PreEmphasis(float coeff, float* data, int len);  // 预加重高频提升
  void Hamming(float* data);                // 逐点乘 Hamming 窗

  // —— 配置（构造时由 micro_model_settings.h 常量算出）——
  int num_bins_;      // Mel 滤波器数（= kFeatureSize）
  int sample_rate_;   // 采样率（= kAudioSampleFrequency）
  int frame_length_;  // 帧长（采样点）= kFeatureDurationMs*sr/1000
  int frame_shift_;   // 帧移（采样点）= kFeatureStrideMs*sr/1000
  int fft_points_;    // FFT 点数 = UpperPowerOfTwo(frame_length_)
  int num_fft_bins_;  // 单边谱点数 = fft_points_/2

  // Mel 三角滤波器组：每个 bin 记录起始 FFT 下标、跨度、三角权重。
  static constexpr int kMaxBinSize = 64;    // 单个 mel 三角覆盖的最大 FFT bin 数
  int bins_first_[kFeatureSize];            // 各 bin 在功率谱中的起始下标
  int bins_size_[kFeatureSize];             // 各 bin 覆盖的 FFT bin 数
  float bins_weight_[kFeatureSize][kMaxBinSize];  // 各 bin 的三角权重

  float hamming_window_[kMaxAudioSampleSize];  // 预算 Hamming 窗（长度上限 400）

  // ⚠️ 固定尺寸 512：隐含假设 fft_points_ <= 512（即 frame_length=400@16k → 512）。
  //    fbank.cc 内 fft_real[512]/fft_img[512]/power[256]/frame_data[400] 同样写死。
  //    若 settings 改大帧长使 fft_points_ 超 512，这些缓冲会溢出，须同步调整。
  int bitrev_[512];        // FFT 位反转表
  float sintbl_[512 + 128];  // FFT 正弦表（额外 128 为算法查表余量）
};

#endif  // FBANK_H_
