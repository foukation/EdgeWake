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
 * File: lightduer_http2_down_channel_internal.h
 * Author: Chen Chi (chenchi01@baidu.com)
 * Desc: http2 down channel (directive receiving) operations
 */

#ifndef BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_HTTP2_DOWN_CHANNEL_INTERNAL_H
#define BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_HTTP2_DOWN_CHANNEL_INTERNAL_H

#include "baidu_json.h"
#include "lightduer_http2_client.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef duer_status_t (*duer_http2_directive_cb)(baidu_json *payload);

void duer_http2_reg_directive_cb(duer_http2_directive_cb cb);

int duer_http2_send_down_channel_request(duer_http2_handle handle, const char *token);

void duer_http2_down_channel_cleanup();

#ifdef __cplusplus
}
#endif

#endif  // BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_HTTP2_DOWN_CHANNEL_INTERNAL_H

