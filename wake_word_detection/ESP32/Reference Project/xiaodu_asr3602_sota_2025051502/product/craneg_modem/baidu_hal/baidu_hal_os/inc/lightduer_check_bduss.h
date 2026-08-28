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
 * File: lightduer_check_bduss.h
 * Author: Chen Chi (chenchi01@baidu.com)
 * Desc: call checkBDUSS api to validate bduss.
 */

#ifndef BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_CHECK_BDUSS_H
#define BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_CHECK_BDUSS_H

#ifdef __cplusplus
extern "C" {
#endif

typedef enum {
    DUER_CHECK_BDUSS_VALID,
    DUER_CHECK_BDUSS_INVALID,
    DUER_CHECK_BDUSS_ERROR
} duer_check_bduss_result_t;

#define DUER_CHECK_BDUSS_MSG_SIZE (128)

duer_check_bduss_result_t duer_do_check_bduss(
        const char *client_id, const char *cuid, const char *sn, const char *user_agent,
        const char *bduss, const char *trace_id, char *err_msg);

#ifdef __cplusplus
}
#endif

#endif  // BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_CHECK_BDUSS_H

