/**
 * @file asr_websocket.cc
 * @brief WebSocket客户端实现
 *
 * 本文件是AsrWebsocket类的实现框架，包含类成员定义和接口实现，
 * 具体需要补充WebSocket连接管理、事件处理、重连逻辑等。
 */

// 内部头文件（位于 src/include/）
#include "asr_websocket.h"
#include "esp_log.h"
#include "esp_timer.h"
#include "esp_crt_bundle.h"

// FreeRTOS头文件
#include "freertos/FreeRTOS.h"
#include "freertos/semphr.h"

namespace ai_sdk {

static const char* TAG = "AsrWebsocket";

AsrWebsocket::AsrWebsocket() {
    // 创建互斥锁，用于保护连接状态和回调函数的线程安全访问
    // 二进制信号量，初始化为1（解锁状态）
    connection_mutex_ = xSemaphoreCreateBinary();
    if (connection_mutex_) {
        xSemaphoreGive(connection_mutex_);  // 初始化为解锁状态
    } else {
        ESP_LOGE(TAG, "Failed to create connection mutex");
    }

    // 初始化连接状态为断开
    is_connected_ = false;
    is_connecting_ = false;
}

AsrWebsocket::~AsrWebsocket() {
    // 析构函数中确保连接被关闭
    // 防止资源泄漏
    if (client_) {
        disconnect();
    }

    // 删除互斥锁，释放系统资源
    if (connection_mutex_) {
        vSemaphoreDelete(connection_mutex_);
        connection_mutex_ = nullptr;
    }
}

bool AsrWebsocket::connect(const AsrWebsocketConfig& config) {
    // 连接建立是异步操作，需要保证线程安全
    // 使用互斥锁防止多个任务同时尝试连接
    if (xSemaphoreTake(connection_mutex_, pdMS_TO_TICKS(1000)) != pdTRUE) {
        ESP_LOGE(TAG, "Failed to acquire mutex for connection");
        return false;
    }

    // 检查是否已经在连接或已连接状态
    // 如果已连接，需要先断开旧连接才能建立新连接
    if (is_connected_ || is_connecting_) {
        ESP_LOGW(TAG, "Already connected or connecting, disconnect first");
        xSemaphoreGive(connection_mutex_);
        return false;
    }

    // 验证URL格式是否合法
    // WebSocket URL必须以ws://或wss://开头
    if (config.url.find("ws://") != 0 && config.url.find("wss://") != 0) {
        ESP_LOGE(TAG, "Invalid WebSocket URL format: %s", config.url.c_str());
        ESP_LOGE(TAG, "URL must start with ws:// or wss://");
        xSemaphoreGive(connection_mutex_);
        return false;
    }

    // 重连时旧客户端可能仍存在（服务端关闭后 client_ 未置空）。
    // 仍在连接中时，先 close() 优雅关闭（发送 Close 帧），再 destroy()。
    // 已断开时，因为 disable_auto_reconnect=true，ESP-IDF 内部任务在
    // 收到 DISCONNECTED 事件后已自动退出，直接 destroy() 即可，不会阻塞。
    if (client_) {
        if (esp_websocket_client_is_connected(client_)) {
            esp_websocket_client_close(client_, pdMS_TO_TICKS(1000));
        }
        esp_websocket_client_destroy(client_);
        client_ = nullptr;
    }

    // 创建 WebSocket 客户端配置
    // 配置参数来源于用户传入的 AsrWebsocketConfig 结构体。
    //
    // 这里采用“先清零，再逐项赋值”的写法，而不是一次性 designated initializer：
    // - 某些工具链/编译选项下，C++ 对 designated 字段顺序非常严格
    // - 后续如果 ESP-IDF 结构体字段顺序调整，逐项赋值更稳健
    esp_websocket_client_config_t ws_config = {};
    ws_config.uri = config.url.c_str();  // WebSocket 服务器 URL
    ws_config.buffer_size = config.buffer_size;  // TX/RX 缓冲区大小（同时用于发送和接收）
    ws_config.network_timeout_ms = config.network_timeout_ms;  // 网络超时（毫秒）

    // WebSocket 任务栈大小：8192 字节
    // 默认值 4096 不足以容纳事件回调中的深层调用链：
    //   client_task → recv → dispatch → EventHandler → parseMessage(JSON) → Play → ESP_LOGI → vprintf
    // 总计 15+ 层栈帧，实测需要 ~3700 字节，4096 会溢出并破坏相邻堆内存。
    ws_config.task_stack = 8192;
    // 禁用 ESP-IDF 内部自动重连，由应用层 (Application::OnAudioChannelClosed) 控制重连逻辑。
    // 避免 ESP-IDF 的 "Reconnect after 10000 ms" 与应用层的立即重连冲突。
    ws_config.disable_auto_reconnect = true;
    ws_config.ping_interval_sec = static_cast<size_t>(config.ping_interval_ms / 1000);  // 心跳间隔（秒）

    // TLS 严格校验：使用系统证书包并保持主机名校验开启
    ws_config.crt_bundle_attach = esp_crt_bundle_attach;
    ws_config.skip_cert_common_name_check = false;

    // 创建WebSocket客户端实例
    // 内部会分配内存和资源，失败时返回nullptr
    // ESP-IDF v5.4: 使用 esp_websocket_client_init 而不是 esp_websocket_client_create
    client_ = esp_websocket_client_init(&ws_config);
    if (!client_) {
        ESP_LOGE(TAG, "Failed to create WebSocket client instance");
        xSemaphoreGive(connection_mutex_);
        return false;
    }

    // 注册事件回调函数
    // 所有WebSocket事件（连接、断开、数据、错误）都会触发EventHandler
    // this指针作为用户参数传递给事件处理函数
    esp_websocket_register_events(client_, WEBSOCKET_EVENT_ANY, EventHandler, this);

    // 启动WebSocket客户端
    // 异步操作，立即返回
    // 实际连接结果通过事件回调通知
    esp_err_t ret = esp_websocket_client_start(client_);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "Failed to start WebSocket client: %s", esp_err_to_name(ret));
        // 启动失败，清理资源
        esp_websocket_client_destroy(client_);
        client_ = nullptr;
        xSemaphoreGive(connection_mutex_);
        return false;
    }

    // 标记为连接中状态
    // 在此状态下可以继续调用sendText/sendBinary，数据会被缓存
    is_connecting_ = true;

    xSemaphoreGive(connection_mutex_);
    return true;  // 成功启动连接流程（异步）
}

