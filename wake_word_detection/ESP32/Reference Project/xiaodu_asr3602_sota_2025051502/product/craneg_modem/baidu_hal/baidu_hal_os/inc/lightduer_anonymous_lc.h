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
 * File: lightduer_anonymous_lc.h
 * Author: Chen Chi (chenchi01@baidu.com)
 * Desc: anonymous long connection
 */

#ifndef BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_ANOMYMOUS_LC_H
#define BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_ANONYMOUS_LC_H

#include "baidu_json.h"
#include "lightduer_types.h"

#ifdef __cplusplus
extern "C" {
#endif

int duer_anonymous_lc_init();

void duer_anonymous_lc_reg_directive_cb(
        duer_status_t (*cb)(baidu_json *payload));

int duer_anonymous_lc_start();

void duer_anonymous_lc_stop();

int duer_anonymous_lc_send_push_ack(const char *message_id);

#ifdef __cplusplus
}
#endif

#endif  // BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_ANONYMOUS_LC_H
