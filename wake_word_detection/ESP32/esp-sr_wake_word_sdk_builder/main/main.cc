/**
 * @file main.cc
 * @brief Wake Word SDK 测试程序
 * 
 * 该文件是 wake_word_sdk 的测试入口，用于验证 SDK 编译是否正常。
 */

#include <stdio.h>
#include <esp_log.h>
#include <freertos/FreeRTOS.h>
#include <freertos/task.h>

#include "wake_word_sdk/wake_word.h"
#include "wake_word_sdk/wake_word_types.h"

#define TAG "WakeWordTest"

extern "C" void app_main(void)
{
    ESP_LOGI(TAG, "Wake Word SDK 测试程序启动");
    ESP_LOGI(TAG, "SDK 编译成功！");
    
    // 打印配置结构体大小，验证头文件正确包含
    ESP_LOGI(TAG, "WakeWordAudioConfig 大小: %d 字节", sizeof(wake_word_sdk::WakeWordAudioConfig));
    ESP_LOGI(TAG, "WakeWordConfig 大小: %d 字节", sizeof(wake_word_sdk::WakeWordConfig));
    ESP_LOGI(TAG, "OPUS_FRAME_DURATION_MS = %d", wake_word_sdk::OPUS_FRAME_DURATION_MS);
    
    // 测试完成
    ESP_LOGI(TAG, "测试完成，进入空闲循环");
    
    while (true) {
        vTaskDelay(pdMS_TO_TICKS(1000));
    }
}