void AsrWebsocket::disconnect() {
    // 检查客户端是否存在
    if (!client_) {
        return;
    }

    // 关闭 WebSocket 连接（等待最多 1 秒）
    esp_websocket_client_close(client_, pdMS_TO_TICKS(1000));

    // 销毁客户端，释放资源
    esp_websocket_client_destroy(client_);
    client_ = nullptr;

    // 重置状态变量
    is_connected_ = false;
    is_connecting_ = false;
}

bool AsrWebsocket::sendText(const std::string& text) {
    // 检查连接状态
    if (!client_ || !is_connected_) {
        ESP_LOGE(TAG, "Not connected, cannot send text");
        return false;
    }

    // 发送文本消息
    // 参数：客户端句柄、文本内容、文本长度、超时时间
    esp_err_t ret = esp_websocket_client_send_text(
        client_,
        text.c_str(),
        text.length(),
        pdMS_TO_TICKS(1000)  // 超时 1 秒
    );

    if (ret < 0) {
        ESP_LOGE(TAG, "Failed to send text: %s", esp_err_to_name(ret));
        return false;
    }

    return true;
}

bool AsrWebsocket::sendBinary(const uint8_t* data, size_t len) {
    // ========================================
    // 使用 ESP-IDF 库函数直接检查连接状态
    // ========================================
    //
    // 为什么不使用 is_connected_？
    // - is_connected_ 是在 WebSocket 事件回调中更新的，有延迟
    // - esp_websocket_client_is_connected() 直接检查库内部状态，更及时
    // - 连接断开后，esp_websocket_client_is_connected() 会立即返回 false
    //   而 is_connected_ 要等到事件回调执行后才更新
    //
    // 效果：
    // - 避免在连接已断开但 is_connected_ 还是 true 时尝试发送
    // - 减少 "Websocket client is not connected" 错误日志
    // ========================================
    if (!client_ || !esp_websocket_client_is_connected(client_)) {
        ESP_LOGW(TAG, "WebSocket not connected, dropping binary data (len=%d)", (int)len);
        return false;
    }

    // 发送二进制数据
    // 参数：客户端句柄、数据指针、数据长度、超时时间
    esp_err_t ret = esp_websocket_client_send_bin(
        client_,
        (const char*)data,
        len,
        pdMS_TO_TICKS(1500)  // 超时 1.5 秒
    );

    if (ret < 0) {
        ESP_LOGE(TAG, "Failed to send binary: %s", esp_err_to_name(ret));
        return false;
    }

    return true;
}

void AsrWebsocket::setMessageCallback(MessageCallback callback) {
    // 设置消息回调函数
    // 当 WebSocket 收到服务器消息时，会通过此回调通知上层
    // 回调参数: data(消息数据), len(数据长度), type(消息类型: 0=文本, 1=二进制)
    message_callback_ = callback;
}

void AsrWebsocket::setEventCallback(EventCallback callback) {
    // 设置事件回调函数
    // 当 WebSocket 连接状态发生变化时（连接成功、断开、错误等），会通过此回调通知上层
    // 回调参数: event_id(事件类型), message(事件描述)
    event_callback_ = callback;
}

