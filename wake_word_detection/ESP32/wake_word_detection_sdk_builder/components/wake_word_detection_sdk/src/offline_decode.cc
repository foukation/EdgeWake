/* 参考 kws_tflite.py decode_outputs 实现
 */
#include "offline_decode.h"

#include <algorithm>
#include <cmath>
#include <cstring>

#include "micro_model_settings.h"
#include "tensorflow/lite/micro/micro_log.h"

namespace offline {

// 帧率：每帧 10ms (frame_shift=160 samples @ 16kHz)
static constexpr float kFrameShiftS = 0.01f;

void DecodeOutputs(float* frame_scores, int* num_total_frames,
                  const float* const* outputs, int num_windows,
                  int window_stride_frames, int wakeword_class_index,
                  const DecodeParams& params, DecodeResult* result) {
  result->detected = false;
  result->max_prob = 0.0f;
  result->num_events = 0;
  std::memset(result->events, 0, sizeof(result->events));

  if (num_windows <= 0 || outputs == nullptr || frame_scores == nullptr) {
    *num_total_frames = 0;
    return;
  }

  // 最大帧数：(N-1)*stride + kFeatureCount
  const int max_frames =
      (num_windows - 1) * window_stride_frames + kFeatureCount;
  std::memset(frame_scores, 0, max_frames * sizeof(float));

  // 合并重叠窗口：取每帧的最大概率
  for (int w = 0; w < num_windows; ++w) {
    const float* out = outputs[w];
    if (out == nullptr) continue;
    const int base = w * window_stride_frames;
    for (int t = 0; t < kFeatureCount; ++t) {
      const int idx = base + t;
      if (idx < max_frames) {
        float v = out[t * kCategoryCount + wakeword_class_index];
        if (v > frame_scores[idx]) frame_scores[idx] = v;
      }
    }
  }
  *num_total_frames = max_frames;

  // max_prob
  for (int i = 0; i < max_frames; ++i) {
    if (frame_scores[i] > result->max_prob) result->max_prob = frame_scores[i];
  }

  // 收集触发帧索引（与 Python triggered_frames 一致）
  static int triggered[512];
  int num_triggered = 0;
  for (int i = 0; i < max_frames && num_triggered < 512; ++i) {
    if (frame_scores[i] > params.threshold) {
      triggered[num_triggered++] = i;
    }
  }

  const int min_gap_frames =
      static_cast<int>(params.min_event_gap_s / kFrameShiftS + 0.5f);
  int last_event_frame = -1000000;

  for (int i = params.trigger_level - 1; i < num_triggered; ++i) {
    const int start_frame = triggered[i - (params.trigger_level - 1)];
    const int cur_frame = triggered[i];
    if (cur_frame - start_frame >= params.window_size) continue;
    if (cur_frame - last_event_frame <= min_gap_frames) continue;

    if (result->num_events < 10) {
      result->events[result->num_events].frame = cur_frame;
      result->events[result->num_events].timestamp_s =
          cur_frame * kFrameShiftS;
      result->events[result->num_events].score = frame_scores[cur_frame];
      result->num_events++;
      last_event_frame = cur_frame;
    }
  }

  result->detected = (result->num_events > 0);
}

}  // namespace offline
