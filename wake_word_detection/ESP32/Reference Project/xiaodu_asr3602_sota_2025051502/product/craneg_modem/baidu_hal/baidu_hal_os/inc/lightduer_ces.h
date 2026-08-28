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
/**
 * File: lightduer_ces.h
 * Auth: Chen Xihao (chenxihao@baidu.com)
 * Desc: Define API of CES
 */

#ifndef BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_CES_H
#define BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_CES_H

#include <stdint.h>
#include "baidu_json.h"
#include "lightduer_types.h"

// event type of network config
#define CES_REFRESH_TOKEN_PAIRED                "networkConfigPaired"
#define CES_DLP_CONNECT_STATUS                  "directives"
#define CES_SPEECH_SERVER_CONNECT_STATUS        "speechServer"
#define CES_BLE_SERVER_STARTUP_STATUS           "bleServerStartupStatus"
#define CES_NETWORK_CONFIG_STARTUP              "networkConfigStartup"
#define CES_NETWORK_CONFIG_TIMEOUT_EXIT         "networkConfigTimeoutExit"
#define CES_NETWORK_CONFIG_UNKOWN_MSG_TYPE      "networkConfigUnkownMsgType"
#define CES_NETWORK_CONFIG_DATA_PROCESS_ERR_TYPE "networkConfigMsgProcessErrType"

#define CES_NETWORK_CONFIG_FIR_PCK              "networkConfigFirPck"
#define CES_NETWORK_CONFIG_SEC_PCK              "networkConfigSecPck"
#define CES_NETWORK_CONFIG_LAST_PCK             "networkConfigLastPck"
#define CES_NETWORK_CONFIG_CONN_ROUTER          "networkConfigConnRouter"
#define CES_NETWORK_CONFIG_NOT_CONN_ROUTER      "networkConfigNotConnRouter"
#define CES_NETWORK_CONFIG_WIFI_PWD_WRONG       "networkConfigWifiPwdWrong"
#define CES_NETWORK_CONFIG_WIFI_DHCP_FAILED     "networkConfigWifiDhcpFailed"
#define CES_NETWORK_CONFIG_NOT_CONN_INTERNET    "networkConfigNotConnInternet"
#define CES_NETWORK_CONFIG_CONN_INTERNET        "networkConfigConnInternetSuc"

// event type of ota
#define CES_OTA_SYSTEM_START                    "otaSystemStart"
#define CES_OTA_FINAL_SUCCESS                   "otaFinalSuccessful"
#define CES_OTA_FINAL_FAILED                    "otaFinalFailed"
#define CES_OTA_FINAL_INTERRUPTED               "otaFinalInterrupted"
#define CES_OTA_UPDATE_BY_SILENT                "otaUpdatebySilent"
#define CES_OTA_UPDATE_BY_APP                   "otaUpdatebyApp"
#define CES_OTA_START                           "otaStart"
// ota part success event should be defined on App
#define CES_OTA_UPDATE_SUCCESSFUL               "otaUpdateSuccessful"
#define CES_OTA_UPDATE_FAILED                   "otaUpdateFailed"

// event type of wifi
#define CES_WIFI_CONNECT_FAIL                   "wifiConnectFail"
#define CES_WIFI_DISCONNECT                     "wifiDisconnect"

// event type of bluetooth
#define CES_BLE_OPEN_FAIL                       "bleOpenFail"
#define CES_BT_OPEN                             "btOpen"
#define CES_BT_CLOSE                            "btClose"
#define CES_BT_CONNECT                          "btConnect"
#define CES_BT_DISCONNECT                       "btDisconnect"
#define CES_BT_PLAY                             "btPlay"
#define CES_BT_PAUSE                            "btPause"
#define CES_BT_RESUME                           "btResume"
#define CES_BT_BACKWARD                         "btBackward"
#define CES_BT_FORWARD                          "btForward"
#define CES_BT_PLAY_UNDER_RUN                   "btPlayUnderrun"
#define CES_BT_TRACK_CHANGE                     "btTrackChange"

