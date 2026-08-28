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
 * File: lightduer_crab_log.h
 * Auth: Gang Chen (chengang12@baidu.com)
 * Desc: Support API of crab log report
 */

#ifndef BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_CRAB_LOG_H
#define BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_CRAB_LOG_H

#include <stdint.h>
#include <stdarg.h>
#include "baidu_json.h"
#include "lightduer_types.h"

typedef int (*duer_crab_upload_cb)(char *fname, char *buf, int len);
typedef void (*duer_crab_start_cb)();
typedef void (*duer_crab_stop_cb)();

void duer_crab_data_release(void *buf);

void duer_crab_reg_upload_cb(duer_crab_upload_cb cb);

void duer_crab_log_hook(const char* format, va_list arg);

duer_status_t duer_crab_log_report(const char *data);

void duer_crab_log_format_hook(const char* format, ...);

void duer_crab_reg_start_cb(duer_crab_start_cb cb);

void duer_crab_reg_stop_cb(duer_crab_stop_cb cb);

#endif /* BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_CRAB_LOG_H */
