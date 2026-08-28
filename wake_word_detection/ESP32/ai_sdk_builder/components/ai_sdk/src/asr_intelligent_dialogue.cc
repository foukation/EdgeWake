/**
 * @file asr_intelligent_dialogue.cc
 * @brief ASR智能对话实现文件
 *
 * 本文件包含 AsrIntelligentDialogue 类的实现，
 * 管理 WebSocket 通信、JSON 解析和状态管理，
 * 用于实时语音识别和智能对话。
 */

// 公开头文件（位于 include/ai_sdk/）
#include "ai_sdk/asr_intelligent_dialogue.h"
#include "ai_sdk/ai_assistant_manager.h"

// 内部头文件（位于 src/include/）
#include "asr_websocket.h"
#include "api_config.h"
#include "assist_utils.h"
#include "cjson_guard.h"  // RAII 封装，防止 cJSON 内存泄漏
#include "esp_log.h"
#include "esp_websocket_client.h"
#include "cJSON.h"
#include "esp_timer.h"
#include <mutex>

// 音频模块内部头文件（位于 src/audio/，通过 PRIV_INCLUDE_DIRS 访问）
#include "audio_input.h"
#include "tts_player.h"

// FreeRTOS头文件
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include <atomic>

namespace ai_sdk {

// 日志标签
static const char* TAG = "AsrIntelligentDialogue";

/**
 * @class AsrIntelligentDialogue::Impl
 * @brief 私有实现类（PIMPL模式）
 *
 * 隐藏实现细节，减少头文件依赖，提高编译速度，
 * 同时提供更好的二进制兼容性。
 */
class AsrIntelligentDialogue::Impl {
private:
    static constexpr int CONNECTION_TIMEOUT_MS = 30000;  // 连接超时时间（30秒）
    static constexpr size_t RECEIVE_TASK_STACK = 4096;   // 接收任务堆栈
    static constexpr UBaseType_t RECEIVE_TASK_PRIO = 5;  // 接收任务优先级

public:
    /**
     * @brief 启动ASR识别
     * @return bool 是否启动成功
     *
     * 实现要点：
     * 1. 检查当前状态（是否已在识别中）
     * 2. 构建WebSocket URL（内部构建，不是外部传入）
     *   - 基础URL: ApiConfig::WSS_WEBSOCKET_ASR_BASE_URL + ApiConfig::ASR_INTELLIGENT_DIALOGUE_API
     *   - 调用 AssistUtils::wssParameter() 添加参数和签名
     * 3. 创建WebSocket配置（AsrWebsocketConfig，设置超时时间）
     * 4. 建立WebSocket连接（异步）
     * 5. 等待连接成功（使用信号量，30秒超时）
     * 6. 发送ASR配置参数（sample rate, format等）
     * 7. 标记为识别中状态
     *
     * 错误处理：
     * - URL构建失败：返回false，通过error_callback_通知
     * - 网络连接失败：异步通知，尝试重连
     * - 连接超时：返回false
     * - 配置发送失败：记录日志，继续尝试
     *
     * 线程安全：使用state_mutex_保护状态变量。
     */
    bool start() {
        // ========================================
        // 第1步：加锁检查状态
        // ========================================
        // 注意：这里使用代码块 {} 限制锁的作用范围
        // 锁只在检查状态时持有，检查完毕后立即释放
        // 这样可以避免后续等待信号量时发生死锁
        {
        std::lock_guard<std::mutex> lock(*state_mutex_);
        if (is_recognizing_) {
            ESP_LOGW(TAG, "Already recognizing, cannot start again");
            return false;
        }
        }  // ← 锁在这里自动释放

        // ========================================
        // 第2步：构建完整WebSocket URL
        // ========================================
        // 基础URL: wss://ivs.chinamobiledevice.com:11443/app-ws/v2/asr
        std::string base_url = std::string(ApiConfig::WSS_WEBSOCKET_ASR_BASE_URL_TEST) +
                              ApiConfig::ASR_INTELLIGENT_DIALOGUE_API;

        // 添加参数和签名（sn, deviceNo, productKey, ts, sign等）
        std::string ws_url = AssistUtils::wssParameter(base_url);
        if (ws_url.empty()) {
            if (error_callback_) {
                error_callback_(-1, "Failed to build WebSocket URL");
                ESP_LOGE(TAG, "错误: code=-1, message=Failed to build WebSocket URL");
            }
            return false;
        }
        // WebSocket URL 包含敏感认证参数，仅 DEBUG 级别输出
        ESP_LOGD(TAG, "WS_URL ：%s", ws_url.c_str());

        // ========================================
        // 第3步：配置WebSocket参数
        // ========================================
        // - 连接超时：10秒
        // - 网络超时：30秒
        // - 心跳间隔：30秒
        AsrWebsocketConfig config;
        config.url = ws_url;
        config.connect_timeout_ms = 10000;   // 10秒
        config.network_timeout_ms = 30000;   // 30秒
        config.ping_interval_ms = 30000;     // 30秒（心跳）
        // 缓冲区大小：8KB（同时用于 TX 和 RX）
        // 注意：此值需大于单次 PCM 音频块大小（5120字节），
        // 避免 ESP-IDF WebSocket 库内部分片发送导致的 transport_poll_write 超时错误
        config.buffer_size = 8192;

        // ========================================
        // 第4步：重置信号量（确保在获取前为0）
        // ========================================
        if (connection_semaphore_ != nullptr) {
            xSemaphoreTake(connection_semaphore_, 0);  // 清空信号量
        }

        // ========================================
        // 第5步：注册事件回调（处理连接成功、断开、错误事件）
        // ========================================
        // 通过 event_callback_ 接收底层 WebSocket 状态变化通知
        // 注意：回调函数在 WebSocket 事件线程中执行，不是 start() 所在的线程
        websocket_.setEventCallback([this](int event_id, const std::string& message) {
            switch (event_id) {
                case WEBSOCKET_EVENT_CONNECTED:
                    this->onConnected();
                    break;

                // ========================================
                // 连接关闭事件处理
                // ========================================
                // 
                // WEBSOCKET_EVENT_CLOSED：正常关闭（服务器发送 Close 帧）
                //   - 业务正常结束，服务器主动关闭连接
                //   - 日志：识别完成（正常关闭）
                //
                // WEBSOCKET_EVENT_DISCONNECTED：异常断开（网络错误、TCP reset）
                //   - 网络层意外断开，可能是网络问题
                //   - 日志：识别完成（连接断开）
                //
                // 两种事件业务逻辑相同：通知上层会话已结束
                // ========================================
                case WEBSOCKET_EVENT_CLOSED:
                    this->onDisconnected(message);
                    // Reset recognition state when server closes connection.
                    // This is critical: when server initiates close (conversation completed),
                    // we must reset is_recognizing_ to allow new sessions to start.
                    // Without this, subsequent start() calls would fail with "Already recognizing".
                    {
                        std::lock_guard<std::mutex> lock(*state_mutex_);
                        is_recognizing_ = false;
                        is_connected_ = false;
                    }
                    // 释放信号量（如果连接阶段还在等待）
                    if (connection_semaphore_ != nullptr) {
                        xSemaphoreGive(connection_semaphore_);
                    }
                    // 调用完成回调
                    if (complete_callback_) {
                        complete_callback_();
                        ESP_LOGI(TAG, "识别完成（正常关闭）");
                    }
                    break;

                case WEBSOCKET_EVENT_DISCONNECTED:
                    this->onDisconnected(message);
                    // Reset recognition state when connection is unexpectedly lost.
                    // Similar to WEBSOCKET_EVENT_CLOSED, we must reset state to allow recovery.
                    // This handles network errors, TCP resets, and other unexpected disconnections.
                    {
                        std::lock_guard<std::mutex> lock(*state_mutex_);
                        is_recognizing_ = false;
                        is_connected_ = false;
                    }
                    // 释放信号量，让 start() 立即返回失败（如果还在等待连接）
                    if (connection_semaphore_ != nullptr) {
                        xSemaphoreGive(connection_semaphore_);
                    }
                    // WebSocket 断开时调用完成回调
                    if (complete_callback_) {
                        complete_callback_();
                        ESP_LOGW(TAG, "识别完成（连接断开）");
                    }
                    break;

                case WEBSOCKET_EVENT_ERROR:
                    // 连接错误时立即停止音频发送
                    // ERROR 事件表示底层传输出错（TCP 写入失败、TLS 错误等），
                    // 连接已不可用。立即重置状态，防止 sendAudio() 继续尝试
                    // 写入已断开的连接，减少 transport_poll_write 错误。
                    // 注意：ERROR 之后还会收到 DISCONNECTED 事件，
                    // complete_callback_ 在 DISCONNECTED 中调用，不受影响。
                    {
                        std::lock_guard<std::mutex> lock(*state_mutex_);
                        is_recognizing_ = false;
                        is_connected_ = false;
                    }
                    // 释放信号量，让 start() 立即返回失败
                    if (connection_semaphore_ != nullptr) {
                        xSemaphoreGive(connection_semaphore_);
                    }
                    // 通知上层发生错误
                    // 连接相关错误使用 code=1
                    if (error_callback_) {
                        error_callback_(1, "WebSocket error: " + message);
                        ESP_LOGE(TAG, "错误: code=1, message=WebSocket error: %s", message.c_str());
                    }
                    break;

                default:
                    break;
            }
        });

        // ========================================
        // 第6步：注册消息回调（处理服务器发送的数据）
        // ========================================
        // 用于接收 ASR 识别结果、对话响应等业务消息
        websocket_.setMessageCallback([this](const uint8_t* data, size_t len, int type) {
            if (type == 1) {
                // 文本消息：JSON 格式的 ASR 结果或对话响应
                this->parseMessage(data, len);
            }
            // 二进制消息不处理
        });

        // ========================================
        // 第7步：发起 WebSocket 连接（异步）
        // ========================================
        if (!websocket_.connect(config)) {
            // 连接相关错误使用 code=1
            if (error_callback_) {
                error_callback_(1, "Failed to initiate WebSocket connection");
                ESP_LOGE(TAG, "错误: code=1, message=Failed to initiate WebSocket connection");
            }
            return false;
        }

        // ========================================
        // 第8步：等待连接成功
        // ========================================
        // 等待事件：onConnected() 在连接成功时释放信号量
        // 如果连接失败或出错，event_callback_ 也会释放信号量，让等待立即返回
        //
        // 重要：此时没有持有 state_mutex_ 锁！
        // 这样 onConnected() 可以获取锁并释放信号量，不会发生死锁
        if (connection_semaphore_ != nullptr) {
            // 等待连接成功信号，最多30秒
            if (xSemaphoreTake(connection_semaphore_, pdMS_TO_TICKS(CONNECTION_TIMEOUT_MS)) != pdTRUE) {
                websocket_.disconnect();
                // 连接超时使用 code=1
                if (error_callback_) {
                    error_callback_(1, "Connection timeout");
                    ESP_LOGE(TAG, "错误: code=1, message=Connection timeout");
                }
                return false;
            }
        }

        // ========================================
        // 第9步：检查连接结果并更新状态
        // ========================================
        // 重新加锁，检查连接状态并设置 is_recognizing_
        {
            std::lock_guard<std::mutex> lock(*state_mutex_);

        // 检查是否真的连接成功
        if (!is_connected_) {
            websocket_.disconnect();
                // 连接失败使用 code=1
            if (error_callback_) {
                    error_callback_(1, "Connection failed");
                    ESP_LOGE(TAG, "错误: code=1, message=Connection failed");
            }
            return false;
        }

            // 标记为识别中状态
            is_recognizing_ = true;
        }  // ← 锁在这里自动释放

        // ========================================
        // 第10步：调用连接成功回调
        // ========================================
        if (connected_callback_) {
            connected_callback_();
            ESP_LOGI(TAG, "wss connected:");
        }

        // ========================================
        // 第11步：发送 Start Signal
        // ========================================
        // 发送完整的 Start Signal，包含设备信息和配置参数
        sendStartSignal();

        // TODO: 第12步：启动接收任务（处理服务器响应）
        // xTaskCreate(ReceiveTaskEntry, "asr_receive", RECEIVE_TASK_STACK, this, ...);

        return true;
    }