// event type of dlna
#define CES_DLNA_SETURI                         "dlnaSetUri"
#define CES_DLNA_PLAY                           "dlnaPlay"
#define CES_DLNA_STOP_PLAY                      "dlnaStopPlay"
#define CES_DLNA_PAUSE                          "dlnaPause"
#define CES_DLNA_RESUME                         "dlnaResume"
#define CES_DLNA_SEEK                           "dlnaSeek"
#define CES_DLNA_FINISHED                       "dlnaFinished"

#define CES_EVENT_TRIGGER_QUERY                 "query"
#define CES_EVENT_TRIGGER_TOUCH                 "touch"
#define CES_EVENT_TRIGGER_APP                   "app"
#define CES_EVENT_TRIGGER_SPEAKER               "speaker"
#define CES_EVENT_TRIGGER_PHONE                 "phone"

// event type of infrared
#define CES_INFRARED_SEND                       "infraredSend"
#define CES_INFRARED_RECEIVE                    "infraredReceive"

enum duer_ces_http_status_e {
    CES_HTTP_STATUS_NONE = -1,
    CES_HTTP_STATUS_OK = 200
};

enum duer_ces_network_config_err_type_e {
    ERROR_TYPE_SUCCESS_EVENT                    = 0,

    // refreshToken & audiolc & dlpProxy
    ERROR_TYPE_OAUTH_FAILED                     = 103,
    ERROR_TYPE_DLP_PROXY_CONNECTED              = 105,
    ERROR_TYPE_SUCCESS_PAIRED                   = 107,
    ERROR_TYPE_SUCCESS_SPEECH_CONNECTED         = 108,

    ERROR_TYPE_CONFIG_NOT_CONN_ROUTER_EVENT     = 300,
    ERROR_TYPE_CONFIG_NOT_CONN_INTERNET_EVENT   = 301,
    ERROR_TYPE_CONFIG_WIFI_PWD_WRONG            = 302,
    ERROR_TYPE_CONFIG_WIFI_DHCP_FAILED          = 303,
    ERROR_TYPE_SUCCESS_CONNECT_ROUTER           = 304,
    ERROR_TYPE_SUCCESS_CONNECT_INTERNET         = 305,

    ERROR_TYPE_CONFIG_STARTUP                   = 310,
    ERROR_TYPE_CONFIG_TIMEOUT_EXIT              = 311,

    ERROR_TYPE_SUCCESS_FIRPCK                   = 320,
    ERROR_TYPE_SUCCESS_LASTPCK                  = 321,
    ERROR_TYPE_SUCCESS_SECPCK                   = 322,

    ERROR_TYPE_UNPACK_ECC_APP_PUBKEY_FAILED     = 330,
    ERROR_TYPE_READ_DEVICE_ECC_PUBKEY_FAILED    = 331,
    ERROR_TYPE_GENERATE_ECC_DEVICE_PAIR_FAILED  = 332,
    ERROR_TYPE_GENERATE_SHARE_KEY_FAILED        = 333,
    ERROR_TYPE_ENCRYPT_RANDOM_FAILED            = 334,

    ERROR_TYPE_UNPACK_DEVICE_KEY_FAILED         = 340,
    ERROR_TYPE_HMAC_SIGN_VERIFY_FAILED          = 341,

    ERROR_TYPE_UNPACK_SSID_INFO_FAILED          = 350,

    // ble status
    ERROR_TYPE_BLE_STARTUP_SUCCESS              = 360,
    ERROR_TYPE_BLE_STARTUP_FAILED               = 361,

    ERROR_TYPE_UNKOWN_MSGTYPE                   = 399
};

enum duer_ces_ota_error_type_e {
    ERROR_TYPE_OTA_FAILED_UPGRADING_EVENT = 100,
    ERROR_TYPE_OTA_FAILED_FINAL_EVENT = 101
};

enum duer_ces_alert_error_type_e {
    ERROR_TYPE_ALERT_PLAY_FAILED = 1011,
    ERROR_TYPE_ALERT_PLAY_CARTON_ERR = 1012
};

