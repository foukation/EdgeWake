#include <stdio.h>
#include "esp_log.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "main_functions.h"

static const char* TAG = "app_main";

void tf_main(void) {
    ESP_LOGI(TAG, "tf_main task started");
    setup();

    for (int i = 0; i < 4; i++) {
        ProcessFileOffline(i);
    }

    vTaskDelete(NULL);
}
  
extern "C" void app_main() {
    xTaskCreate((TaskFunction_t)&tf_main, "tensorflow", 48 * 1024, NULL, 8, NULL);
    vTaskDelete(NULL);
}
  
