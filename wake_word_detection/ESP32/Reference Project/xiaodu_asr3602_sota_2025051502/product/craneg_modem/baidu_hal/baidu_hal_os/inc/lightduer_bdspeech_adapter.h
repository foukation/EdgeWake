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
 * File: lightduer_bdspeech_adapter.h
 * Auth: Su Hao (suhao@baidu.com)
 * Desc: Adapt all the bdspeech apis.
 */

#ifndef BAIDU_DUER_LIBDUER_DEVICE_MODULES_CONNAGENT_BDSPEECH_LIGHTDUER_BDSPEECH_ADAPTER_H
#define BAIDU_DUER_LIBDUER_DEVICE_MODULES_CONNAGENT_BDSPEECH_LIGHTDUER_BDSPEECH_ADAPTER_H

#include "lightduer_bdspeech_wrapper.h"
#include "lightduer_ca_conf.h"
#include "lightduer_asr.h"

typedef struct _duer_bdsadp_s *duer_bdsadp_handler;

typedef duer_status_t (*duer_bdsadp_data_callback)(const char *, duer_size_t);

typedef duer_status_t (*duer_bdsadp_event_callback)(duer_context, bds_client_event_t *event);

typedef duer_status_t (*duer_bdsadp_flow_data_callback)(const char *, const void *, duer_size_t);

typedef duer_status_t (*duer_bdsadp_session_finish_cb)(void *);

duer_bdsadp_handler duer_bdsadp_create(void);

void duer_bdsadp_destroy(duer_bdsadp_handler handler);

duer_status_t duer_bdsadp_boot(duer_bdsadp_handler handler, int launch_mode);

duer_status_t duer_bdsadp_start(duer_bdsadp_handler handler, duer_conf_handler conf);

duer_status_t duer_bdsadp_start_link(duer_bdsadp_handler handler);

duer_status_t duer_bdsadp_stop_link(duer_bdsadp_handler handler);

duer_status_t duer_bdsadp_link_send_ping(duer_bdsadp_handler handler);

duer_status_t duer_bdsadp_restart(duer_bdsadp_handler handler);
duer_status_t duer_bdsadp_stop(duer_bdsadp_handler handler);

duer_status_t duer_bdsadp_event_start(duer_bdsadp_handler handler);

duer_status_t duer_bdsadp_event_send(duer_bdsadp_handler handler,
                                     const void *data,
                                     duer_size_t size,
                                     const char *sn);
duer_status_t duer_bdsadp_event_cancel(duer_bdsadp_handler handler);

duer_status_t duer_bdsadp_asr_start(duer_bdsadp_handler handler, const duer_asrparam_t *param);

duer_status_t duer_bdsadp_asr_stop(duer_bdsadp_handler handler);

duer_status_t duer_bdsadp_asr_cancel(duer_bdsadp_handler handler);

duer_status_t duer_bdsadp_record_stop(duer_bdsadp_handler handler);

duer_status_t duer_bdsadp_wakeup_start(duer_bdsadp_handler handler);

duer_status_t duer_bdsadp_wakeup_stop(duer_bdsadp_handler handler);

duer_status_t duer_bdsadp_recorder_start(duer_bdsadp_handler handler, int type);

duer_status_t duer_bdsadp_recorder_stop(duer_bdsadp_handler handler, int type);

duer_status_t duer_bdsadp_set_data_listener(duer_bdsadp_handler handler,
                                            duer_bdsadp_data_callback listener);

duer_status_t duer_bdsadp_set_event_listener(duer_bdsadp_handler handler,
                                             duer_bdsadp_event_callback listener,
                                             duer_context context);

duer_status_t duer_bdsadp_set_flow_data_listener(duer_bdsadp_handler handler,
                                            duer_bdsadp_flow_data_callback listener);

duer_status_t duer_bdsadp_set_flow_data_listener(duer_bdsadp_handler handler,
                                                 duer_bdsadp_flow_data_callback listener);

duer_status_t duer_bdsadp_asr_set_mode(duer_bdsadp_handler handler, int mode);

duer_status_t duer_bdsadp_wakeup_restrain(duer_bdsadp_handler handler);

duer_status_t duer_bdsadp_set_config(duer_bdsadp_handler handler,
                                     uint32_t primary_pid,
                                     uint32_t assist_pid,
                                     const char *key);

void duer_bdsadp_set_session_finish_cb(duer_bdsadp_handler handler,
                                       duer_bdsadp_session_finish_cb cb);

void duer_bdsadp_ignore_asr_tts(duer_bool ignore);

duer_status_t duer_bdsadp_refresh_asr_config(duer_bdsadp_handler handler);

#endif/*BAIDU_DUER_LIBDUER_DEVICE_MODULES_CONNAGENT_BDSPEECH_LIGHTDUER_BDSPEECH_ADAPTER_H*/
