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
 * File: lightduer_http_client_ops.h
 * Auth: CHEN YUN (chenyun08@baidu.com)
 * Desc: HTTP DNS Client Socket Head File
 */

#ifndef BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_HTTP_DNS_CLIENT_OPS_H
#define BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_HTTP_DNS_CLIENT_OPS_H

#include "lightduer_http_client.h"
#include "lightduer_http_host_ip_list.h"

#ifdef __cplusplus
extern "C" {
#endif
typedef enum
{
    HTTP_DNS_QUERY_UNKNOWN = -1,
    HTTP_DNS_QUERY_START,
    HTTP_DNS_QUERYING,
    HTTP_DNS_QUERY_SUCC,
    HTTP_DNS_QUERY_FAIL,
}duer_http_dns_query_state_t;

typedef void (*duer_http_dns_query_callback_f)(duer_host_ip_list_t *host_ip_list, int count, void *ctx);

void duer_http_dns_task_init();

void duer_http_dns_task_destroy();

// Async resolves ip.
// Note: The callback is responsible for freeing memory of host_ip_list.
int duer_http_dns_query_async(const char *host, duer_http_dns_query_callback_f callback, void *ctx);

int duer_http_dns_set_account(const char *account_id, const char *secret);

// Set fallback ip request url. Only for testing.
void duer_http_dns_set_fallback_ip_url(const char *url);

// Schedules a timer for requesting fallback ip.
int duer_http_dns_start_request_fallback_ip(
        duer_http_dns_query_callback_f callback, duer_u32_t delay_ms, void *ctx);

void duer_http_dns_set_connect_timeout_ms(size_t connect_timeout_ms);

void duer_http_dns_set_recv_timeout_ms(size_t recv_timeout_ms);

#ifdef __cplusplus
}
#endif
#endif // BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_HTTP_DNS_CLIENT_OPS_H
