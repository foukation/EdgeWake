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
 * File: libduer_broadlink_types.h
 * Auth: lingfeng meng (menglingfeng@baidu.com)
 * Desc: broadlink types
 */

#ifndef BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_BROADLINK_TYPES_H
#define BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_BROADLINK_TYPES_H
#include <stdint.h>
#include "lightduer_types.h"
#include "lightduer_bd_timer.h"
#ifdef __cplusplus
extern "C" {
#endif
typedef enum {
    // DiscoverDevice指令到达超时
    DISCOVER_DEVICE_DISCOVER_TIMEOUT,
    // StartReceiveDeviceChallenge达到指令超时
    DISCOVER_DEVICE_RECEIVE_DEVICE_CHALLENGE_TIMEOUT,
    // SendCloudChallengeAndSignature指令到达超时时间
    DISCOVER_DEVICE_RECEIVE_SIGNATURE_TIMEOUT,
    // SendSharedKey指令到达超时时间
    DISCOVER_DEVICE_RECEIVE_TOKEN_TIMEOUT,
    // DeviceConfig指令到达超时时间
    DISCOVER_DEVICE_CONFIG_TIMEOUT,
    // 设备联网失败
    DISCOVER_DEVICE_CONFIG_FAILED,
    // 未知失败原因
    DISCOVER_DEIVCE_FAILED
} duer_discover_device_error_reason;

typedef enum {
    // 连接ap超时
    CONFIG_FAILED_CONNECT_AP_TIMEOUT,
    // 发送数据包失败
    CONFIG_FAILED_SEND_PACKET_FAILED,
    // 超过最大重试次数
    CONFIG_FAILED_EXCEED_MAX_RETRY_TIMES,
    // 校验音箱身份失败
    CONFIG_FAILED_INVALID_SOUND_BOX_ID,
    // ssid和pwd解析异常
    CONFIG_FAILED_PARSE_SSID_PWD_ERROR,
    // 接收到不支持的消息类型
    CONFIG_FAILED_UNSUPPORT_MESSAGE,
    // 配置超时，超过10分钟配置时间，设备准备挂起
    CONFIG_FAILED_CONFIGRUATION_TIMEOUT,
    // 未知错误
    CONFIG_FAILED_UNKNOWN_ERROR
} duer_discover_device_error_code;

typedef enum {
    // DiscoverDevice指令定时器
    TIMER_TYPE_DISCOVER_DEVICE,

    // StartReceiveDeviceChallenge指令定时器
    TIMER_TYPE_START_RECEIVE_DEVICE_CHALLENGE,

    // SendCloudChallengeAndSignature指令定时器
    TIMER_TYPE_SEND_CLOUD_CHALLENGE_AND_SIGNATURE,

    // SendSharedKey指令定时器
    TIMER_TYPE_SEND_SHARED_KEY,

    // DeviceConfig指令定时器
    TIMER_TYPE_DEVICE_CONFIG,

    // 定时器类型未知
    TIMER_TYPE_UNKNOWN
} duer_bd_timer_type;

typedef struct broad_link_s broad_link_t;
typedef struct broad_link_ops_s broad_link_ops_t;

struct broad_link_s {
    bd_timer_t local_timer;
    bd_timer_t global_timer;
    broad_link_ops_t *ops;
    /// start local timer
    void (*start_local_timer)(duer_bd_timer_type timer_type, int timeout);
    /// stop local timer
    void (*stop_local_timer)();
    /// start global timer
    void (*start_global_timer)();
    /// stop global timer
    void (*stop_global_timer)();
};

struct broad_link_ops_s {
    /// init sdk
    int (*init_sdk)();
    /// print version
    int (*print_version)();
    /// start user auth
    int (*start_user_auth)();
    /// set challenge and ginature
    int (*set_challenge_and_signature)(uint8_t *challenge,uint16_t challenge_len, uint8_t *signature, uint16_t signature_len);
    /// set waln params
    int (*set_wlan_params)(uint8_t *sharedkey);
    /// bind token success
    int (*bind_token_success)();
    /// deinit sdk
    int (*deinit_sdk)();
    /// send action
    int (*send_action)(uint8_t *frame, uint16_t frame_len);
};

extern broad_link_t g_broadlink;

#ifdef __cplusplus
}
#endif

#endif // BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_BROADLINK_TYPES_H
