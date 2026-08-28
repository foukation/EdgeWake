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
 *
 * File: lightduer_auto_dns_local.h
 * Auth: Chen Chi (chenchi01@baidu.com)
 * Desc: auto dns local resolve.
 */
#ifndef BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_AUTO_DNS_LOCAL_H
#define BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_AUTO_DNS_LOCAL_H

#include "lightduer_lib.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 * init data structures for local resolve.
 */
int duer_auto_dns_local_resolve_init();

/**
 * destroy data structures for local resolve.
 */
void duer_auto_dns_local_resolve_destroy();

/**
 * local dns resolve without timeout.
 *
 * @RETURN: DUER_OK if successful, otherwise failed.
 */
int duer_auto_dns_sync_local_resolve(const char *host, duer_u32_t *ip);

/**
 * Return true if duer_auto_dns_local_resolve_init() has been called successfully.
 */
duer_bool duer_auto_dns_is_local_resolve_initialized();

/**
 * Start an async local dns resolve.
 *
 * PARAM
 * @param[in] host: the host to be resolved
 *
 * @RETURN: context pointer. duer_auto_dns_local_resolve_wait() needs this pointer.
 *          NULL if failed.
 */
void *duer_auto_dns_local_resolve_start(const char *host);

/**
 * Wait for local dns resolve result.
 *
 * PARAM
 * @param[in] ctx: context pointer.
 * @param[in] timeout_ms: timeout in milliseconds.
 * @param[out] ip: the resolved ip.
 *
 * @RETURN: DUER_OK if successful, otherwise failed.
 */
int duer_auto_dns_local_resolve_wait(void *ctx, duer_u32_t timeout_ms, duer_u32_t *ip);

/**
 * Return true if the parameter is a valid ip.
 */
duer_bool duer_auto_dns_is_valid_ip(duer_u32_t ip);

#ifdef __cplusplus
}
#endif

#endif  // BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_AUTO_DNS_LOCAL_H

