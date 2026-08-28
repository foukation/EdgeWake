/**
 * Copyright (2017) Baidu Inc. All rights reserved.
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
 * File: duerapp.c
 * Auth: Zhang Leliang(zhanglelaing@baidu.com)
 * Desc: Duer Application Main.
 */

#include <string.h>
#include <stdbool.h>

#include "lightduer.h"
#include "lightduer_voice.h"
#include "lightduer_net_ntp.h"
#include "lightduer_memory.h"
#include "lightduer_log.h"
#include "lightduer_ap_info.h"
#include "lightduer_lib.h"
#include "lightduer_profile.h"
// #include "duerapp_args.h"
#include "duerapp_config.h"
#include "lightduer_key.h"
#include "lightduer_sleep.h"
#include "lightduer_connagent.h"
#include "lightduer_dcs.h"
#include "lightduer_dcs_local.h"
#include "lightduer_system_info.h"
// #include "lightduer_ws_client.h"
#include "lightduer_thread.h"
#include "lightduer_auto_dns.h"
// #include "lightduer_dcs_sota_private.h"
// #include "lightduer_sota_asr.h"
// #include "lightduer_file_recorder.h"
#include "lightduer_ds_report.h"
#include "lightduer_anonymous_auth.h"
// #include "lightduer_watch_api.h"
#include "lightduer_http_client_ops.h"
#include "lightduer_http_client.h"
#include "lightduer_net_trans_common_events_handle.h"
#include "lightduer_net_trans_encrypted.h"

#include "lightduer_priority_conf.h"

duer_system_static_info_t g_system_static_info = {
    .os_version         = "Ubuntu 16.04.4",
    .sw_version         = "dcs3.0-demo",
    .brand              = "Baidu",
    .hardware_version   = "1.0.0",
    .equipment_type     = "PC",
    .ram_size           = 4096 * 1024,
    .rom_size           = 2048 * 1024,
};

#define SOTA_PID_STREAMING 100
#define SOTA_PID_NON_STREAMING 301

extern int duer_init_play(void);

static bool s_started = false;
static bool s_asr_started = false;
static bool s_asr_error = false;
static int s_is_token = 0;
static int s_reconnect_time = 1;

static void __auto_link_click_send_init(void);

static void on_asr_middle_result(const char *result)
{
    DUER_LOGI("asr middle result : %s", result);
}

static void on_asr_final_result(const char *result)
{
    DUER_LOGI("asr final result : %s", result);
}

static void duer_record_event_callback(int event, const void *data, size_t param)
{
    DUER_LOGD("record callback : %d", event);
    switch (event) {
    case REC_START:
    {
        s_asr_started = DUER_FALSE;
        s_asr_error = DUER_FALSE;
        duer_dcs_on_listen_started();
        duer_voice_start(param);
        while (!s_asr_started && !s_asr_error)
        {
            DUER_LOGI("wait asr started/error event");
            duer_sleep(500);
        }
        DUER_LOGI("=======s_asr_started========");
        break;
    }
    case REC_DATA:
        if (s_asr_started)
        {
            duer_voice_send(data, param);
        }
        break;
    case REC_STOP:
        DUER_LOGD("stop send voice: %s", (const char *)data);
        duer_voice_stop();
        duer_dcs_on_listen_stopped();
        break;
    case REC_CANCEL:
        DUER_LOGD("cancel send voice");
        duer_voice_cancel();
        break;
    default:
        DUER_LOGE("event not supported: %d!", event);
        break;
    }
}

static void duer_dcs_init()
{
    duer_dcs_framework_init();
    duer_dcs_voice_input_init();
    duer_dcs_voice_output_init();
    duer_dcs_speaker_control_init();
    duer_dcs_audio_player_init();
    duer_dcs_screen_init();
    duer_dcs_sota_private_init();
    duer_dcs_sync_state();
    duer_dcs_screen_ext_card_watch_pay_init();
}

