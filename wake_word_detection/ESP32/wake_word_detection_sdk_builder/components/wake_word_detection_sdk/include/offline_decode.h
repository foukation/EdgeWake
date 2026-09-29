/* 参考 compare_onnx_tflite_output.py / kws_tflite.py 的 decode_outputs 逻辑
 * 用于离线音频唤醒识别的后处理
 */
// =============================================================================
// offline_decode.h —— 唤醒 SDK 的离线判决器（后处理，链路收尾环节）
//
// 作用：把模型对一整段音频（多个推理窗口）输出的逐帧概率，经
//       “合并窗口 → 过阈值 → 时序聚合 → 去重”判定为明确结论：
//       有没有唤醒（detected）、在第几帧/第几秒触发（events）。
// 场景：离线——面向已知的整段音频（如 wav 文件）一次性判决，用于文件识别/回归验证。
//       （实时逐窗判决请用 recognize_commands.h 的流式版本。）
// 特点：纯后处理、无动态内存（结果用定长数组），适配 MCU；算法与训练侧 Python 对齐。
// =============================================================================

// 头文件保护：防止本头在一次编译中被重复包含而引发重定义错误。
#ifndef TENSORFLOW_LITE_MICRO_EXAMPLES_MICRO_SPEECH_OFFLINE_DECODE_H_
#define TENSORFLOW_LITE_MICRO_EXAMPLES_MICRO_SPEECH_OFFLINE_DECODE_H_

#include <cstddef>
#include <cstdint>

namespace offline {

// 单次触发事件（一次判定为唤醒的记录）
struct WakeEvent {
  int frame;          // 触发所在帧索引（第几帧）
  float timestamp_s;  // 触发时刻（秒）= frame × 帧移(0.01s)
  float score;        // 该触发帧的唤醒词概率
};

// 解码参数（与 Python decode_outputs 对齐）
struct DecodeParams {
  float threshold = 0.5f;       // 触发阈值：帧概率 > 此值才算“触发帧”
  int window_size = 10;          // 窗口内帧数：触发帧须落在此帧宽内才算有效聚合
  int trigger_level = 1;         // 窗口内需达到的触发帧数（够数才判为一次事件）
  float min_event_gap_s = 2.0f; // 事件间最小间隔（秒），抑制同一次唤醒被重复计数
};

// 解码结果
struct DecodeResult {
  bool detected = false;   // 是否检出唤醒（num_events > 0）
  float max_prob = 0.0f;   // 整段音频逐帧概率的最大值（与阈值无关，用于观测置信度）
  int num_events = 0;      // 实际触发事件个数
  WakeEvent events[10];    // 触发事件列表，最多 10 个
};

// 将多窗口输出合并为逐帧分数，并解码为唤醒事件。
// frame_scores        : [输出] 逐帧分数缓冲区，长度须 >= max_frames；仅存 wakeword_class_index 的概率
//                       （max_frames = (num_windows-1)*window_stride_frames + kFeatureCount）
// num_total_frames    : [输出] 实际写入的总帧数（= max_frames）
// outputs             : 各窗口的模型输出指针数组，每窗口 kFeatureCount 帧
// num_windows         : 窗口数
// window_stride_frames: 窗口间帧步长（400ms=40 帧，非重叠）
// wakeword_class_index: 取模型输出中哪个类别的概率（本项目单类别，传 0）
// params              : 判决参数
// result              : [输出] 判决结果
void DecodeOutputs(float* frame_scores, int* num_total_frames,
                  const float* const* outputs, int num_windows,
                  int window_stride_frames, int wakeword_class_index,
                  const DecodeParams& params, DecodeResult* result);

}  // namespace offline

#endif  // TENSORFLOW_LITE_MICRO_EXAMPLES_MICRO_SPEECH_OFFLINE_DECODE_H_
