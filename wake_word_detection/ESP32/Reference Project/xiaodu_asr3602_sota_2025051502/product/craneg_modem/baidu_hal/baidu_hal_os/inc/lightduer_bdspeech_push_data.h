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
#include "lightduer_bdspeech_parser.h"

duer_status_t duer_bds_push_data_handle(duer_bds_dp_hdlr_t parser,
                                        bdsc_event_data_t *data);


#ifdef __cplusplus
}
#endif

#endif/*BAIDU_DUER_LIGHTDUER_MODULE_CONNAGENT_BDSPEECH_LIGHTDUER_ASR_H*/
