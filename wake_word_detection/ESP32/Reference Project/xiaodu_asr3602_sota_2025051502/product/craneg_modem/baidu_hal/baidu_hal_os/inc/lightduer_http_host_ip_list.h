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
 * File: lightduer_http_host_ip_list.h
 * Auth: Chen Chi (chenchi01@baidu.com)
 * Desc: duer_host_ip_list_t and duer_ip_list_t declaration.
 */

#ifndef BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_HTTP_HOST_IP_LIST_H
#define BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_HTTP_HOST_IP_LIST_H

#include "lightduer_types.h"

#ifdef __cplusplus
extern "C" {
#endif

#define DUER_HTTP_DNS_IP_LEN_MAX  (20)  // the max length of ip

// ipv4 address in host byte order.
typedef duer_u32_t duer_ip_item_t;

// an array of ip item.
typedef struct duer_ip_list_s {
    duer_ip_item_t *list;
    duer_u32_t count;
} duer_ip_list_t;

// struct of a host and an ip list.
typedef struct duer_host_ip_list_s {
    char *host;
    duer_ip_list_t *ip_list;
    duer_u32_t ttl;
} duer_host_ip_list_t;

// ip_list operations.
//   alloc an ip list
duer_ip_list_t *duer_http_alloc_ip_list(int count);

//   copy the elements [index, index + max_count) in an ip list.
//   For example: a list contains 5 elements, index = 3, max_count = 4, then
//                the returned list will contain 3, 4, 0, 1.
duer_ip_list_t *duer_http_copy_ip_list(duer_ip_list_t *node, int index, int max_count);

//   free a linked list.
void duer_http_free_ip_list(duer_ip_list_t *ip_list);

// free an array of host ip list.
void duer_http_free_host_ip_list(duer_host_ip_list_t *host_ip_list, int count);

#ifdef __cplusplus
}
#endif
#endif // BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_HTTP_HOST_IP_LIST_H