enum duer_ces_wifi_error_type_e {
    ERROR_TYPE_WIFI_AUTH_FAIL = 100,
    ERROR_TYPE_WIFI_ASSOC_FAIL = 101,
    ERROR_TYPE_WIFI_INITIACTIVE = 102
};

enum duer_ces_infrared_error_type_e {
    ERROR_TYPE_INFRARED_NO_ERR = 0,
    ERROR_TYPE_INFRARED_REC_TIMEOUT = 100
};

enum duer_ces_request_type_e {
    CES_REQUEST_TYPE_VOICE = 1,
    CES_REQUEST_TYPE_CLICK = 2,
    CES_REQUEST_TYPE_TEXT = 3,
    CES_REQUEST_TYPE_PLAYER = 4
};

enum duer_ces_asr_type_e {
    CES_ASR_TYPE_AUTO_LISTEN = 1,
    CES_ASR_TYPE_CLICK_SPEEK = 2
};

enum duer_ces_wakeup_type_e {
    CES_WAKEUP_TYPE_INTERRUPT = 1,
    CES_WAKEUP_TYPE_IDLE = 2
};

enum duer_ces_tts_play_error_type_e {
    CES_TTS_ERR_TYPE_NO_DATA = 100,
    CES_TTS_ERR_TYPE_PLAYER = 101,
    CES_TTS_ERR_TYPE_CANCEL = 102
};

enum duer_ces_reboot_error_type_e {
    CES_REBOOT_TYPE_NORMAL = 100,
    CES_REBOOT_TYPE_HOT = 101,
    CES_REBOOT_TYPE_OTA = 102,
    CES_REBOOT_TYPE_RESET = 103,
    CES_REBOOT_TYPE_COLD = 104,

    CES_REBOOT_TYPE_ABNORMAL = 200,
    CES_REBOOT_TYPE_KERNEL_PANIC = 201,
    CES_REBOOT_TYPE_ONE_CPU_HANG = 202,
    CES_REBOOT_TYPE_KERNEL_EMERGENCY = 203,
    CES_REBOOT_TYPE_ALL_CPU_HANG = 204,
};

enum duer_get_music_info_finish_type_e {
    CES_GET_MUSIC_INFO_FINISH_TYPE_SUCC = 1,
    CES_GET_MUSIC_INFO_FINISH_TYPE_TIMEOUT = 2,
    CES_GET_MUSIC_INFO_FINISH_TYPE_STOP = 3,
};

enum duer_ces_asr_mode_e {
    CES_ASR_MODE_UNKOWN = 0,
    CES_ASR_MODE_OPUS = 1,
    CES_ASR_MODE_FEATURE = 2
};

enum duer_ces_mic_detect_status_e {
    CES_MIC_DETECT_OK = 0,
    CES_MIC_DETECT_AM_ALL_ZERO = 1,
    CES_MIC_DETECT_INCONSISTENCY = 2,
    CES_MIC_DETECT_AM_CLIPPING = 4,

    // this error has no mapping in linux version
    CES_MIC_DETECT_UNIFORM = 1024
};

enum duer_ces_dsp_detect_status_e {
    CES_DSP_DETECT_RUNTIME_ERROR = 7004
};

/**
 * DESC:
 * Initialize the ces module
 *
 * PARAM: none
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int  duer_ces_init(void);

// 301 - 101
/**
 * DESC:
 * Report log when anonymous long connection ping request send failed.
 *
 * PARAM
 * @param[in] err_msg: the description of error
 * @param[in] http_status: the http response code
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_report_ping_message_request_send_failed(const char *err_msg, int http_status);

/**
 * DESC:
 * Report device status which includes system info,
 * this API should be called CA link connected.
 *
 * PARAM: none
 *
 * @return: return DUER_OK when success, negative when fail.
 */
// int duer_ces_report_device_status(void);

