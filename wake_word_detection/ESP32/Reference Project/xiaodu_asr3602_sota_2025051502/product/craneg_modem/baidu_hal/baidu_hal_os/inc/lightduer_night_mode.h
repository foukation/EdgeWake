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
 * File: libduer_night_mode.h
 * Auth: lingfeng meng (menglingfeng@baidu.com)
 * Desc: night mode information
 */

#ifndef BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_NIGHT_MODE_H
#define BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_NIGHT_MODE_H
#include <stdint.h>
#include "lightduer_types.h"
#ifdef __cplusplus
extern "C" {
#endif
typedef struct night_mode_s night_mode_t;
typedef struct night_mode_ops_s night_mode_ops_t;

struct night_mode_s {
    /// enter/exit doNotDisturbMode now
    duer_bool current_in_night_mode;
    /// open or close night_mode
    duer_bool night_mode_now;

    /// device should enter/exit doNotDisturbMode at m_doNotDisturbModeStartSecOfDay/m_doNotDisturbModeEndSecOfDay
    duer_bool night_mode_autostart;

    /// expire time
    uint32_t expire_time;

    uint32_t gmt_timeout;

    /// turn on time
    char turn_on_time[6];

    /// turn off time
    char turn_off_time[6];

    //operations
    night_mode_ops_t *ops;
};

struct night_mode_ops_s {
    /// init the night mode information
    int (*init)(night_mode_t *night_modep);

    /// obtain the night mode info
    void (*get_night_mode)(night_mode_t *night_mode_src, night_mode_t *night_mode_dst);

    /// set the night mode info
    void (*set_night_mode)(night_mode_t *night_mode_dst, night_mode_t *night_mode_src);

    /// set the night mode now flag
    void (*set_night_mode_part)(night_mode_t *night_mode_dst, uint8_t in_night_mode, uint32_t gmt_timeout);

    duer_bool (*get_current_night_mode)(night_mode_t *night_modep, uint8_t *in_night_mode);

    /// set current night mode flag
    void (*set_current_night_mode)(night_mode_t *night_mode_dst, uint8_t in_night_mode);

    /// start night mode timer
    void (*start_nm_timer)(night_mode_t *night_modep);

    /// uninit the night mode info
    void (*uninit)(night_mode_t *night_modep);
};

extern night_mode_t g_night_mode;

/*
 * parse time string to int
 *
 * @param time_str : char *
 *        time : int *
 *
 * @return duer_bool: Success: DUER_TRUE
 *              Failed:  DUER_FALSE
 */
duer_bool parse_time_from_str(char *time_str, unsigned int *time);

/*
 * Register night mode ops
 *
 * @param ops:
 *
 * @return duer_status_t: Success: DUER_OK
 *              Failed:  Other
 */
duer_status_t duer_register_night_mode_ops(night_mode_ops_t *ops);

/*
 * unRegister night mode
 *
 * @param void:
 *
 * @return duer_status_t: Success: DUER_OK
 *              Failed:  Other
 */
duer_status_t duer_unregister_night_mode();

#ifdef __cplusplus
}
#endif

#endif // BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_NIGHT_MODE_H
