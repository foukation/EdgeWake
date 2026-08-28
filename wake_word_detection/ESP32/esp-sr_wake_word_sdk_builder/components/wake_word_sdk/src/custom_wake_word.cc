/**
 * @file custom_wake_word.cc
 * @brief ESP32-S3/P4 自定义唤醒词实现
 * 
 * 使用 Multinet 进行自定义唤醒词/命令词检测。
 */

#include "custom_wake_word.h"
#include "wake_word_sdk/wake_word_types.h"

#include <esp_log.h>
#include <esp_timer.h>
#include <esp_mn_iface.h>
#include <esp_mn_models.h>
#include <esp_mn_speech_commands.h>
#include <opus_encoder.h>
#include <cJSON.h>

#define TAG "CustomWakeWord"

namespace wake_word_sdk {

CustomWakeWord::CustomWakeWord()
    : wake_word_pcm_(), wake_word_opus_() {
}

CustomWakeWord::~CustomWakeWord() {
    // 清理 Multinet 资源
    if (multinet_model_data_ != nullptr && multinet_ != nullptr) {
        multinet_->destroy(multinet_model_data_);
        multinet_model_data_ = nullptr;
    }

    // 清理编码任务资源
    if (wake_word_encode_task_stack_ != nullptr) {
        heap_caps_free(wake_word_encode_task_stack_);
    }

    if (wake_word_encode_task_buffer_ != nullptr) {
        heap_caps_free(wake_word_encode_task_buffer_);
    }

    // 清理模型资源
    if (models_ != nullptr) {
        esp_srmodel_deinit(models_);
    }
}

void CustomWakeWord::ParseWakenetModelConfig() {
    // 检查是否提供了资源读取回调
    if (!config_.read_asset) {
        ESP_LOGW(TAG, "未提供资源读取回调，跳过 index.json 解析");
        return;
    }

    // 读取 index.json 配置文件
    void* ptr = nullptr;
    size_t size = 0;
    if (!config_.read_asset("index.json", &ptr, &size)) {
        ESP_LOGE(TAG, "读取 index.json 失败");
        return;
    }

    // 解析 JSON
    cJSON* root = cJSON_ParseWithLength(static_cast<char*>(ptr), size);
    if (root == nullptr) {
        ESP_LOGE(TAG, "解析 index.json 失败");
        return;
    }

    // 获取 multinet_model 配置
    cJSON* multinet_model = cJSON_GetObjectItem(root, "multinet_model");
    if (cJSON_IsObject(multinet_model)) {
        // 读取语言配置
        cJSON* language = cJSON_GetObjectItem(multinet_model, "language");
        if (cJSON_IsString(language)) {
            language_ = language->valuestring;
            ESP_LOGI(TAG, "语言: %s", language_.c_str());
        }

        // 读取超时时间配置
        cJSON* duration = cJSON_GetObjectItem(multinet_model, "duration");
        if (cJSON_IsNumber(duration)) {
            duration_ = duration->valueint;
            ESP_LOGI(TAG, "超时时间: %d ms", duration_);
        }

        // 读取阈值配置
        cJSON* threshold = cJSON_GetObjectItem(multinet_model, "threshold");
        if (cJSON_IsNumber(threshold)) {
            threshold_ = threshold->valuedouble;
            ESP_LOGI(TAG, "检测阈值: %.2f", threshold_);
        }

        // 读取命令词列表
        cJSON* commands = cJSON_GetObjectItem(multinet_model, "commands");
        if (cJSON_IsArray(commands)) {
            for (int i = 0; i < cJSON_GetArraySize(commands); i++) {
                cJSON* command = cJSON_GetArrayItem(commands, i);
                if (cJSON_IsObject(command)) {
                    cJSON* command_name = cJSON_GetObjectItem(command, "command");
                    cJSON* text = cJSON_GetObjectItem(command, "text");
                    cJSON* action = cJSON_GetObjectItem(command, "action");
                    if (cJSON_IsString(command_name) && cJSON_IsString(text) && cJSON_IsString(action)) {
                        commands_.push_back({command_name->valuestring, text->valuestring, action->valuestring});
                        ESP_LOGI(TAG, "命令词: %s, 文本: %s, 动作: %s", 
                            command_name->valuestring, text->valuestring, action->valuestring);
                    }
                }
            }
        }
    }
    cJSON_Delete(root);
}

bool CustomWakeWord::Initialize(const WakeWordConfig& config, srmodel_list_t* models_list) {
    // 保存配置
    config_ = config;
    commands_.clear();

    // 初始化模型列表
    if (models_list == nullptr) {
        language_ = "cn";
        models_ = esp_srmodel_init("model");
#ifdef CONFIG_CUSTOM_WAKE_WORD
        // 从 Kconfig 读取自定义唤醒词配置
        threshold_ = CONFIG_CUSTOM_WAKE_WORD_THRESHOLD / 100.0f;
        commands_.push_back({CONFIG_CUSTOM_WAKE_WORD, CONFIG_CUSTOM_WAKE_WORD_DISPLAY, "wake"});
#endif
    } else {
        models_ = models_list;
        // 从 index.json 解析配置
        ParseWakenetModelConfig();
    }

    // 检查模型是否有效
    if (models_ == nullptr || models_->num == -1) {
        ESP_LOGE(TAG, "初始化 Wakenet 模型失败");
        return false;
    }

    // 初始化 Multinet（命令词识别）
    mn_name_ = esp_srmodel_filter(models_, ESP_MN_PREFIX, language_.c_str());
    if (mn_name_ == nullptr) {
        ESP_LOGW(TAG, "语言 '%s' 的 Multinet 模型未找到，尝试使用任意 Multinet 模型", language_.c_str());
        mn_name_ = esp_srmodel_filter(models_, ESP_MN_PREFIX, NULL);
    }
    if (mn_name_ == nullptr) {
        ESP_LOGE(TAG, "初始化 Multinet 失败，mn_name 为空");
        ESP_LOGI(TAG, "请参考文档添加自定义唤醒词");
        return false;
    }

    // 创建 Multinet 实例
    multinet_ = esp_mn_handle_from_name(mn_name_);
    multinet_model_data_ = multinet_->create(mn_name_, duration_);
    multinet_->set_det_threshold(multinet_model_data_, threshold_);

    // 设置命令词
    esp_mn_commands_clear();
    for (int i = 0; i < commands_.size(); i++) {
        esp_mn_commands_add(i + 1, commands_[i].command.c_str());
    }
    esp_mn_commands_update();
    
    // 打印当前激活的命令词
    multinet_->print_active_speech_commands(multinet_model_data_);

    ESP_LOGI(TAG, "自定义唤醒词检测器初始化成功");
    return true;
}

void CustomWakeWord::OnWakeWordDetected(std::function<void(const std::string& wake_word)> callback) {
    wake_word_detected_callback_ = callback;
}

void CustomWakeWord::Start() {
    running_ = true;
    ESP_LOGI(TAG, "唤醒词检测已启动");
}

void CustomWakeWord::Stop() {
    running_ = false;
    ESP_LOGI(TAG, "唤醒词检测已停止");
}

void CustomWakeWord::Feed(const std::vector<int16_t>& data) {
    if (multinet_model_data_ == nullptr || !running_) {
        return;
    }

    esp_mn_state_t mn_state;
    
    // 如果输入是双声道，需要提取左声道数据
    if (config_.audio.input_channels == 2) {
        auto mono_data = std::vector<int16_t>(data.size() / 2);
        for (size_t i = 0, j = 0; i < mono_data.size(); ++i, j += 2) {
            mono_data[i] = data[j];
        }

        StoreWakeWordData(mono_data);
        mn_state = multinet_->detect(multinet_model_data_, const_cast<int16_t*>(mono_data.data()));
    } else {
        StoreWakeWordData(data);
        mn_state = multinet_->detect(multinet_model_data_, const_cast<int16_t*>(data.data()));
    }
    
    // 处理检测结果
    if (mn_state == ESP_MN_STATE_DETECTING) {
        // 正在检测中，继续
        return;
    } else if (mn_state == ESP_MN_STATE_DETECTED) {
        // 检测到命令词
        esp_mn_results_t *mn_result = multinet_->get_results(multinet_model_data_);
        for (int i = 0; i < mn_result->num && running_; i++) {
            ESP_LOGI(TAG, "检测到自定义唤醒词: command_id=%d, string=%s, prob=%f", 
                    mn_result->command_id[i], mn_result->string, mn_result->prob[i]);
            auto& command = commands_[mn_result->command_id[i] - 1];
            if (command.action == "wake") {
                last_detected_wake_word_ = command.text;
                running_ = false;
                
                // 调用回调函数
                if (wake_word_detected_callback_) {
                    wake_word_detected_callback_(last_detected_wake_word_);
                }
            }
        }
        multinet_->clean(multinet_model_data_);
    } else if (mn_state == ESP_MN_STATE_TIMEOUT) {
        // 检测超时，清理状态
        ESP_LOGD(TAG, "命令词检测超时，清理状态");
        multinet_->clean(multinet_model_data_);
    }
}

size_t CustomWakeWord::GetFeedSize() {
    if (multinet_model_data_ == nullptr) {
        return 0;
    }
    return multinet_->get_samp_chunksize(multinet_model_data_);
}

void CustomWakeWord::StoreWakeWordData(const std::vector<int16_t>& data) {
    // 存储音频数据到 PCM 队列
    wake_word_pcm_.push_back(data);
    // 保留约 2 秒的数据（采样率 16000，每块 512 样本，约 30ms）
    while (wake_word_pcm_.size() > 2000 / 30) {
        wake_word_pcm_.pop_front();
    }
}

void CustomWakeWord::EncodeWakeWordData() {
    const size_t stack_size = 4096 * 7;
    wake_word_opus_.clear();
    
    // 分配任务栈（使用 PSRAM）
    if (wake_word_encode_task_stack_ == nullptr) {
        wake_word_encode_task_stack_ = (StackType_t*)heap_caps_malloc(stack_size, MALLOC_CAP_SPIRAM);
        assert(wake_word_encode_task_stack_ != nullptr);
    }
    // 分配任务控制块（使用内部 RAM）
    if (wake_word_encode_task_buffer_ == nullptr) {
        wake_word_encode_task_buffer_ = (StaticTask_t*)heap_caps_malloc(sizeof(StaticTask_t), MALLOC_CAP_INTERNAL);
        assert(wake_word_encode_task_buffer_ != nullptr);
    }

    // 创建编码任务
    wake_word_encode_task_ = xTaskCreateStatic([](void* arg) {
        auto this_ = (CustomWakeWord*)arg;
        {
            auto start_time = esp_timer_get_time();
            // 创建 Opus 编码器（16kHz，单声道，20ms 帧）
            auto encoder = std::make_unique<OpusEncoderWrapper>(16000, 1, OPUS_FRAME_DURATION_MS);
            encoder->SetComplexity(0);  // 0 是最快的编码速度

            int packets = 0;
            // 编码所有 PCM 数据
            for (auto& pcm: this_->wake_word_pcm_) {
                encoder->Encode(std::move(pcm), [this_](std::vector<uint8_t>&& opus) {
                    std::lock_guard<std::mutex> lock(this_->wake_word_mutex_);
                    this_->wake_word_opus_.emplace_back(std::move(opus));
                    this_->wake_word_cv_.notify_all();
                });
                packets++;
            }
            this_->wake_word_pcm_.clear();

            auto end_time = esp_timer_get_time();
            ESP_LOGI(TAG, "编码唤醒词 Opus %d 包，耗时 %ld ms", packets, (long)((end_time - start_time) / 1000));

            // 添加空包表示编码结束
            std::lock_guard<std::mutex> lock(this_->wake_word_mutex_);
            this_->wake_word_opus_.push_back(std::vector<uint8_t>());
            this_->wake_word_cv_.notify_all();
        }
        vTaskDelete(NULL);
    }, "encode_wake_word", stack_size, this, 2, wake_word_encode_task_stack_, wake_word_encode_task_buffer_);
}

bool CustomWakeWord::GetWakeWordOpus(std::vector<uint8_t>& opus) {
    std::unique_lock<std::mutex> lock(wake_word_mutex_);
    // 等待 Opus 数据可用
    wake_word_cv_.wait(lock, [this]() {
        return !wake_word_opus_.empty();
    });
    // 取出数据
    opus.swap(wake_word_opus_.front());
    wake_word_opus_.pop_front();
    // 空包表示结束
    return !opus.empty();
}

}  // namespace wake_word_sdk
