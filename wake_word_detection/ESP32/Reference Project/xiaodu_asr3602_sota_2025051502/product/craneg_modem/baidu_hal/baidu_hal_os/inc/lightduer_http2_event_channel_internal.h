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
 * File: lightduer_http2_event_channel_internal.h
 * Author: Chen Chi (chenchi01@baidu.com)
 * Desc: http2 event channel (sending push_ack) operations
 */

#ifndef BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_HTTP2_EVENT_CHANNEL_INTERNAL_H
#define BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_HTTP2_EVENT_CHANNEL_INTERNAL_H

#include "lightduer_http2_client.h"

#ifdef __cplusplus
extern "C" {
#endif

int duer_http2_event_init();

void duer_http2_event_cleanup();

void duer_http2_event_destroy();

int duer_http2_send_push_ack(const char *message_id);

int duer_http2_check_event_queue(duer_http2_handle handle, const char *token);

#ifdef __cplusplus
}
#endif

#endif  // BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_HTTP2_EVENT_CHANNEL_INTERNAL_H

