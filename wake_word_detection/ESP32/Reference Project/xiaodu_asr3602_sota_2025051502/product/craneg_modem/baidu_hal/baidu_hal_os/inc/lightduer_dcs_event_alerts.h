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
 * File: lightduer_dcs_event_alerts.h
 * Auth: lingfeng meng (menglingfeng@baidu.com)
 * Desc: apply APIs to support DCS Event Alerts Interface.
 */
#ifndef BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_DCS_EVENT_ALERTS_H
#define BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_DCS_EVENT_ALERTS_H
#include "lightduer_dcs_local.h"
#include "lightduer_connagent.h"
#include "lightduer_log.h"
#include "lightduer_ds_log_dcs.h"

typedef struct event_alerts_ops_s {
    /// play url
    void (*play)(char *url, uint16_t count, int volume);
} event_alerts_ops_t;

/* Error Code. */
typedef enum {
    PLAY_ALERT_SUCCEEDED,
    PLAY_ALERT_FAILED
} duer_mesh_event_alerts_type;

/**
 * DESC:
 * Initialize event alerts interface.
 *
 * PARAM: none
 *
 * @RETURN: none
 */
void duer_dcs_event_alerts_init(void);

/*
 * Register event alerts ops
 *
 * @param ops:
 *
 * @return duer_status_t: Success: DUER_OK
 *              Failed:  Other
 */
duer_status_t duer_register_event_alerts_ops(event_alerts_ops_t *ops);

/*
 * unRegister event alerts ops
 *
 * @param void:
 *
 * @return duer_status_t: Success: DUER_OK
 *              Failed:  Other
 */
duer_status_t duer_unregister_event_alerts_ops();

void duer_report_play_alert_event(duer_mesh_event_alerts_type type);
#endif/*BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_DCS_EVENT_ALERTS_H*/