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
// Author: Chang Li (changli@baidu.com)
//
// Description: NTP client API decleration.

#include <stdint.h>
#include "lightduer_types.h"

#ifndef BAIDU_IOT_TINYDU_IOT_OS_SRC_IOT_BAIDU_CA_SOURCE_BAIDU_CA_IOT_NTP_H
#define BAIDU_IOT_TINYDU_IOT_OS_SRC_IOT_BAIDU_CA_SOURCE_BAIDU_CA_IOT_NTP_H

#ifdef __cplusplus
extern "C" {
#endif

typedef struct _DuerTime {
    uint32_t sec;
    uint32_t usec;
} DuerTime;

#define NTP_PKG_LEN 48
#define NTP_MAC_LEN 20  // contains 4 Bytes:Key ID and 16 Bytes:Message Authentication Code

#define MD5_KEYID_LEN  4
#define MD5_DIGEST_LEN 16
#define MD5_KEY_LEN    16

typedef struct _md5_key {
    uint32_t key_id;
    char key_str[MD5_KEY_LEN];
} Md5Key;

typedef void (*duer_ntp_time_change_callback_t)();

// External callback for getting current time.
typedef int (*duer_ntp_get_time_callback_t)();

/**
 * @brief check if a timestamp is valid (the timestamp >= a specific time point)
 *
 * @param time_sec: timestamp in seconds.
 *
 * @return 0 if invalid, 1 if valid.
 */
int duer_is_time_valid(uint32_t time_sec);

/**
 * @brief get NTP time
 * 
 * @param host: NTP server name. If host is NULL, use "s2c.time.edu.cn" by
 * default.
 *
 * @param timeout: timeout to acquire the NTP time, unit is micro seconds. If
 * it's 0, use 2000 ms by default. 
 *
 * @param[out] result_time: the acquired NTP time.
 *
 * @param key: Using keyed MD5 for Authentication for some specific NTP servers.
 * If unnecessary, specify it NULL.
 *
 * @param[out] clock_offset: the time difference (server_time - client_time) in microseconds.
 *
 * @return 0 if ok, negative value if failed. 
 */
int duer_ntp_client(
        char* host, int timeout, DuerTime *result_time, Md5Key *key, int64_t *clock_offset);

/**
 * @brief init mutex for ntp, must be called before any other ntp function.
 *
 * @return 0 if ok, negative value if failed.
 */
int duer_ntp_init();

/**
 * @brief destroy mutex for ntp.
 */
void duer_ntp_destroy();

/**
 * @brief sync ntp time in an endless loop until success. settimeofday will be called when the time
 *        is successfully retrieved.
 *
 * @param timeout_per_req: timeout in milliseconds for each request.
 * @param get_time_callback: external callback for getting current time.
 */
void duer_ntp_set_time_until_succ(
        int timeout_per_req, duer_ntp_get_time_callback_t get_time_callback);

/**
 * @brief return DUER_TRUE if ntp time is synchronized successfully.
 */
duer_bool duer_ntp_is_set_time_succ();

/**
 * @brief wait until timeout or the ntp time is successfully retrieved
 *
 * @param timeout: timeout in milliseconds
 *
 * @param[out] time: the acquired NTP time.
 *
 * @return 0 if ok, negative value if failed.
 */
int duer_ntp_wait_until_set_time(int timeout, DuerTime *time);

/**
 * @brief set current time. If ntp is already synchronized, this function has no effect.
 *
 * @return 0 if ok, negative value if failed.
 */
int duer_ntp_set_time(const DuerTime *time);

/**
 * @brief start timer for periodically sync with NTP server.
 *
 * @return 0 if ok, negative value if failed.
 */
int duer_ntp_start_poll();

/**
 * @brief register a callback for ntp time change.
 *
 * @return 0 if ok, negative value if failed.
 */
int duer_ntp_reg_time_change_callback(duer_ntp_time_change_callback_t callback);

/**
 * @brief invoke callbacks when time changes. Only for internal usage.
 */
void duer_ntp_invoke_time_change_callback();

/**
 * @brief set time and invoke callbacks. Only for testing usage.
 */
int duer_ntp_set_time_for_testing(const DuerTime *time);

#ifdef __cplusplus
}
#endif

#endif // BAIDU_IOT_TINYDU_IOT_OS_SRC_IOT_BAIDU_CA_SOURCE_BAIDU_CA_IOT_NTP_H

