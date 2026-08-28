/* Copyright 2018 The TensorFlow Authors. All Rights Reserved.
 * Feature pipeline logic adapted from wekws/runtime/core/frontend/feature_pipeline.cc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
==============================================================================*/

#include "feature_pipeline.h"

#include <algorithm>
#include <cstring>

#include "fbank.h"
#include "micro_model_settings.h"
#include "tensorflow/lite/micro/micro_log.h"

// 每帧所需采样数、帧移采样数（与 wekws frame_length/frame_shift 对应）
static constexpr int kAudioSampleDurationCount =
    kFeatureDurationMs * kAudioSampleFrequency / 1000;
static constexpr int kAudioSampleStrideCount =
    kFeatureStrideMs * kAudioSampleFrequency / 1000;

// 生成 kFeatureCount 帧所需的最小采样数（单次推理窗口）
static constexpr size_t kMinSamplesForFeatures =
    kAudioSampleDurationCount + (kFeatureCount - 1) * kAudioSampleStrideCount;

static Fbank g_fbank;

FeaturePipeline::FeaturePipeline(int feature_size, float* feature_data)
    : feature_size_(feature_size),
      feature_data_(feature_data),
      remained_wav_len_(0) {
  std::memset(remained_wav_, 0, sizeof(remained_wav_));
  for (int n = 0; n < feature_size_; ++n) {
    feature_data_[n] = 0.0f;
  }
}

FeaturePipeline::~FeaturePipeline() {}

void FeaturePipeline::Reset() {
  remained_wav_len_ = 0;
  std::memset(remained_wav_, 0, sizeof(remained_wav_));
}

bool FeaturePipeline::AcceptWaveform(const int16_t* wav, size_t wav_len) {
  if (wav == nullptr || wav_len == 0) return false;

  // 合并 remained_wav 与新区块（可以应对大块特征数据，比如一下子传入16000采样）
  size_t total_len = remained_wav_len_ + wav_len;

  // 动态分配 combined 缓冲区，避免大量数据导致栈溢出
  int16_t* combined = new int16_t[total_len];
  if (!combined) {
    MicroPrintf("FeaturePipeline: failed to allocate combined buffer");
    remained_wav_len_ = 0;
    return false;
  }

  std::memcpy(combined, remained_wav_, remained_wav_len_ * sizeof(int16_t));
  std::memcpy(combined + remained_wav_len_, wav, wav_len * sizeof(int16_t));

  // 若有足够采样，调用 fbank.Compute（与 wekws fbank 一致）
  bool produced = false;
  if (total_len >= kMinSamplesForFeatures) {
    int num_frames = g_fbank.Compute(combined, total_len, feature_data_,
                                     kFeatureCount);
    if (num_frames > 0) {
      // 若帧数不足 kFeatureCount，用 0 填充剩余帧
      for (int i = num_frames; i < kFeatureCount; ++i) {
        std::memset(feature_data_ + i * kFeatureSize, 0,
                    kFeatureSize * sizeof(float));
      }
      produced = true;
    }

    // 保留未消耗的残差（与 wekws 一致：left_samples = waves.size() - frame_shift * num_frames）
    // wekws fbank: num_frames = 1 + (num_samples - frame_length) / frame_shift
    int num_frames_residual = 1 + (total_len - kAudioSampleDurationCount) / kAudioSampleStrideCount;
    if (num_frames_residual > kFeatureCount) num_frames_residual = kFeatureCount;
    if (num_frames_residual < 1) num_frames_residual = 0;

    // wekws 公式：consumed = frame_shift * num_frames，保留从 consumed 到末尾的采样
    size_t consumed = (num_frames_residual > 0)
        ? (static_cast<size_t>(num_frames_residual) * kAudioSampleStrideCount)
        : 0;
    size_t left_samples = (consumed < total_len) ? (total_len - consumed) : 0;

    remained_wav_len_ = std::min(left_samples, static_cast<size_t>(kRemainedWavMaxSize));
    if (remained_wav_len_ > 0 && total_len >= remained_wav_len_) {
      std::memcpy(remained_wav_,
                  combined + (total_len - remained_wav_len_),
                  remained_wav_len_ * sizeof(int16_t));
    } else {
      remained_wav_len_ = 0;
    }
  } else {
    // 不足一帧，全部保留到 remained_wav
    remained_wav_len_ = std::min(total_len, static_cast<size_t>(kRemainedWavMaxSize));
    std::memcpy(remained_wav_, 
                combined + (total_len - remained_wav_len_), 
                remained_wav_len_ * sizeof(int16_t));
  }
  
  delete[] combined;
  return produced;
}