// 301 - 2712
/**
 * DESC:
 * Report log when dcs received throw exception.
 *
 * PARAM:
 * @param[in] msg_id: the message id
 * @param[in] err_msg: the description of error
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_throw_exception_received(const char *msg_id, const char *err_msg);

// 301 - 2711
/**
 * DESC:
 * Report log when dcs received invalid directive.
 *
 * PARAM:
 * @param[in] msg_id: the message id
 * @param[in] name_space: the namespace of directive
 * @param[in] name: the name of directive
 * @param[in] err_msg: the description of error
 * @param[in] http_status: http status code of request, if can't get it, set -1
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_invalid_directive_received(const char *msg_id, const char *name_space,
                                        const char *name, const char *err_msg, int err_msg_len,
                                        int http_status);

/**
 * DESC:
 * Set the sequnce number of current asr.
 *
 * PARAM:
 * @param[in] sn: the sequence number of asr, set NULL to clear
 *
 * @return: none
 */
void duer_ces_set_asr_sn(const char *sn);

/**
 * DESC:
 * Set the asr mode.
 *
 * PARAM:
 * @param[in] mode: the mode of asr, define in enum duer_ces_asr_mode_e
 *
 * @return: none
 */
void duer_ces_set_asr_mode(int mode);

// 301 - 2400: asr client error
/**
 * DESC:
 * Report log when asr client error.
 *
 * PARAM:
 * @param[in] err_code: the error code of asr client
 * @param[in] err_msg: the description of error
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_asr_client_error(int err_code, const char *err_msg);

/**
 * DESC:
 * Report when asr fails because long connection is unavaible.
 *
 * PARAM:
 * @param[in] err_msg: the description of error
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_asr_lc_unavailable_error(const char *err_msg);

// 301 - 2300: asr server error
/**
 * DESC:
 * Report log when asr server error.
 *
 * PARAM:
 * @param[in] err_no: the error code of asr server
 * @param[in] err_msg: the description of error
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_asr_server_error(int err_no, const char *err_msg);

// 301 - 2600: wakeup error
/**
 * DESC:
 * Report log when wakeup error.
 *
 * PARAM:
 * @param[in] err_msg: the description of error
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_wakeup_error(const char *err_msg);

// 301 - 2700
/**
 * DESC:
 * Report log when asr has result but duer service no result.
 *
 * PARAM: none
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_duer_service_error(void);

// 301 - 2730
/**
 * DESC:
 * Report log when post event to server failed.
 *
 * PARAM:
 * @param[in] event: event json, like {"header":{...}, "payload":{...}}
 * @param[in] err_msg: the description of error
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_event_post_error(const baidu_json *event, const char *err_msg);

// 301 - 4111
/**
 * DESC:
 * Report log when tts synthesize failed.
 *
 * PARAM:
 * @param[in] err_msg: the description of error
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_tts_synthesize_error(const char *err_msg);

// 302
/**
 * DESC:
 * Report log when post event to server successed.
 *
 * PARAM:
 * @param[in] msg_id: the message id
 * @param[in] name_space: the namespace of directive
 * @param[in] name: the name of directive
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_event_request_send_success(const char *msg_id, const char *name_space,
                                        const char *name);

// 303-100
/**
 * DESC:
 * Report log when failed to receive down stream directive from anonymous long connection.
 *
 * PARAM:
 * @param[in] error_msg: the description of error
 * @param[in] http_status: the http response code
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_down_stream_message_request_failed(const char *err_msg, int http_status);

/// 303-106
/**
 * DESC:
 * Report log when successful to receive down stream directive from anonymous long connection.
 *
 * PARAM:
 * @param[in] http_status: the http response code
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_down_stream_message_request_success(int http_status);

// 303-6100
/**
 * DESC:
 * Report log when long link connected.
 *
 * PARAM:
 * @param[in] msg_id: the message id
 * @param[in] sn: the sequence number of asr
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_link_connected(const char *msg_id, const char *sn);

// 303-6101
/**
 * DESC:
 * Report log when long link disconnected.
 *
 * PARAM:
 * @param[in] msg_id: the message id
 * @param[in] sn: the sequence number of asr
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_link_disconnected(const char *msg_id, const char *sn);

// 303-6102
/**
 * DESC:
 * Report log when long link connect failed.
 *
 * PARAM:
 * @param[in] msg_id: the message id
 * @param[in] sn: the sequence number of asr
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_link_connect_failed(const char *msg_id, const char *sn, const char *err_msg);

// 304-2480
/**
 * DESC:
 * Report log when asr canceled.
 *
 * PARAM: none
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_asr_canceled(void);

// 307
/**
 * DESC:
 * Report the event of network config.
 *
 * PARAM:
 * @param[in] name: event name, the macro defined above like "CES_NETWORK_CONFIG_*"
 * @param[in] error_type: error type, defined in enum duer_ces_network_config_err_type_e
 * @param[in] log_id: the log id of network config, if log_id is NULL, means use local log id
 * @param[in] err_msg: the description of error
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_network_config_event(const char *name, const int error_type,
                                  const char *log_id, const char *err_msg);

// 308-0
/**
 * DESC:
 * Report the successful event of ota.
 *
 * PARAM:
 * @param[in] name: event name, the macro defined above like "CES_OTA_*"
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_ota_success_event(const char *name);

// 308-100
/**
 * DESC:
 * Report the failed event of ota.
 *
 * PARAM:
 * @param[in] event_type: the event type
 * @param[in] err_code: the error code, defined in enum duer_ces_ota_error_type_e
 * @param[in] reason: the failed reason
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_ota_failed_event(const char *event_type, int err_code, const char *reason);

// 311
/**
 * DESC:
 * Report the playback failed event of alert.
 *
 * PARAM:
 * @param[in] err_msg: the description of error
 * @param[in] error_type: error type, defined in enum duer_ces_alert_error_type_e
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_alert_playback_failed(const char *err_msg, int error_type);

// 314-3000
/**
 * DESC
 * Report a successful check bduss dcs interface invocation.
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_check_bduss_send_success();

// 314-3001
/**
 * DESC
 * Report a failed check bduss dcs interface invocation.
 *
 * PARAM
 * @param[in] err_msg: the description of error
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_check_bduss_send_failed(const char *err_msg);

// 314-3013
/**
 * DESC
 * Report a successful ReplaceAccountInfo directive execution.
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_replace_account_info_success();

// 314-3014
/**
 * DESC
 * Report a failed ReplaceAccountInfo directive execution.
 *
 * PARAM
 * @param[in] err_msg: the description of error
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_replace_account_info_failed(const char *err_msg);

// 315
/**
 * DESC:
 * Report the event of wifi.
 *
 * PARAM:
 * @param[in] event_type: event name, the macro defined above like "CES_WIFI_*"
 * @param[in] error_type: error type, defined in enum duer_ces_wifi_error_type_e
 * @param[in] reason: wifi disconnected reason
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_event_wifi(const char *event_type, int error_type, int reason);

// 316
/**
 * DESC:
 * Report the event of bluetooth.
 *
 * PARAM:
 * @param[in] event_type: event name, the macro defined above like "CES_BT_*"
 * @param[in] event_flag: trigger type , the macro defined above like "CES_EVENT_TRIGGER_*"
 * @param[in] event_value: the name of song or singer, optional
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_event_bluetooth(const char *event_type, const char *event_flag,
                             const char *event_value);

// 317
/**
 * DESC:
 * Report the event of dlna.
 *
 * PARAM:
 * @param[in] event_type: event name, the macro defined above like "CES_DLNA_*"
 * @param[in] event_flag: trigger type , the macro defined above like "CES_EVENT_TRIGGER_*"
 * @param[in] event_value: the name of song or singer, optional
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_event_dlna(const char *event_type, const char *event_flag, const char *event_value);

// 318
/**
 * DESC:
 * Report the event of infrared.
 *
 * PARAM:
 * @param[in] event_type: event name, the macro defined above like "CES_INFRARED_*"
 * @param[in] error_type: error type, defined in enum duer_ces_infrared_error_type_e
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_event_infrared(const char *event_type, const int error_type);

// 4005
/**
 * DESC:
 * Report the event of wakeup
 *
 * PARAM:
 * @param[in] is_one_shot: whether one shot
 * @param[in] wakeup_type: defined in enum duer_ces_wakeup_type_e
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_event_wakeup(duer_bool is_one_shot, int wakeup_type);

// 4017
/**
 * DESC:
 * Report log when hourly chime
 *
 * PARAM:
 * @param[in] assert_id: assert id
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_event_hourly_chime(const char *assert_id);

// 4014
/**
 * DESC:
 * Report log when play local prompt tone
 *
 * PARAM:
 * @param[in] url: the url of prompt tone
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_event_local_play_started(const char *url);

// 4016
/**
 * DESC:
 * Report log when play tts flow failed
 *
 * PARAM:
 * @param[in] err_type: define in enum duer_ces_tts_play_error_type_e
 * @param[in] msg_id: the message id
 * @param[in] content_id: the content id
 * @param[in] err_msg: the description of error
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_event_tts_play_failed(const int err_type, const char *msg_id,
                                   const char *content_id, const char *err_msg);

// 5060 -- normal reboot
/**
 * DESC:
 * Report log when device reboot normally
 *
 * PARAM:
 * @param[in] err_type: the reason of reboot,  define in enum duer_ces_reboot_error_type_e
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_event_normal_reboot(const int err_type, const char *err_msg);

// 5061 -- abnormal reboot
/**
 * DESC:
 * Report log when device reboot abnormally
 *
 * PARAM:
 * @param[in] err_type: the reason of reboot,  define in enum duer_ces_reboot_error_type_e
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_event_abnormal_reboot(const int err_type, const char *err_msg);

// 5080 -- Mic Online Detect
/**
 * DESC:
 * Report the status of mic
 *
 * PARAM:
 * @param[in] mic_detect_status: detect status, bitwise-or of enum duer_ces_mic_detect_status_e
 * @param[in] volume: device volume
 * @param[in] desc: description
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_event_mic_online_detect(int mic_detect_status, int volume, const char *desc);

// 5085 -- Device health monitor
/**
 * DESC:
 * Report device cpu, mem, disk info
 *
 * PARAM:
 * @param[in] uptime: system uptime in seconds.
 * @param[in] cpu_temperature: cpu temperature.
 * @param[in] cpu_usages: cpu usage percentage * 100.
 * @param[in] cpu_count: number of cpu.
 * @param[in] spram_avail_KB: available spram in KB.
 * @param[in] spram_total_KB: total spram in KB.
 * @param[in] internal_ram_avail_KB: available internal ram in KB
 * @param[in] internal_ram_total_KB: total internal ram in KB
 * @param[in] data_partition_avail: available data partition size in KB.
 * @param[in] data_partition_total: total data partition size in KB.
 */
