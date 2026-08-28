/*
 * Copyright (c) 2019 Baidu, Inc. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/**
 * @file lightduer_ota_sdk_wrapper.h
 * @author zhangguopeng01@baidu.com
 */

#ifndef BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_OTA_SDK_WRAPPER_H
#define BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_OTA_SDK_WRAPPER_H

#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

#include "lightduer_ota_common.h"

/** @brief ota stream */
typedef void* duer_ota_sdk_stream_t;

/** @brief ota string */
typedef void* duer_ota_sdk_string_t;

/** @brief ota callback functions */
typedef int(*ota_sdk_callback_func_t)();

typedef struct _ota_sdk_callbacks_t {
    /** OTA_UPDATE_EVENT_START */
    ota_sdk_callback_func_t func_start;

    /** OTA_UPDATE_EVENT_END */
    ota_sdk_callback_func_t func_end;

    /** OTA_UPDATE_EVENT_PROCESS_FILE_STREAM */
    ota_sdk_callback_func_t func_process_file_stream;

    /** OTA_UPDATE_EVENT_HASH_LIST */
    ota_sdk_callback_func_t func_hash_list;

    /** OTA_UPDATE_EVENT_VERIFY */
    ota_sdk_callback_func_t func_verify;

    /** OTA_UPDATE_EVENT_TASK_STATUS */
    ota_sdk_callback_func_t func_task_status;

    /** OTA_UPDATE_EVENT_CUSTOM_INFO */
    ota_sdk_callback_func_t func_custom_info;

    /** OTA_UPDATE_EVENT_PKG_DESC */
    ota_sdk_callback_func_t func_pkg_desc;

    /** OTA_UPDATE_EVENT_PKG_METHOD */
    ota_sdk_callback_func_t func_pkg_method;
} ota_sdk_callbacks_t;

/** ota sdk log level */
typedef enum _ota_sdk_log_level_t {
    /** debug */
    OTA_SDK_LOG_LEVEL_DEBUG,

    /** info */
    OTA_SDK_LOG_LEVEL_INFO,

    /** warning */
    OTA_SDK_LOG_LEVEL_WARNING,

    /** error */
    OTA_SDK_LOG_LEVEL_ERROR,

    OTA_SDK_LOG_LEVEL_MAX,
} ota_sdk_log_level_t;

/**
 * @brief 初始化ota sdk
 * @param[in] pid: product id of ota
 * @param[in] secret: secret key
 * @param[in] public_key: public pem data
 * @param[in] timeout_ms: timeout in milliseconds
 * @param[in] read_cache_callback
 * @param[in] write_cache_callback
 * @param[in] cache_max_size
 * @param[in] log_level: log level of ota sdk
 * @return
 *     DUER_OTA_RESULT_OK: succeed
 *     otherwise: failed
 */
duer_ota_result_t ota_sdk_init(const char* pid, const char* secret, const char* public_key, uint32_t timeout,
        ota_sdk_callback_func_t read_cache_callback, ota_sdk_callback_func_t write_cache_callback,
        uint32_t cache_max_size, ota_sdk_log_level_t log_level);

/**
 * @brief 反初始化ota sdk
 * @param Null
 * @return
 *     DUER_OTA_RESULT_OK: succeed
 *     otherwise: failed
 */
duer_ota_result_t ota_sdk_deinit(void);

/**
 * @brief 判断端上是否有运行中的ota查询、更新任务
 * @param[out] is_running: yes / no
 * @return
 *     DUER_OTA_RESULT_OK: succeed
 *     otherwise: failed
 */
duer_ota_result_t ota_sdk_is_running(int* is_running);

/**
 * @brief 查询是否存在新版本
 * @param[in] current_version: 当前版本
 * @param[in] device_id：设备id
 * @param[in] is_app_request: 是否是从手机APP发起的请求，yes/no
 * @param[out] exist_new_version: 是否有新版本，YES/NO
 * @param[out] version：版本情况
 * @param[out] release_notes：release notes
 * @return
 *     DUER_OTA_RESULT_OK: succeed
 *     otherwise: failed
 */
duer_ota_result_t ota_sdk_inquire_new_version(const char* current_version, const char* device_id,
        int is_app_request, int* exist_new_version, char* version, char* release_notes);

/**
 * @brief 开始升级
 * @param[in] current_version: 当前版本
 * @param[in] device_id：设备id
 * @param[in] is_app_request: 是否是从手机APP发起的请求，yes/no
 * @param[in] callbacks: 回调函数
 * @return
 *     DUER_OTA_RESULT_OK: succeed
 *     otherwise: failed
 */
duer_ota_result_t ota_sdk_do_upgrade(const char* current_version, const char* device_id,
        int is_app_request, ota_sdk_callbacks_t* callbacks);

/**
 * @brief 从ota stream中读取数据
 * @param[in] stream: ota stream
 * @param[out] buffer: buffer to store ota data
 * @param[in] buffer_size: buffer size
 * @param[out] bytes_readed: readed bytes
 * @return
 *     DUER_OTA_RESULT_OK: succeed
 *     otherwise: failed
 */
duer_ota_result_t ota_sdk_read_stream(duer_ota_sdk_stream_t stream, void* buffer,
        size_t bytes_read, size_t* bytes_readed);

/**
 * @brief 主动上报ota升级信息
 * @param[in] current_version: version of current firmware
 * @return
 *     DUER_OTA_RESULT_OK: succeed
 *     otherwise: failed
 */
duer_ota_result_t ota_sdk_upload_report_cache(const char* current_version);

#ifdef __cplusplus
}
#endif

#endif /* BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_OTA_SDK_WRAPPER_H */
