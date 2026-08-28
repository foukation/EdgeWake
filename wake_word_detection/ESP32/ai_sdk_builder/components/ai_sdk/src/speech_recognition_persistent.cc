/**
 * @file speech_recognition_persistent.cc
 * @brief 持续语音识别实现
 *
 * 实现要点：
 * - WebSocket 传输层复用 AsrWebsocket（协议无关的纯传输封装）
 * - URL 签名复用 AssistUtils::wssParameter()
 * - 端点：ApiConfig::AUTOMATIC_SPEECH_RECOGNITION_PERSISTENT_API ("/app-ws/v1/long-asr")
 * - 连接同步：FreeRTOS 二值信号量（同 AsrIntelligentDialogue 模式）
 * - 消息处理：所有文本帧全量反序列化后透传（不过滤 type 字段）
 * - stop()：发送 finish 帧，等待服务端关闭
 * - cancel()：发送 CANCEL 帧，主动断开
 */

// 公开头文件（位于 include/ai_sdk/）
#include "ai_sdk/speech_recognition_persistent.h"
#include "ai_sdk/ai_assistant_manager.h"

// 内部头文件（位于 src/include/）
#include "asr_websocket.h"
#include "api_config.h"
#include "assist_utils.h"
#include "cjson_guard.h"
#include "esp_log.h"
#include "cJSON.h"

#include <mutex>

// 音频模块内部头文件（位于 src/audio/，通过 PRIV_INCLUDE_DIRS 访问）
#include "audio_input.h"

// FreeRTOS 头文件
#include "freertos/FreeRTOS.h"
#include "freertos/semphr.h"

namespace ai_sdk {

// 日志标签
static const char* TAG = "SpeechRecognitionPersistent";

/**
 * @class SpeechRecognitionPersistent::Impl
 * @brief 私有实现类（PIMPL 模式）
 *
 * 隐藏实现细节，减少头文件依赖。
 * 结构与 AsrIntelligentDialogue::Impl 保持一致（双 mutex + 信号量模式）。
 */
class SpeechRecognitionPersistent::Impl {
private:
    // 连接等待超时：30 秒（与 AsrIntelligentDialogue 保持一致）
    static constexpr int CONNECTION_TIMEOUT_MS = 30000;

public:
    // =========================================================================
    // 构造 / 析构
    // =========================================================================

    Impl() {
        state_mutex_ = std::make_unique<std::mutex>();
        callback_mutex_ = std::make_unique<std::mutex>();
        connection_semaphore_ = xSemaphoreCreateBinary();
        is_recognizing_ = false;
        is_connected_ = false;
    }

    ~Impl() {
        if (is_connected_) {
            // 强制断开，释放资源
            websocket_.disconnect();
        }
        if (audio_input_) {
            audio_input_->Stop();
            audio_input_.reset();
        }
        if (connection_semaphore_ != nullptr) {
            vSemaphoreDelete(connection_semaphore_);
            connection_semaphore_ = nullptr;
        }
    }

    // =========================================================================
    // 回调设置
    // =========================================================================

    void setCallbacks(ResultCallback on_result,
                      ErrorCallback  on_error,
                      CloseCallback  on_close) {
        std::lock_guard<std::mutex> lock(*callback_mutex_);
        result_callback_ = std::move(on_result);
        error_callback_ = std::move(on_error);
        close_callback_ = std::move(on_close);
    }

    // =========================================================================
    // 启动
    // =========================================================================

