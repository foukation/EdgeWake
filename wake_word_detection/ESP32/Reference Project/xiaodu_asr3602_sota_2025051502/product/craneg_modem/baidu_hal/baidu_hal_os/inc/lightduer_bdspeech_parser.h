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
 * File: lightduer_bdspeech_parser.h
 * Auth: Gang Chen (chengang12@baidu.com)
 * Desc: Bdspeech data parse APIs.
 */

#ifndef BAIDU_DUER_LIBDUER_DEVICE_MODULES_CONNAGENT_BDSPEECH_LIGHTDUER_BDSPEECH_PARSER_H
#define BAIDU_DUER_LIBDUER_DEVICE_MODULES_CONNAGENT_BDSPEECH_LIGHTDUER_BDSPEECH_PARSER_H

#include "lightduer_bdspeech_event.h"

typedef void *duer_bds_dp_hdlr_t;

typedef enum {
    BDS_DATA_PARSE_FAILED = -1,
    BDS_DATA_PARSER_CACHING,
    BDS_DATA_PARSE_PART_FINISHED,
    BDS_DATA_PARSE_FINISHED,
} duer_bds_dp_status_t;

typedef struct {
    int header_size;
    duer_context context;
} duer_bds_dp_param_t;

typedef duer_status_t (*duer_bds_dp_data_callback)(const char *, duer_size_t);
typedef duer_status_t (*duer_bds_dp_flow_callback)(const char *, const void *, duer_size_t);

duer_bds_dp_hdlr_t duer_bds_dp_create(duer_bds_dp_param_t *param);

void duer_bds_dp_destroy(duer_bds_dp_hdlr_t hdlr);

duer_bds_dp_status_t duer_bds_data_parse(duer_bds_dp_hdlr_t hdlr,
                                                 bdsc_event_data_t *data,
                                                 int data_off);

void duer_bds_dp_clear(duer_bds_dp_hdlr_t hdlr, char *sn);

void duer_bds_set_header_size(duer_bds_dp_hdlr_t hdlr, uint32_t size);

duer_context duer_bds_dp_get_context(duer_bds_dp_hdlr_t hdlr);

void duer_bds_dp_expired_clear(duer_bds_dp_hdlr_t hdlr);

duer_status_t duer_bds_dp_set_data_cb(duer_bds_dp_hdlr_t hdlr,
                                      duer_bds_dp_data_callback cb);

duer_status_t duer_bds_dp_set_flow_cb(duer_bds_dp_hdlr_t hdlr,
                                      duer_bds_dp_flow_callback cb);

#endif/*BAIDU_DUER_LIBDUER_DEVICE_MODULES_CONNAGENT_BDSPEECH_LIGHTDUER_BDSPEECH_PARSER_H*/