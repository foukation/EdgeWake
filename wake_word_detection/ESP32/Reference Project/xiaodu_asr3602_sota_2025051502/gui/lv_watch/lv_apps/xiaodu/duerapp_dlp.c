#include "lightduer_dcs.h"
#include "lightduer_lib.h"
#include "lightduer_log.h"
#include "lightduer_coap_defs.h"
#include "lightduer_memory.h"
#include "lightduer_profile.h"
#include "lightduer_dlp_router.h"
#include "lightduer_dlp_dcs_adapter.h"
#include "lightduer_dcs_router.h"
#include "lightduer_dcs_local.h"
#include "lightduer_mutex.h"
#include "lightduer_uuid.h"
#include "lightduer_timestamp.h"

#include "lightduer_thread.h"
#include "lightduer_priority_conf.h"
#include "lightduer_sleep.h"

#define DLP_SYSTEM_UPDATE                               "dlp.system_update"

typedef enum {
    dlp_system_update_report_status_install_success = 0,
    dlp_system_update_report_status_install_failed,
    dlp_system_update_report_status_install,
    dlp_system_update_report_status_begin,
    dlp_system_update_report_status_download,
    dlp_system_update_report_status_download_success,
    dlp_system_update_report_status_download_failed,
    dlp_system_update_report_status_end,
    dlp_system_update_report_status_end_reboot
} dlp_system_update_report_status;

__attribute__((weak)) void duer_dlp_system_update_handle_get_status(const char *currentVersion, const char *targetVersion)
{
    baidu_json *root = NULL;
    baidu_json *to_client = NULL;
    baidu_json *header = NULL;
    baidu_json *payload = NULL;

    DUER_LOGI("update to ota");

    do {
        root = baidu_json_CreateObject();
        if (!root) {
            DUER_LOGW("Create root err.");
            break;
        }

        to_client = duer_create_dcs_event(DLP_SYSTEM_UPDATE, "Status", DUER_FALSE);
        if (!to_client) {
            DUER_LOGW("Create to_client err.");
            break;
        }

        baidu_json_AddItemToObjectCS(root, DLP_TO_CLIENT_KEY, to_client);

        header = baidu_json_GetObjectItem(to_client, DLP_HEADER_KEY);
        if (!header) {
            DUER_LOGI("Get header err.");
            break;
        }

        payload = baidu_json_GetObjectItem(to_client, DLP_PAYLOAD_KEY);
        if (!payload) {
            DUER_LOGI("Get payload err.");
            break;
        }

        char uuid[DUER_UUID_STRLEN] = {'\0'};
        duer_generate_uuid(uuid);
        baidu_json_AddStringToObjectCS(header, DLP_MESSAGE_ID_KEY, uuid);

        // 当前版本号
        baidu_json_AddStringToObjectCS(payload, "currentVersion", currentVersion);
        // 当前版本特性描述
        baidu_json_AddStringToObjectCS(payload, "currentVersionDesc", "currentVersionDesc");

        // 目标版本号
        baidu_json_AddStringToObjectCS(payload, "targetVersion", targetVersion);
        // 目标版本特性描述
        baidu_json_AddStringToObjectCS(payload, "targetVersionDesc", "targetVersionDesc");

        // 设备当前状态
        // NOT_UPDATABLE软件已更新到最新版本（或设备busy处于不可更新状态）
        // UPDATABLE可以进行系统软件更
        // UPDATING设备收到Update命令，已经开始执行更新操作（更新完毕后应转为NOT_UPDATABLE)
        // UPDATE_SUCCESS
        // UPDATE_FAIL
        baidu_json_AddStringToObjectCS(payload, "state", "UPDATABLE");

        // 需要展现给用户的消息，例如音箱升级成功推送UPDATE_SUCCESS消息时，附带的message可以是"升级成功，音箱即将重新启动
        baidu_json_AddStringToObjectCS(payload, "message", "message");
        

        // char *x = baidu_json_PrintBuffered(root, JSON_PREBUFF_SIZE_1024, 1);
        // if (x) {
        //     DUER_LOGI("[%s]:[%s]", __FUNCTION__, x);
        //     baidu_json_release(x);
        // }

        duer_handle_event(root);
        baidu_json_Delete(root);

        return 0;
    } while(0);

    if (root) {
        baidu_json_Delete(root);
    }
}