int duer_ces_event_device_health_monitor(
        int uptime, int cpu_temperature, const float *cpu_usages, int cpu_count,
        int spram_avail_KB, int spram_total_KB,
        int internal_ram_avail_KB, int internal_ram_total_KB,
        int data_partition_avail_KB, int data_partition_total_KB);

// 5105: wifi detect monitor
/**
 * DESC:
 * Report wifi detect information
 *
 * PARAM:
 * @param[in] wifi_data: json format.
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_event_wifi_monitor(const char *wifi_data);

// 5156 - audio play cancelled by wakeup
/**
 * DESC:
 * Report when audio play is cancelled when wakeup occurs during tts playing before audio play.
 *
 * PARAM:
 * @param[in] duration: play duration
 * @param[in] desc: description
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_event_audio_play_cancelled(int duration, const char *desc);

// 5218 - dsp abnormal status detect
/**
 * DESC:
 * Report when dsp encounters error and needs to be restarted.
 *
 * PARAM:
 * @param[in] err_type: error code
 * @param[in] restart: restart dsp or not
 * @param[in] desc: description
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_event_dsp_abnormal_detect(int err_type, duer_bool restart, const char *desc);

// flow
// 4001
/**
 * DESC:
 * Init dcs player velocity stat.
 *
 * PARAM:
 * @param[in] msg_id: the message id
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_init_dcs_player_velocity_stat(const char *msg_id);

/**
 * DESC:
 * Set the dialog id of dcs player velocity stat.
 *
 * PARAM:
 * @param[in] dialog_id: the dialog id
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_dpvs_set_dialog_id(const char *dialog_id);

/**
 * DESC:
 * Clock the directive time of dcs player.
 *
 * PARAM:none
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_dpvs_clock_directive_time(void);

/**
 * DESC:
 * Set the url of dcs player velocity stat.
 *
 * PARAM:
 * @param[in] url: the resource url
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_dpvs_set_resource_url(const char *url);

/**
 * DESC:
 * Get the url of dcs player velocity stat.
 *
 * PARAM: none
 *
 * @return: return the url.
 */
