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
 * File: lightduer_bdspeech_tts.h
 * Auth: Gang Chen (chengang12@baidu.com)
 * Desc: Process tts data flow.
 */

#ifndef BAIDU_DUER_LIGHTDUER_MODULE_CONNAGENT_BDSPEECH_LIGHTDUER_BDSPEECH_ASR_H
#define BAIDU_DUER_LIGHTDUER_MODULE_CONNAGENT_BDSPEECH_LIGHTDUER_BDSPEECH_ASR_H

#ifdef __cplusplus
extern "C" {
#endif

#include "lightduer_types.h"
#include "bds_client_event.h"

typedef struct _duer_bds_tts_s *duer_bds_tts_handler;

typedef duer_status_t (*duer_tts_data_cb)(const char *, const void *, duer_size_t);

typedef void (*duer_tts_header_cb)(const char *, int len, void *ctx);

duer_bds_tts_handler duer_bds_tts_create();

void duer_bds_tts_destroy(duer_bds_tts_handler handler);

duer_status_t duer_bds_tts_set_data_cb(duer_bds_tts_handler handler,
                                       duer_tts_data_cb cb);

duer_status_t duer_bds_tts_handle(duer_bds_tts_handler handler       , bds_client_event_t *event);

void duer_bds_tts_header_reg_cb(duer_bds_tts_handler handler, duer_tts_header_cb cb, void *ctx);

#ifdef __cplusplus
}
#endif

#endif/*BAIDU_DUER_LIGHTDUER_MODULE_CONNAGENT_BDSPEECH_LIGHTDUER_ASR_H*/