__attribute__((weak)) void duer_dlp_system_update_handle_send_status(dlp_system_update_report_status status, int percent)
{
    baidu_json *root = NULL;
    baidu_json *to_client = NULL;
    baidu_json *header = NULL;
    baidu_json *payload = NULL;
    char *status_text = "end";

    DUER_LOGI("send to ota");

    do {
        root = baidu_json_CreateObject();
        if (!root) {
            DUER_LOGW("Create root err.");
            break;
        }

        to_client = duer_create_dcs_event(DLP_SYSTEM_UPDATE, "UpdateProgressStatus", DUER_FALSE);
        if (!to_client) {
            DUER_LOGW("Create to_client err.");
            break;
        }

        baidu_json_AddItemToObjectCS(root, DLP_TO_CLIENT_KEY, to_client);

        header = baidu_json_GetObjectItem(to_client, DLP_HEADER_KEY);
        if (!header) {
            DUER_LOGI("Get header err.");
            break;
        }

        payload = baidu_json_GetObjectItem(to_client, DLP_PAYLOAD_KEY);
        if (!payload) {
            DUER_LOGI("Get payload err.");
            break;
        }

        char uuid[DUER_UUID_STRLEN] = {'\0'};
        duer_generate_uuid(uuid);
        baidu_json_AddStringToObjectCS(header, DLP_MESSAGE_ID_KEY, uuid);

        switch (status) {
            case dlp_system_update_report_status_install_success:
                status_text = "install_success";
                break;
            case dlp_system_update_report_status_install_failed:
                status_text = "install_failed";
                break;
            case dlp_system_update_report_status_install:
                status_text = "install";
                break;
            case dlp_system_update_report_status_begin:
                status_text = "begin";
                break;
            case dlp_system_update_report_status_download:
                status_text = "download";
                break;
            case dlp_system_update_report_status_download_success:
                status_text = "download_success";
                break;
            case dlp_system_update_report_status_download_failed:
                status_text = "download_failed";
                break;
            case dlp_system_update_report_status_end:
                status_text = "end";
                break;
            case dlp_system_update_report_status_end_reboot:
                status_text = "end_reboot";
                break;
            default:
                DUER_LOGE("unknow status");
                return;
        }

        // 升级中的某个中间状态
        baidu_json_AddStringToObjectCS(payload, "updateState", status_text);
        // 某个中间状态的百分比。如果这个状态不存在百分比，值需要设为-1
        // 注意状态值的发送不能过于频繁，否则会让时序乱掉，建议3-5秒发送一次
        baidu_json_AddNumberToObjectCS(payload, "percent", percent);

        char *x = baidu_json_PrintBuffered(root, JSON_PREBUFF_SIZE_1024, 1);
        if (x) {
            DUER_LOGI("[%s]:[%s]", __FUNCTION__, x);
            baidu_json_release(x);
        }

        duer_handle_event(root);
        baidu_json_Delete(root);

        return 0;
    } while(0);

    if (root) {
        baidu_json_Delete(root);
    }
}

static void __system_update_simulate_task(unsigned int argc, duer_thread_entry_args_t *params)
{
    dlp_system_update_report_status status = dlp_system_update_report_status_begin;
    int percent = -1;
    int random_time_ms = 1000;

    duer_dlp_system_update_handle_send_status(dlp_system_update_report_status_begin, -1);
        
    percent = 0;
    while (percent <= 100) {
        
        duer_dlp_system_update_handle_send_status(dlp_system_update_report_status_download, percent);
        percent += 20;
        duer_sleep(random_time_ms);
    }
    duer_dlp_system_update_handle_send_status(dlp_system_update_report_status_download_success, -1);

    duer_dlp_system_update_handle_get_status("2", "2");

    // percent = 0;
    // while (percent <= 100) {
        
    //     duer_dlp_system_update_handle_send_status(dlp_system_update_report_status_install, percent++);
    //     duer_sleep(random_time_ms);
    // }
    // duer_dlp_system_update_handle_send_status(dlp_system_update_report_status_install_success, -1);

    // duer_dlp_system_update_handle_send_status(dlp_system_update_report_status_end, -1);
    // duer_dlp_system_update_handle_send_status(dlp_system_update_report_status_end_reboot, -1);

    duer_thread_exit(params, 1);
}

