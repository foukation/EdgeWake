/**
 * @file main.cc
 * @brief AI SDK Demo 入口
 *
 * 初始化 NVS、事件循环、WiFi 连接，
 * 等待网络就绪后启动 AI SDK 演示。
 */
#include <cstring>
#include <esp_log.h>
#include <esp_err.h>
#include <nvs.h>
#include <nvs_flash.h>
#include <esp_event.h>
#include <esp_wifi.h>
#include <esp_netif.h>
#include <freertos/FreeRTOS.h>
#include <freertos/task.h>
#include <freertos/event_groups.h>
#include <esp_sntp.h>

#include "demo_config.h"

static const char* TAG = "App";

// ============================================================================
// WiFi 事件处理
// ============================================================================

/** WiFi 连接成功事件位 */
#define WIFI_CONNECTED_BIT  BIT0
/** WiFi 连接失败事件位 */
#define WIFI_FAIL_BIT       BIT1

/** WiFi 最大重试次数 */
#define WIFI_MAX_RETRY      10

static EventGroupHandle_t s_wifi_event_group = nullptr;
static int s_retry_count = 0;

/**
 * @brief WiFi 和 IP 事件处理函数
 */
static void wifi_event_handler(void* arg, esp_event_base_t event_base,
                               int32_t event_id, void* event_data)
{
    if (event_base == WIFI_EVENT && event_id == WIFI_EVENT_STA_START) {
        esp_wifi_connect();
    } else if (event_base == WIFI_EVENT && event_id == WIFI_EVENT_STA_DISCONNECTED) {
        if (s_retry_count < WIFI_MAX_RETRY) {
            esp_wifi_connect();
            s_retry_count++;
            ESP_LOGW(TAG, "WiFi disconnected, retry %d/%d ...", s_retry_count, WIFI_MAX_RETRY);
        } else {
            xEventGroupSetBits(s_wifi_event_group, WIFI_FAIL_BIT);
            ESP_LOGE(TAG, "WiFi connection failed after %d retries", WIFI_MAX_RETRY);
        }
    } else if (event_base == IP_EVENT && event_id == IP_EVENT_STA_GOT_IP) {
        ip_event_got_ip_t* event = (ip_event_got_ip_t*)event_data;
        ESP_LOGI(TAG, "WiFi connected, IP: " IPSTR, IP2STR(&event->ip_info.ip));
        s_retry_count = 0;
        xEventGroupSetBits(s_wifi_event_group, WIFI_CONNECTED_BIT);
    }
}

/**
 * @brief 初始化 WiFi Station 模式并等待连接
 *
 * SSID 和密码从 demo_config.h 中读取。
 *
 * @return ESP_OK 连接成功, ESP_FAIL 连接失败
 */
