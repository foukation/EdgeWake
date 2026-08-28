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
 * File: lightduer_profile.h
 * Auth: Chen Xihao (chenxihao@baidu.com)
 * Desc: Get device profile information
 */

#ifndef BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_PROFILE_H
#define BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_PROFILE_H

#include "lightduer_types.h"

#ifdef __cplusplus
extern "C" {
#endif

#define DEVICE_SN_LEN               21
#define CLIENT_ID_LEN               33
#define SECRET_ID_LEN               33
#define WIFI_MAC_LEN                18
#define DEVICE_ECC_PUB_KEY_LEN      64
#define DEVICE_PREFIX_LEN           16

#define BDSPEECH_HOST_LEN           64
#define BDSPEECH_KEY_LEN            64
#define BDSPEECH_LC_URL_LEN         64
#define ASR_ENGINE_URI_LEN          64
#define EVT_UPLOAD_ENGINE_URI_LEN   64
#define DEVICE_LOGIN_URL_LEN        128

#define OS_NAME_LEN                 16
#define OS_VERSION_LEN              16
#define DEVICE_BRAND_LEN            16
#define DEVICE_MODEL_LEN            16
#define FIRMWARE_VERSION_LEN        32

#define DEVICE_ID_LEN               65
#define BDUSS_LEN                   193
#define ACCESS_TOKEN_LEN            256
#define DEVICE_NAME_LEN             16
#define DUMI_USER_DEVICE_ID_LEN     21

#define OPENAPI_URI_LEN             64
#define OPENAPI_PACKAGE_NAME_LEN    64
#define OPENAPI_PACKAGE_SIGN_LEN    64
#define WECHAT_URI_LEN              64
#define BOTWATCH_URI_LEN            64

typedef struct duer_profile_const_params_s {
    char device_sn[DEVICE_SN_LEN];
    char client_id[CLIENT_ID_LEN];
    char secret_id[SECRET_ID_LEN];
    char wifi_mac[WIFI_MAC_LEN];
    char device_ecc_pub_key[DEVICE_ECC_PUB_KEY_LEN];
    char device_prefix[DEVICE_PREFIX_LEN];

    char os_name[OS_NAME_LEN];
    char os_version[OS_VERSION_LEN];
    char device_brand[DEVICE_BRAND_LEN];
    char device_model[DEVICE_MODEL_LEN];
    char firmware_version[FIRMWARE_VERSION_LEN];
} duer_profile_constant_params_t;

typedef struct duer_profile_variable_params_s {
    char device_id[DEVICE_ID_LEN];
    char bduss[BDUSS_LEN];
    char device_bduss[BDUSS_LEN];
    char access_token[ACCESS_TOKEN_LEN];
    char device_name[DEVICE_NAME_LEN];
    int bdspeech_pid_type;
    int voice_id;
    char dumi_user_device_id[DUMI_USER_DEVICE_ID_LEN];
    char bdspeech_lc_url[BDSPEECH_LC_URL_LEN];  // bdspeech long connection URL

    char bdspeech_host[BDSPEECH_HOST_LEN];
    char bdspeech_key[BDSPEECH_KEY_LEN];
    char asr_engine_uri[ASR_ENGINE_URI_LEN];
    char evt_upload_engine_uri[EVT_UPLOAD_ENGINE_URI_LEN];
    char device_login_url[DEVICE_LOGIN_URL_LEN];
    int bdspeech_port;
    int bdspeech_primary_pid;
    int bdspeech_assist_pid;

    char openapi_uri[OPENAPI_URI_LEN];
    char openapi_package_name[OPENAPI_PACKAGE_NAME_LEN];
    char openapi_package_sign[OPENAPI_PACKAGE_SIGN_LEN];
    char wechat_uri[WECHAT_URI_LEN];
    char anonymous_access_token[ACCESS_TOKEN_LEN];
    char botwatch_uri[BOTWATCH_URI_LEN];
    int is_verified_device;
} duer_profile_variable_params_t;

typedef enum duer_profile_param_key_e {
    // constant params
    PROFILE_KEY_DEVICE_SN = 0,
    PROFILE_KEY_CLIENT_ID,
    PROFILE_KEY_SECRET_ID,
    PROFILE_KEY_WIFI_MAC,
    PROFILE_KEY_DEVICE_ECC_PUB_KEY,
    PROFILE_KEY_DEVICE_PREFIX,
    PROFILE_KEY_OS_NAME,
    PROFILE_KEY_OS_VERSION,
    PROFILE_KEY_DEVICE_BRAND,
    PROFILE_KEY_DEVICE_MODEL,
    PROFILE_KEY_FIRMWARE_VERSION,

    // variable params
    PROFILE_KEY_DEVICE_ID = 0x100,
    PROFILE_KEY_BDUSS,
    PROFILE_KEY_DEVICE_BDUSS,
    PROFILE_KEY_ACCESS_TOKEN,
    PROFILE_KEY_DEVICE_NAME,
    PROFILE_KEY_BDSPEECH_HOST,
    PROFILE_KEY_BDSPEECH_KEY,
    PROFILE_KEY_BDSPEECH_PORT,
    PROFILE_KEY_BDSPEECH_PRIMARY_PID,
    PROFILE_KEY_BDSPEECH_ASSIST_PID,
    PROFILE_KEY_BDSPEECH_PID_TYPE,
    PROFILE_KEY_VOICE_ID,
    PROFILE_KEY_DUMI_USER_DEVICE_ID,
    PROFILE_KEY_BDSPEECH_LC_URL,
    PROFILE_KEY_ASR_KEY_ENGINE_URI,
    PROFILE_KEY_EVT_UPLOAD_KEY_ENGINE_URI,
    PROFILE_KEY_DEVICE_LOGIN_URL,
    PROFILE_KEY_OPENAPI_URI,
    PROFILE_KEY_OPENAPI_PACKAGE_NAME,
    PROFILE_KEY_OPENAPI_PACKAGE_SIGN,
    PROFILE_KEY_WECHAT_URI,
    PROFILE_KEY_ANONYMOUS_ACCESS_TOKEN,
    PROFILE_KEY_BOTWATCH_URI,
    PROFILE_KEY_MAX
} duer_profile_param_key_t;

#if defined APPLOGIN_CHECK
typedef enum
{
    // DUER_APPLOGIN_STATUS_UNKNOWN = 0,
    DUER_APPLOGIN_NOT_RUNNING = 0,
    DUER_APPLOGIN_RUNNING,
    DUER_APPLOGIN_SYS_ERROR,
    DUER_APPLOGIN_NOT_PASSED,
    DUER_APPLOGIN_PASSED,
}duer_applogin_check_status;
extern volatile duer_applogin_check_status applogin_check_state;
#endif

int duer_profile_init(duer_profile_constant_params_t *params);

const char *duer_profile_get_string_param(duer_profile_param_key_t key);

int duer_profile_set_string_param(duer_profile_param_key_t key, const char *value);

int duer_profile_get_int_param(duer_profile_param_key_t key);

int duer_profile_set_int_param(duer_profile_param_key_t key, int value);

signed char duer_porfile_applogin_check(void);

#include "lightduer_http_client.h"
duer_http_result_t duerapp_openapi_applogin_sync_request(duer_http_data_handler http_response);

#ifdef __cplusplus
}
#endif

#endif // BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_PROFILE_H
