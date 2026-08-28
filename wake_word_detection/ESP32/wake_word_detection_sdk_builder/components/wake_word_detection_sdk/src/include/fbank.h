/* Copyright (c) 2017 Personal (Binbin Zhang), adapted for ESP-IDF
 * Fbank implementation based on wekws/runtime/core/frontend/fbank.h
 * Embedded-friendly: no std::vector, fixed buffers
 */

#ifndef FBANK_H_
#define FBANK_H_

#include <cstddef>
#include <cstdint>

#include "micro_model_settings.h"

// Fbank: wekws-style filter bank, outputs float（无量化）
// Config: num_bins=40, frame_length=25ms, frame_shift=10ms, sample_rate=16000
class Fbank {
 public:
  Fbank();
  ~Fbank() = default;

  // Compute fbank from int16 PCM. Output: float [num_frames][num_bins]
  // Returns number of frames produced (max kFeatureCount).
  // feat_output: row-major [frame][bin], size >= num_frames * kFeatureSize
  int Compute(const int16_t* wave, size_t num_samples,
              float* feat_output, int max_frames = kFeatureCount);

 private:
  static int UpperPowerOfTwo(int n);
  static float MelScale(float freq);
  static float InverseMelScale(float mel_freq);
  static float GenerateGaussianNoise();  // Box-Muller，用于 dither
  void PreEmphasis(float coeff, float* data, int len);
  void Hamming(float* data);

  int num_bins_;
  int sample_rate_;
  int frame_length_;
  int frame_shift_;
  int fft_points_;
  int num_fft_bins_;

  // Mel filter bank: for each bin, first_index and weights
  static constexpr int kMaxBinSize = 64;
  int bins_first_[kFeatureSize];
  int bins_size_[kFeatureSize];
  float bins_weight_[kFeatureSize][kMaxBinSize];

  float hamming_window_[kMaxAudioSampleSize];
  int bitrev_[512];
  float sintbl_[512 + 128];
};

#endif  // FBANK_H_
