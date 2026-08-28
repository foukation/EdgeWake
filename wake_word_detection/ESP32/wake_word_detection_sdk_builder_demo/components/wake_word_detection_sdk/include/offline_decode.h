/* 参考 compare_onnx_tflite_output.py / kws_tflite.py 的 decode_outputs 逻辑
 * 用于离线音频唤醒识别的后处理
 */
#ifndef TENSORFLOW_LITE_MICRO_EXAMPLES_MICRO_SPEECH_OFFLINE_DECODE_H_
#define TENSORFLOW_LITE_MICRO_EXAMPLES_MICRO_SPEECH_OFFLINE_DECODE_H_

#include <cstddef>
#include <cstdint>

namespace offline {

// 单次触发事件
struct WakeEvent {
  int frame;
  float timestamp_s;
  float score;
};

// 解码参数（与 Python decode_outputs 对齐）
struct DecodeParams {
  float threshold = 0.5f;       // 触发阈值
  int window_size = 10;          // 窗口内帧数
  int trigger_level = 1;         // 窗口内需达到的触发帧数
  float min_event_gap_s = 2.0f; // 事件间最小间隔（秒）
};

// 解码结果
struct DecodeResult {
  bool detected = false;
  float max_prob = 0.0f;
  int num_events = 0;
  WakeEvent events[10];  // 最多 10 个事件
};

// 将多窗口输出合并为逐帧分数，并解码
// frame_scores: 输出，[max_frames] 仅存储 wakeword_class_index 的概率
// outputs: 各窗口的模型输出，每窗口 kFeatureCount 帧
// num_windows: 窗口数
// window_stride_frames: 窗口间帧步长（400ms=40 帧，非重叠）
void DecodeOutputs(float* frame_scores, int* num_total_frames,
                  const float* const* outputs, int num_windows,
                  int window_stride_frames, int wakeword_class_index,
                  const DecodeParams& params, DecodeResult* result);

}  // namespace offline

#endif  // TENSORFLOW_LITE_MICRO_EXAMPLES_MICRO_SPEECH_OFFLINE_DECODE_H_
