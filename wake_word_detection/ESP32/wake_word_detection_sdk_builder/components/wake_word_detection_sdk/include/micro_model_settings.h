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

// =============================================================================
// micro_model_settings.h —— 唤醒 SDK 的编译期参数中心（配置常量总表）
//
// 作用：集中定义音频采集、Fbank 特征、模型输入输出、分块/窗口、解码等
//       全部关键常量，作为全体模块的“单一事实来源”统一引用
//       （feature_pipeline / fbank / recognize_commands / offline_decode 均 #include 本文件）。
// 特点：全部为 constexpr 编译期常量，零运行时开销，可用于数组大小声明。
//
// ⚠️ 这些值与训练侧配置 dataset_conf.fbank_conf 严格对应（num_mel_bins/frame_shift/
//    frame_length 等）。推理侧特征算法必须与训练侧完全一致，否则特征分布错位、
//    识别失准。故 **禁止单独修改本文件中的任何常量**，改动须与训练侧同步。
//
// 📐 关键换算关系：
//    每帧采样数     = 帧长(25ms) × 16k / 1000        = 400 采样
//    帧移采样数     = 帧移(10ms) × 16k / 1000        = 160 采样
//    单窗口特征数   = 帧数(40) × 特征维度(40)         = 1600（模型输入长度）
//    单窗口时长     ≈ 帧长 + (帧数-1) × 帧移 = 25 + 39×10 = 415ms ≈ 6640 采样
// =============================================================================

// 以下参数与训练配置 dataset_conf.fbank_conf 一致
constexpr int kAudioSampleFrequency = 16000; // 采样率 16kHz（resample_conf.resample_rate），全链路基准
constexpr int kFeatureSize = 40;             // 每帧特征维度：mel 滤波器个数（fbank_conf.num_mel_bins）
constexpr int kFeatureStrideMs = 10;         // 帧移，单位 ms：相邻帧起点间隔（=160 采样，帧间重叠）（fbank_conf.frame_shift）
constexpr int kFeatureDurationMs = 25;       // 帧长，单位 ms：每帧覆盖的音频时长（=400 采样）（fbank_conf.frame_length）
// 模型输入 [1, 40, 40] → 40 帧 × 40 维 fbank
constexpr int kFeatureCount = 40;            // 一个推理窗口的帧数（模型一次吃 40 帧）
// 单窗口特征总元素数 = 40 帧 × 40 维 = 1600，即模型输入长度；
// 接入方分配特征缓冲区 float feats[kFeatureElementCount] 时使用
constexpr int kFeatureElementCount = (kFeatureSize * kFeatureCount);

// 单帧对应的采样点数 = 帧长(25ms) × 16k / 1000 = 400（“每帧采样数”，名字中 Max 为历史沿用）
constexpr int kMaxAudioSampleSize =
    kFeatureDurationMs * kAudioSampleFrequency / 1000;

// WAV 分块处理参数：加载 -> 按 chunk 分块 -> 每块内非重叠窗口提取特征并推理
constexpr int kChunkSeconds = 5;           // 每块时长（秒），默认 5 秒
// 注意区分：kFeatureStrideMs(10ms) 是【窗口内帧】的滑动步长；
//          kWindowStrideMs(400ms) 是【窗口之间】的滑动步长（非重叠）。二者含义不同勿混。
constexpr int kWindowStrideMs = 400;       // 非重叠窗口步长（毫秒），400ms = 40 帧，与 Python kws_tflite._iter_windows 一致

// Fbank 处理参数（与 Python FbankExtractor 对齐）
constexpr float kFbankDither = 0.0f;           // 高斯噪声 dither，0 表示关闭（推理侧关闭以保证确定性）
constexpr float kFbankPreemphCoeff = 0.97f;    // 预加重系数：y[n] = x[n] - 0.97·x[n-1]，提升高频、平衡频谱
constexpr float kFbankLogEpsilon = 1.2e-7f;    // log 下界，避免 log(0) = -inf

// 模型输出类别
// 注意：<FILLER> 标签 ID=-1，是负样本标识，不是模型输出的独立通道。
// 模型输出 [1, 40, 1]：40 帧 × 1 个类别（LINGXILINGXI）的概率。
constexpr int kCategoryCount = 1;              // 输出类别数 = 1（单类别检测器，只判是否唤醒词）
constexpr const char *kCategoryLabels[kCategoryCount] = {
    "LINGXILINGXI",
};

#endif // TENSORFLOW_LITE_MICRO_EXAMPLES_MICRO_SPEECH_MICRO_MODEL_SETTINGS_H_