    /**
     * @brief WebSocket连接成功回调
     *
     * 由 event_callback_ 在收到 WEBSOCKET_EVENT_CONNECTED 事件时调用
     * 负责更新连接状态并释放信号量，唤醒等待中的 start() 函数
     *
     * 执行顺序：
     * 1. 先加锁更新 is_connected_ 状态
     * 2. 释放锁
     * 3. 再释放信号量唤醒 start()
     *
     * 这样可以确保 start() 被唤醒后检查 is_connected_ 时，状态已经是 true
     */
    void onConnected() {
        // 先加锁更新状态
        {
        std::lock_guard<std::mutex> lock(*state_mutex_);
        is_connected_ = true;
        }  // ← 锁在这里释放

        // 释放信号量，唤醒等待的 start() 函数
        // 注意：必须在锁释放之后再释放信号量
        // 这样 start() 被唤醒后可以立即获取锁检查状态
        if (connection_semaphore_ != nullptr) {
            xSemaphoreGive(connection_semaphore_);
        }
    }

    /**
     * @brief WebSocket断开回调
     * @param error 错误信息
     */
    void onDisconnected(const std::string& error = "") {
        std::lock_guard<std::mutex> lock(*state_mutex_);
        is_connected_ = false;
    }

    /**
     * @brief 连接同步信号量
     * 用于等待WebSocket连接建立
     * - 连接成功：give() 释放信号量
     * - 连接失败：超时或错误处理
     *
     * 实现方式：使用 FreeRTOS 二值信号量（初始为0，连接成功后give）
     */
    SemaphoreHandle_t connection_semaphore_ = nullptr;

