/* Copyright 2020 The TensorFlow Authors. All Rights Reserved.

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

// This is a standard TensorFlow Lite FlatBuffer model file that has been
// converted into a C data array, so it can be easily compiled into a binary
// for devices that don't have a file system. It was created using the command:
// xxd -i model.tflite > model.cc

// 头文件保护：防止本头在一次编译中被重复包含而引发重定义错误。
#ifndef TENSORFLOW_LITE_MICRO_EXAMPLES_MICRO_SPEECH_MICRO_FEATURES_MODEL_H_
#define TENSORFLOW_LITE_MICRO_EXAMPLES_MICRO_SPEECH_MICRO_FEATURES_MODEL_H_

// 唤醒词模型（TFLite FlatBuffer）二进制数据，由 xxd 从 model.tflite 生成，
// 数据体定义在 model.cc（≈345KB，编进静态库）。单类别检测器，唤醒词 LINGXILINGXI。
// 用法：tflite::GetModel(g_model) 交给 TFLite Micro 解释器加载（无需文件系统）。
extern const unsigned char g_model[];   // 内嵌的唤醒模型二进制数据（只读字节数组，真身在 model.cc）
extern const int g_model_len;   // g_model 的字节长度

#endif  // TENSORFLOW_LITE_MICRO_EXAMPLES_MICRO_SPEECH_MICRO_FEATURES_MODEL_H_
