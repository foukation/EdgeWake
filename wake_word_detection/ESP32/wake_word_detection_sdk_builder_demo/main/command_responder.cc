/* Copyright 2019 The TensorFlow Authors. All Rights Reserved.

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

#include "command_responder.h"
#include "tensorflow/lite/micro/micro_log.h"

// The default implementation writes out the name of the recognized command
// to the error console. Real applications will want to take some custom
// action instead, and should implement their own versions of this function.
// max_prob: 唤醒词类别在所有帧上的最大概率（与阈值、触发条件无关）
// events: 满足触发条件的事件列表，每个事件含 frame、timestamp_s、score
// detected: 是否存在至少一个事件
void RespondToCommand(int32_t current_time, const char* found_command,
                      float score, bool is_new_command, float max_prob) {
  // 仅在检测到唤醒词时打印，避免每个窗口都输出
  if (!is_new_command) {
    return;
  }
  int frame = current_time / 10;
  double timestamp_s = current_time / 1000.0;
  MicroPrintf("detected: True\nmax_prob: %.15g\nevents: [{'frame': %d, 'timestamp_s': %.6f, 'score': %.15g}]",
              (double)max_prob, frame, timestamp_s, (double)score);
}

