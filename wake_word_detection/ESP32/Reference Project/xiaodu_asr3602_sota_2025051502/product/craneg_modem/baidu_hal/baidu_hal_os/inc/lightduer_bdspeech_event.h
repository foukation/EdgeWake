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
 * File: lightduer_bdspeech_event.h
 * Auth: Su Hao (suhao@baidu.com)
 * Desc: Wrap the bds client events.
 */

#ifndef BAIDU_DUER_LIBDUER_DEVICE_MODULES_CONNAGENT_BDSPEECH_LIGHTDUER_BDSPEECH_EVENT_H
#define BAIDU_DUER_LIBDUER_DEVICE_MODULES_CONNAGENT_BDSPEECH_LIGHTDUER_BDSPEECH_EVENT_H

#include "lightduer_types.h"
#include "bds_client_event.h"

typedef struct _duer_bdsevt_message_s *duer_bdsevt_msg_handler;

duer_bdsevt_msg_handler duer_bdsevt_message_create(bds_client_event_t *event, void *custom);

bds_client_event_t *duer_bdsevt_message_get_event(duer_bdsevt_msg_handler handler);

duer_context duer_bdsevt_message_get_context(duer_bdsevt_msg_handler handler);

void duer_bdsevt_message_destroy(duer_bdsevt_msg_handler handler);

#endif/*BAIDU_DUER_LIBDUER_DEVICE_MODULES_CONNAGENT_BDSPEECH_LIGHTDUER_BDSPEECH_EVENT_H*/