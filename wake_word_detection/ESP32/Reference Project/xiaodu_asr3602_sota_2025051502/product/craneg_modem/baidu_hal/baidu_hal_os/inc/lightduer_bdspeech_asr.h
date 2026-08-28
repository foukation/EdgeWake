/**
 * Copyright (2017) Baidu Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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
 * File: lightduer_bdspeech_asr.h
 * Auth: Su Hao (suhao@baidu.com)
 * Desc: Control the bds client asr.
 */

#ifndef BAIDU_DUER_LIBDUER_DEVICE_MODULES_CONNAGENT_BDSPEECH_LIGHTDUER_BDSPEECH_ASR_H
#define BAIDU_DUER_LIBDUER_DEVICE_MODULES_CONNAGENT_BDSPEECH_LIGHTDUER_BDSPEECH_ASR_H

#include "lightduer_bdspeech_wrapper.h"
#include "lightduer_asr.h"

typedef struct _duer_bdsasr_s *duer_bdsasr_handler;

duer_bdsasr_handler duer_bdsasr_create(duer_bdspeech_handler speech);

void duer_bdsasr_destroy(duer_bdsasr_handler handler);

duer_status_t duer_bdsasr_start(duer_bdsasr_handler handler, const duer_asrparam_t *param);

duer_status_t duer_bdsasr_stop(duer_bdsasr_handler handler);

duer_status_t duer_bdsasr_cancel(duer_bdsasr_handler handler);

duer_status_t duer_bdsasr_record_stop(duer_bdsasr_handler handler);

duer_status_t duer_bdsasr_event_handle(duer_bdsasr_handler handler, bds_client_event_t *event);

#endif/*BAIDU_DUER_LIBDUER_DEVICE_MODULES_CONNAGENT_BDSPEECH_LIGHTDUER_BDSPEECH_ASR_H*/