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

#include "recognize_commands.h"

#include <limits>

RecognizeCommands::RecognizeCommands(int32_t average_window_duration_ms,
                                     float detection_threshold,
                                     int32_t suppression_ms,
                                     int32_t minimum_count)
    : average_window_duration_ms_(average_window_duration_ms),
      detection_threshold_(detection_threshold),
      suppression_ms_(suppression_ms), minimum_count_(minimum_count),
      previous_results_() {
  previous_top_label_ = "silence";
  previous_top_label_time_ = std::numeric_limits<int32_t>::min();
}

TfLiteStatus RecognizeCommands::ProcessLatestResults(
    const TfLiteTensor *latest_results, const int32_t current_time_ms,
    const char **found_command, float *score, bool *is_new_command,
    float *max_prob) {
  const int expected_elements = kFeatureCount * kCategoryCount; // for model output [1, 40, 1] meaning 40 time steps
  int actual_elements = 1;
  for (int i = 0; i < latest_results->dims->size; i++) {
    actual_elements *= latest_results->dims->data[i];
  }

  // Allow elements to equal output length
  if (actual_elements != expected_elements && actual_elements != kCategoryCount) {
    MicroPrintf(
        "The results for recognition should contain %d or %d elements, but there are "
        "%d",
        kCategoryCount, expected_elements, actual_elements);
    return kTfLiteError;
  }

  if (latest_results->type != kTfLiteFloat32) {
    MicroPrintf(
        "The results for recognition should be float elements, but are %d",
        latest_results->type);
    return kTfLiteError;
  }

  if ((!previous_results_.empty()) &&
      (current_time_ms < previous_results_.front().time_)) {
    MicroPrintf("Results must be fed in increasing time order, but received a "
                "timestamp of %d that was earlier than the previous one of %d",
                current_time_ms, previous_results_.front().time_);
    return kTfLiteError;
  }

  // Add the latest results to the head of the queue.
  const float* scores_ptr =
      reinterpret_cast<const float*>(latest_results->data.raw);

  // wekws 风格：取整个窗口内每个类别的最大得分（max-over-time pooling）。
  // 模型输出 [1, 40, kCategoryCount]，唤醒词的峰值可能出现在任意帧，
  // 固定取最后一帧会漏检。参考 compute_det.py 的逐帧触发逻辑。
  static float window_max_scores[kCategoryCount];
  for (int c = 0; c < kCategoryCount; c++) {
    window_max_scores[c] = -1.0f;
  }
  if (actual_elements == expected_elements) {
    for (int t = 0; t < kFeatureCount; t++) {
      for (int c = 0; c < kCategoryCount; c++) {
        float v = scores_ptr[t * kCategoryCount + c];
        if (v > window_max_scores[c]) window_max_scores[c] = v;
      }
    }
  } else {
    // 单帧输出 fallback
    for (int c = 0; c < kCategoryCount; c++) {
      window_max_scores[c] = scores_ptr[c];
    }
  }

  previous_results_.push_back({current_time_ms, window_max_scores});

  // max_prob: 全局最大唤醒词概率，与阈值、触发条件无关，只取历史最大值
  static float global_max_prob = 0.0f;
  if (window_max_scores[0] > global_max_prob) {
    global_max_prob = window_max_scores[0];
  }

  // Prune any earlier results that are too old for the averaging window.
  const int64_t time_limit = current_time_ms - average_window_duration_ms_;
  while ((!previous_results_.empty()) &&
         previous_results_.front().time_ < time_limit) {
    previous_results_.pop_front();
  }

  *max_prob = global_max_prob;

  // Need at least 2 frames for "2 consecutive frames" detection logic.
  const int64_t how_many_results = previous_results_.size();
  if (how_many_results < minimum_count_) {
    *found_command = previous_top_label_;
    *score = 0;
    *is_new_command = false;
    return kTfLiteOk;
  }

  // Get current frame (latest) top label and score.
  const float *current_scores =
      previous_results_.back().scores;
  int current_top_index = 0;
  float current_top_score = 0;
  for (int i = 0; i < kCategoryCount; ++i) {
    float s = current_scores[i];
    if (s > current_top_score) {
      current_top_score = s;
      current_top_index = i;
    }
  }
  const char *current_top_label = kCategoryLabels[current_top_index];

  // Suppression: within suppression_ms after last trigger, do not treat as new command.
  int64_t time_since_last_top;
  if ((previous_top_label_ == kCategoryLabels[0]) ||
      (previous_top_label_time_ == std::numeric_limits<int32_t>::min())) {
    time_since_last_top = std::numeric_limits<int32_t>::max();
  } else {
    time_since_last_top = current_time_ms - previous_top_label_time_;
  }

  // Trigger: 1 consecutive frame over threshold, and outside suppression period.
  // We use current frame only to avoid missing short peaks.
  bool over_threshold = (current_top_score > detection_threshold_);
  bool outside_suppression = (time_since_last_top > suppression_ms_);

  if (over_threshold && outside_suppression) {
    previous_top_label_ = current_top_label;
    previous_top_label_time_ = current_time_ms;
    *is_new_command = true;
  } else {
    *is_new_command = false;
  }
  *found_command = current_top_label;
  *score = current_top_score;

  return kTfLiteOk;
}
