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
 * File: lightduer_bdspeech_wrapper.h
 * Auth: Su Hao (suhao@baidu.com)
 * Desc: Wrap the bds client interfaces.
 */

#ifndef BAIDU_DUER_LIBDUER_DEVICE_MODULES_CONNAGENT_BDSPEECH_LIGHTDUER_BDSPEECH_WRAPPER_H
#define BAIDU_DUER_LIBDUER_DEVICE_MODULES_CONNAGENT_BDSPEECH_LIGHTDUER_BDSPEECH_WRAPPER_H

#include "lightduer_types.h"
#include "lightduer_ca_conf.h"
#include "bds_client.h"
#include "lightduer_asr.h"

typedef struct _duer_bdspeech_s *duer_bdspeech_handler;

duer_bdspeech_handler duer_bdspeech_create(void);

duer_status_t duer_bdspeech_boot(duer_bdspeech_handler handler, int mode);

duer_status_t duer_bdspeech_start(duer_bdspeech_handler handler, duer_conf_handler config);

duer_status_t duer_bdspeech_restart(duer_bdspeech_handler handler);
duer_status_t duer_bdspeech_stop(duer_bdspeech_handler handler);

duer_status_t duer_bdspeech_set_listener(duer_bdspeech_handler handler, bds_client_event_listener_f callback, void *custom);

duer_status_t duer_bdspeech_event_start(duer_bdspeech_handler handler);

duer_status_t duer_bdspeech_event_send(duer_bdspeech_handler handler,
                                       const void *buffer,
                                       size_t size,
                                       const char *sn);
duer_status_t duer_bdspeech_event_cancel(duer_bdspeech_handler handler);

duer_status_t duer_bdspeech_wakeup_start(duer_bdspeech_handler handler);

duer_status_t duer_bdspeech_wakeup_stop(duer_bdspeech_handler handler);

duer_status_t duer_bdspeech_asr_start(duer_bdspeech_handler handler, const duer_asrparam_t *param);

duer_status_t duer_bdspeech_asr_stop(duer_bdspeech_handler handler);

duer_status_t duer_bdspeech_asr_cancel(duer_bdspeech_handler handler);

duer_status_t duer_bdspeech_lc_url_set(const char *url);

duer_status_t duer_bdspeech_link_start(duer_bdspeech_handler handler);

duer_status_t duer_bdspeech_link_stop(duer_bdspeech_handler handler);

duer_status_t duer_bdspeech_link_send_ping(duer_bdspeech_handler handler);


duer_status_t duer_bdspeech_recorder_start(duer_bdspeech_handler handler, int type);

duer_status_t duer_bdspeech_recorder_stop(duer_bdspeech_handler handler, int type);

duer_status_t duer_bdspeech_asr_set_mode(duer_bdspeech_handler handler, int mode);

duer_status_t duer_bdspeech_wakeup_restrain(duer_bdspeech_handler handler);

duer_status_t duer_bdspeech_switch_ip(duer_bdspeech_handler handler);

duer_status_t duer_bdspeech_switch_config(duer_bdspeech_handler handler,
                                          duer_u32_t primary_pid,
                                          duer_u32_t assist_pid,
                                          const char *key);

void duer_bdspeech_destroy(duer_bdspeech_handler handler);

void duer_bdsspeech_send_tts_header(duer_bdspeech_handler hdlr,const void *data, int len);

duer_status_t duer_bdspeech_refresh_asr_config(duer_bdspeech_handler handler);

#ifdef FUNC_PATH_DISPLAY_SUPPORT
#define FUNC_ENTER(...)     DUER_LOGI("%s enter", __func__)
#define FUNC_EXIT(...)     DUER_LOGI("%s exit", __func__)
#else
#define FUNC_ENTER(...)
#define FUNC_EXIT(...)
#endif

#endif/*BAIDU_DUER_LIBDUER_DEVICE_MODULES_CONNAGENT_BDSPEECH_LIGHTDUER_BDSPEECH_WRAPPER_H*/