const char *duer_ces_dpvs_get_resource_url(void);

/**
 * DESC:
 * Set the url ip of dcs player velocity stat.
 *
 * PARAM:
 * @param[in] ip: the ip of resource url
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_dpvs_set_resource_ip(const char *ip);

/**
 * DESC:
 * Clock the start time of dcs player.
 *
 * PARAM:none
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_dpvs_clock_player_start_time(void);

/**
 * DESC:
 * Clock the ready time of dcs player.
 *
 * PARAM:none
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_dpvs_clock_player_ready_time(void);

/**
 * DESC:
 * Clock the start time of dcs player to get music info.
 *
 * PARAM:none
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_dpvs_clock_player_get_music_info_start_time(void);

/**
 * DESC:
 * Clock the end time of dcs player to get music info.
 *
 * PARAM:none
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_dpvs_clock_player_get_music_info_end_time(void);

/**
 * DESC:
 * Set the retry cout of dcs player to get music info.
 *
 * PARAM:
 * @param[in] count: retry cout
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_dpvs_set_get_music_info_retry_count(int count);

/**
 * DESC:
 * Set the finish type of dcs player to get music info.
 *
 * PARAM:
 * @param[in] type: defined in enum duer_get_music_info_finish_type_e
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_dpvs_set_get_music_info_finish_type(int type);

/**
 * DESC:
 * Set the request type of dcs player velocity stat.
 *
 * PARAM:
 * @param[in] request_type: defined in enum duer_ces_request_type_e
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_dpvs_set_request_type(int request_type);

/**
 * DESC:
 * Set the asr type of dcs player velocity stat.
 *
 * PARAM:
 * @param[in] asr_type: defined in enum duer_ces_asr_type_e
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_dpvs_set_asr_type(int asr_type);

/**
 * DESC:
 * Clear the dcs player velocity stat.
 *
 * PARAM:none
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_dpvs_clear(void);

/**
 * DESC:
 * Report the dcs player velocity stat.
 *
 * PARAM:none
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_dpvs_complete(void);

// 4003
/**
 * DESC:
 * Init dcs client velocity stat.
 *
 * PARAM:
 * @param[in] msg_id: the message id
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_init_dcs_client_velocity_stat(const char *msg_id);

/**
 * DESC:
 * Set the dialog id of dcs client velocity stat.
 *
 * PARAM:
 * @param[in] dialog_id: the dialog id
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_dcvs_set_dialog_id(const char *dialog_id);

/**
 * DESC:
 * Clock the request start time of dcs client.
 *
 * PARAM:none
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_dcvs_clock_request_start_time(void);

/**
 * DESC:
 * Clock the recoder button release time of dcs client.
 *
 * PARAM:none
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_dcvs_clock_button_release_time(void);

/**
 * DESC:
 * Clock the vad end time of dcs client.
 *
 * PARAM:none
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_dcvs_clock_vad_end_time(void);

/**
 * DESC:
 * Clock the stop listen time of dcs client.
 *
 * PARAM:none
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_dcvs_clock_stop_listen_time(void);

/**
 * DESC:
 * Clock the audio send end time of dcs client.
 *
 * PARAM:none
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_dcvs_clock_audio_send_end_time(void);

/**
 * DESC:
 * Clock the asr result time of dcs client.
 *
 * PARAM:none
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_dcvs_clock_asr_result_time(void);

/**
 * DESC:
 * Set the asr result text of dcs client.
 *
 * PARAM:
 * @param[in] text: the text of asr result
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_dcvs_set_asr_result_text(const char *text);

/**
 * DESC:
 * Clock the duer_result_parsing_time of dcs client.
 *
 * PARAM:none
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_dcvs_clock_duer_result_parsing_time(void);

/**
 * DESC:
 * Set the directive name of dcs client velocity stat.
 *
 * PARAM:
 * @param[in] name: the directive name
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_dcvs_set_duer_directive_name(const char *name);

/**
 * DESC:
 * Clock the duer_result_time of dcs client.
 *
 * PARAM:none
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_dcvs_clock_duer_result_time(void);

/**
 * DESC:
 * Clock the duer_result_enque_time of dcs client.
 *
 * PARAM:none
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_dcvs_clock_duer_result_enque_time(void);

/**
 * DESC:
 * Clock the duer_result_throw_time of dcs client.
 *
 * PARAM:none
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_dcvs_clock_duer_result_throw_time(void);

/**
 * DESC:
 * Clock the duer_speak_time of dcs client.
 *
 * PARAM:none
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_dcvs_clock_duer_speak_time(void);

/**
 * DESC:
 * Clock the duer_speak_enque_time of dcs client.
 *
 * PARAM:none
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_dcvs_clock_duer_speak_enque_time(void);

/**
 * DESC:
 * Clock the duer_speak_pop_time of dcs client.
 *
 * PARAM:none
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_dcvs_clock_duer_speak_pop_time(void);

/**
 * DESC:
 * Clock the tts_data_time of dcs client.
 *
 * PARAM:none
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_dcvs_clock_tts_data_time(void);

/**
 * DESC:
 * Clock the tts_voice_time of dcs client.
 *
 * PARAM:none
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_dcvs_clock_tts_voice_time(void);

/**
 * DESC:
 * Set the request type of dcs player velocity stat.
 *
 * PARAM:
 * @param[in] request_type: defined in enum duer_ces_request_type_e
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_dcvs_set_request_type(int request_type);

/**
 * DESC:
 * Set the asr type of dcs client velocity stat.
 *
 * PARAM:
 * @param[in] asr_type: defined in enum duer_ces_asr_type_e
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_dcvs_set_asr_type(int asr_type);

/**
 * DESC:
 * Clear the dcs client velocity stat.
 *
 * PARAM:none
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_dcvs_clear(void);

/**
 * DESC:
 * Report the dcs client velocity stat.
 *
 * PARAM:none
 *
 * @return: return DUER_OK when success, negative when fail.
 */
int duer_ces_dcvs_complete(void);

/**
 * DESC:
 * Set the location info.
 *
 * PARAM:
 * @param[in] lo: longitude
 * @param[in] la: latitude
 * @param[in] location_system: such as "wgs84", "bd09mc", "bd09ll"
 *
 * @return: none.
 */
void duer_ces_set_location(double lo, double la, const char *location_system);

/**
 * DESC:
 * Control the print of ces msg
 *
 * PARAM:
 * @param[in] enable: whether print ces msg
 *
 * @return: none.
 */
void duer_ces_print_msg(duer_bool enable);

/**
 * DESC:
 * Notify ces sender audio play status
 *
 * PARAM:
 * @param[in] playing: whether audio is playing
 *
 * @return: none.
 */
void duer_ces_notify_audio_playing(duer_bool playing);

// only for test
void duer_ces_test(void);

#endif /* BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_CES_H */
