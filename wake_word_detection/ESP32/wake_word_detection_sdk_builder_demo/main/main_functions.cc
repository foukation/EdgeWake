/* Copyright 2020-2023 The TensorFlow Authors. All Rights Reserved.

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

#include <algorithm>
#include <cstdint>
#include <iterator>

#include "main_functions.h"

#include "command_responder.h"
#include "feature_pipeline.h"
#include "micro_model_settings.h"
#include "offline_decode.h"
#include "model.h"
#include "recognize_commands.h"
#include "tensorflow/lite/micro/system_setup.h"
#include "tensorflow/lite/schema/schema_generated.h"
#include "tensorflow/lite/core/c/common.h"
#include "tensorflow/lite/micro/micro_interpreter.h"
#include "tensorflow/lite/micro/micro_log.h"
#include "tensorflow/lite/micro/micro_mutable_op_resolver.h"
#include "esp_attr.h"
#include "esp_log.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"

static const char* TAG = "tf_main";

// test_data 内嵌录音（由 test_data/CMakeLists.txt 的 EMBED_FILES 生成，
// 符号 = _binary_<文件名把 . 换成 _>_start/_end）。改用预编库后，读文件逻辑
// 从 SDK 内部移到 demo 层，统一通过公开接口 AcceptWaveform 喂数据。
extern const uint8_t seg_001_start[]  asm("_binary_seg_001_wav_start");
extern const uint8_t seg_001_end[]    asm("_binary_seg_001_wav_end");
extern const uint8_t seg_002_start[]  asm("_binary_seg_002_wav_start");
extern const uint8_t seg_002_end[]    asm("_binary_seg_002_wav_end");
extern const uint8_t seg_003_start[]  asm("_binary_seg_003_wav_start");
extern const uint8_t seg_003_end[]    asm("_binary_seg_003_wav_end");
extern const uint8_t bg_noise_start[] asm("_binary_sample_004_wav_start");
extern const uint8_t bg_noise_end[]   asm("_binary_sample_004_wav_end");

// 以下常量由公开头 micro_model_settings.h 的参数推导（原在 SDK 内部 feature_pipeline.cc）
static constexpr int kAudioSampleDurationCount =
    kFeatureDurationMs * kAudioSampleFrequency / 1000;
static constexpr int kAudioSampleStrideCount =
    kFeatureStrideMs * kAudioSampleFrequency / 1000;
// 生成 kFeatureCount 帧（一个推理窗口）所需的最小采样数
static constexpr size_t kMinSamplesForFeatures =
    kAudioSampleDurationCount + (kFeatureCount - 1) * kAudioSampleStrideCount;
// 分块与非重叠窗口（与训练侧 kws_tflite._iter_windows 对齐）
static constexpr size_t kChunkSamples =
    (size_t)kChunkSeconds * kAudioSampleFrequency;
static constexpr size_t kWindowStrideSamples =
    (size_t)kWindowStrideMs * kAudioSampleFrequency / 1000;
// 16-bit 单声道 PCM 的 WAV 头固定 44 字节
static constexpr size_t kWavHeaderSize = 44;

// Globals, used for compatibility with Arduino-style sketches.
namespace {
const tflite::Model* model = nullptr; // 模型，在tflite micro中用于存储模型数据。
tflite::MicroInterpreter* interpreter = nullptr; // 解释器，执行模型推理
TfLiteTensor* model_input = nullptr; // 模型输入，在tflite micro中用于存储模型输入数据。
FeaturePipeline* feature_provider = nullptr;  // 特征管道（替换原 FeatureProvider），基于 wekws feature_pipeline 逻辑
RecognizeCommands* recognizer = nullptr; // 识别器，在tflite micro中用于识别指令，输入音频数据特征，输出识别结果。
int32_t previous_time = 0;

// Create an area of memory to use for input, output, and intermediate arrays.
// The size of this will depend on the model you're using, and may need to be
// determined by experimentation.
// 模型需要约 395KB 缓冲区，预留 512KB
// 放入 PSRAM 以节省内部 SRAM（需 CONFIG_SPIRAM_ALLOW_BSS_SEG_EXTERNAL_MEMORY=y）
constexpr int kTensorArenaSize = 512 * 1024;
EXT_RAM_BSS_ATTR uint8_t tensor_arena[kTensorArenaSize];
float feature_buffer[kFeatureElementCount];  // float 特征，无量化
float* model_input_buffer = nullptr;
}  // namespace

// The name of this function is important for Arduino compatibility.
void setup() {
  ESP_LOGI(TAG, "setup start");
  // Map the model into a usable data structure. This doesn't involve any
  // copying or parsing, it's a very lightweight operation.
  model = tflite::GetModel(g_model);
  ESP_LOGI(TAG, "model loaded");
  if (model->version() != TFLITE_SCHEMA_VERSION) {
    MicroPrintf("Model provided is schema version %d not equal to supported "
                "version %d.", model->version(), TFLITE_SCHEMA_VERSION);
    return;
  }

  // 加载模型需要用到的算子
  // Pull in only the operation implementations we need.
  // This relies on a complete list of all the ops needed by this graph.
  // An easier approach is to just use the AllOpsResolver, but this will
  // incur some penalty in code space for op implementations that are not
  // needed by this graph.
  //
  // tflite::AllOpsResolver resolver;
  // NOLINTNEXTLINE(runtime-global-variables)
  static tflite::MicroMutableOpResolver<16> micro_op_resolver;
  if (micro_op_resolver.AddFullyConnected() != kTfLiteOk) {
    MicroPrintf("Could not add FullyConnected op");
    return;
  }
  if (micro_op_resolver.AddReshape() != kTfLiteOk) {
    MicroPrintf("Could not add Reshape op");
    return;
  }
  if (micro_op_resolver.AddSoftmax() != kTfLiteOk) {
    MicroPrintf("Could not add Softmax op");
    return;
  }
  if (micro_op_resolver.AddConv2D() != kTfLiteOk) {
    MicroPrintf("Could not add Conv2D op");
    return;
  }
  if (micro_op_resolver.AddDepthwiseConv2D() != kTfLiteOk) {
    MicroPrintf("Could not add DepthwiseConv2D op");
    return;
  }
  if (micro_op_resolver.AddAveragePool2D() != kTfLiteOk) {
    MicroPrintf("Could not add AveragePool2D op");
    return;
  }
  if (micro_op_resolver.AddMaxPool2D() != kTfLiteOk) {
    MicroPrintf("Could not add MaxPool2D op");
    return;
  }
  if (micro_op_resolver.AddAdd() != kTfLiteOk) {
    MicroPrintf("Could not add Add op");
    return;
  }
  if (micro_op_resolver.AddSub() != kTfLiteOk) {
    MicroPrintf("Could not add Sub op");
    return;
  }
  if (micro_op_resolver.AddMul() != kTfLiteOk) {
    MicroPrintf("Could not add Mul op");
    return;
  }
  if (micro_op_resolver.AddQuantize() != kTfLiteOk) {
    MicroPrintf("Could not add Quantize op");
    return;
  }
  if (micro_op_resolver.AddDequantize() != kTfLiteOk) {
    MicroPrintf("Could not add Dequantize op");
    return;
  }
  if (micro_op_resolver.AddTranspose() != kTfLiteOk) {
    MicroPrintf("Could not add Transpose op");
    return;
  }
  if (micro_op_resolver.AddSlice() != kTfLiteOk) {
    MicroPrintf("Could not add Slice op");
    return;
  }
  if (micro_op_resolver.AddConcatenation() != kTfLiteOk) {
    MicroPrintf("Could not add Concatenation op");
    return;
  }
  if (micro_op_resolver.AddLogistic() != kTfLiteOk) {
    MicroPrintf("Could not add Logistic op");
    return;
  }

  // Build an interpreter to run the model with.
  // 创建解释器
  ESP_LOGI(TAG, "creating interpreter...");
  static tflite::MicroInterpreter static_interpreter(
      model, micro_op_resolver, tensor_arena, kTensorArenaSize);
  interpreter = &static_interpreter;
  ESP_LOGI(TAG, "interpreter created");

  // Allocate memory from the tensor_arena for the model's tensors.
  // 分配内存给模型（本模型通常很快，换更大模型可能较慢）
  ESP_LOGI(TAG, "AllocateTensors start...");
  TfLiteStatus allocate_status = interpreter->AllocateTensors();
  ESP_LOGI(TAG, "AllocateTensors done");
  if (allocate_status != kTfLiteOk) {
    MicroPrintf("AllocateTensors() failed");
    return;
  }

  // Get information about the memory area to use for the model's input.
  // 获取模型输入的内存区域
  model_input = interpreter->input(0);
  const int expected_elements = kFeatureCount * kFeatureSize;  // 1600
  int actual_elements = 1;
  for (int i = 0; i < model_input->dims->size; i++) {
    actual_elements *= model_input->dims->data[i];
  }
  // 支持 [1, 1600] 或 [1, 40, 40]，元素总数须为 1600
  if (actual_elements != expected_elements || model_input->type != kTfLiteFloat32) {
    MicroPrintf("Bad input tensor: shape dims=%d, type=%d, elements=%d (expected %d)",
                model_input->dims->size, model_input->type, actual_elements,
                expected_elements);
    return;
  }
  model_input_buffer = tflite::GetTensorData<float>(model_input);

  // --- 状态初始化 (State Initialization) ---
  // 模型第一次推理前，需要将状态张量 (Cache/Hidden States) 清零，避免垃圾数据影响识别。
  for (int i = 1; i < interpreter->inputs_size(); i++) {
    TfLiteTensor* state_tensor = interpreter->input(i);
    std::memset(state_tensor->data.raw, 0, state_tensor->bytes);
  }
  ESP_LOGI(TAG, "All input states initialized to zero");
  // ----------------------------------------

  // Prepare to access the audio spectrograms from a microphone or other source
  // that will provide the inputs to the neural network.
  // NOLINTNEXTLINE(runtime-global-variables)
  // 创建特征管道（基于 wekws feature_pipeline 逻辑，支持 AcceptWaveform 流式累积）
  static FeaturePipeline static_feature_pipeline(kFeatureElementCount,
                                                 feature_buffer);
  feature_provider = &static_feature_pipeline;

  // 创建识别器
  static RecognizeCommands static_recognizer;
  recognizer = &static_recognizer;

  previous_time = 0;

  ESP_LOGI(TAG, "setup complete (chunk mode: %ds chunk, %dms stride)",
         kChunkSeconds, kWindowStrideMs);
}

// 说明：原 TFLite 模板的流式麦克风入口 loop() 已删除。
// 本 demo 不接麦克风，只跑 test_data 录音的离线识别（见 ProcessFileOffline）。

// 离线模式：参考 Python kws_tflite.process_file 实现
bool ProcessFileOffline(int file_idx) {
  if (feature_provider == nullptr || interpreter == nullptr) return false;
  if (file_idx < 0 || file_idx > 3) return false;

  static const uint8_t* const files[] = {
      seg_001_start, seg_002_start, seg_003_start, bg_noise_start
  };
  static const uint8_t* const file_ends[] = {
      seg_001_end, seg_002_end, seg_003_end, bg_noise_end
  };
  static const char* const file_names[] = {
      "seg_001", "seg_002", "seg_003", "bg_noise"
  };

  // 重置模型状态（与 Python reset_states 一致）
  for (int i = 1; i < interpreter->inputs_size(); i++) {
    TfLiteTensor* state_tensor = interpreter->input(i);
    std::memset(state_tensor->data.raw, 0, state_tensor->bytes);
  }

  // 输出缓冲：最多 64 窗口
  static constexpr int kMaxWindows = 64;
  static float output_buffer[kMaxWindows][kFeatureCount * kCategoryCount];
  static float frame_scores[1024];
  static const float* output_ptrs[kMaxWindows];

  const uint8_t* file_ptr = files[file_idx];
  const size_t file_bytes = (size_t)(file_ends[file_idx] - file_ptr);
  const size_t file_samples =
      (file_bytes > kWavHeaderSize) ? (file_bytes - kWavHeaderSize) / 2 : 0;

  int num_windows = 0;
  // 遍历分块 + 块内非重叠窗口（与原 SDK PopulateFeatureDataForFile 相同的走法），
  // 每个窗口用公开接口 AcceptWaveform 提特征后再推理。
  for (size_t chunk_start_sample = 0;
       chunk_start_sample < file_samples && num_windows < kMaxWindows;
       chunk_start_sample += kChunkSamples) {
    const size_t chunk_end_sample =
        std::min(chunk_start_sample + kChunkSamples, file_samples);
    const size_t chunk_size_samples = chunk_end_sample - chunk_start_sample;

    for (size_t window_offset = 0;
         window_offset + kMinSamplesForFeatures <= chunk_size_samples &&
         num_windows < kMaxWindows;
         window_offset += kWindowStrideSamples) {
      vTaskDelay(pdMS_TO_TICKS(1));  // 每窗口前让出 CPU

      const size_t window_start_sample = chunk_start_sample + window_offset;
      const int16_t* audio_ptr = reinterpret_cast<const int16_t*>(
          file_ptr + kWavHeaderSize + window_start_sample * 2);

      // 每窗口独立提特征（与原逐窗口 Reset + AcceptWaveform 行为一致）
      feature_provider->Reset();
      if (!feature_provider->AcceptWaveform(audio_ptr, kMinSamplesForFeatures)) {
        MicroPrintf("AcceptWaveform failed in ProcessFileOffline");
        break;
      }

      vTaskDelay(pdMS_TO_TICKS(1));  // FFT 后、Invoke 前让出 CPU

      for (int i = 0; i < kFeatureElementCount; i++) {
        model_input_buffer[i] = feature_buffer[i];
      }

      if (interpreter->Invoke() != kTfLiteOk) {
        MicroPrintf("Invoke failed in ProcessFileOffline");
        break;
      }

      vTaskDelay(pdMS_TO_TICKS(1));  // Invoke 后让出 CPU

      TfLiteTensor* output = interpreter->output(0);
      const float* out_data =
          reinterpret_cast<const float*>(output->data.raw);
      int total = 1;
      for (int i = 0; i < output->dims->size; i++)
        total *= output->dims->data[i];
      float* dst = output_buffer[num_windows];
      for (int i = 0; i < total && i < kFeatureCount * kCategoryCount; i++) {
        dst[i] = out_data[i];
      }
      output_ptrs[num_windows] = dst;
      num_windows++;
    }
  }

  // 解码（参考 Python decode_outputs）
  offline::DecodeParams params;
  params.threshold = 0.5f;
  params.window_size = 10;
  params.trigger_level = 1;
  params.min_event_gap_s = 2.0f;

  offline::DecodeResult result;
  int num_total_frames = 0;
  offline::DecodeOutputs(
      frame_scores, &num_total_frames,
      output_ptrs, num_windows,
      kWindowStrideMs / kFeatureStrideMs,  // 10 帧
      0,  // wakeword_class_index
      params, &result);

  // 输出格式与 Python compare_onnx_tflite_output.py 对齐
  float duration_ms = num_total_frames * kFeatureStrideMs;
  ESP_LOGI(TAG, "------------ %s (offline) ------------", file_names[file_idx]);
  ESP_LOGI(TAG, "audio_duration_ms: %.0f", (double)duration_ms);
  ESP_LOGI(TAG, "outputs_windows: %d", num_windows);
  ESP_LOGI(TAG, "detected: %s", result.detected ? "True" : "False");
  if (result.detected) {
    // 检测到时输出唤醒词标签（kCategoryLabels[0]）
    ESP_LOGI(TAG, "wake_word: %s", kCategoryLabels[0]);
  }
  ESP_LOGI(TAG, "max_prob: %.6f", (double)result.max_prob);
  ESP_LOGI(TAG, "events: %d", result.num_events);
  for (int i = 0; i < result.num_events; i++) {
    ESP_LOGI(TAG, "  [%d] frame=%d timestamp_s=%.6f score=%.6f",
             i, result.events[i].frame,
             (double)result.events[i].timestamp_s,
             (double)result.events[i].score);
  }

  return result.detected;
}
