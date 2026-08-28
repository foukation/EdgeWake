/**
 * Copyright (2019) Baidu Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * File: lightduer_recorder.h
 * Auth: Chen Xihao (chenxihao@baidu.com)
 * Desc: Light duer APIS.
 */

#ifndef BAIDU_DUER_LIGHTDUER_MODULE_CONNAGENT_BDSPEECH_LIGHTDUER_RECORDER_H
#define BAIDU_DUER_LIGHTDUER_MODULE_CONNAGENT_BDSPEECH_LIGHTDUER_RECORDER_H

#include "lightduer_types.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef enum {
    DUER_RECORDER_TYPE_PCM0, // for sonic config
    DUER_RECORDER_TYPE_PCM1, // for bt call
    DUER_RECORDER_TYPE_ASR,  // for dma
} duer_recorder_type_e;

int duer_recorder_start(duer_recorder_type_e type, duer_data_callback callback);

int duer_recorder_stop(duer_recorder_type_e type);

#ifdef __cplusplus
}
#endif

#endif/*BAIDU_DUER_LIGHTDUER_MODULE_CONNAGENT_BDSPEECH_LIGHTDUER_RECORDER_H*/