void duer_txt2tts(const char *text)
{
    duer_status_t result = DUER_OK;
    baidu_json *speech = NULL;
    baidu_json *log = NULL;
    baidu_json *data = NULL;
    speech = baidu_json_CreateObject();
    log = baidu_json_CreateObject();
    data = baidu_json_CreateObject();
    if (data == NULL) {
        DUER_LOGE("data json create fail");
        return;
    }
    baidu_json_AddStringToObjectCS(data, "txt", text);
    baidu_json_AddNumberToObjectCS(log, "id", 123);
    baidu_json_AddItemToObjectCS(log, "data", (baidu_json*)data);
    baidu_json_AddStringToObjectCS(log, "extra", "liulili");
    baidu_json_AddItemToObjectCS(speech, "duer_speech", (baidu_json*)log);
    result = duer_data_report(speech, DUER_SPEECH);
    if (result != DUER_OK) {
        DUER_LOGE("report speech fail: %d", result);
    }
    if (speech != NULL) {
        baidu_json_Delete(speech);
    }
}

static void duer_reconnect_thread()
{
    pthread_detach(pthread_self());
    if (s_reconnect_time >= 16) {
        s_reconnect_time = 1;
    }
    // sleep(s_reconnect_time);
    s_reconnect_time *= 2;
    DUER_LOGI("Start reconnect baidu cloud!");
    duer_start(NULL, 0);
}

static void duer_event_hook(duer_event_t *event)
{
    if (!event) {
        DUER_LOGE("NULL event!!!");
        return;
    }

    DUER_LOGD("event: %d", event->_event);
    switch (event->_event) {
    case DUER_EVENT_STARTED:
        DUER_LOGI("===============DUER_EVENT_STARTED==============");
        // duer_dcs_init();
        // duer_dcs_capability_declare(DCS_TTS_HTTPS_PROTOCAL_SUPPORTED);
        s_reconnect_time = 1;
        s_started = true;
        break;
    case DUER_EVENT_ASR_STARTED:
        DUER_LOGI("===============DUER_EVENT_ASR_STARTED==============");
        s_asr_started = DUER_TRUE;
        s_asr_error = DUER_FALSE;
        break;
    case DUER_EVENT_ASR_ERROR:
        DUER_LOGI("===============DUER_EVENT_ASR_ERROR==============");
        s_asr_error = DUER_TRUE;
        s_asr_started = DUER_FALSE;
        break;
    case DUER_EVENT_ASR_FINISHED:
        DUER_LOGI("===============DUER_EVENT_ASR_FINISHED==============");
        on_asr_final_result(event->_object);
        s_asr_started = DUER_FALSE;
        break;
    case DUER_EVENT_ASR_CANCELLED:
        DUER_LOGI("===============DUER_EVENT_ASR_CANCELLED==============");
        s_asr_started = DUER_FALSE;
        break;
    case DUER_EVENT_ASR_PROCESSING:
        DUER_LOGI("===============DUER_EVENT_ASR_PROCESSING==============");
        on_asr_middle_result(event->_object);
        break;
    case DUER_EVENT_STOPPED:
        DUER_LOGI("===============DUER_EVENT_STOPPED==============");
        s_started = false;
#if 0
        pthread_t reconnect_threadid;
        int ret = pthread_create(&reconnect_threadid, NULL,
                        (void *)duer_reconnect_thread, NULL);
        if(ret != 0)
        {
            DUER_LOGE("Create reconnect pthread error!");
        } else {
            pthread_setname_np(reconnect_threadid, "reconnect");
        }
#endif
        break;
    // case DUER_EVENT_LINK_STARTED:
    //     DUER_LOGI("===============DUER_EVENT_LINK_STARTED==============");
    //     __auto_link_click_send_init();
    //     break;
    // case DUER_EVENT_LINK_STOPPED:
    //     DUER_LOGI("===============DUER_EVENT_LINK_STOPPED==============");
    //     break;
    default:
        DUER_LOGI("unknown event %d", event->_event);
        break;
    }
}

