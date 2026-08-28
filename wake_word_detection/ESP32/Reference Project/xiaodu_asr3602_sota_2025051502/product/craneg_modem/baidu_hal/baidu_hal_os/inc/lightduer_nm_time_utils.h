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
/*
 * File: libduer_nm_time_utils.h
 * Auth: lingfeng meng (menglingfeng@baidu.com)
 * Desc: Provide time utils APIs
 */

#ifndef BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_NM_TIME_UTILS_H
#define BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_NM_TIME_UTILS_H
#include <stdint.h>
#include "lightduer_types.h"
#include "lightduer_night_mode.h"
#ifdef __cplusplus
extern "C" {
#endif
typedef struct nm_time_utils_s nm_time_utils_t;

struct nm_time_utils_s {

    /// turn on time (seconds of day)
    uint32_t turn_on_time;

    /// turn off time (seconds of day)
    uint32_t turn_off_time;

    /// obtain the GMT Sec and currentSecOfDay
    void (*get_gmt_and_currentsec_of_day)(uint32_t *gmt_sec, unsigned int *currentsec_of_day);

    /// obtain the expire time in night mode
    uint32_t (*get_night_mode_expire_time)(nm_time_utils_t *p_time_utils, night_mode_t *night_modep, uint8_t in_night_mode, uint32_t *timeout_gmt_sec);
};

extern nm_time_utils_t nm_time_utils; 

#ifdef __cplusplus
}
#endif

#endif // BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_NM_TIME_UTILS_H
