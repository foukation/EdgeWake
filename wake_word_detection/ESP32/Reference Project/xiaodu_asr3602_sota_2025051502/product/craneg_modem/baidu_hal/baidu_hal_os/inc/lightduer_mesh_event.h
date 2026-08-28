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
 * File: libduer_mesh_event.h
 * Auth: lingfeng meng (menglingfeng@baidu.com)
 * Desc: mesh event functions
 */

#ifndef BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_MESH_EVENT_H
#define BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_MESH_EVENT_H
#include <stdint.h>
#include "lightduer_types.h"
#include "lightduer_dcs_local.h"
#include "lightduer_ds_log_dcs.h"
#include "lightduer_ble_mesh.h"
#ifdef __cplusplus
extern "C" {
#endif
/*
 * duer issue get state succeeded event
 *
 * @param attribute : char *
 *        attribute_value : char *
 *
 * @return none
 */
void duer_issue_get_state_succeeded(char *attribute, char *attribute_value);

/*
 * duer issue infrared data succeeded event
 *
 * @param   token : char *
 *          ack_list : mesh_list_t *
 *
 * @return none
 */
void duer_issue_infrared_data_succeeded(char *token, mesh_list_t *ack_list);

/*
 * duer issue infrared data received event
 *
 * @param 
 *        token : char *
 *        unicast_addr : uint32_t
 *        opcode : uint32_t
 *        data   : uint8_t
 *        data_len : uint32_t
 *
 * @return none
 */
void duer_issue_infrared_data_received_event(char *token, uint32_t unicast_addr, uint32_t opcode, uint8_t *data, uint32_t data_len);

/*
 * duer issue mesh data succeeded event
 *
 * @param   token : char *
 *          ack_list : mesh_list_t *
 *          address: char *
 *
 * @return none
 */
void duer_issue_mesh_data_succeeded(char *token, mesh_list_t *ack_list, char *address);

/*
 * duer issue mesh data received event
 *
 * @param 
 *        token : char *
 *        unicast_addr : uint32_t
 *        opcode : uint32_t
 *        data   : uint8_t
 *        data_len : uint32_t
 *
 * @return none
 */
void duer_issue_mesh_data_received_event(char *token, uint32_t unicast_addr, uint32_t opcode, uint8_t *data, uint32_t data_len);
/*
 * duer issue succeeded event
 *
 * @param duer_dcs_mesh_event_type : type
 *        token : char *
 *
 * @return none
 */
void duer_issue_common_succeeded_event(duer_dcs_mesh_event_type type, char *token);

/*
 * duer issue failed event
 *
 * @param duer_dcs_mesh_event_type : type
 *        token : char *
 *        error_code : char *
 *        error_message : char *
 *
 * @return none
 */
void duer_issue_common_failed_event(duer_dcs_mesh_event_type type, char *token, char *error_code, char *error_message, char* addr);

/*
 * duer issue scan device finished
 *
 * @param token : char *
 *        scan_dev_info : mesh_scan_device_info *
 *
 * @return none
 */
void duer_issue_scan_device_finished(char *token, mesh_scan_device_info *scan_dev_info, char *error_code, char *error_message);

/*
 * duer issue provision device finished
 *
 * @param token : char *
 *        provision_device_finished : mesh_provision_device_finished *
 *
 * @return none
 */
void duer_issue_provision_device_finished(char *token, mesh_provision_device_finished *provision_device_finished);

/*
 * duer issue mesh button clicked event
 *
 * @param node_addr : uint16_t
 *        uuid : uint8_t *
 *        click_type : uint32_t
 *        vbat : uint32_t
 *
 * @return none
 */
void duer_issue_mesh_button_clicked(uint16_t node_addr, uint8_t uuid[], uint32_t click_type, uint16_t vbat);

/*
 * duer issue mesh button clicked event
 *
 * @param   token : char *
 *          state_list : mesh_list_t *
 *
 * @return none
 */
void duer_issue_state_changed(char *token, mesh_list_t *state_list);
#ifdef __cplusplus
}
#endif

#endif // BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_MESH_EVENT_H