static int duer_init_profile(void)
{
    int ret = 0;
    duer_profile_constant_params_t params = {
        .os_name = "linux",
        .os_version = DUER_OS_VERSION,
        .firmware_version = "1.0",
        .device_sn = "11223344556677889900aa",
        .client_id = "YOUR_CLIENTID",
        .secret_id = "YOUR_SECRET",
    };

    ret = duer_profile_init(&params);
    // extern void duer_profile_forceupdate_cuid(const char *cuid);
    duer_profile_set_string_param(PROFILE_KEY_BDSPEECH_HOST, "vse.baidu.com");
    duer_profile_set_int_param(PROFILE_KEY_BDSPEECH_PORT, 443);
    duer_profile_set_string_param(PROFILE_KEY_BDSPEECH_KEY, "com.baidu.dumi.dcs31.xiaotiancai");
    duer_profile_set_int_param(PROFILE_KEY_BDSPEECH_PID_TYPE, 1);
    duer_profile_set_int_param(PROFILE_KEY_BDSPEECH_PRIMARY_PID, 1428);
    duer_profile_set_string_param(PROFILE_KEY_EVT_UPLOAD_KEY_ENGINE_URI, "");
    duer_profile_set_string_param(PROFILE_KEY_SOTA_APP_ID, "YOUR_APP_ID");
    duer_profile_set_string_param(PROFILE_KEY_SOTA_APP_KEY, "YOUR_APP_KEY");
    duer_profile_set_int_param(PROFILE_KEY_SOTA_PID, SOTA_PID_STREAMING);
    duer_profile_set_string_param(PROFILE_KEY_SOTA_ASR_PROXY, "wss://duer-kids.baidu.com/sota/realtime_asr");
    // duer_profile_set_int_param(PROFILE_KEY_SOTA_DCS_CONTEXT, 6); // 6 for nlu
    // duer_profile_set_int_param(PROFILE_KEY_SOTA_DCS_CONTEXT, 4);
    duer_profile_set_int_param(PROFILE_KEY_SOTA_ASR_ONLY, 0);
    // duer_profile_set_string_param(PROFILE_KEY_SOTA_EVENT_URI, "wss://duer-kids.baidu.com/mqtt");
    duer_profile_set_string_param(PROFILE_KEY_SOTA_EVENT_URI, "wss://duer-iotlp.baidu.com/mqtt");
    // duer_profile_set_int_param(PROFILE_KEY_SOTA_LINK_TYPE, 2); // 0 - DISABLE 1 - SHORT 2 - LONG

    duer_set_didp_version(INVALID_DIDP_VERSION);
    duer_set_didp_version(34);
    duer_set_didp_version(12);
    duer_update_user_agent();
    if (ret != DUER_OK) {
        DUER_LOGE("init profile failed");
        return ret;
    }
    duer_profile_set_string_param(PROFILE_KEY_SOTA_APP_ID, "duer_kids_watch");
    duer_profile_set_string_param(PROFILE_KEY_SOTA_APP_KEY, "a85377f6dc03fd3c5b910ae52b3a19b0");
    return 0;
}

static duer_connagent_type test_conns[] = {
    DUER_CONNAGENT_SOTA,
    // DUER_CONNAGENT_BDSPEECH_WATCH,
};
#define TEST_CONN_LEN (sizeof(test_conns) / sizeof(test_conns[0]))

typedef struct {
    int     wakeup_status;
    size_t  param_length;
    char    *param;
    int     print_voice;
} duer_asrparam_t;

duer_status_t on_anonymous_auth(const char *access_token)
{
    DUER_LOGI("access_token = %s", access_token);
    s_is_token = (access_token && DUER_STRLEN(access_token) > 0) ? 1 : 2;
}

void anonymous_task(unsigned int argc, duer_thread_entry_args_t *params)
{
    int ret = duer_anonymous_auth_request(on_anonymous_auth);
    if (ret != 0) {
        on_anonymous_auth(NULL);
    }
    duer_thread_exit(params, 1);
}

#include "lightduer_random.h"

extern unsigned int pay_send_times;
extern unsigned int pay_ack_times;
static void __auto_link_click_send_task(unsigned int argc, duer_thread_entry_args_t *params)
{
    unsigned int random_time_ms = 100U;

    for (;;) {
        random_time_ms = 200 + duer_random() % 100;
        random_time_ms *= 1000;
        // random_time_ms = 1000;
        DUER_LOGI("liutai: %d", random_time_ms);
        duer_dcs_on_link_clicked("dueros://cc33b657-eacb-2b9e-4f7c-e2fe002b1327/buyXiaoduWatchVip?source=wujian");
        pay_send_times++;
        DUER_LOGI("you need pay, sended: %u, akced: %u", pay_send_times, pay_ack_times);
        duer_sleep(random_time_ms);
    }
    duer_thread_exit(params, 1);
}

