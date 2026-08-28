
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
 * File: libduer_dcs_system.h
 * Auth: lingfeng meng (menglingfeng@baidu.com)
 * Desc:  apply APIs to support DCS system interface.
 */

#ifndef BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_DCS_SYSTEM_H
#define BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_DCS_SYSTEM_H
typedef struct sys_info_s sys_info_t;
typedef struct sys_info_ops_s sys_info_ops_t;

struct sys_info_s {
    // ssid
    uint8_t *ssid;
    // ip addr
    uint32_t sta_ip;
    // mac addr
    char *mac;
    // router mac adder
    char *router_mac;
    // device sn
    char *sn;
    //operations
    sys_info_ops_t *ops;
};

struct sys_info_ops_s {
    /// init the ble mesh information
    int (*init)(sys_info_t *sys_info_p);

    /// get sys info
    void (*get_sys_info)(sys_info_t *sys_info_p);
 
    /// uninit the ble mesh info
    void (*uninit)(sys_info_t *sys_info_p);
};

/*
 * Register system information ops
 *
 * @param ops:
 *
 * @return duer_status_t: Success: DUER_OK
 *              Failed:  Other
 */
duer_status_t duer_register_sys_info_ops(sys_info_ops_t *ops);

/*
 * unRegister system information
 *
 * @param void:
 *
 * @return duer_status_t: Success: DUER_OK
 *              Failed:  Other
 */
duer_status_t duer_unregister_sys_info();
#endif // BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_DCS_SYSTEM_H