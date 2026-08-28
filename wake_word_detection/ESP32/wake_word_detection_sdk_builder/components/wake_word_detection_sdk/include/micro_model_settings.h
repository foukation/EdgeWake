/* Copyright 2023 The TensorFlow Authors. All Rights Reserved.
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

#ifndef TENSORFLOW_LITE_MICRO_EXAMPLES_MICRO_SPEECH_MICRO_MODEL_SETTINGS_H_
#define TENSORFLOW_LITE_MICRO_EXAMPLES_MICRO_SPEECH_MICRO_MODEL_SETTINGS_H_

// 以下参数与训练配置 dataset_conf.fbank_conf 一致
constexpr int kAudioSampleFrequency = 16000; // resample_conf.resample_rate
constexpr int kFeatureSize = 40;             // fbank_conf.num_mel_bins
constexpr int kFeatureStrideMs = 10;         // fbank_conf.frame_shift
constexpr int kFeatureDurationMs = 25;       // fbank_conf.frame_length
// 模型输入 [1, 40, 40] → 40 帧 × 40 维 fbank
constexpr int kFeatureCount = 40;
constexpr int kFeatureElementCount = (kFeatureSize * kFeatureCount);

// 单帧特征所需的最大采样数
constexpr int kMaxAudioSampleSize =
    kFeatureDurationMs * kAudioSampleFrequency / 1000;

// WAV 分块处理参数：加载 -> 按 chunk 分块 -> 每块内非重叠窗口提取特征并推理
constexpr int kChunkSeconds = 5;           // 每块时长（秒），默认 5 秒
constexpr int kWindowStrideMs = 400;       // 非重叠窗口步长（毫秒），400ms = 40 帧，与 Python kws_tflite._iter_windows 一致

// Fbank 处理参数（与 Python FbankExtractor 对齐）
constexpr float kFbankDither = 0.0f;           // 高斯噪声 dither，0 表示关闭
constexpr float kFbankPreemphCoeff = 0.97f;    // 预加重系数
constexpr float kFbankLogEpsilon = 1.2e-7f;    // log 下界，避免 log(0)

// 模型输出类别
// 注意：<FILLER> 标签 ID=-1，是负样本标识，不是模型输出的独立通道。
// 模型输出 [1, 40, 1]：40 帧 × 1 个类别（LINGXILINGXI）的概率。
constexpr int kCategoryCount = 1;
constexpr const char *kCategoryLabels[kCategoryCount] = {
    "LINGXILINGXI",
};

#endif // TENSORFLOW_LITE_MICRO_EXAMPLES_MICRO_SPEECH_MICRO_MODEL_SETTINGS_H_