bool AsrWebsocket::isConnected() const {
    // 返回当前连接状态
    // true: 已建立连接，可以收发数据
    // false: 未连接或连接中
    return is_connected_;
}

bool AsrWebsocket::isConnecting() const {
    // 返回连接中状态
    // true: 正在连接（已调用start，但尚未收到connected事件）
    // false: 空闲或已连接
    return is_connecting_;
}

// WebSocket事件处理器
void AsrWebsocket::EventHandler(void* event_handler_arg,
                                esp_event_base_t event_base,
                                int32_t event_id,
                                void* event_data) {
    // event_handler_arg 是 AsrWebsocket 实例指针
    AsrWebsocket* websocket = static_cast<AsrWebsocket*>(event_handler_arg);
    if (!websocket) {
        ESP_LOGE(TAG, "Invalid event handler argument");
        return;
    }

    // 处理 WebSocket 事件
    switch (event_id) {
        case WEBSOCKET_EVENT_CONNECTED:
            {
                // 获取时间戳（秒.毫秒格式）
                double time_sec = (double)esp_timer_get_time() / 1000000.0;

                ESP_LOGI(TAG, "%.3f WebSocket connected successfully", time_sec);

                websocket->is_connected_ = true;
                websocket->is_connecting_ = false;

                // 调用事件回调通知上层连接成功
                // 上层（AsrDialogue）应通过 setEventCallback() 注册回调来处理连接事件
                if (websocket->event_callback_) {
                    websocket->event_callback_(event_id, "Connected");
                }
            }
            break;

        // ========================================
        // 连接关闭事件处理
        // ========================================
        // 
        // WEBSOCKET_EVENT_CLOSED：正常关闭（服务器发送 Close 帧）
        // WEBSOCKET_EVENT_DISCONNECTED：异常断开（网络错误、TCP reset）
        //
        // 两种事件业务逻辑相同：更新状态，通知上层连接已断开
        // 使用 fallthrough 合并处理
        // ========================================
        case WEBSOCKET_EVENT_CLOSED:
            {
                ESP_LOGI(TAG, "WebSocket closed normally (server close)");
                websocket->is_connected_ = false;
                websocket->is_connecting_ = false;

                if (websocket->event_callback_) {
                    websocket->event_callback_(event_id, "Closed");
                }
            }
            break;

        case WEBSOCKET_EVENT_DISCONNECTED:
            {
                ESP_LOGW(TAG, "WebSocket disconnected abnormally (network error)");
                websocket->is_connected_ = false;
                websocket->is_connecting_ = false;

                if (websocket->event_callback_) {
                    websocket->event_callback_(event_id, "Disconnected");
                }
            }
            break;

        case WEBSOCKET_EVENT_DATA:
            {
                // ========================================
                // 处理 WebSocket 消息重组
                // ========================================
                // 一条消息可能被两层拆分：
                //   1) 协议层多帧：首帧 fin=false，末帧 fin=true（op_code=0 为续帧）
                //   2) 单帧超 buffer：同一帧分多次事件，payload_offset 递增
                // 必须两层都收齐才回调：当前帧收齐(frame_complete) 且 是末帧(fin)。
                // ========================================
                auto* data = (esp_websocket_event_data_t*)event_data;
                if (!data || !websocket->message_callback_) {
                    break;
                }

                // 整条消息首帧：缓存 op_code（续帧 op_code=0，不可作为整条消息类型）
                if (data->payload_offset == 0 && websocket->message_buffer_.empty()) {
                    websocket->message_op_code_ = data->op_code;
                }

                // 累积当前片段（统一覆盖多帧与单帧超 buffer 两种拆分）
                websocket->message_buffer_.insert(
                    websocket->message_buffer_.end(),
                    (const uint8_t*)data->data_ptr,
                    (const uint8_t*)data->data_ptr + data->data_len
                );

                // 当前帧是否收齐（处理单帧超 buffer 的分块）
                bool frame_complete = (data->payload_offset + data->data_len >= data->payload_len);

                // 仅当“当前帧收齐”且“是整条消息末帧(fin)”时才回调
                if (frame_complete && data->fin) {
                    websocket->message_callback_(
                        websocket->message_buffer_.data(),
                        websocket->message_buffer_.size(),
                        websocket->message_op_code_
                    );
                    websocket->message_buffer_.clear();
                    websocket->message_buffer_.shrink_to_fit();
                }
                // frame_complete 但 fin=false → 等下一帧；frame_complete=false → 等同帧下一块
            }
            break;

        case WEBSOCKET_EVENT_ERROR:
            {
                websocket->is_connected_ = false;
                websocket->is_connecting_ = false;

                if (websocket->event_callback_) {
                    websocket->event_callback_(event_id, "Error");
                }
            }
            break;

        default:
            break;
    }
}

}  // namespace ai_sdk
