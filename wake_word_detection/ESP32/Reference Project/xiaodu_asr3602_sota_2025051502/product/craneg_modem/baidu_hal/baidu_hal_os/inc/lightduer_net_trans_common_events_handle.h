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
 * Author: Chen Chi (chenchi01@baidu.com)
 * Desc: common events handle for unimportant blocking net io (ntp/http dns).
 */

#ifndef BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_NET_TRANS_COMMON_EVENTS_HANDLE
#define BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_NET_TRANS_COMMON_EVENTS_HANDLE

#include "lightduer_events.h"

#ifdef __cplusplus
extern "C" {
#endif

/** Create the events handle. */
int duer_trans_init_common_events_handle();

/** Destroy the events handle */
void duer_trans_destroy_common_events_handle();

/** Get the events handle */
duer_events_handler duer_trans_get_common_events_handle();

#ifdef __cplusplus
}
#endif

#endif  // BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_NET_TRANS_COMMON_EVENTS_HANDLE
