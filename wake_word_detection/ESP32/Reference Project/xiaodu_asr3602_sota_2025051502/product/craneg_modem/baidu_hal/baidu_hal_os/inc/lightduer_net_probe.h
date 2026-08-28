/*
 * Copyright (c) 2019 Baidu, Inc. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
/**
 * @file lightduer_net_probe.h
 * @author wangning23@baidu.com
 */

#ifndef BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_NET_PROBE_H
#define BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_NET_PROBE_H

#ifdef __cplusplus
extern "C" {
#endif

#include <stdbool.h>

typedef enum {
    NET_UNAVAILABLE,
    NET_AVAILABLE
} internet_available_t;

typedef int (*duer_notify_net_status)(internet_available_t status);

typedef int (*duer_network_failed_dump_f)();

/**
 * DESC:Set connect router status
 * @PARAM[in] status.
 * @RETURN:   none.
 */
void duer_set_connect_router_status(bool status);

/**
 * DESC:Notify to probe net when wakeup
 * @PARAM[in] callback.
 * @RETURN:   none.
 */
void duer_notify_verify_net(void);

/**
 * DESC:Init net probe
 * @PARAM[in] callback.
 * @RETURN:   none.
 */
int duer_net_probe_init(void);

void duer_verify_cur_net(void);

internet_available_t duer_get_net_probe_status(void);

char *duer_get_net_probe_info_string();

void duer_net_probe_register_failed_cb(duer_network_failed_dump_f cb);

#ifdef __cplusplus
}
#endif

#endif // BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_NET_PROBE_H
