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
 * File: lightduer_auto_dns.h
 * Auth: Chen Xihao (chenxihao@baidu.com)
 * Desc: Support auto dns to resolve ip address
 */

#ifndef BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_AUTO_DNS_H
#define BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_AUTO_DNS_H

#include "lightduer_types.h"
#include "lightduer_http_host_ip_list.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef int (*duer_auto_dns_read_storage_f)(char *buf, duer_u32_t max_length, duer_u32_t *length);
typedef int (*duer_auto_dns_write_storage_f)(const char *buf, duer_u32_t length);

typedef enum {
    DUER_AUTO_DNS_RESOLVE_HTTP,
    DUER_AUTO_DNS_RESOLVE_LOCAL,
    DUER_AUTO_DNS_RESOLVE_DYNAMIC_FALLBACK,
    DUER_AUTO_DNS_RESOLVE_STATIC_FALLBACK,
    DUER_AUTO_DNS_RESOLVE_HOST_IS_IP,
    DUER_AUTO_DNS_RESOLVE_FAILED,
} duer_auto_dns_resolve_type_t;

/**
 * DESC:
 * Initialize the context of auto dns
 *
 * PARAM:
 * @param[in] account_id: the account id of baidu http dns
 * @param[in] secret: the secret of baidu http dns
 *
 * @RETURN: success return 0, failed return negative number.
 */
int duer_auto_dns_init(const char *account_id, const char *secret);

/**
 * DESC:
 * Register callbacks for load/dump fallback ip cache to flash storage.
 *
 * PARAM:
 * @param[in] write_storage_callback: callback for writing fallback ip to flash
 */
void duer_auto_dns_reg_storage_callback(
        duer_auto_dns_write_storage_f write_storage_callback);

/**
 * DESC:
 * Uninitialize the context of auto dns
 *
 * @RETURN: success return 0, failed return negative number.
 */
int duer_auto_dns_deinit(void);

/**
 * DESC:
 * Resolve domain name to get ip address
 * resolve order: http dns cache -> local dns -> fallback cache
 * if auto dns is not been initialized, only local dns to resovle
 *
 * PARAM:
 * @param[in] host: domain name or ip address
 * @param[out] ip: ip address
 *
 * @RETURN: success return 0, failed return negative number.
 */
int duer_auto_dns_resolve(const char *host, char *ip);

/**
 * DESC:
 * Same as duer_audo_dns_resolve(), except that ipv4 address is returned as uint32
 * (in host order).
 *
 * PARAM:
 * @param[in] host: domain name or ip address
 * @param[out] ip: ip address (host order)
 *
 * @RETURN: success return 0, failed return negative number.
 */
int duer_auto_dns_resolve_uint(const char *host, duer_u32_t *ip);

/**
 * DESC:
 * Same as duer_audo_dns_resolve(), except that multiple ipv4 address is returned
 * (up to max_ip_count).
 *
 * PARAM:
 * @param[in] host: domain name or ip address
 * @param[in] max_ip_count: max ip count to be returned.
 * @param[out] ip_list: resolved ip list.
 *
 * @RETURN: success return 0, failed return negative number.
 */
int duer_auto_dns_resolve_multi_ip(const char *host, int max_ip_count, duer_ip_list_t **ip_list);

/**
 * DESC:
 * Same as duer_audo_dns_resolve_multi_ip(), except that dns resolve type is returned.
 *
 * PARAM:
 * @param[in] host: domain name or ip address
 * @param[in] max_ip_count: max ip count to be returned.
 * @param[out] ip_list: resolved ip list.
 * @param[out] resolve_type: which type of cache/service is the ip resolved from.
 *
 * @RETURN: success return 0, failed return negative number.
 */
int duer_auto_dns_resolve_multi_ip_with_type(const char *host, int max_ip_count, duer_ip_list_t **ip_list,
        duer_auto_dns_resolve_type_t *resolve_type);

/**
 * DESC:
 * Start scheduling timers for presolving hosts.
 * This function should be called when ntp is ready.
 */
void duer_auto_dns_start_pre_resolve();

/**
 * DESC:
 * Fill fallback ip entries from a serialized buffer.
 *
 * PARAM:
 * @param[in] buffer: the serialized buffer for fallback ip. NULL if the fallback ip does not exist.
 * @param[in] len: buffer length.
 */
int duer_auto_dns_fill_fallback_ip(const char *buffer, duer_u32_t len);

/**
 * DESC:
 * Start scheduling timers for retrieving fallback ip from cloud server.
 * This function should be called when ntp is ready.
 *
 * PARAM:
 * @param[in] last_request_time: last timestamp when the fallback ip was retrieved from server.
 */
void duer_auto_dns_start_request_fallback_ip(duer_u32_t last_request_time);

/**
 * DESC:
 * Print auto dns cache info.
 */
void duer_auto_dns_print_cache();

/**
 * DESC:
 * Set fallback ip request url. Only for testing.
 */
void duer_auto_dns_set_fallback_ip_url(const char *url);

/**
 * DESC:
 * Set fallback ip request duration in seconds. Only for testing.
 */
int duer_auto_dns_set_fallback_ip_duration(int duration_sec);

/**
 * DESC:
 * Reset fallback ip request duration to default. Only for testing.
 */
void duer_auto_dns_reset_fallback_ip_duration();

/**
 * DESC:
 * Enable print all resolved ip. Only for testing.
 */
void duer_auto_dns_set_print_all_resolved_ip(duer_bool is_print);

#ifdef __cplusplus
}
#endif

#endif /* BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_AUTO_DNS_H */
