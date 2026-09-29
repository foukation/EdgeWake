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

// 头文件保护：防止本头在一次编译中被重复包含而引发重定义错误。
#ifndef TENSORFLOW_LITE_MICRO_EXAMPLES_MICRO_SPEECH_FEATURE_PIPELINE_H_
#define TENSORFLOW_LITE_MICRO_EXAMPLES_MICRO_SPEECH_FEATURE_PIPELINE_H_

#include "tensorflow/lite/c/common.h"  // TfLite C 基础类型定义

// =============================================================================
// FeaturePipeline —— 唤醒 SDK 的音频前端（特征提取器）
//
// 作用：把原始 PCM 音频流式转换成模型输入所需的 Fbank 特征
//       （链路 PCM →【本类】→ 模型 Invoke → 解码 的第一环）。
// 算法：移植自 wekws feature_pipeline；接口与 TFLite Micro FeatureProvider 兼容。
// 机制：流式处理——每次喂入的音频先与上次残差拼接，按帧算特征，
//       未凑满整帧的尾部存入残差，下次续算，因此可任意大小分块喂入。
//
// 【输入音频格式契约】喂入的 PCM 必须为：
//   16kHz 采样率 / 16bit 有符号 / 小端 / 单声道 / 裸 PCM（不带 WAV 头）。
//   非此格式会静默产生错误特征，导致识别异常。
//
// 【典型用法】
//   float feats[kFeatureElementCount];               // 调用方分配并持有
//   FeaturePipeline fp(kFeatureElementCount, feats);
//   fp.Reset();                                      // 开始一段独立音频前
//   if (fp.AcceptWaveform(pcm, num_samples)) {       // 攒够一窗返回 true
//     // feats 已更新，可送模型 Invoke
//   }
// =============================================================================
class FeaturePipeline {
 public:
  // 构造。
  // feature_size : 特征缓冲区元素个数，应传 kFeatureElementCount(=1600)。
  // feature_data : 特征输出缓冲区，由【调用方】分配并负责其生命周期；
  //                本类仅向其中写入，不分配也不释放，容量须 >= feature_size。
  FeaturePipeline(int feature_size, float* feature_data);

  // 析构。不释放 feature_data_（缓冲区归调用方所有）。
  ~FeaturePipeline();

  // 追加音频波形并尝试生成一个推理窗口的特征（流式输入主入口）。
  // wav     : 输入 PCM 指针（int16，须满足上方【输入音频格式契约】）。
  // wav_len : 采样点数（个数，不是字节数！字节数 = wav_len * 2）。
  // 返回值  : true  = 已攒够一个推理窗口(40 帧≈6640 采样)，特征写入 feature_data_；
  //           false = 音频不足一窗口，已缓存为残差待下次拼接，feature_data_ 未更新。
  // 备注    : 支持任意大小分块喂入（如一次 160 或 16000 采样均可）。
  bool AcceptWaveform(const int16_t* wav, size_t wav_len);

  // 重置内部状态（清空残差）。开始处理一段新的独立音频/新窗口前调用。
  void Reset();

 private:
  int feature_size_;      // 特征缓冲区元素个数（= 构造传入值，应为 kFeatureElementCount）
  float* feature_data_;   // 特征输出缓冲区（外部持有，本类只写入，不分配/释放）

  // 残差缓冲区容量上限（采样点数）。取 1024：足以容纳"不足一帧步长(160 采样)"的
  // 尾部余量，同时防止残差无界增长。
  static constexpr size_t kRemainedWavMaxSize = 1024;

  // 残差缓冲区：暂存本次未凑满整帧的尾部采样，下次 AcceptWaveform 时拼到新数据前继续处理。
  int16_t remained_wav_[kRemainedWavMaxSize];

  // 残差当前有效采样点数（范围 0 ~ kRemainedWavMaxSize），标记 remained_wav_ 中有效数据长度。
  size_t remained_wav_len_;
};

#endif  // TENSORFLOW_LITE_MICRO_EXAMPLES_MICRO_SPEECH_FEATURE_PIPELINE_H_