static void __auto_link_click_send_init(void)
{
    static signed char send_click_inited = 0;
    if (0 != send_click_inited) {
        return;
    }

    send_click_inited = 1;
    duer_thread_t hdlr = NULL;

    hdlr = duer_thread_create("auto_link_click_task",
                              "auto_link_click_queue",
                              16384,
                              1,
                              duer_priority_get(DUER_TASK_OPENAPI_HTTP));
    if (!hdlr)
    {
        DUER_LOGE("Create auto_link_click_task failed.");
        return;
    }

    duer_status_t ret = duer_thread_start(hdlr,
                                          __auto_link_click_send_task,
                                          NULL);
    if (ret != DUER_OK)
    {
        duer_thread_destroy(hdlr);
        DUER_LOGE("start auto_link_click_task failed.");
        return;
    }

    DUER_LOGI("Create auto_link_click_task Success.");
}

static void user_start_asr(void)
{
    DUER_LOGI("debug asr_start cmd");
    duer_asr_cancel();
    duer_asrparam_t param = {.wakeup_status = 200};
    baidu_json *event = duer_dcs_listen_start_prepare();
    duer_dcs_listen_start(&param, event);
}

int xiaodu_main(void)
{
    int loop = 0;
    DUER_LOGI("before initialize");// will not print due to before initialize

    // Initialize the Duer OS
    // duer_args_t duer_args = DEFAULT_ARGS;
    // duer_args_parse(argc, argv, &duer_args, true);
    // char *voice_record_path = "voice.opus";

    duer_adapter_initialize();
    for (int i = 0; i < 1; i--)
    {
        DUER_LOGI("\n========ROUND %d========\n", i);
        // duer_file_recorder_set_path(voice_record_path, 0);
        duer_trans_init_common_events_handle();
        duer_auto_dns_init(NULL, NULL);

        duer_connagent_register_all();
        duer_set_active_connagents(TEST_CONN_LEN, test_conns);
        duer_init_ssl_cacert();

        duer_init_profile();
        duer_set_event_callback(duer_event_hook);
        duer_initialize();
        DUER_LOGI("after initialize");

        // duer_record_set_event_callback(duer_record_event_callback);
        duer_voice_initialize();
        duer_wifi_set_connected(1);
        int ret = duer_watch_api_task_execute(anonymous_task, NULL);
        if (ret != 0) {
            DUER_LOGE("start anonymous task failed: %d");
            return 1;
        }

        while (!s_is_token)
        {
            DUER_LOGI("waiting for anonymous auth ...");
            duer_sleep(1000);
        }
        if (s_is_token != 1)
        {
            DUER_LOGE("get token failed");
            return 2;
        }
    
        // Set the Duer Event Callback
        duer_start(NULL, 0);

        while (!s_started)
        {
            DUER_LOGI("waiting for duer started event ...");
            duer_sleep(1000);
        }
        duer_dcs_init();
        extern void duer_dlp_init();
        duer_dlp_init();
        // DUER_LOGI("in main thread send record file");
        // duer_dcs_on_link_clicked("dueros://server.dueros.ai/query?q=床前明月光的下一句");
        // duer_asr_cancel();
        // duer_record_all(duer_args.voice_record_path, duer_args.interval_between_query * 1000);
        // duer_txt2tts();

        
        // user_start_asr();
        // sleep(1);
        // user_start_asr();
        

        // sleep(20);
        // duer_dcs_uninitialize();
        // duer_stop();
        while (s_started || 1)
        {
            // wait for DUER_EVENT_STOPPED
            DUER_LOGI("waiting for duer stopped event ...");
            // sleep(1);
        }
        duer_auto_dns_deinit();
        duer_trans_destroy_common_events_handle();
        duer_finalize();
        duer_ds_report_destroy();
        // sleep(3);

        duer_file_recorder_clear_path();
        duer_voice_finalize();
        duer_profile_deinit();
        duer_deinit_ssl_cacert();
        duer_connagent_unregister_all();
        // sleep(1);
    }
    duer_adapter_deinitialize();
    // sleep(1);

    return 0;
}