    /**
     * @brief 构造函数
     *
     * 初始化内部状态，创建必要的资源（互斥锁、信号量等）。
     */
    Impl() {
        // 创建状态互斥锁（保护 is_recognizing_ 和 is_connected_）
        state_mutex_ = std::make_unique<std::mutex>();

        // 创建回调互斥锁（保护回调函数指针）
        callback_mutex_ = std::make_unique<std::mutex>();

        // 创建连接信号量（初始为0，用于连接同步）
        connection_semaphore_ = xSemaphoreCreateBinary();

        is_recognizing_ = false;
        is_connected_ = false;
        receive_task_ = nullptr;
    }

    /**
     * @brief 析构函数
     *
     * 释放所有资源，关闭WebSocket连接，销毁互斥锁和信号量。
     */
    ~Impl() {
        if (is_connected_) {
            stop();
        }

        // 等待接收任务结束
        if (receive_task_ != nullptr) {
            vTaskDelete(receive_task_);
            receive_task_ = nullptr;
        }

        // 删除连接信号量
        if (connection_semaphore_ != nullptr) {
            vSemaphoreDelete(connection_semaphore_);
            connection_semaphore_ = nullptr;
        }
    }

    /**
     * @brief 停止ASR识别并断开连接
     *
     * 实现要点：
     * 1. 检查 WebSocket 连接状态（is_connected_）
     *    - 使用 is_connected_ 而非 is_recognizing_，确保在整个会话生命周期内
     *      都可以停止（包括 fin_result 之后、TTS 回复阶段）
     * 2. 断开 WebSocket 连接
     * 3. 重置所有状态变量
     *
     * 注意：finish 信号在收到 fin_result 时发送（见 parseMessage），
     * stop() 只负责断开连接和清理状态。
     */
    void stop() {
        // 检查连接状态
        // 使用 is_connected_ 判断会话是否还在进行，而非 is_recognizing_。
        // is_recognizing_ 在收到 fin_result 后就会变为 false，
        // 但此时会话可能仍在进行（服务器还在发送 TTS 回复等数据）。
        {
            std::lock_guard<std::mutex> lock(*state_mutex_);
            if (!is_connected_) {
                ESP_LOGW(TAG, "Not connected, nothing to stop");
                return;
            }
        }

        // ========================================
        // 断开 WebSocket 连接
        // ========================================
        // finish 信号已移至 fin_result 消息处理中发送（收到最终识别结果时），
        // stop() 只负责断开连接和重置状态。
        // complete_callback_ 会在 WEBSOCKET_EVENT_CLOSED/DISCONNECTED 事件中调用。
        websocket_.disconnect();

        // 暂停录音（如果已初始化），停止向 WebSocket 发送音频
        if (audio_input_) {
            audio_input_->SetRecording(false);
        }

        // 停止 TTS 播放（如果正在播放）
        if (tts_player_) {
            tts_player_->Stop();
        }

        // 重置状态变量
        {
            std::lock_guard<std::mutex> lock(*state_mutex_);
            is_recognizing_ = false;
            is_connected_ = false;
        }
    }

    /**
     * @brief 发送音频数据
     * @param data PCM音频数据
     * @param len 数据长度
     *
     * 实现要点：
     * 1. 检查连接状态（必须已连接且正在识别）
     * 2. 数据缓存（如果网络慢）
     * 3. 分包发送（每包5120字节）
     * 4. 添加音频帧头（时间戳、序列号）
     * 5. 处理发送失败情况
     *
     * 性能优化：
     * - 使用内存池减少malloc/free
     * - 批量发送减少系统调用
     * - 双缓冲机制避免阻塞
     *
     * 错误处理：
     * - 连接断开：调用error_callback_
     * - 发送失败：重试3次，失败后停止识别
     */
    void sendAudio(const uint8_t* data, size_t len) {
        // 检查是否已连接且正在识别
        {
            std::lock_guard<std::mutex> lock(*state_mutex_);
            if (!is_recognizing_ || !is_connected_) {
                // 会话已结束或连接已断开，自动停止 AudioInput 录音。
                // 避免 AudioInput 持续采集无用音频并反复触发本回调，
                // 减少 CPU 消耗和日志噪音。
                // 重连时 OpenAudioChannel() 会调用 setRecording(true) 恢复录音。
                if (audio_input_) {
                    audio_input_->SetRecording(false);
                }
                ESP_LOGW(TAG, "Not ready, stopped recording and dropping audio data");
                return;
            }
        }

        // 发送音频数据（Opus 编码后的二进制数据）
        websocket_.sendBinary(data, len);
    }

    /**
     * @brief 检查是否已连接
     * @return bool true 已连接，false 未连接
     */
    bool isConnected() const {
        std::lock_guard<std::mutex> lock(*state_mutex_);
        return is_connected_;
    }

    /**
     * @brief 检查是否正在识别
     * @return bool true 正在识别，false 未识别
     */
    bool isRecognizing() const {
        std::lock_guard<std::mutex> lock(*state_mutex_);
        return is_recognizing_;
    }

    // =========================================================================
    // 音频模块接口（ASR 集成层，任务 24-25）
    // =========================================================================