    /**
     * @brief 启动持续识别
     *
     * 流程：
     * 1. 检查状态防止重复启动
     * 2. 构建 URL（base + endpoint + 签名参数）
     * 3. 注册 WebSocket 事件回调和消息回调
     * 4. 发起连接（异步），信号量同步等待
     * 5. 发送 START 帧
     */
    bool start(int dev_pid) {
        // 第 1 步：防止重复启动
        {
            std::lock_guard<std::mutex> lock(*state_mutex_);
            if (is_recognizing_) {
                ESP_LOGW(TAG, "Already recognizing, cannot start again");
                return false;
            }
        }

        // 第 2 步：构建 URL
        std::string base_url = std::string(ApiConfig::WSS_WEBSOCKET_ASR_BASE_URL_TEST)
                             + ApiConfig::AUTOMATIC_SPEECH_RECOGNITION_PERSISTENT_API;
        std::string ws_url = AssistUtils::wssParameter(base_url);
        if (ws_url.empty()) {
            if (error_callback_) {
                error_callback_(-1, "Failed to build WebSocket URL");
                ESP_LOGE(TAG, "错误: code=-1, message=Failed to build WebSocket URL");
            }
            return false;
        }
        ESP_LOGD(TAG, "WS_URL: %s", ws_url.c_str());

        // 第 3 步：WebSocket 配置
        AsrWebsocketConfig config;
        config.url                = ws_url;
        config.connect_timeout_ms = 10000;
        config.network_timeout_ms = 30000;
        config.ping_interval_ms   = 30000;
        config.buffer_size        = 8192;

        // 清空信号量，防止上次残留信号
        if (connection_semaphore_ != nullptr) {
            xSemaphoreTake(connection_semaphore_, 0);
        }

        // 第 4 步：注册 WebSocket 事件回调
        websocket_.setEventCallback([this](int event_id, const std::string& message) {
            switch (event_id) {
                case WEBSOCKET_EVENT_CONNECTED:
                    {
                        std::lock_guard<std::mutex> lock(*state_mutex_);
                        is_connected_ = true;
                    }
                    if (connection_semaphore_ != nullptr) {
                        xSemaphoreGive(connection_semaphore_);
                    }
                    break;

                case WEBSOCKET_EVENT_CLOSED:
                    {
                        std::lock_guard<std::mutex> lock(*state_mutex_);
                        is_recognizing_ = false;
                        is_connected_ = false;
                    }
                    if (connection_semaphore_ != nullptr) {
                        xSemaphoreGive(connection_semaphore_);
                    }
                    ESP_LOGI(TAG, "连接关闭（正常）");
                    {
                        std::lock_guard<std::mutex> lock(*callback_mutex_);
                        if (close_callback_) close_callback_();
                    }
                    break;

                case WEBSOCKET_EVENT_DISCONNECTED:
                    // 以下两条 DBG 日志是 AB-BA 死锁修复验证留下的脚手架，
                    // 默认不输出，需调试时可在 menuconfig 中将本模块日志级别设为 DEBUG
                    ESP_LOGD(TAG, "DBG-DC: enter, before state_mutex_ lock");
                    {
                        std::lock_guard<std::mutex> lock(*state_mutex_);
                        is_recognizing_ = false;
                        is_connected_ = false;
                    }
                    ESP_LOGD(TAG, "DBG-DC: state_mutex_ released, before give sem");
                    if (connection_semaphore_ != nullptr) {
                        xSemaphoreGive(connection_semaphore_);
                    }
                    ESP_LOGW(TAG, "连接断开（异常）");
                    {
                        std::lock_guard<std::mutex> lock(*callback_mutex_);
                        if (close_callback_) close_callback_();
                    }
                    break;

                case WEBSOCKET_EVENT_ERROR:
                    {
                        std::lock_guard<std::mutex> lock(*state_mutex_);
                        is_recognizing_ = false;
                        is_connected_ = false;
                    }
                    if (connection_semaphore_ != nullptr) {
                        xSemaphoreGive(connection_semaphore_);
                    }
                    {
                        std::lock_guard<std::mutex> lock(*callback_mutex_);
                        if (error_callback_) {
                            error_callback_(1, "WebSocket error: " + message);
                        }
                    }
                    ESP_LOGE(TAG, "错误: code=1, message=WebSocket error: %s", message.c_str());
                    break;

                default:
                    break;
            }
        });

        // 第 5 步：注册消息回调（只处理文本帧，op_code == 1）
        websocket_.setMessageCallback([this](const uint8_t* data, size_t len, int op_code) {
            if (op_code == 1) {
                parseMessage(data, len);
            }
        });

        // 第 6 步：发起连接（异步）
        if (!websocket_.connect(config)) {
            if (error_callback_) {
                error_callback_(1, "Failed to initiate WebSocket connection");
                ESP_LOGE(TAG, "错误: code=1, message=Failed to initiate WebSocket connection");
            }
            return false;
        }

        // 第 7 步：等待连接建立（最长 30 秒）
        if (connection_semaphore_ != nullptr) {
            if (xSemaphoreTake(connection_semaphore_,
                               pdMS_TO_TICKS(CONNECTION_TIMEOUT_MS)) != pdTRUE) {
                websocket_.disconnect();
                if (error_callback_) {
                    error_callback_(1, "Connection timeout");
                    ESP_LOGE(TAG, "错误: code=1, message=Connection timeout");
                }
                return false;
            }
        }

        // 第 8 步：检查连接是否真正成功
        //
        // 修复 AB-BA 死锁（2026-05-20）：
        //   旧实现把 websocket_.disconnect() 包在 state_mutex_ 持锁范围内，
        //   而 disconnect() 内部会调用 esp_websocket_client_destroy()，
        //   destroy() 会等待 WebSocket 客户端任务退出。
        //
        //   与此同时，该客户端任务在收到 DISCONNECTED 事件后会进入
        //   本文件 setEventCallback 注册的 lambda，需要重新获取 state_mutex_。
        //   由此形成 AB-BA 互锁：
        //     本任务：持有 state_mutex_，等 WS task 退出
        //     WS task： 等 state_mutex_ 才能退出
        //
        //   解决办法：把“判断连接结果”与“调用 disconnect()”分成两段，
        //   在持锁段内只读/写共享状态，离开锁之后再做可能阻塞的 IO 操作。
        // 以下 DBG-STEP8 系列日志是 AB-BA 死锁修复验证留下的脚手架，
        // 默认不输出，需调试时可在 menuconfig 中将本模块日志级别设为 DEBUG
        ESP_LOGD(TAG, "DBG-STEP8: before lock state_mutex_, is_connected_=%d", (int)is_connected_);

        bool need_disconnect = false;
        {
            std::lock_guard<std::mutex> lock(*state_mutex_);
            if (!is_connected_) {
                // 连接未真正建立（典型场景：WS 握手返回非 101 状态码，
                // 触发 WEBSOCKET_EVENT_ERROR / DISCONNECTED）。
                // 这里只记录意图，等出了锁再实际断开。
                need_disconnect = true;
            } else {
                // 连接成功，原子地切换到识别中状态
                is_recognizing_ = true;
            }
        }   // ← state_mutex_ 在此释放，确保接下来的 disconnect() 不会与
            //   WS task 的事件回调争抢同一把锁。

        if (need_disconnect) {
            ESP_LOGD(TAG, "DBG-STEP8: before disconnect()");
            websocket_.disconnect();          // 不再持锁，可安全等待 WS task 退出
            ESP_LOGD(TAG, "DBG-STEP8: after disconnect()");

            // 调用用户的 error_callback_ 时用 callback_mutex_ 保护，
            // 与本文件其他回调触发点（ERROR/DISCONNECTED 分支）保持一致：
            //   - 防止与 setCallbacks() 并发热替换回调时出现野指针
            //   - 持锁顺序固定为 state_mutex_ → 释放 → callback_mutex_，
            //     与事件回调中的顺序一致，不会形成新的反向死锁
            {
                std::lock_guard<std::mutex> lock(*callback_mutex_);
                if (error_callback_) {
                    error_callback_(1, "Connection failed");
                    ESP_LOGE(TAG, "错误: code=1, message=Connection failed");
                }
            }
            return false;
        }

        // 第 9 步：发送 START 帧
        sendStartFrame(dev_pid);
        ESP_LOGI(TAG, "Persistent ASR started, dev_pid=%d", dev_pid);
        return true;
    }