static esp_err_t wifi_init_sta(void)
{
    s_wifi_event_group = xEventGroupCreate();

    /* 初始化 TCP/IP 协议栈 */
    ESP_ERROR_CHECK(esp_netif_init());
    esp_netif_create_default_wifi_sta();

    /* WiFi 驱动初始化 */
    wifi_init_config_t cfg = WIFI_INIT_CONFIG_DEFAULT();
    ESP_ERROR_CHECK(esp_wifi_init(&cfg));

    /* 注册事件处理 */
    esp_event_handler_instance_t instance_any_id;
    esp_event_handler_instance_t instance_got_ip;
    ESP_ERROR_CHECK(esp_event_handler_instance_register(
        WIFI_EVENT, ESP_EVENT_ANY_ID, &wifi_event_handler, nullptr, &instance_any_id));
    ESP_ERROR_CHECK(esp_event_handler_instance_register(
        IP_EVENT, IP_EVENT_STA_GOT_IP, &wifi_event_handler, nullptr, &instance_got_ip));

    /* 配置 WiFi 参数 */
    wifi_config_t wifi_config = {};
    strlcpy((char*)wifi_config.sta.ssid, DEMO_WIFI_SSID, sizeof(wifi_config.sta.ssid));
    strlcpy((char*)wifi_config.sta.password, DEMO_WIFI_PASSWORD, sizeof(wifi_config.sta.password));
    wifi_config.sta.threshold.authmode = WIFI_AUTH_WPA2_PSK;

    ESP_ERROR_CHECK(esp_wifi_set_mode(WIFI_MODE_STA));
    ESP_ERROR_CHECK(esp_wifi_set_config(WIFI_IF_STA, &wifi_config));
    ESP_ERROR_CHECK(esp_wifi_start());

    ESP_LOGI(TAG, "WiFi STA init done, connecting to \"%s\" ...", DEMO_WIFI_SSID);

    /* 等待连接结果 */
    EventBits_t bits = xEventGroupWaitBits(s_wifi_event_group,
        WIFI_CONNECTED_BIT | WIFI_FAIL_BIT,
        pdFALSE, pdFALSE, portMAX_DELAY);

    /* 注销事件处理（连接阶段已结束） */
    // 首次连网后保持 Wi-Fi/IP 事件回调常驻。
    // 这样链路波动时仍可持续处理断线与重连。
    (void)instance_got_ip;
    (void)instance_any_id;

    if (bits & WIFI_CONNECTED_BIT) {
        // 将 Wi-Fi 固定为性能模式，提升实时 ASR 上行稳定性。
        // 避免省电模式引入 ACK 延迟抖动，从而触发 WebSocket 写超时
        // （例如 transport_poll_write）。
        ESP_ERROR_CHECK(esp_wifi_set_ps(WIFI_PS_NONE));
        ESP_LOGI(TAG, "WiFi power save disabled (WIFI_PS_NONE)");
        return ESP_OK;
    }
    return ESP_FAIL;
}

// ============================================================================
// 应用入口
// ============================================================================

extern "C" void test_ai_sdk_functions(void);

// 首次 asr.start() 失败后无法恢复 复现实验（定义在 repro_persistent_asr_start_failure.cpp）
extern "C" void start_persistent_asr_start_failure_repro_test(void);

extern "C" void app_main(void)
{
    /* 初始化 NVS：WiFi 驱动依赖 NVS 存储 */
    esp_err_t ret = nvs_flash_init();
    if (ret == ESP_ERR_NVS_NO_FREE_PAGES || ret == ESP_ERR_NVS_NEW_VERSION_FOUND) {
        ESP_ERROR_CHECK(nvs_flash_erase());
        ret = nvs_flash_init();
    }
    ESP_ERROR_CHECK(ret);

    /* 创建默认事件循环 */
    ESP_ERROR_CHECK(esp_event_loop_create_default());

    ESP_LOGI(TAG, "========================================");
    ESP_LOGI(TAG, "  AI SDK Demo v1.0.0");
    ESP_LOGI(TAG, "========================================");

    /* WiFi 连接 */
    ret = wifi_init_sta();
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "WiFi connection failed, cannot proceed");
        return;
    }

    /* WiFi 已连接，初始化 SNTP 时间同步 */
    /* ASR WebSocket URL 签名依赖 AssistUtils::timestamp()，需要准确的系统时间 */
    esp_sntp_setoperatingmode(SNTP_OPMODE_POLL);
    esp_sntp_setservername(0, "ntp.aliyun.com");
    esp_sntp_setservername(1, "pool.ntp.org");
    esp_sntp_init();
    ESP_LOGI(TAG, "SNTP time sync initialized");

    /* 启动 AI SDK 演示 */
    test_ai_sdk_functions();

    /* 启动 "首次 asr.start() 失败后无法恢复" 复现实验 */
    /* 注意：本实验要求 SDK 未被 initialize，会自行完成 initialize/obtainDeviceInfo */
    /* 如需恢复正常 demo，注释下面这行并取消上面 test_ai_sdk_functions() 注释 */
    // start_persistent_asr_start_failure_repro_test();

    /* 主任务保持运行 */
    while (true) {
        vTaskDelay(pdMS_TO_TICKS(10000));
    }
}