static void duer_dlp_system_update_simulate_start(void)
{
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
                                          __system_update_simulate_task,
                                          NULL);
    if (ret != DUER_OK)
    {
        duer_thread_destroy(hdlr);
        DUER_LOGE("start auto_link_click_task failed.");
        return;
    }

    DUER_LOGI("Create auto_link_click_task Success.");
}

__attribute__((weak)) void duer_dlp_system_update_handle_update(const char *from)
{
    duer_dlp_system_update_simulate_start();
}

__attribute__((weak)) void duer_dlp_system_update_handle_test_set_version(const char *version)
{
    DUER_LOGW("Please implement duer_dlp_system_update_handle_test_set_version to set version");
}

static duer_status_t  duer_dlp_system_update_cb(const baidu_json *directive)
{
    
    char *out = baidu_json_Print(directive);
    DUER_LOGI("dlp system update directive received: %s", out);
    DUER_FREE(out);

        baidu_json *dlp_to_server = NULL;
    baidu_json *dlp_header = NULL;
    baidu_json *dlp_name = NULL;
    baidu_json *payload = NULL;
    baidu_json *version = NULL;
    baidu_json *from = NULL;

    DUER_LOGI("duer_dlp_ota_callback received dlp message.");
    dlp_to_server = baidu_json_GetObjectItem(directive, "to_server");
    if (!dlp_to_server) {
        DUER_LOGE("get dlp_to_server err.");
        return DUER_ERR_FAILED;
    }

    dlp_header = baidu_json_GetObjectItem(dlp_to_server, "header");
    if (!dlp_header) {
        DUER_LOGE("get dlp_header err.");
        return DUER_ERR_FAILED;
    }

    dlp_name = baidu_json_GetObjectItem(dlp_header, "name");
    if (!dlp_name || !dlp_name->valuestring) {
        DUER_LOGE("get dlp_name err.");
        return DUER_ERR_FAILED;
    }

    payload = baidu_json_GetObjectItem(dlp_to_server, "payload");
    if (!payload) {
        DUER_LOGE("get payload err.");
        return DUER_ERR_FAILED;
    }

    if (!DUER_STRCMP(dlp_name->valuestring, "GetStatus")) {
        const int MIN_INTERVAL_MS = 10000; // 10 seconds
        static uint32_t last_ts = 0;
        uint32_t now = duer_timestamp();
        if (last_ts > 0 && DUER_TIME_DIFF(last_ts, now) < MIN_INTERVAL_MS) {
            DUER_LOGW("dlp.system_update.Update is coming too frequently, drop it");
            return DUER_OK;
        }
        last_ts = now;
        duer_dlp_system_update_handle_get_status("1", "2");
    } else if (!DUER_STRCMP(dlp_name->valuestring, "Update")) {
        from = baidu_json_GetObjectItem(payload, "from");
        duer_dlp_system_update_handle_update((from && from->valuestring) ? from->valuestring : "APP");
    } else if (!DUER_STRCMP(dlp_name->valuestring, "TestSetVersion")) {
        version = baidu_json_GetObjectItem(payload, "version");
        if (version && version->valuestring) {
            duer_dlp_system_update_handle_test_set_version(version->valuestring);
        }
    } else {
        DUER_LOGE("no match name");
        return DUER_ERR_FAILED;
    }

}

void duer_dlp_control_init(void)
{
    // duer_add_dlp_directive(DLP_CHILD_WATCH_CONTROL, duer_dlp_child_watch_control_cb);
    // duer_add_dlp_directive("dlp.system_information", duer_dlp_system_info_cb);
    duer_add_dlp_directive(DLP_SYSTEM_UPDATE, duer_dlp_system_update_cb);
    DUER_LOGI("duer_dlp_child_watch_control_init ok");
}

void duer_dlp_init()
{
    static duer_bool s_is_initialized = DUER_FALSE;
    if (s_is_initialized == DUER_TRUE) {
        return;
    }
	
    duer_dlp_module_init();

    duer_dlp_control_init();
	
    s_is_initialized = DUER_TRUE;
    DUER_LOGI("duer_dlp_init init ok");	
}