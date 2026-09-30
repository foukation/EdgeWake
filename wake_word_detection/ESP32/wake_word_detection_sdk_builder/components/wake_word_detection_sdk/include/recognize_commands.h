/* Copyright 2017 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

// =============================================================================
// recognize_commands.h —— 流式（在线）唤醒判决器
// -----------------------------------------------------------------------------
// 作用：把「逐窗口的模型输出概率」按时间流实时判决为「是否发生一次新的唤醒命中」。
//       调用方每处理完一个音频窗口、拿到模型 Invoke 的输出张量后，连同该窗口的
//       时间戳一起喂入本判决器，判决器维护一小段历史 + 抑制期状态，输出本次是否
//       为「新命中」。
//
// 在链路中的位置：
//       PCM → feature_pipeline（特征）→ TFLite 模型 Invoke（逐帧概率）
//            → RecognizeCommands（本文件·流式判决）→ 命中
//
// 流式 vs 离线（重要区分）：
//   - recognize_commands（本文件）：在线逐窗判决，带抑制期(suppression)与时序
//     状态，要求时间戳单调递增，适合边采集边判决的实时场景。
//   - offline_decode：离线整段判决，一次性对已采完的整段输出做合并/过阈/聚合，
//     无跨调用时序状态。
//
// 实现特性：
//   - 无动态内存分配（历史队列为定长环形缓冲），适配 MCU。
//   - 单类别唤醒检测器场景下，kCategoryCount == 1，唤醒词概率即 scores[0]。
// =============================================================================

// 注意：下面的头文件保护宏名沿用自上游 TFLite Micro（micro_speech 示例），
// 与本项目实际文件路径无关，仅作唯一性保护，勿据此推断源码目录结构。
#ifndef TENSORFLOW_LITE_MICRO_EXAMPLES_MICRO_SPEECH_RECOGNIZE_COMMANDS_H_
#define TENSORFLOW_LITE_MICRO_EXAMPLES_MICRO_SPEECH_RECOGNIZE_COMMANDS_H_

#include <cstdint>

#include "tensorflow/lite/c/common.h"   // TfLiteTensor / TfLiteStatus
#include "micro_model_settings.h"       // kCategoryCount / kFeatureCount / kCategoryLabels
#include "tensorflow/lite/micro/micro_log.h"  // MicroPrintf（错误日志）

// 定长环形缓冲区：只保存"最近一小段时间内"的推理结果，供判决时回看。
// 这是 std::deque 的极简替代——只实现判决所需的少量操作，且不使用任何动态内存
// 分配，因此更适合微控制器；代价是容量有硬上限（kMaxResults）。
// 语义为环形队列：满时 push_back 会自动丢弃最旧元素（不报错、不扩容）。
class PreviousResultsQueue {
 public:
  PreviousResultsQueue() : front_index_(0), size_(0) {}

  // 一条推理结果：记录该窗口各类别得分及其采集时间戳。
  struct Result {
    Result() : time_(0), scores() {}
    Result(int32_t time, const float* input_scores) : time_(time) {
      for (int i = 0; i < kCategoryCount; ++i) {
        scores[i] = input_scores[i];
      }
    }
    int32_t time_;                 // 该结果对应窗口的时间戳（毫秒）
    float scores[kCategoryCount];  // 各类别得分（本项目单类别时即 scores[0]）
  };

  int size() { return size_; }      // 当前元素个数
  bool empty() { return size_ == 0; }
  Result& front() { return results_[front_index_]; }  // 最旧元素（队头）
  // 最新元素（队尾）。内部按环形下标回绕定位。
  Result& back() {
    int back_index = front_index_ + (size_ - 1);
    if (back_index >= kMaxResults) {
      back_index -= kMaxResults;
    }
    return results_[back_index];
  }

  // 追加一条最新结果到队尾；若已满则先自动弹出最旧元素腾位（不报错）。
  void push_back(const Result& entry) {
    if (size() >= kMaxResults) {
      // MicroPrintf("Couldn't push_back latest result, too many already!");
      // Automatically pop the oldest element to make room for the new one
      pop_front();
    }
    size_ += 1;
    back() = entry;
  }

  // 弹出并返回最旧元素（队头）；队列为空时打印错误并返回默认 Result()。
  Result pop_front() {
    if (size() <= 0) {
      MicroPrintf("Couldn't pop_front result, none present!");
      return Result();
    }
    Result result = front();
    front_index_ += 1;
    if (front_index_ >= kMaxResults) {
      front_index_ = 0;
    }
    size_ -= 1;
    return result;
  }

  // 便捷遍历：按「距队头的偏移」取元素（0=最旧）。offset 越界时打印错误并夹取
  // 到最后一个有效元素，不会越界访问。
  Result& from_front(int offset) {
    if ((offset < 0) || (offset >= size_)) {
      MicroPrintf("Attempt to read beyond the end of the queue!");
      offset = size_ - 1;
    }
    int index = front_index_ + offset;
    if (index >= kMaxResults) {
      index -= kMaxResults;
    }
    return results_[index];
  }

 private:
  static constexpr int kMaxResults = 50;  // 历史结果硬上限（环形缓冲容量）
  Result results_[kMaxResults];           // 底层定长存储

  int front_index_;  // 队头在 results_ 中的物理下标（环形起点）
  int size_;         // 当前有效元素个数
};

// 流式唤醒判决器。
//
// 用法：按你想要的配置构造一个对象，然后把每个音频窗口的模型输出（连同单调递增
// 的时间戳）持续喂给 ProcessLatestResults()。对象内部维护历史与抑制期状态，
// 因此必须按时间顺序连续喂入同一实例。
//
// ⚠️ 行为说明（与早期上游"窗口平均平滑"注释不同，以本实现为准）：
//   本实现采用 wekws 风格判决：
//     1) 对模型输出做 max-over-time pooling（取整窗内每类别的最大得分），避免
//        唤醒峰值出现在窗口中间时被漏检；
//     2) 触发判据为「当前帧最高分 > detection_threshold」的单帧过阈，而非窗口
//        平均；
//     3) 命中后进入 suppression_ms 抑制期，期内不再判为新命中，抑制重复触发。
class RecognizeCommands {
 public:
  // 构造判决器（参数均带默认值）。
  //   average_window_duration_ms (默认 1000)：历史回看窗口长度（毫秒）。本实现中
  //       仅用于裁剪队列里超出该时间跨度的旧结果，并非做窗口内平均。
  //   detection_threshold (默认 0.5)：当前帧最高分超过此值即视为过阈。值越高
  //       精确率越高、召回越低。
  //   suppression_ms (默认 2000)：一次命中后的抑制期（毫秒），期内不再判为新命中，
  //       用于抑制同一次唤醒的重复触发。
  //   minimum_count (默认 1)：历史队列中结果数达到此下限后才开始判决，避免刚开始
  //       积累时的误判。
  explicit RecognizeCommands(int32_t average_window_duration_ms = 1000,
                             float detection_threshold = 0.5f,
                             int32_t suppression_ms = 2000,
                             int32_t minimum_count = 1);

  // 喂入一个窗口的模型输出并做判决。
  // 入参：
  //   latest_results : 模型输出张量，必须为 float32；形状为 [1, kFeatureCount,
  //                    kCategoryCount]（即 [1,40,1]，40 个时间步）或退化的
  //                    [kCategoryCount]。元素个数不符或类型非 float32 → 返回
  //                    kTfLiteError。
  //   current_time_ms: 本窗口时间戳（毫秒），**必须相对上次调用单调递增**，否则
  //                    返回 kTfLiteError。
  // 出参：
  //   found_command  : 当前帧最高分对应的类别标签字符串。
  //   score          : 当前帧最高分。
  //   is_new_command : 本次是否产生一次「新命中」（过阈且不在抑制期）= detected。
  //   max_prob       : 唤醒词类别（scores[0]）出现过的历史最大概率，与阈值/触发
  //                    条件无关。
  //                    ⚠️ 注意：该峰值为进程级历史最大值，跨调用累积且不随实例
  //                    复位（多实例会共享同一累积值）；如需重新计峰值须重启进程。
  // 返回：kTfLiteOk 成功；输入形状/类型错误或时间戳回退时 kTfLiteError。
  TfLiteStatus ProcessLatestResults(const TfLiteTensor* latest_results,
                                    const int32_t current_time_ms,
                                    const char** found_command, float* score,
                                    bool* is_new_command, float* max_prob);

 private:
  // —— 配置（构造时固定）——
  int32_t average_window_duration_ms_;  // 历史回看窗口长度（ms），用于裁剪旧结果
  float detection_threshold_;           // 过阈阈值
  int32_t suppression_ms_;              // 命中后抑制期（ms）
  int32_t minimum_count_;               // 开始判决所需的最小历史结果数

  // —— 运行时状态 ——
  PreviousResultsQueue previous_results_;  // 最近一段时间的历史结果队列
  const char* previous_top_label_;         // 上次命中的类别标签（抑制期判定用）
  int32_t previous_top_label_time_;        // 上次命中的时间戳（ms）
};

#endif  // TENSORFLOW_LITE_MICRO_EXAMPLES_MICRO_SPEECH_RECOGNIZE_COMMANDS_H_
