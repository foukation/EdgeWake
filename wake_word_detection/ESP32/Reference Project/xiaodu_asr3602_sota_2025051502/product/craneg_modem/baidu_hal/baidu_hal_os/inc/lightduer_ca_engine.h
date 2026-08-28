/**
 * Copyright (2017) Baidu Inc. All rights reserved.
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
 * File: lightduer_engine.h
 * Auth: Su Hao (suhao@baidu.com)
 * Desc: Light duer IoT CA engine.
 */

#ifndef BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_ENGINE_H
#define BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_ENGINE_H

#include "baidu_json.h"
#include "lightduer_types.h"
#include "lightduer_coap_defs.h"

#ifdef __cplusplus
extern "C" {
#endif

enum duer_engine_events_enum {
    DUER_EVT_CREATE,
    DUER_EVT_START,
    DUER_EVT_ADD_RESOURCES,
    DUER_EVT_ENQUEUE_REPORT,
    DUER_EVT_ENQUEUE_RESPONSE,
    DUER_EVT_SEND_DATA,
    DUER_EVT_DATA_AVAILABLE,
    DUER_EVT_STOP,
    DUER_EVT_DESTROY,
    DUER_EVT_WAKEUP,
    DUER_EVT_ASR_START,
    DUER_EVT_ASR_MID,
    DUER_EVT_ASR_END,
    DUER_EVT_ASR_CANCEL,
    DUER_EVT_DSP_OK,
    DUER_EVT_DSP_ERROR,
    DUER_EVT_DISCONNECTED,
    DUER_EVENTS_TOTAL
};

typedef enum {
    DUER_DATA_REPORT_PARALLEL,
    DUER_DATA_REPORT_SERIAL,
} duer_data_mode_t;

typedef void (*duer_engine_notify_func)(int event, int status, int what, void *object);
typedef duer_status_t (*duer_engine_flow_data_cb)(const char *, const void *, duer_size_t);
typedef duer_status_t (*duer_engine_session_finish_cb)(void *);
typedef duer_status_t (*duer_engine_data_cb)(const char *, duer_size_t);
typedef void (*duer_engine_asr_cancel_cb)(void);
typedef void (*duer_engine_data_flushed_cb)(void);
typedef void (*duer_engine_wakeup_state_changed_cb)(duer_bool enabled);
typedef void (*duer_engine_data_report_finish_cb)(const char *, duer_bool, const char *);

void duer_engine_register_notify(int what, void *object);

void duer_engine_create(int what, void *object);

void duer_engine_start(int what, void *object);

void duer_engine_add_resources(int what, void *object);

void duer_engine_data_available(int what, void *object);

void duer_engine_enqueue_report_data(int what, void *data);

void duer_engine_enqueue_response_data(int what, void *data);

void duer_engine_stop(int what, void *object);

void duer_enable_engine(void);

void duer_engine_destroy(int what, void *object);

int duer_engine_qcache_length(void);

duer_bool duer_engine_is_started(void);

const char *duer_engine_get_bind_token(void);

void duer_engine_bds_link_start();

void duer_engine_bds_link_stop();

void duer_engine_bds_link_send_ping(void);

void duer_engine_asr_start(int what, void *object);

void duer_engine_asr_stop(int what, void *object);

void duer_engine_asr_cancel(int what, void *object);

void duer_engine_set_asr_cancel_cb(duer_engine_asr_cancel_cb cb);

void duer_engine_set_wakeup_state_changed_cb(duer_engine_wakeup_state_changed_cb cb);

void duer_engine_wakeup_start(int what, void *object);

void duer_engine_wakeup_stop(int what, void *object);

void duer_engine_recorder_start(int what, void *object);

void duer_engine_recorder_stop(int what, void *object);

void duer_engine_finalize(int what, void *object);

duer_status_t duer_bdsadp_user_stop(void);
duer_status_t duer_bdsadp_user_restart(void);

void duer_engine_set_flow_data_cb(duer_engine_flow_data_cb cb);

void duer_engine_bds_boot(int what, void *object);

void duer_engine_asr_set_mode(int what, void *object);

void duer_engine_set_session_finish_cb(duer_engine_session_finish_cb cb);

void duer_engine_bds_log_level_set(int level);

duer_status_t duer_engine_set_bds_cfg(duer_u32_t primary_pid,
                                      duer_u32_t assist_pid,
                                      const char *key);

void duer_engine_print_event_switch(duer_bool state);

duer_bool duer_engine_need_print_event();

void duer_engine_set_data_cb(duer_engine_data_cb cb);

void duer_engine_set_data_flushed_cb(duer_engine_data_flushed_cb cb);

void duer_engine_wakeup_restrain(int what, void *object);

void duer_engine_reg_data_report_finish_cb(duer_engine_data_report_finish_cb cb);

void duer_engine_ignore_asr_tts(duer_bool ignore);

void duer_engine_refresh_bds_asr_config(int what, void *data);

#ifdef __cplusplus
}
#endif

#endif/*BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_ENGINE_H*/
