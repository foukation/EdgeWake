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
 * File: lightduer_anonymous_auth.h
 * Author: Chen Chi (chenchi01@baidu.com)
 * Desc: anonymous http2 transport auth token
 */

#ifndef BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_ANONYMOUS_AUTH_H
#define BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_ANONYMOUS_AUTH_H

#include "lightduer_http2_client.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef void (*duer_anonymous_auth_token_cb_t)(duer_http2_handle handle, const char *token);

int duer_restore_anonymous_auth_token(const char *token);

duer_bool duer_need_refresh_anonymous_auth_token(duer_u32_t curr_time);

/**
 * Submit request for getting anonymous auth token.
 *
 * @RETURN: DUER_OK if successful, otherwise failed.
 */
int duer_start_get_anonymous_auth_token(
        duer_http2_handle handle, duer_anonymous_auth_token_cb_t callback);

/**
 * Returns DUER_TRUE if auth token is ready.
 */
duer_bool duer_has_anonymous_auth_token();

/**
 * Returns anonymous auth token.
 */
const char *duer_get_anonymous_auth_token();

#ifdef __cplusplus
}
#endif

#endif  // BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_ANONYMOUS_AUTH_H

