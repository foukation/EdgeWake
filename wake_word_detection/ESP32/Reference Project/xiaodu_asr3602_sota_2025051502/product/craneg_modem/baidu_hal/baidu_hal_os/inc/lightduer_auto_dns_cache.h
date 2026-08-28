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
 * File: lightduer_auto_dns_cache.c
 * Auth: Chen Chi (chenchi01@baidu.com)
 * Desc: auto dns cache data structure
 */

#ifndef BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_AUTO_DNS_CACHE_H
#define BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_AUTO_DNS_CACHE_H

#include "lightduer_http_host_ip_list.h"
#include "lightduer_lib.h"
#include "lightduer_queue_cache.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 * @RETURN: DUER_TRUE if duer_init_host_cache_mutex() has run successfully.
 */
duer_bool duer_is_host_cache_mutex_initialized();

/**
 * Init mutex for host cache.
 *
 * @RETURN: DUER_OK if successful.
 */
int duer_init_host_cache_mutex();

/**
 * Destroy mutex for host cache.
 */
void duer_destroy_host_cache_mutex();

/**
 * Destroy a host cache list (without lock).
 */
void duer_destroy_host_cache_list(duer_qcache_handler list);

/**
 * Print a host cache list for debugging.
 */
void duer_print_host_cache_list(duer_qcache_handler list);

/**
 * Print an ip list for debugging.
 */
int duer_print_ip_list(duer_ip_list_t *ip_list);

/**
 * Search a host in a host cache list.
 *
 * PARAM:
 * @param[in] list: the cache list
 * @param[in] host: the target host to be searched
 * @param[in] max_len: max ip count to return
 * @param[out] time_since_expired: seconds since the cache is expired. Negative if not expired.
 *
 * @RETURN: the ip list for the host, NULL if the host not found in cache.
 */
duer_ip_list_t *duer_find_from_host_cache(
        duer_qcache_handler list, const char *host, int max_len, int *time_since_expired);

/**
 * Search a host in multiple host cache lists.
 *
 * PARAM:
 * @param[in] list0: the first cache list
 * @param[in] list1: the second cache list
 * @param[in] list2: the third cache list
 * @param[in] host: the target host to be searched
 * @param[in] max_len: max ip count to return
 * @param[out] time_since_expired: seconds since the cache is expired. Negative if not expired.
 * @param[out] from: the index of cache list containing the host
 *
 * @RETURN: the ip list for the host, NULL if the host not found in cache.
 */
duer_ip_list_t *duer_find_from_multi_host_cache(
        duer_qcache_handler list0, duer_qcache_handler list1, duer_qcache_handler list2,
        const char *host, int max_len, int *time_since_expired, int *from);

/**
 * Update a host entry in a cache list.
 */
int duer_update_host_cache(duer_qcache_handler list, duer_qcache_handler dynamic_fallback_list,
        char *host, duer_ip_list_t *ip_list, duer_u32_t ttl);

/**
 * Replace all cache entries of a cache list.
 */
void duer_replace_host_cache(duer_qcache_handler list, duer_host_ip_list_t *host_ip_list,
        int count);

/**
 * Serialize cache entries to a buffer.
 */
int duer_dump_host_cache(duer_qcache_handler list, char *buf, duer_u32_t max_length,
        duer_u32_t *length);
/**
 * Deserialize a buffer into cache list.
 */
int duer_load_host_cache(duer_qcache_handler list, const char *buf, duer_u32_t length);

/**
 * Fill a single ip into ip_list.
 */
int fill_single_ip_into_list(duer_u32_t ip, duer_ip_list_t **ip_list);
/**
 * Fill static host ips into cache.
 */
void fill_host_ips_into_cache(const char *(*host_ip_list)[2], int count,
        duer_qcache_handler list);

#ifdef __cplusplus
}
#endif

#endif  // BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_AUTO_DNS_CACHE_H