    // =========================================================================
    // 停止 / 取消
    // =========================================================================

    /**
     * @brief 优雅停止
     *
     * 发送 finish 帧后等待服务端关闭连接。
     * 服务端会返回最后一段识别结果，然后主动关闭 WebSocket。
     * 触发 close_callback_。
     */
    void stop() {
        {
            std::lock_guard<std::mutex> lock(*state_mutex_);
            if (!is_connected_) {
                ESP_LOGW(TAG, "Not connected, nothing to stop");
                return;
            }
            // 停止接受音频，但不立即断开（等服务端关闭）
            is_recognizing_ = false;
        }

        // 暂停麦克风录音（如果已初始化）
        if (audio_input_) {
            audio_input_->SetRecording(false);
        }

        // 发送 finish 帧，服务端返回最后结果后关闭连接
        sendFinishFrame();
    }

    /**
     * @brief 立即取消
     *
     * 发送 CANCEL 帧后主动断开连接，不等待服务端。
     * 触发 close_callback_。
     */
    void cancel() {
        {
            std::lock_guard<std::mutex> lock(*state_mutex_);
            if (!is_connected_) {
                ESP_LOGW(TAG, "Not connected, nothing to cancel");
                return;
            }
        }

        // 暂停麦克风录音（如果已初始化）
        if (audio_input_) {
            audio_input_->SetRecording(false);
        }

        // 发送 CANCEL 帧
        sendCancelFrame();

        // 主动断开（不等待服务端）
        websocket_.disconnect();

        {
            std::lock_guard<std::mutex> lock(*state_mutex_);
            is_recognizing_ = false;
            is_connected_ = false;
        }
    }

