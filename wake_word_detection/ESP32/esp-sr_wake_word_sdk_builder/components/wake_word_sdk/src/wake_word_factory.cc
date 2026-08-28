/**
 * @file wake_word_factory.cc
 * @brief 语音唤醒工厂函数实现
 * 
 * 提供工厂函数，根据芯片类型和模型类型自动选择合适的唤醒词实现。
 * 用户无需关心具体使用哪个实现类。
 */

#include "wake_word_sdk/wake_word.h"
#include "esp_wake_word.h"
#include "afe_wake_word.h"
#include "custom_wake_word.h"

#include <esp_log.h>
#include <model_path.h>

#define TAG "WakeWordFactory"

namespace wake_word_sdk {

/**
 * @brief 创建唤醒词实例
 * 
 * 根据目标芯片和模型列表，自动选择合适的唤醒词实现：
 * - ESP32-S3/P4：优先使用 CustomWakeWord（Multinet），其次 AfeWakeWord（Wakenet+AFE）
 * - ESP32/C3 等：使用 EspWakeWord（Wakenet 无 AFE）
 * 
 * @param config 唤醒词配置
 * @param models_list 语音识别模型列表
 * @return 唤醒词实例指针，失败返回 nullptr
 */
std::unique_ptr<WakeWord> CreateWakeWord(
    const WakeWordConfig& config,
    srmodel_list_t* models_list
) {
    if (models_list == nullptr) {
        ESP_LOGE(TAG, "models_list 为空，无法创建唤醒词实例");
        return nullptr;
    }

    std::unique_ptr<WakeWord> wake_word;

#if CONFIG_IDF_TARGET_ESP32S3 || CONFIG_IDF_TARGET_ESP32P4
    // ESP32-S3 和 ESP32-P4 支持 Multinet 和 AFE
    if (esp_srmodel_filter(models_list, ESP_MN_PREFIX, NULL) != nullptr) {
        // 有 Multinet 模型，使用 CustomWakeWord（支持自定义唤醒词）
        ESP_LOGI(TAG, "检测到 Multinet 模型，使用 CustomWakeWord");
        wake_word = std::make_unique<CustomWakeWord>();
    } else if (esp_srmodel_filter(models_list, ESP_WN_PREFIX, NULL) != nullptr) {
        // 有 Wakenet 模型，使用 AfeWakeWord（带 AFE 的唤醒词检测）
        ESP_LOGI(TAG, "检测到 Wakenet 模型，使用 AfeWakeWord");
        wake_word = std::make_unique<AfeWakeWord>();
    } else {
        ESP_LOGW(TAG, "未检测到支持的语音模型");
        return nullptr;
    }
#else
    // ESP32/C3 等芯片使用简单的 Wakenet
    if (esp_srmodel_filter(models_list, ESP_WN_PREFIX, NULL) != nullptr) {
        ESP_LOGI(TAG, "检测到 Wakenet 模型，使用 EspWakeWord");
        wake_word = std::make_unique<EspWakeWord>();
    } else {
        ESP_LOGW(TAG, "未检测到支持的语音模型");
        return nullptr;
    }
#endif

    // 初始化唤醒词实例
    if (wake_word) {
        if (!wake_word->Initialize(config, models_list)) {
            ESP_LOGE(TAG, "唤醒词初始化失败");
            return nullptr;
        }
        ESP_LOGI(TAG, "唤醒词实例创建成功");
    }

    return wake_word;
}

}  // namespace wake_word_sdk
