/* Copyright (c) 2017 Personal (Binbin Zhang), adapted for ESP-IDF
 * Fbank implementation based on wekws/runtime/core/frontend/fbank.h
 */

#include "fbank.h"
#include "fft.h"

#include <cmath>
#include <cstdlib>
#include <cstring>
#include <limits>

#ifndef M_PI
#define M_PI 3.1415926535897932384626433832795
#endif

Fbank::Fbank()
    : num_bins_(kFeatureSize),
      sample_rate_(kAudioSampleFrequency),
      frame_length_(kFeatureDurationMs * kAudioSampleFrequency / 1000),
      frame_shift_(kFeatureStrideMs * kAudioSampleFrequency / 1000),
      fft_points_(UpperPowerOfTwo(frame_length_)),
      num_fft_bins_(fft_points_ / 2) {
  // FFT tables
  fft_make_sintbl(fft_points_, sintbl_);
  fft_make_bitrev(fft_points_, bitrev_);

  // Hamming window: 0.54 - 0.46 * cos(2*pi*n/(frame_size-1))
  double a = 2.0 * M_PI / (frame_length_ - 1);
  for (int i = 0; i < frame_length_; i++) {
    hamming_window_[i] = static_cast<float>(0.54 - 0.46 * cos(a * i));
  }

  // Mel filter bank: Python FbankExtractor 风格
  // mel_points = linspace(low_mel, high_mel, num_bins+2)
  // bin_points = floor(fft_size * hz_points / sr)
  // 三角滤波器: [start,center) 上升, [center,end) 下降
  float low_mel = MelScale(20.0f);
  float high_mel = MelScale(static_cast<float>(sample_rate_) / 2.0f);
  int bin_points[kFeatureSize + 2];
  for (int i = 0; i < num_bins_ + 2; i++) {
    float mel = low_mel + (high_mel - low_mel) * i / (num_bins_ + 1);
    float hz = InverseMelScale(mel);
    int b = static_cast<int>(floorf(fft_points_ * hz / sample_rate_));
    bin_points[i] = (b >= num_fft_bins_) ? (num_fft_bins_ - 1) : b;
  }

  for (int i = 0; i < num_bins_; i++) {
    int start = bin_points[i];
    int center = bin_points[i + 1];
    int end = bin_points[i + 2];
    if (end > num_fft_bins_) end = num_fft_bins_;

    bins_first_[i] = start;
    int idx = 0;
    for (int k = start; k < end && idx < kMaxBinSize; k++) {
      float w = 0.0f;
      if (center > start && k < center) {
        w = static_cast<float>(k - start) / (center - start);
      } else if (end > center && k >= center) {
        w = static_cast<float>(end - k) / (end - center);
      }
      bins_weight_[i][idx++] = w;
    }
    bins_size_[i] = idx;
  }
}

int Fbank::Compute(const int16_t* wave, size_t num_samples,
                   float* feat_output, int max_frames) {
  if (wave == nullptr || feat_output == nullptr || num_samples < static_cast<size_t>(frame_length_))
    return 0;

  int num_frames = 1 + (num_samples - frame_length_) / frame_shift_;
  if (num_frames > max_frames) num_frames = max_frames;
  if (num_frames < 1) return 0;

  float fft_real[512];
  float fft_img[512];
  float power[256];
  float frame_data[400];

  for (int i = 0; i < num_frames; i++) {
    const int16_t* src = wave + i * frame_shift_;
    for (int j = 0; j < frame_length_; j++)
      frame_data[j] = static_cast<float>(src[j]);

    // Dither（可选，与 Python config.get("DITHER", 0.0) 对齐）
    if (kFbankDither > 0.0f) {
      for (int j = 0; j < frame_length_; j++)
        frame_data[j] += GenerateGaussianNoise() * kFbankDither;
    }

    // Remove DC offset
    float mean = 0;
    for (int j = 0; j < frame_length_; j++) mean += frame_data[j];
    mean /= frame_length_;
    for (int j = 0; j < frame_length_; j++) frame_data[j] -= mean;

    // Pre-emphasis（与 Python config.get("PREEMPH_COEFFICIENT", 0.97) 对齐）
    PreEmphasis(kFbankPreemphCoeff, frame_data, frame_length_);
    Hamming(frame_data);

    // FFT
    std::memset(fft_img, 0, sizeof(fft_img));
    std::memset(fft_real + frame_length_, 0,
                sizeof(float) * (fft_points_ - frame_length_));
    std::memcpy(fft_real, frame_data, sizeof(float) * frame_length_);
    fft_compute(bitrev_, sintbl_, fft_real, fft_img, fft_points_);

    // Power spectrum
    for (int j = 0; j < num_fft_bins_; j++)
      power[j] = fft_real[j] * fft_real[j] + fft_img[j] * fft_img[j];

    // Mel filter bank + log（与 Python log(maximum(mel_energies, 1.2e-7)) 对齐）
    float* out_row = feat_output + i * kFeatureSize;
    for (int j = 0; j < num_bins_; j++) {
      float mel_energy = 0;
      int s = bins_first_[j];
      int sz = bins_size_[j];
      for (int k = 0; k < sz; k++)
        mel_energy += bins_weight_[j][k] * power[s + k];

      if (mel_energy < kFbankLogEpsilon) mel_energy = kFbankLogEpsilon;
      out_row[j] = logf(mel_energy);
    }
  }
  return num_frames;
}

int Fbank::UpperPowerOfTwo(int n) {
  return static_cast<int>(pow(2, ceil(log(n) / log(2))));
}

float Fbank::MelScale(float freq) {
  return 1127.0f * logf(1.0f + freq / 700.0f);
}

float Fbank::InverseMelScale(float mel_freq) {
  return 700.0f * (expf(mel_freq / 1127.0f) - 1.0f);
}

float Fbank::GenerateGaussianNoise() {
  const float rmax = static_cast<float>(RAND_MAX) + 1.0f;
  float u1, u2;
  do {
    u1 = static_cast<float>(rand()) / rmax;
  } while (u1 <= 0.0f);
  do {
    u2 = static_cast<float>(rand()) / rmax;
  } while (u2 <= 0.0f);
  return sqrtf(-2.0f * logf(u1)) * cosf(2.0f * static_cast<float>(M_PI) * u2);
}

void Fbank::PreEmphasis(float coeff, float* data, int len) {
  if (coeff == 0) return;
  for (int i = len - 1; i > 0; i--)
    data[i] -= coeff * data[i - 1];
  data[0] -= coeff * data[0];
}

void Fbank::Hamming(float* data) {
  for (int i = 0; i < frame_length_; i++)
    data[i] *= hamming_window_[i];
}