    // =========================================================================
    // 音频发送
    // =========================================================================

    /**
     * @brief 发送音频数据
     *
     * 检查 is_recognizing_ 和 is_connected_ 双重条件。
     * 不满足时自动暂停 AudioInput 录音，避免无效采集。
     */
    void sendAudio(const uint8_t* data, size_t len) {
        {
            std::lock_guard<std::mutex> lock(*state_mutex_);
            if (!is_recognizing_ || !is_connected_) {
                // 会话已结束或连接已断开，自动停止 AudioInput 录音。
                // 避免 AudioInput 持续采集无用音频并反复触发本回调。
                if (audio_input_) {
                    audio_input_->SetRecording(false);
                }
                ESP_LOGW(TAG, "Not ready, stopped recording and dropping audio data");
                return;
            }
        }
        websocket_.sendBinary(data, len);
    }

    // =========================================================================
    // 状态查询
    // =========================================================================

    bool isConnected() const {
        std::lock_guard<std::mutex> lock(*state_mutex_);
        return is_connected_;
    }

    bool isRecognizing() const {
        std::lock_guard<std::mutex> lock(*state_mutex_);
        return is_recognizing_;
    }

    // =========================================================================
    // 内置音频驱动（可选）
    // =========================================================================

    /**
     * @brief 初始化音频模块
     *
     * 只创建 AudioInput（不创建 TtsPlayer，持续识别无 TTS 输出）。
     * AudioInput 输出固定为 PCM 16-bit/16kHz/Mono，5120 字节/次。
     */
    bool initAudio(AudioCodec* codec) {
        if (!codec) {
            ESP_LOGE(TAG, "initAudio failed: codec is null");
            return false;
        }

        audio_input_ = std::make_unique<AudioInput>();
        if (!audio_input_->Initialize(codec)) {
            ESP_LOGE(TAG, "initAudio failed: AudioInput::Initialize() failed");
            audio_input_.reset();
            return false;
        }

        // 录音数据回调 → sendAudio()
        audio_input_->SetAudioDataCallback([this](const uint8_t* data, size_t len) {
            sendAudio(data, len);
        });

        if (!audio_input_->Start()) {
            ESP_LOGE(TAG, "initAudio failed: AudioInput::Start() failed");
            audio_input_.reset();
            return false;
        }

        ESP_LOGI(TAG, "AudioInput initialized");
        return true;
    }

