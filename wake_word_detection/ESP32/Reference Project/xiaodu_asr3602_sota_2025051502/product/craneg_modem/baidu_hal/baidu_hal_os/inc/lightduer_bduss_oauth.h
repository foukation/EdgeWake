// Copyright (2019) Baidu Inc. All rights reserved.
/**
 * File: lightduer_bduss_oauth.h
 * Auth: Wang Ning (wangning23@baidu.com)
 * Desc: lightduer bduss oauth, Refresh Access-Token.
 */

#ifndef BAIDU_LIGHTDUER_BDUSS_OAUTH_H
#define BAIDU_LIGHTDUER_BDUSS_OAUTH_H

#include "lightduer_http_client.h"

typedef enum {
    REFRESH_TYPE_LOGIN_OAUTH,
    REFRESH_TYPE_EXPIRE,
    REFRESH_TYPE_REBOOT,
    REFRESH_TYPE_RELINK,
    REFRESH_TYPE_12_HOURS,
    REFRESH_TYPE_UNKNOWN
} refresh_token_type_t;

typedef enum {
    STATUS_OK,
    /*不用重试(配网+定时) 未登录 失效不刷新deviceBduss*/
    STATUS_ERROR_NOT_LOGIN                    = 2,

    /*重试1次(配网) 数据库写入失败 任何时候延迟10s*/
    STATUS_ERROR_DB_WRITE                     = 4,

    /*不进行重试(定时) 重复请求*/
    STATUS_ERROR_MULTI_REQUEST                = 8,

    /*不进行重试(定时) 参数错误*/
    STATUS_ERROR_PARAM_INVALID                = 1002,

    /*重试1次(配网) 服务内部错误 任何时候延迟10s*/
    STATUS_ERROR_SERVER_INTERNAL_ERROR        = 1500,

    /*配网过程中需要重试 重试10次 10s间隔(配网) 未找到用户设备记录*/
    DEVICE_STATUS_ERR_RELATIONSHIP_NOT_FOUND  = 1501,

    /*配网过程中需要重试 重试10次 10s间隔(配网) 未找到access token*/
    DEVICE_STATUS_ERR_ACCESS_TOKEN_NOT_FOUND  = 1502,

    /*不进行重试 未找到refresh token*/
    DEVICE_STATUS_ERR_REFRESH_TOKEN_NOT_FOUND = 1503,

    /*不进行重试 设备已解绑*/
    DEVICE_STATUS_ERR_HASBEEN_UNBIND          = 1504,

    /*不进行重试 无效的client_id*/
    DEVICE_STATUS_ERR_CLIENTID_INVALID        = 1511,

    /*不进行重试 access token失效*/
    DEVICE_STATUS_ERR_ACCESS_TOKEN_INVALID    = 1512,

    /*不进行重试 refresh token失效*/
    DEVICE_STATUS_ERR_REFRESH_TOKEN_INVALID   = 1513,

    /*未知错误*/
    DEVICE_STATUS_UNKNOW_ERROR                = 10000,
} resp_status_code_t;

typedef enum {
    REASON_NET_CONF_START,     // start network config
    REASON_NET_CONF_TIMEOUT,   // network config timeout
    REASON_NET_CONF_SUCCESS,   // network config success
    REASON_NET_CONNECT,        // network available
    REASON_NET_DISCONNECT,     // network unavailable
    REASON_LONG_CON_DISCONNECT,// disconnect speech-long-connection
    REASON_DEVICE_BDUSS_UPDATE // device bduss update by anonymous LC
} refresh_reason_t;

typedef enum {
    DUER_NET_DISCONNECT,
    DUER_NET_CONNECT
} duer_net_status_t;

typedef enum {
    DUER_DISABLE,
    DUER_ENABLE
} duer_ret_t;

typedef int (*duer_notify_access_token_handle)(const char *data, size_t len);

/**
 * @brief init bduss oauth.
 */
int duer_bduss_oauth_init(void);

/**
 * @brief notify bduss oauth refresh.
 */
int duer_notify_refresh_action(refresh_reason_t reason);

/**
 * @brief set callback
 */
void duer_set_bduss_oauth_cb(duer_notify_access_token_handle callback);

#endif //BAIDU_LIGHTDUER_BDUSS_OAUTH_H
