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
 * File: lightduer_http2_buffer.h
 * Author: Chen Chi (chenchi01@baidu.com)
 * Desc: buffer management
 */

#ifndef BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_HTTP2_BUFFER_H
#define BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_HTTP2_BUFFER_H

#include "baidu_json.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef struct {
    char *buffer;
    size_t max_size;
    size_t size;

    int status;
} duer_http2_buffer;

void duer_http2_buffer_init(duer_http2_buffer *ptr);

int duer_http2_buffer_append(duer_http2_buffer *ptr, const char *data, size_t size);

baidu_json *duer_http2_buffer_append_json_part(
        duer_http2_buffer *ptr, const char *data, size_t size);

void duer_http2_buffer_clear(duer_http2_buffer *ptr);

#ifdef __cplusplus
}
#endif

#endif  // BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_HTTP2_BUFFER_H

