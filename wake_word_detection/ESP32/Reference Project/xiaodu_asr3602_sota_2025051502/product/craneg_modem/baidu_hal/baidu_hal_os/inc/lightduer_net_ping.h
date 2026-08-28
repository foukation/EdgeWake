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
 * @file lightduer_net_ping.h
 * @author chenchi01@baidu.com
 */

#ifndef BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_NET_PING_H
#define BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_NET_PING_H

#include "lightduer_lib.h"
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef struct duer_ping_result_s {
    uint32_t ip;
    uint32_t send_count;
    uint32_t recv_count;
    uint32_t error_count;
    uint32_t dns_resolve_fail;
    uint32_t max_time;
    uint32_t min_time;
    uint32_t avg_time;
} duer_ping_result_t;

typedef void (*duer_ping_result_cb_t)(const duer_ping_result_t *result, void *data);

/**
 * init data structures (mutex) for ping.
 */
int duer_ping_init();

/**
 * Submit ping request
 *
 * @param[in] ip: ping target ip
 * @param[in] count: ping count for this ip
 * @param[in] callback: function pointer which will be called when all ping requests are done for
 *                      this ip
 * @param[in] data: custom data passed as parameter of the callback
 *
 * @RETURN: DUER_OK if successful, otherwise failed.
 */
int duer_submit_ping_request_by_ip(
        uint32_t ip, uint32_t count, duer_ping_result_cb_t callback, void *data);

/**
 * Submit ping request
 *
 * @param[in] host: ping target host.
 * @param[in] count: ping count for this host
 * @param[in] callback: function pointer which will be called when all ping requests are done for
 *                      this ip
 * @param[in] data: custom data passed as parameter of the callback
 *
 * @RETURN: DUER_OK if successful, otherwise failed.
 */
int duer_submit_ping_request_by_host(
        const char *host, uint32_t count, duer_ping_result_cb_t callback, void *data);

/**
 * Start ping request. A new task will be created in PSRAM.
 *
 * @RETURN: DUER_OK if successful, otherwise failed.
 */
int duer_ping_start();

/**
 * Stop ping request.
 */
void duer_ping_stop();

#ifdef __cplusplus
}
#endif

#endif  // BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_NET_PING_H