    void setRecording(bool enable) {
        if (!audio_input_) {
            ESP_LOGW(TAG, "setRecording: audio not initialized, call initAudio() first");
            return;
        }
        audio_input_->SetRecording(enable);
    }

private:
    // =========================================================================
    // 协议帧实现
    // =========================================================================

    /**
     * @brief 发送 START 帧
     *
     * 协议：
     * {"type":"START","data":{"dev_pid":15372,"format":"pcm","sample":16000}}
     *
     * 注意：type 值为大写 "START"，与 AsrIntelligentDialogue 的小写 "start" 不同。
     */
    void sendStartFrame(int dev_pid) {
        cJSON* root = cJSON_CreateObject();
        cJSONGuard root_guard(root);
        if (!root) {
            ESP_LOGE(TAG, "Failed to create JSON root object");
            return;
        }

        cJSON_AddStringToObject(root, "type", "START");

        cJSON* data = cJSON_CreateObject();
        if (!data) {
            ESP_LOGE(TAG, "Failed to create JSON data object");
            return;
        }
        cJSON_AddNumberToObject(data, "dev_pid", dev_pid);
        cJSON_AddStringToObject(data, "format",  "pcm");
        cJSON_AddNumberToObject(data, "sample",  16000);
        cJSON_AddItemToObject(root, "data", data);

        char* json_str = cJSON_PrintUnformatted(root);
        if (json_str) {
            ESP_LOGD(TAG, "sendStartFrame: %s", json_str);
            if (!websocket_.sendText(json_str)) {
                ESP_LOGW(TAG, "Failed to send START frame");
            }
            free(json_str);
        } else {
            ESP_LOGE(TAG, "Failed to serialize START frame JSON");
        }
    }

    /**
     * @brief 发送 FINISH 帧（优雅结束）
     *
     * 协议：{"type":"finish"}
     * 服务端返回最后一段结果后主动关闭连接。
     */
    void sendFinishFrame() {
        if (websocket_.sendText(R"({"type":"finish"})")) {
            ESP_LOGI(TAG, "sendFinishFrame");
        } else {
            ESP_LOGW(TAG, "Failed to send FINISH frame");
        }
    }

    /**
     * @brief 发送 CANCEL 帧（立即取消）
     *
     * 协议：{"type":"CANCEL"}
     * 服务端迅速关闭连接，不再返回识别结果。
     */
    void sendCancelFrame() {
        if (websocket_.sendText(R"({"type":"CANCEL"})")) {
            ESP_LOGI(TAG, "sendCancelFrame");
        } else {
            ESP_LOGW(TAG, "Failed to send CANCEL frame");
        }
    }

    // =========================================================================
    // 消息解析
    // =========================================================================