    /**
     * @brief 初始化内置音频模块
     *
     * 绑定 AudioCodec，在 SDK 内部创建并初始化：
     * - AudioInput：麦克风录音模块
     * - TtsPlayer：TTS 播放模块
     *
     * AudioInput 的数据回调内部连接到 sendAudio()，
     * 确保录音数据自动发往云端 ASR。
     *
     * @param codec 已创建并 Start() 的 AudioCodec 实例
     * @return true 成功，false 失败（codec 为 null 或模块初始化失败）
     */
    bool initAudio(AudioCodec* codec) {
        if (!codec) {
            ESP_LOGE(TAG, "initAudio failed: codec is null");
            return false;
        }

        // 创建并初始化 AudioInput
        audio_input_ = std::make_unique<AudioInput>();
        if (!audio_input_->Initialize(codec)) {
            ESP_LOGE(TAG, "initAudio failed: AudioInput::Initialize() failed");
            audio_input_.reset();
            return false;
        }

        // 设置录音数据回调：录音数据 → sendAudio() → WebSocket → 云端 ASR
        audio_input_->SetAudioDataCallback([this](const uint8_t* data, size_t len) {
            sendAudio(data, len);
        });

        // 启动录音任务（初始状态为暂停，调用 setRecording(true) 才开始采集）
        if (!audio_input_->Start()) {
            ESP_LOGE(TAG, "initAudio failed: AudioInput::Start() failed");
            audio_input_.reset();
            return false;
        }

        // 创建并初始化 TtsPlayer
        tts_player_ = std::make_unique<TtsPlayer>();
        if (!tts_player_->Initialize(codec)) {
            ESP_LOGE(TAG, "initAudio failed: TtsPlayer::Initialize() failed");
            tts_player_.reset();
            // AudioInput 已启动，但 TtsPlayer 失败，仍认为初始化失败
            // 停止 AudioInput
            audio_input_->Stop();
            audio_input_.reset();
            return false;
        }

        // Bridge internal TTS completion events to ASR public callback.
        // This callback runs in TTS play task context.
        tts_player_->SetCompletionCallback([this](const std::string& url,
                                                  bool completed,
                                                  bool all_done) {
            TtsPlaybackCallback cb_copy = nullptr;
            {
                std::lock_guard<std::mutex> lock(*callback_mutex_);
                cb_copy = tts_playback_callback_;
            }
            if (cb_copy) {
                cb_copy(url, completed, all_done);
            }
        });

        ESP_LOGI(TAG, "Audio modules initialized (AudioInput + TtsPlayer)");
        return true;
    }

    /**
     * @brief 控制麦克风录音开关
     *
     * @param enable true 开始录音，false 暂停录音
     */
    void setRecording(bool enable) {
        if (!audio_input_) {
            ESP_LOGW(TAG, "setRecording: audio not initialized, call initAudio() first");
            return;
        }
        audio_input_->SetRecording(enable);
    }

    /**
     * @brief 设置是否自动播放 TTS
     *
     * @param enable true 自动播放 Speak 指令中的 URL，false 不自动播放
     */
    void setAutoPlayTts(bool enable) {
        if (!tts_player_) {
            ESP_LOGW(TAG, "setAutoPlayTts: audio not initialized, call initAudio() first");
            return;
        }
        auto_play_tts_.store(enable);
        ESP_LOGI(TAG, "setAutoPlayTts: %s", enable ? "enabled" : "disabled");
    }

    /**
     * @brief 设置回调函数
     * @param connected_cb 连接成功回调（可选，可为nullptr）
     * @param asr_cb ASR识别结果回调（中间结果和最终结果）
     * @param dialogue_cb 对话结果回调
     * @param error_cb 错误回调
     * @param complete_cb 识别完成回调（可选，可为nullptr）
     *
     * 实现要点：
     * 1. 线程安全：使用互斥锁保护回调函数指针
     * 2. 空值检查：允许传入nullptr，表示忽略该类型回调
     * 3. 立即生效：设置后立即用于后续事件
     *
     * 回调调用时机：
     * - connected_cb: WebSocket连接建立成功时
     * - asr_cb: 收到ASR识别结果时（中间结果或最终结果）
     * - dialogue_cb: 收到对话响应时
     * - error_cb: 发生错误时（网络、协议、超时等）
     * - complete_cb: 整个识别会话完成时
     */
    void setCallbacks(ConnectedCallback connected_cb,
                     AsrCallback asr_cb,
                     DialogueCallback dialogue_cb,
                     ErrorCallback error_cb,
                     CompleteCallback complete_cb) {
        // 使用互斥锁保护回调设置，确保线程安全
        std::lock_guard<std::mutex> lock(*callback_mutex_);

        // 保存回调函数指针
        // 允许传入 nullptr，表示忽略该类型回调
        connected_callback_ = connected_cb;
        asr_callback_ = asr_cb;
        dialogue_callback_ = dialogue_cb;
        error_callback_ = error_cb;
        complete_callback_ = complete_cb;
    }

    void setTtsPlaybackCallback(TtsPlaybackCallback cb) {
        std::lock_guard<std::mutex> lock(*callback_mutex_);
        tts_playback_callback_ = std::move(cb);
    }

private:
    /**
     * @brief WebSocket客户端
     * 封装底层通信细节
     */
    AsrWebsocket websocket_;

    /**
     * @brief 连接成功回调
     * WebSocket连接建立时调用
     */
    ConnectedCallback connected_callback_;

    /**
     * @brief ASR回调
     * 处理语音识别结果（中间和最终）
     */
    AsrCallback asr_callback_;

    /**
     * @brief 对话回调
     * 处理完整对话响应
     */
    DialogueCallback dialogue_callback_;

    /**
     * @brief 错误回调
     * 处理各类错误
     */
    ErrorCallback error_callback_;

    /**
     * @brief 完成回调
     * 整个识别会话完成时调用
     */
    CompleteCallback complete_callback_;
    TtsPlaybackCallback tts_playback_callback_;

    /**
     * @brief 状态互斥锁
     * 保护is_recognizing_和is_connected_等状态变量
     */
    std::unique_ptr<std::mutex> state_mutex_;

    /**
     * @brief 回调互斥锁
     * 保护回调函数指针
     */
    std::unique_ptr<std::mutex> callback_mutex_;

    /**
     * @brief 是否正在识别
     * true: start()成功，stop()前保持true
     * false: 空闲或已停止
     */
    bool is_recognizing_ = false;

    /**
     * @brief WebSocket连接状态
     * true: 已连接，可发送数据
     * false: 未连接或连接中
     */
    bool is_connected_ = false;

    /**
     * @brief 接收任务句柄
     * 用于处理服务器响应
     */
    TaskHandle_t receive_task_ = nullptr;

    // =========================================================================
    // 音频模块（ASR 集成层，任务 24-25）
    // =========================================================================

    /**
     * @brief 麦克风录音模块（由 initAudio() 创建）
     *
     * 内部采集 PCM 数据并自动调用 sendAudio() 发送到云端 ASR。
     * nullptr 表示未初始化（厂商未调用 initAudio()）。
     */
    std::unique_ptr<AudioInput> audio_input_;

    /**
     * @brief TTS 播放模块（由 initAudio() 创建）
     *
     * 收到 Speak 指令且 auto_play_tts_ 为 true 时，
     * 自动从 HTTP URL 下载并播放 MP3 音频。
     * nullptr 表示未初始化（厂商未调用 initAudio()）。
     */
    std::unique_ptr<TtsPlayer> tts_player_;

    /**
     * @brief 是否自动播放 TTS（由 setAutoPlayTts() 控制）
     *
     * true：收到 Speak 指令时，自动调用 TtsPlayer::Play(url)
     * false：不自动播放，只通过 DialogueCallback 通知厂商
     */
    std::atomic<bool> auto_play_tts_{false};

