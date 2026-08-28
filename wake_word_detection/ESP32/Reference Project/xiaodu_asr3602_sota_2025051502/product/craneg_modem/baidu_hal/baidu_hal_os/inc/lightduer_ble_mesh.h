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
 * File: libduer_ble_mesh.h
 * Auth: lingfeng meng (menglingfeng@baidu.com)
 * Desc: ble mesh information
 */

#ifndef BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_BLE_MESH_H
#define BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_BLE_MESH_H
#include <stdint.h>
#include "lightduer_types.h"
#include "lightduer_mesh_types.h"
#ifdef __cplusplus
extern "C" {
#endif
typedef struct ble_mesh_s ble_mesh_t;
typedef struct ble_mesh_ops_s ble_mesh_ops_t;

struct ble_mesh_s {
    void *token_map;
    //operations
    ble_mesh_ops_t *ops;
};

struct ble_mesh_ops_s {
    /// init the ble mesh information
    int (*init)();
    /// get mesh status
    uint8_t (*get_mesh_status)(uint8_t *status);
    /// enable mesh
    void (*enable_mesh)(mesh_enable_info *mesh_enable_p);
    /// disable mesh
    void (*disable_mesh)();
    /// set standby mode
    void (*set_standby_mode)();
    /// scan device
    void (*scan_device)(mesh_scan_info *mesh_scan_p);
    /// stop scan device
    void (*stop_scan_device)();
    /// set transmit time
    void (*set_net_transmit_time)(uint16_t val);
    /// provision device
    void (*provision_device)(mesh_provision_device_info *provision_dev_p);
    /// remove device
    void (*remove_device)(uint32_t unicast_addr, uint8_t *net_key, uint8_t *app_key);
    /// add subscription
    void (*add_subscription)(mesh_add_sub_info *add_sub_info);
    /// delete subscription
    void (*delete_subscription)(mesh_delete_sub_info *del_sub_info);
    /// delete all subscription
    void (*delete_all_subscription)(uint32_t unicast_addr);
    /// update subscription
    void (*update_subscription)(mesh_update_sub_info *update_sub_info);
    /// set publication
    void (*set_publication)(uint32_t unicast_addr, uint32_t publication_addr);
    /// get state
    void (*get_state)(uint32_t unicast_addr, char *attribue);
    /// set state
    void (*set_state)(uint32_t addr, uint8_t attribue_type, void *attribue_value, uint8_t val_len, uint32_t timeout_in_seconds);
    /// send infrared data
    void (*send_infrared_data)(uint32_t addr, uint32_t send_timeout, uint32_t max_retry,
                                mesh_list_t *data_list, char *token);
    /// send mesh data
    void (*send_mesh_data)(uint32_t addr, uint32_t send_timeout, uint32_t max_retry,
                                mesh_list_t *data_list, char *token);
    /// uninit the ble mesh info
    void (*uninit)();
};

typedef struct {
  char *key;
  char *value; 
} mesh_token_data;

/*
 * Set mesh token
 *
 * @param key   : char *
 *        value : char *
 *
 * @return none
 */
void set_mesh_token(char *key, char *value);

/*
 * Set mesh token
 *
 * @param key   : char *
 *
 * @return char *
 */
char *get_mesh_token(char *key);

/*
 * Register ble mesh ops
 *
 * @param ops:
 *
 * @return duer_status_t: Success: DUER_OK
 *              Failed:  Other
 */
duer_status_t duer_register_ble_mesh_ops(ble_mesh_ops_t *ops);

/*
 * unRegister ble mesh
 *
 * @param void:
 *
 * @return duer_status_t: Success: DUER_OK
 *              Failed:  Other
 */
duer_status_t duer_unregister_ble_mesh();

/*
 * delete mesh node
 *
 * @param key   : char *
 *
 * @return none
 */
void delete_token_node(char* key);

extern const char *MESH_NAMESPACE;
extern const char *DCS_ENABLE_MESH_NAME;
extern const char *DCS_DISABLE_MESH_NAME;
extern const char *DCS_SET_MESH_STANDBY_NAME;
extern const char *DCS_SCAN_DEVICE_NAME;
extern const char *DCS_PROVISION_DEVICE_NAME;
extern const char *DCS_REMOVE_DEVICE_NAME;
extern const char *DCS_ADD_SUBSCRIPTION_NAME;
extern const char *DCS_DELETE_SUBSCRIPTION_NAME;
extern const char *DCS_DELETE_ALL_SUBSCRIPTION_NAME;
extern const char *DCS_UPDATE_SUBSCRIPTION_NAME;
extern const char *DCS_SET_PUBLICATION_NAME;
extern const char *DCS_GET_STATE_NAME;
extern const char *DCS_SET_STATE_NAME;
extern const char *DCS_SEND_INFRARED_DATA_NAME;
extern const char *DCS_SEND_MESH_DATA_NAME;

extern ble_mesh_t g_ble_mesh;

#ifdef __cplusplus
}
#endif

#endif // BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_BLE_MESH_H