    /**
     * @brief 解析服务端文本消息
     *
     * 所有消息全量反序列化后透传给 result_callback_，不过滤 type 字段。
     * 这与服务端实际行为一致：客户端不预判消息类型，由调用方根据
     * err_no 和 type 字段决定如何处理。
     *
     * 已知 type 值：
     * - "FIN_TEXT"：一段完整语音的最终识别结果
     */
    void parseMessage(const uint8_t* payload, size_t len) {
        std::string json_str(reinterpret_cast<const char*>(payload), len);
        ESP_LOGD(TAG, "[onMessage] %s", json_str.c_str());

        cJSON* root = cJSON_Parse(json_str.c_str());
        if (!root) {
            ESP_LOGE(TAG, "[onMessage] JSON parse failed");
            std::lock_guard<std::mutex> lock(*callback_mutex_);
            if (error_callback_) {
                error_callback_(-1, "JSON parse failed");
            }
            return;
        }
        cJSONGuard root_guard(root);

        // 辅助 lambda：安全取值，字段缺失时返回默认值
        auto get_str = [&](const char* key) -> std::string {
            cJSON* item = cJSON_GetObjectItem(root, key);
            return (item && cJSON_IsString(item)) ? item->valuestring : "";
        };
        auto get_int = [&](const char* key) -> int {
            cJSON* item = cJSON_GetObjectItem(root, key);
            return (item && cJSON_IsNumber(item)) ? item->valueint : 0;
        };
        auto get_long = [&](const char* key) -> long {
            cJSON* item = cJSON_GetObjectItem(root, key);
            return (item && cJSON_IsNumber(item)) ? (long)item->valuedouble : 0L;
        };

        // 反序列化所有字段
        SpeechRecognitionPersistentResult r;
        r.err_no       = get_int("err_no");
        r.err_msg      = get_str("err_msg");
        r.log_id       = get_long("log_id");
        r.sn           = get_str("sn");
        r.type         = get_str("type");
        r.result       = get_str("result");
        r.start_time   = get_long("start_time");
        r.end_time     = get_long("end_time");
        r.product_id   = get_int("product_id");
        r.product_line = get_str("product_line");

        ESP_LOGD(TAG, "[%s] err_no=%d, result=%s, sn=%s",
                 r.type.c_str(), r.err_no, r.result.c_str(), r.sn.c_str());

        // 全部透传给回调，不过滤 type
        // 注意：不改变 is_recognizing_ 状态，连接保持，继续等待下一段语音
        std::lock_guard<std::mutex> lock(*callback_mutex_);
        if (result_callback_) {
            result_callback_(r);
        }
    }

    // =========================================================================
    // 成员变量
    // =========================================================================

    AsrWebsocket websocket_;                         ///< WebSocket 传输层（复用）

    ResultCallback result_callback_;                 ///< 识别结果回调
    ErrorCallback  error_callback_;                  ///< 错误回调
    CloseCallback  close_callback_;                  ///< 连接关闭回调

    std::unique_ptr<std::mutex> state_mutex_;        ///< 保护 is_connected_ / is_recognizing_
    std::unique_ptr<std::mutex> callback_mutex_;     ///< 保护回调函数指针

    bool is_connected_   = false;                    ///< WebSocket 已连接
    bool is_recognizing_ = false;                    ///< 已发 START 帧，正在识别

    SemaphoreHandle_t connection_semaphore_ = nullptr; ///< 连接同步信号量

    std::unique_ptr<AudioInput> audio_input_;        ///< 内置音频模块（可选）
};

// =============================================================================
// 单例实现
// =============================================================================

SpeechRecognitionPersistent& SpeechRecognitionPersistent::getInstance() {
    static SpeechRecognitionPersistent instance;
    return instance;
}

// 构造函数
SpeechRecognitionPersistent::SpeechRecognitionPersistent()
    : impl_(std::make_unique<Impl>()) {}

// 析构函数
SpeechRecognitionPersistent::~SpeechRecognitionPersistent() = default;

// =============================================================================
// 公有接口转发到实现类
// =============================================================================

void SpeechRecognitionPersistent::setCallbacks(ResultCallback on_result,
                                                ErrorCallback  on_error,
                                                CloseCallback  on_close) {
    impl_->setCallbacks(std::move(on_result), std::move(on_error), std::move(on_close));
}

bool SpeechRecognitionPersistent::start(int dev_pid) {
    return impl_->start(dev_pid);
}

void SpeechRecognitionPersistent::stop() {
    impl_->stop();
}

void SpeechRecognitionPersistent::cancel() {
    impl_->cancel();
}

void SpeechRecognitionPersistent::sendAudio(const uint8_t* data, size_t len) {
    impl_->sendAudio(data, len);
}

bool SpeechRecognitionPersistent::isConnected() const {
    return impl_->isConnected();
}

bool SpeechRecognitionPersistent::isRecognizing() const {
    return impl_->isRecognizing();
}

bool SpeechRecognitionPersistent::initAudio(AudioCodec* codec) {
    return impl_->initAudio(codec);
}

void SpeechRecognitionPersistent::setRecording(bool enable) {
    impl_->setRecording(enable);
}

} // namespace ai_sdk
