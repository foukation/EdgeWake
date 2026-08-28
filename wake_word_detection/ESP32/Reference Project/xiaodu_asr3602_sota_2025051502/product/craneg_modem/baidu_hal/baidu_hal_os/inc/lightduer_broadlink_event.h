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
 * File: lightduer_broadlink_event.h
 * Auth: lingfeng meng (menglingfeng@baidu.com)
 * Desc: broadlink event api functions
 */

#ifndef BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_BROAD_LINK_H
#define BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_BROAD_LINK_H
#include <stdint.h>
#include "lightduer_types.h"
#include "lightduer_broadlink_types.h"
#include "lightduer_bd_timer.h"
#include "lightduer_dcs_local.h"
#include "lightduer_ds_log_dcs.h"
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Register broadlink ops
 *
 * @param ops:
 *
 * @return duer_status_t: Success: DUER_OK
 *              Failed:  Other
 */
duer_status_t duer_register_broadlink_ops(broad_link_ops_t *ops);

/*
 * duer issue device discovered event
 *
 * @param mac : const char *
 *        device_info : const char *
 *
 * @return duer_bool
 */
duer_bool duer_issue_device_discovered(const char *mac, const char *device_info);

/*
 * duer issue device discovered failed event
 *
 * @param reason : duer_discover_device_error_reason
 *        code : duer_discover_device_error_code
 *
 * @return duer_bool
 */
duer_bool duer_issue_device_discover_failed(duer_discover_device_error_reason reason,
                                    duer_discover_device_error_code code);
/*
 * duer issue device challenge received event
 *
 * @param mac : mac address
 *        deviceInfo : const char *
 *        challenge: const char *
 *
 * @return duer_bool
 */
duer_bool duer_issue_device_challenge_received(const char *mac, const char *deviceInfo, const char *challenge);

/*
 * duer issue device signature received event
 *
 * @param mac : mac address
 *        deviceInfo : char *
 *        signature: char *
 *
 * @return duer_bool
 */
duer_bool duer_issue_device_signature_received(const char *mac, const char *deviceInfo, const char *signature);

/*
 * duer issue device token received event
 *
 * @param mac : mac address
 *        deviceInfo : char *
 *        token_str: char *
 *
 * @return duer_bool
 */
duer_bool duer_issue_device_token_received(const char *mac, const char *deviceInfo, const char* token_str);

extern const char *BROADLINK_NAMESPACE;

extern char *s_discover_device_token;
extern char *s_stop_discover_devcie_token;
extern char *s_start_recevice_device_challenge_token;
extern char *s_send_cloud_challenge_and_signature_token;
extern char *s_send_shared_key_token;
extern char *s_device_config_token;
#ifdef __cplusplus
}
#endif

#endif // BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_BROAD_LINK_H
