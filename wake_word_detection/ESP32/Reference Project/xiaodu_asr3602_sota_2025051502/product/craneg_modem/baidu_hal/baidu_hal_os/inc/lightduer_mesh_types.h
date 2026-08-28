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
 * File: libduer_mesh_types.h
 * Auth: lingfeng meng (menglingfeng@baidu.com)
 * Desc: mesh types
 */

#ifndef BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_MESH_TYPES_H
#define BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_MESH_TYPES_H
#include <stdint.h>
#include "lightduer_types.h"
#include "lightduer_mesh_list.h"
#ifdef __cplusplus
extern "C" {
#endif
typedef enum {
    ENABLE_MESH,
    DISABLE_MESH,
    RESET_MESH,
    SET_MESH_STANDBY_MODE,
    REMOVE_DEVICE,
    ADD_SUBSCRIPTION,
    DEL_SUBSCRIPTION,
    DEL_ALL_SUBSCRIPTION,
    UPDATE_SUBSCRIPTION,
    SET_PUBLICATION,
    SET_STATE,
    GET_STATE,
    SEND_INFRARED_DATA,
    SEND_MESH_DATA
} duer_dcs_mesh_event_type;

typedef struct {
    uint32_t unicast_addr;
    uint8_t net_key[16];
    uint8_t app_key[16];
    char *user_id;
} mesh_enable_info;

typedef struct {
    int32_t timeout;
    int32_t window;
    int32_t interval;  
} mesh_scan_info;

typedef struct {
    int uuidnum;
    //char uuids[100][33];//
    uint8_t uuids[20][16];//
} mesh_scan_device_info;

typedef struct {
    uint8_t uuid[16];
    uint32_t unicast_addr;
    int subscription_addr_num;
    int subscription_addr[10];
    uint8_t auth_value[16];
    int timeout;    
} mesh_provision_device_info;

typedef struct {
    uint8_t uuid[16];
    uint16_t vid;
    uint16_t padding_var;
    uint8_t device_key[16];
    uint8_t random_value[16];
    char status[8];
    char error_code[52];
    char error_message[52];
    int elmnum;
    mesh_list_t *dev_list[10];
} mesh_provision_device_finished;

typedef struct {
    uint32_t opcode;                            // The ir model opcode.
    uint32_t expect_ack_opcode;                 // The expected ack opcode.
    uint8_t data_len;                           // The ir data len.
    uint8_t data[8];                            // The sending ir data.
} mesh_infrared_data;

typedef struct {
  uint32_t unicast_addr;
  char *attribute;
  char *attribute_value;  
} mesh_state_data;

/* Light color attribute val struct. */
typedef struct {
    uint16_t hue;                               // The hue val.
    uint16_t saturation;                        // The saturation val.
    uint16_t lightness;                          // The percentage of lightness.
} mesh_attr_val_color_t;

/* Light temperature attribute val struct. */
typedef struct {
    uint16_t temp;                              // The temperature.
    int16_t delta_uv;                           // The delta uv.
    uint16_t lightness;                          // The percentage of lightness.
} mesh_attr_val_temp_t;

/* Update subscription val struct */
typedef struct {
    uint32_t flag;
    uint32_t unicast_addr;
    uint16_t elem_idx;
    uint32_t unsubscription_addr;
    uint16_t subscription_addr[10];
    int subscription_addr_count;
    uint16_t modelids[5];
    int modelid_count;
    int timeout;
} mesh_update_sub_info, mesh_add_sub_info, mesh_delete_sub_info;

#ifdef __cplusplus
}
#endif

#endif // BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_MESH_TYPES_H