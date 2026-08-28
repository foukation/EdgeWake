/**
 * Copyright (2017) Baidu Inc. All rights reserved.
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
 * File: lightduer_dcs_synchronize_key_state.h
 * Auth: Chen Chi (chenchi01@baidu.com)
 * Desc: SynchronizeKeyState event and directive support.
 */

#ifndef BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_DCS_SYNCHRONIZE_KEY_STATE_H
#define BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_DCS_SYNCHRONIZE_KEY_STATE_H

#include "baidu_json.h"
#include "lightduer_types.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef struct {
    /** Store the SynchronizeKeyState configs to flash */
    duer_status_t (*store_config)(duer_bool enabled, duer_u32_t interval_sec);

    /** Get current key state as a json */
    baidu_json *(*get_key_state)();
} duer_sync_key_state_ops_t;

/**
 * Init SynchronizeKeyState configs. Starts timers if enabled is true.
 *
 * PARAM[in] enabled: if SynchronizeKeyState is enabled.
 * PARAM[in] sync_interval_sec: synchronize interval in seconds.
 * PARAM[in] ops: function pointers for sync operations, must be not NULL.
 */
duer_status_t duer_dcs_synchronize_key_state_init(
        duer_bool enabled, duer_u32_t sync_interval_sec, const duer_sync_key_state_ops_t *ops);

#ifdef __cplusplus
}
#endif

#endif  // BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_DCS_SYNCHRONIZE_KEY_STATE_H