    /**
     * @brief 接收任务入口函数
     * @param param 用户参数（impl_指针）
     *
     * 任务职责：
     * 1. 等待WebSocket消息
     * 2. 解析JSON响应
     * 3. 区分响应类型（ASR中间结果、最终结果、对话指令）
     * 4. 调用相应回调函数
     * 5. 处理连接断开和重连
     *
     * 任务生命周期：
     * - start()时创建，stop()时删除
     * - 阻塞接收消息，不占用CPU
     * - 支持任务通知机制（关闭时唤醒）
     */
    static void ReceiveTaskEntry(void* param);

    /**
     * @brief 解析服务器消息
     * @param payload 消息体
     * @param len 消息长度
     *
     * 消息类型：
     * 1. mid_result: {"type": "mid_result", "result": "部分文本"}
     * 2. fin_result: {"type": "fin_result", "result": "完整文本"}
     * 3. inside_rc: {"type": "inside_rc", "data": {...}}
     * 4. dcs_decide: {"type": "dcs_decide", "end": 1}
     *
     * 实现步骤：
     * 1. JSON解析（使用cJSON）
     * 2. 提取type字段
     * 3. 根据类型分发处理
     * 4. 构造AsrResult或DialogueResult
     * 5. 调用回调函数
     * 6. 释放JSON对象
     *
     * 错误处理：
     * - JSON解析失败：记录日志，调用error_callback_
     * - 缺少必要字段：视为错误消息
     * - 未知类型：忽略或记录警告
     */
    void parseMessage(const uint8_t* payload, size_t len);

