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
 * File: lightduer_bdspeech_parser_adapter.h
 * Auth: Gang Chen (chengang12@baidu.com)
 * Desc: Bdspeech data parser adapter.
 */

#ifndef BAIDU_DUER_LIBDUER_DEVICE_MODULES_CONNAGENT_BDSPEECH_LIGHTDUER_BDSPEECH_PARSER__ADAPTER_H
#define BAIDU_DUER_LIBDUER_DEVICE_MODULES_CONNAGENT_BDSPEECH_LIGHTDUER_BDSPEECH_PARSER__ADAPTER_H

#include "lightduer_bdspeech_parser.h"
#include "bds_client_event.h"

typedef void *duer_bds_dpadp_hdlr_t;
typedef duer_status_t (*duer_bds_dpadp_data_callback)(const char *, duer_size_t);
typedef duer_status_t (*duer_bds_dpadp_flow_callback)(const char *, const void *, duer_size_t);
typedef duer_status_t (*duer_dbs_dpadp_session_finish_cb)(void *);

duer_bds_dpadp_hdlr_t duer_bds_dpadp_create(void);

void duer_bds_dpadp_destroy(duer_bds_dpadp_hdlr_t adapter);

duer_status_t duer_bds_dpadp_event_handle(duer_bds_dpadp_hdlr_t adapter,
                                          bds_client_event_t *event);

duer_status_t duer_bds_dpadp_set_data_cb(duer_bds_dpadp_hdlr_t adapter,
                                         duer_bds_dpadp_data_callback cb);

void duer_bds_dpadp_set_session_finish_cb(duer_bds_dpadp_hdlr_t adapter,
                                          duer_dbs_dpadp_session_finish_cb cb);

void duer_bds_dpadp_set_flow_cb(duer_bds_dpadp_hdlr_t adapter,
                                duer_bds_dpadp_flow_callback cb);

#endif/*BAIDU_DUER_LIBDUER_DEVICE_MODULES_CONNAGENT_BDSPEECH_LIGHTDUER_BDSPEECH_PARSER__ADAPTER_H*/