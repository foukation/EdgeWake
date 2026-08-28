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

#ifndef TENSORFLOW_LITE_MICRO_EXAMPLES_MICRO_SPEECH_FEATURE_PIPELINE_H_
#define TENSORFLOW_LITE_MICRO_EXAMPLES_MICRO_SPEECH_FEATURE_PIPELINE_H_

#include "tensorflow/lite/c/common.h"

// FeaturePipeline: 基于 wekws feature_pipeline 逻辑的特征生成类
// 采用流式处理：AcceptWaveform 累积音频，按帧生成特征，保留 remained_wav 残差
// 接口与 FeatureProvider 兼容，可直接替换
class FeaturePipeline {
 public:
  FeaturePipeline(int feature_size, float* feature_data);
  ~FeaturePipeline();

  // wekws 风格接口：追加音频波形（用于流式输入）
  // 返回 true 表示已生成新特征并更新 feature_data_
  bool AcceptWaveform(const int16_t* wav, size_t wav_len);

  // 重置内部状态
  void Reset();

 private:
  int feature_size_;
  float* feature_data_;

  // wekws 风格：上一帧未消耗完的音频残差
  static constexpr size_t kRemainedWavMaxSize = 1024;
  int16_t remained_wav_[kRemainedWavMaxSize];
  size_t remained_wav_len_;
};

#endif  // TENSORFLOW_LITE_MICRO_EXAMPLES_MICRO_SPEECH_FEATURE_PIPELINE_H_