    /**
     * @brief 发送 Start Signal
     *
     * 建联成功后发送，包含设备信息和配置参数
     *
     * JSON 结构：
     * {
     *   "type": "start",
     *   "data": {
     *     "cuid": "设备号",
     *     "format": "pcm",
     *     "sample": 16000,
     *     "dialog_request_id": "唯一ID",
     *     "client_context": [...]
     *   }
     * }
     */
    void sendStartSignal();
};

// 单例实现
AsrIntelligentDialogue& AsrIntelligentDialogue::getInstance() {
    static AsrIntelligentDialogue instance;
    return instance;
}

// 构造函数
AsrIntelligentDialogue::AsrIntelligentDialogue() : impl_(std::make_unique<Impl>()) {}

// 析构函数
AsrIntelligentDialogue::~AsrIntelligentDialogue() = default;

// 公有接口转发到实现类
void AsrIntelligentDialogue::setCallbacks(ConnectedCallback connected_cb,
                              AsrCallback asr_cb,
                              DialogueCallback dialogue_cb,
                              ErrorCallback error_cb,
                              CompleteCallback complete_cb) {
    impl_->setCallbacks(connected_cb, asr_cb, dialogue_cb, error_cb, complete_cb);
}

bool AsrIntelligentDialogue::start() {
    return impl_->start();
}

void AsrIntelligentDialogue::stop() {
    impl_->stop();
}

void AsrIntelligentDialogue::sendAudio(const uint8_t* data, size_t len) {
    impl_->sendAudio(data, len);
}

bool AsrIntelligentDialogue::isConnected() const {
    return impl_->isConnected();
}

bool AsrIntelligentDialogue::isRecognizing() const {
    return impl_->isRecognizing();
}

bool AsrIntelligentDialogue::initAudio(AudioCodec* codec) {
    return impl_->initAudio(codec);
}

void AsrIntelligentDialogue::setRecording(bool enable) {
    impl_->setRecording(enable);
}

void AsrIntelligentDialogue::setAutoPlayTts(bool enable) {
    impl_->setAutoPlayTts(enable);
}

void AsrIntelligentDialogue::setTtsPlaybackCallback(TtsPlaybackCallback cb) {
    impl_->setTtsPlaybackCallback(std::move(cb));
}

// 接收任务入口函数空实现
void AsrIntelligentDialogue::Impl::ReceiveTaskEntry(void* param) {
    // TODO: 实现接收任务
    // - 等待WebSocket消息
    // - 解析JSON响应
    // - 调用相应回调函数
}

// 消息解析函数实现
void AsrIntelligentDialogue::Impl::parseMessage(const uint8_t* payload, size_t len) {
    // 将 payload 转换为字符串（用于日志和解析）
    std::string json_str(reinterpret_cast<const char*>(payload), len);

    // 完整响应内容仅 DEBUG 级别输出
    ESP_LOGD(TAG, "[WebSocket.onMessage] Response: %s", json_str.c_str());

    // 解析 JSON
    cJSON* root = cJSON_Parse(json_str.c_str());
    if (!root) {
        // 坏片段仅丢弃，不上报致命错误，避免误杀整轮对话
        ESP_LOGW(TAG, "[WebSocket.onMessage] 丢弃无法解析的片段(len=%d)，不中断对话", (int)len);
        return;
    }

    // 获取消息类型
    cJSON* type_item = cJSON_GetObjectItem(root, "type");
    if (!type_item || !cJSON_IsString(type_item)) {
        ESP_LOGW(TAG, "[WebSocket.onMessage] Message missing 'type' field");
        cJSON_Delete(root);
        return;
    }

    const char* type = type_item->valuestring;

    // ========================================
    // 根据消息类型分发处理
    // ========================================

    if (strcmp(type, "mid_result") == 0) {
        // ASR 中间结果（实时识别）- 高频输出，仅 DEBUG 级别
        cJSON* result = cJSON_GetObjectItem(root, "result");
        if (result && cJSON_IsString(result)) {
            ESP_LOGD(TAG, "[WebSocket.onMessage.mid_result] %s", result->valuestring);

            if (asr_callback_) {
                AsrResult asr_result;
                asr_result.text = result->valuestring;
                asr_result.is_final = false;
                asr_callback_(asr_result);

                ESP_LOGD(TAG, "实时识别结果: %s", result->valuestring);
            }
        }
    }
    else if (strcmp(type, "fin_result") == 0) {
        // ========================================
        // ASR 最终识别结果
        // ========================================
        //
        // fin_result 表示服务器已完成语音识别，后续只会发送：
        //   - inside_rc（对话/TTS 回复）
        //   - dcs_decide end=1（会话结束）
        //
        // 处理步骤：
        // 1. 停止音频发送（is_recognizing_ = false）
        //    避免服务器关闭 TCP 时客户端仍在写入，触发 transport_poll_write 错误。
        // 2. 发送 finish 信号通知服务器客户端已确认识别完成
        //    协议要求：start signal 中设置了 need_dialogue_finish = true，
        //    服务器期望客户端在收到 fin_result 后回复 finish 信号。
        // 3. 回调通知上层最终识别结果
        // ========================================

        // 步骤 1：停止音频发送
        {
            std::lock_guard<std::mutex> lock(*state_mutex_);
            is_recognizing_ = false;
        }

        // 步骤 2：发送 finish 信号，通知服务器客户端已确认识别完成
        std::string finish_msg = R"({"type": "finish"})";
        if (websocket_.sendText(finish_msg)) {
            ESP_LOGI(TAG, "[fin_result] Finish signal sent");
        } else {
            ESP_LOGW(TAG, "[fin_result] Failed to send finish signal");
        }

        // 步骤 3：回调通知上层最终识别结果
        cJSON* result = cJSON_GetObjectItem(root, "result");
        if (result && cJSON_IsString(result)) {
            ESP_LOGD(TAG, "[WebSocket.onMessage.fin_result] %s", result->valuestring);

            if (asr_callback_) {
                AsrResult asr_result;
                asr_result.text = result->valuestring;
                asr_result.is_final = true;

                // 提取情绪标签（服务器在 fin_result 中返回）
                cJSON* emotion = cJSON_GetObjectItem(root, "emotion");
                if (emotion && cJSON_IsString(emotion)) {
                    asr_result.emotion = emotion->valuestring;
                    ESP_LOGD(TAG, "[fin_result] emotion: %s", emotion->valuestring);
                }

                asr_callback_(asr_result);

                ESP_LOGD(TAG, "最终识别结果: %s", result->valuestring);
            }
        }
    }
    else if (strcmp(type, "inside_rc") == 0) {
        // ========================================
        // 智能对话结果
        // ========================================
        ESP_LOGD(TAG, "[WebSocket.onMessage.inside_rc] Data received");

        cJSON* data = cJSON_GetObjectItem(root, "data");
        if (data) {
            // 解析公共字段
            std::string qid_str;
            int is_end_val = 0;

            cJSON* qid = cJSON_GetObjectItem(data, "qid");
            if (qid && cJSON_IsString(qid)) {
                qid_str = qid->valuestring;
            }

            cJSON* is_end = cJSON_GetObjectItem(data, "is_end");
            if (is_end && cJSON_IsNumber(is_end)) {
                is_end_val = is_end->valueint;
            }

            // 解析 directives 数组（data 下的 data 数组）
            cJSON* directives = cJSON_GetObjectItem(data, "data");
            if (directives && cJSON_IsArray(directives)) {
                // 遍历每个 directive，逐个调用回调
                int directives_count = cJSON_GetArraySize(directives);
                for (int i = 0; i < directives_count; i++) {
                    cJSON* directive = cJSON_GetArrayItem(directives, i);
                    if (!directive) continue;

                    cJSON* header = cJSON_GetObjectItem(directive, "header");
                    cJSON* payload_obj = cJSON_GetObjectItem(directive, "payload");

                    // 构建 DialogueResult
                    DialogueResult dialogue_result;
                    dialogue_result.qid = qid_str;
                    dialogue_result.is_end = is_end_val;

                    std::string directive_name;

                    // 序列化 header 为 JSON 字符串
                    if (header) {
                        char* header_str = cJSON_PrintUnformatted(header);
                        if (header_str) {
                            dialogue_result.header = header_str;
                            free(header_str);
                        }

                        // 提取 name 作为便捷字段
                        cJSON* name = cJSON_GetObjectItem(header, "name");
                        if (name && cJSON_IsString(name)) {
                            directive_name = name->valuestring;
                            dialogue_result.directive = directive_name;
                        }
                    }

                    // 序列化 payload 为 JSON 字符串
                    if (payload_obj) {
                        char* payload_str = cJSON_PrintUnformatted(payload_obj);
                        if (payload_str) {
                            dialogue_result.payload = payload_str;
                            free(payload_str);
                        }
                    }

                    // ========================================
                    // 按 directive 类型输出详细日志
                    // ========================================
                    if (directive_name == "RenderProcessing") {
                        cJSON* percent = cJSON_GetObjectItem(payload_obj, "percent");
                        if (percent && cJSON_IsNumber(percent)) {
                            ESP_LOGD(TAG, "isGenerating = true 进度 ：%d", percent->valueint);
                        }
                    } else if (directive_name == "Nlu") {
                        cJSON* nlu = cJSON_GetObjectItem(payload_obj, "nlu");
                        if (nlu) {
                            char* nlu_str = cJSON_PrintUnformatted(nlu);
                            if (nlu_str) {
                                ESP_LOGD(TAG, "意图 ：%s", nlu_str);
                                free(nlu_str);
                            }
                        }
                    } else if (directive_name == "NluTag") {
                        cJSON* domain = cJSON_GetObjectItem(payload_obj, "domian");  // 注意：服务器拼写是 "domian"
                        cJSON* intent = cJSON_GetObjectItem(payload_obj, "intent");
                        ESP_LOGD(TAG, "NluTag = domain: %s intent: %s",
                                 domain && cJSON_IsString(domain) ? domain->valuestring : "",
                                 intent && cJSON_IsString(intent) ? intent->valuestring : "");
                    } else if (directive_name == "RenderMultiImageCard") {
                        cJSON* images = cJSON_GetObjectItem(payload_obj, "images");
                        if (images && cJSON_IsArray(images) && cJSON_GetArraySize(images) > 0) {
                            cJSON* first_img = cJSON_GetArrayItem(images, 0);
                            if (first_img) {
                                cJSON* url = cJSON_GetObjectItem(first_img, "url");
                                if (url && cJSON_IsString(url)) {
                                    ESP_LOGD(TAG, "percent = 100 图片 ：%s", url->valuestring);
                                }
                            }
                        }
                    } else if (directive_name == "Play") {
                        cJSON* audioItem = cJSON_GetObjectItem(payload_obj, "audioItem");
                        if (audioItem) {
                            cJSON* stream = cJSON_GetObjectItem(audioItem, "stream");
                            cJSON* extension = cJSON_GetObjectItem(audioItem, "extension");
                            const char* mediaUrl = "";
                            const char* albumName = "";
                            if (stream) {
                                cJSON* url = cJSON_GetObjectItem(stream, "url");
                                if (url && cJSON_IsString(url)) mediaUrl = url->valuestring;
                            }
                            if (extension && cJSON_IsString(extension)) albumName = extension->valuestring;
                            ESP_LOGD(TAG, "Play = mediaUrl: %s albumName: %s", mediaUrl, albumName);
                        }
                    } else if (directive_name == "RenderStreamCard") {
                        cJSON* answer = cJSON_GetObjectItem(payload_obj, "answer");
                        if (answer && cJSON_IsString(answer)) {
                            ESP_LOGD(TAG, "answer content ：%s", answer->valuestring);
                        }
                    } else if (directive_name == "Speak") {
                        cJSON* url_item = cJSON_GetObjectItem(payload_obj, "url");
                        const char* url_str = (url_item && cJSON_IsString(url_item))
                                              ? url_item->valuestring : "";
                        ESP_LOGD(TAG, "Speak = qid: %s ttlUrl: %s",
                                 qid_str.c_str(), url_str);

                        // 自动播放 TTS（如果已初始化且开关已打开）
                        if (auto_play_tts_.load() && tts_player_ && url_str[0] != '\0') {
                            ESP_LOGI(TAG, "Auto-playing TTS: %s", url_str);
                            tts_player_->Play(std::string(url_str));
                        }
                    } else if (!directive_name.empty()) {
                        ESP_LOGD(TAG, "未处理的消息类型: %s, payload: %s",
                                 directive_name.c_str(), dialogue_result.payload.c_str());
                    }

                    // 每个 directive 都调用一次回调
                    if (dialogue_callback_) {
                        dialogue_callback_(dialogue_result);
                    }
                }
            } else {
                // 没有 directives 数组时也调用回调（兼容处理）
                DialogueResult dialogue_result;
                dialogue_result.qid = qid_str;
                dialogue_result.is_end = is_end_val;
                if (dialogue_callback_) {
                    dialogue_callback_(dialogue_result);
                }
            }

            // ========================================
            // is_end == 1 时解析 assistant_answer
            // ========================================
            if (is_end_val == 1) {
                cJSON* assistant_answer = cJSON_GetObjectItem(data, "assistant_answer");
                if (assistant_answer && cJSON_IsString(assistant_answer)) {
                    std::string answer_content = assistant_answer->valuestring;

                    // 尝试解析 assistant_answer 中的 content 字段
                    cJSON* answer_json = cJSON_Parse(answer_content.c_str());
                    if (answer_json) {
                        cJSON* content = cJSON_GetObjectItem(answer_json, "content");
                        if (content && cJSON_IsString(content)) {
                            answer_content = content->valuestring;
                        }
                        cJSON_Delete(answer_json);
                    }

                    ESP_LOGD(TAG, " isEnd == 1 answer content ：%s", answer_content.c_str());

                    // 发送 is_end=1 的回调
                    DialogueResult end_result;
                    end_result.qid = qid_str;
                    end_result.is_end = 1;
                    end_result.assistant_answer_content = answer_content;

                    if (dialogue_callback_) {
                        dialogue_callback_(end_result);
                    }
                }
            }
        }
    }
    else if (strcmp(type, "ready") == 0) {
        ESP_LOGD(TAG, "[WebSocket.onMessage.ready] Received");
    }
    else if (strcmp(type, "dcs_decide") == 0) {
        // 决策消息，检查 end 标志
        // dcs_decide end=1 是服务器发送的最后一条业务消息，表示会话结束。
        // 收到后必须立即停止发送音频，否则服务器关闭 TCP 连接时
        // 客户端的 sendAudio() 仍在写入，导致 transport_poll_write 错误。
        // complete_callback_ 会在后续 WEBSOCKET_EVENT_CLOSED/DISCONNECTED 事件中调用。
        cJSON* end = cJSON_GetObjectItem(root, "end");
        if (end && cJSON_IsNumber(end) && end->valueint == 1) {
            ESP_LOGI(TAG, "[dcs_decide] Session ending, stopping audio send");
            std::lock_guard<std::mutex> lock(*state_mutex_);
            is_recognizing_ = false;
        }
    }
    else if (strstr(json_str.c_str(), "directive") != nullptr) {
        // ========================================
        // 处理独立的 directive 消息
        // ========================================
        ESP_LOGD(TAG, "[WebSocket.onMessage.directive] Directive received:");
        ESP_LOGD(TAG, "[WebSocket.onMessage.directive] Content: %s", json_str.c_str());

        cJSON* directive = cJSON_GetObjectItem(root, "directive");
        if (directive) {
            cJSON* header = cJSON_GetObjectItem(directive, "header");
            cJSON* payload_obj = cJSON_GetObjectItem(directive, "payload");

            DialogueResult dialogue_result;

            if (header) {
                char* header_str = cJSON_PrintUnformatted(header);
                if (header_str) {
                    dialogue_result.header = header_str;
                    free(header_str);
                }

                cJSON* namespace_item = cJSON_GetObjectItem(header, "namespace");
                cJSON* name = cJSON_GetObjectItem(header, "name");
                if (namespace_item && cJSON_IsString(namespace_item) &&
                    name && cJSON_IsString(name)) {
                    dialogue_result.directive = std::string(namespace_item->valuestring) +
                                                "." + name->valuestring;
                    ESP_LOGD(TAG, "[WebSocket.onMessage.directive] Namespace: %s, Name: %s",
                             namespace_item->valuestring, name->valuestring);
                }
            }

            if (payload_obj) {
                char* payload_str = cJSON_PrintUnformatted(payload_obj);
                if (payload_str) {
                    dialogue_result.payload = payload_str;
                    ESP_LOGD(TAG, "[WebSocket.onMessage.directive] Payload: %s", payload_str);
                    free(payload_str);
                }
            }

            if (dialogue_callback_) {
                dialogue_callback_(dialogue_result);
            }
        }
    }
    else {
        // 未知消息类型
        ESP_LOGD(TAG, "[WebSocket.onMessage] Unknown type: %s", type);
    }

    // 释放 JSON 对象
    cJSON_Delete(root);
}

// 发送 Start Signal 实现
void AsrIntelligentDialogue::Impl::sendStartSignal() {
    // 创建根 JSON 对象（使用 RAII 自动管理内存）
    cJSON* root = cJSON_CreateObject();
    cJSONGuard root_guard(root);  // 自动释放，防止内存泄漏
    if (!root) {
        ESP_LOGE(TAG, "[sendAudioData] Failed to create JSON root object");
        return;
    }

    // 设置消息类型为 "start"
    cJSON_AddStringToObject(root, "type", "start");

    // 创建 data 对象
    // 注意：data 会被 cJSON_AddItemToObject 添加到 root，由 root 管理生命周期
    cJSON* data = cJSON_CreateObject();
    if (!data) {
        ESP_LOGE(TAG, "[sendAudioData] Failed to create JSON data object");
        return;  // root_guard 会自动释放 root
    }

    // 获取设备配置信息
    const auto& config = AIAssistantManager::getInstance().config();

    // 基础配置
    cJSON_AddStringToObject(data, "cuid", config.deviceNo.c_str());
    // 音频格式：pcm（原始 PCM 数据）
    // 由于 ESP32 的 Opus 编码帧时长（60ms）与服务器期望的帧时长（10ms）不匹配，
    // 导致服务器解码失败（opus: buffer too small / corrupted stream）。
    // 改为发送 PCM 数据可避免此问题。
    cJSON_AddStringToObject(data, "format", "pcm");
    cJSON_AddNumberToObject(data, "sample", 16000);
    cJSON_AddNumberToObject(data, "support_dcs", 2);
    // chunk_size：服务器协议参数
    // 注意：这与客户端发送阈值 BYTES_PER_FRAME (640) 是不同的概念
    cJSON_AddNumberToObject(data, "chunk_size", 10240);
    cJSON_AddBoolToObject(data, "support_tts", true);
    cJSON_AddBoolToObject(data, "support_text2dcs", true);
    cJSON_AddStringToObject(data, "client_ip", "");
    cJSON_AddBoolToObject(data, "access_rc", true);
    cJSON_AddBoolToObject(data, "support_part_tts", true);
    cJSON_AddBoolToObject(data, "need_stoplisten", true);
    cJSON_AddBoolToObject(data, "need_dialogue_finish", true);
    cJSON_AddBoolToObject(data, "result_trans2directive", false);

    // 情绪识别配置（emotion_config）
    // 启用后服务器在 fin_result 中返回 "emotion" 字段，标识用户语音的情绪
    cJSON* emotion_config = cJSON_CreateObject();
    if (emotion_config) {
        cJSON_AddBoolToObject(emotion_config, "enable", true);
        cJSON_AddStringToObject(emotion_config, "labels",
            "happy,angry,dejected,wronged,thingking,terrified,smirk,confused,bored,dizzy,chaos,wink");
        cJSON_AddItemToObject(data, "emotion_config", emotion_config);
    }

    // 中控配置版本（rc_version）
    // 服务器根据此版本号返回对应的智能对话配置
    // 如果版本号不存在，服务器会返回错误：
    // {"status":"error","result":"inside_rc req err: ...version not exist..."}
    if (!config.centralConfigVersion.empty()) {
        cJSON_AddStringToObject(data, "rc_version", config.centralConfigVersion.c_str());
    }

    // 生成唯一对话 ID（UUID + 时间戳）
    // Generate a unique ID for each dialog session.
    // Use std::to_string instead of printf-style formatting to avoid
    // toolchain-dependent integer format issues.
    const int64_t timestamp_ms = esp_timer_get_time() / 1000;  // us -> ms
    const std::string dialog_id =
        "esp32-" + config.deviceNo + "-" + std::to_string((long long)timestamp_ms);
    cJSON_AddStringToObject(data, "dialog_request_id", dialog_id.c_str());

    // ========================================================================
    // 创建 client_context 数组（设备状态信息）
    // ========================================================================
    // 包含 SpeechState（TTS配置）、Volume、PlaybackState 等设备状态
    cJSON* client_context = cJSON_CreateArray();
    if (client_context) {
        // ====================================================================
        // 1. Speech State（TTS 配置）
        // ====================================================================
        // 发送 TTS 配置给服务器，控制语音合成的音色、语速、音调和音量
        cJSON* speech_state = cJSON_CreateObject();
        cJSON* speech_header = cJSON_CreateObject();
        cJSON_AddStringToObject(speech_header, "namespace", "ai.fxzsos.device_interface.voice_output");
        cJSON_AddStringToObject(speech_header, "name", "SpeechState");
        cJSON_AddItemToObject(speech_state, "header", speech_header);
        
        cJSON* speech_payload = cJSON_CreateObject();
        // TTS 音色 ID
        cJSON_AddNumberToObject(speech_payload, "voiceId", config.dialogueTtsConfig.voiceId);
        // TTS 来源
        cJSON_AddStringToObject(speech_payload, "source", "baidu_tsn");
        
        // baidu_tsn 子对象（TTS 参数）
        cJSON* baidu_tsn = cJSON_CreateObject();
        cJSON_AddNumberToObject(baidu_tsn, "speed", config.dialogueTtsConfig.speed);
        cJSON_AddNumberToObject(baidu_tsn, "pitch", config.dialogueTtsConfig.pitch);
        cJSON_AddNumberToObject(baidu_tsn, "volume", config.dialogueTtsConfig.volume);
        cJSON_AddItemToObject(speech_payload, "baidu_tsn", baidu_tsn);
        
        cJSON_AddItemToObject(speech_state, "payload", speech_payload);
        cJSON_AddItemToArray(client_context, speech_state);

        // ====================================================================
        // 2. Speaker Controller State（音量控制）
        // ====================================================================
        cJSON* speaker_state = cJSON_CreateObject();
        cJSON* speaker_header = cJSON_CreateObject();
        cJSON_AddStringToObject(speaker_header, "namespace", "ai.fxzsos.device_interface.speaker_controller");
        cJSON_AddStringToObject(speaker_header, "name", "Volume");
        cJSON_AddItemToObject(speaker_state, "header", speaker_header);
        cJSON* speaker_payload = cJSON_CreateObject();
        cJSON_AddNumberToObject(speaker_payload, "volume", 50);
        cJSON_AddBoolToObject(speaker_payload, "muted", false);
        cJSON_AddItemToObject(speaker_state, "payload", speaker_payload);
        cJSON_AddItemToArray(client_context, speaker_state);

        // ====================================================================
        // 3. Audio Player State（播放状态）
        // ====================================================================
        cJSON* audio_state = cJSON_CreateObject();
        cJSON* audio_header = cJSON_CreateObject();
        cJSON_AddStringToObject(audio_header, "namespace", "ai.fxzsos.device_interface.audio_player");
        cJSON_AddStringToObject(audio_header, "name", "PlaybackState");
        cJSON_AddItemToObject(audio_state, "header", audio_header);
        cJSON* audio_payload = cJSON_CreateObject();
        cJSON_AddStringToObject(audio_payload, "playerActivity", "FINISHED");
        cJSON_AddNumberToObject(audio_payload, "offsetInMilliseconds", 0);
        cJSON_AddStringToObject(audio_payload, "token", "");
        cJSON_AddItemToObject(audio_state, "payload", audio_payload);
        cJSON_AddItemToArray(client_context, audio_state);

        cJSON_AddItemToObject(data, "client_context", client_context);
    }

    // 将 data 添加到 root
    cJSON_AddItemToObject(root, "data", data);

    // 序列化 JSON
    char* json_str = cJSON_PrintUnformatted(root);
    if (json_str) {
        // 获取时间戳（秒.毫秒格式）
        double time_sec = (double)esp_timer_get_time() / 1000000.0;

        ESP_LOGD(TAG, "[sendAudioData] %.3f Sending start signal:", time_sec);
        ESP_LOGD(TAG, "[sendAudioData] Start signal content: %s", json_str);

        // 发送到服务器
        if (!websocket_.sendText(json_str)) {
            ESP_LOGW(TAG, "[sendAudioData] Failed to send start signal");
        } else {
            ESP_LOGD(TAG, "[sendAudioData] Start signal sent successfully");
        }

        // 释放 JSON 字符串
        free(json_str);
    } else {
        ESP_LOGE(TAG, "[sendAudioData] Failed to serialize JSON");
    }

    // root_guard 会在函数结束时自动释放 root（RAII）
}

}  // namespace ai_sdk
