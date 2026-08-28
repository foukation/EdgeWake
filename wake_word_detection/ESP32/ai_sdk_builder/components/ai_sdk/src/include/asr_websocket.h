/**
 * @file asr_websocket.h
 * @brief WebSocket客户端封装类（内部头文件）
 *
 * ============================================================================
 * ⚠️ 内部头文件 - 请勿在 SDK 外部使用
 * ============================================================================
 *
 * 此文件是 AI SDK 的内部实现细节，不属于公开 API。
 * 上层业务代码不应直接引用此头文件。
 *
 * 如需进行语音识别，请使用公开 API：
 * - AsrIntelligentDialogue
 *
 * 功能说明：
 * - 封装 ESP-IDF esp_websocket_client 组件
 * - 提供 WebSocket 连接、发送、接收功能
 * - 支持 SSL/TLS 加密连接
 * - 支持消息分片处理
 */
#pragma once

#include <string>
#include <functional>
#include <vector>
#include "esp_websocket_client.h"

// FreeRTOS头文件
#include "freertos/FreeRTOS.h"
#include "freertos/semphr.h"

namespace ai_sdk {

/**
 * @struct AsrWebsocketConfig
 * @brief WebSocket客户端配置结构体（内部使用）
 *
 * 包含建立WebSocket连接所需的所有参数。
 * 支持SSL/TLS加密连接，可配置证书验证。
 */
struct AsrWebsocketConfig {
    /**
     * WebSocket服务器URL
     * 格式：wss://host:port/path 或 ws://host:port/path
     */
    std::string url;

    /**
     * 连接超时时间（毫秒）
     * 默认：10000ms（10秒）
     */
    int connect_timeout_ms = 10000;

    /**
     * 网络超时时间（毫秒）
     * 默认：30000ms（30秒）
     */
    int network_timeout_ms = 30000;

    /**
     * 心跳间隔（毫秒）
     * 0：禁用自动心跳
     * 默认：5000ms（5秒）
     */
    int ping_interval_ms = 5000;

    /**
     * SSL证书验证
     * true：验证服务器证书（生产环境）
     * false：跳过验证（测试环境）
     * 默认：true
     */
    bool verify_cert = true;

    /**
     * 缓冲区大小（字节）
     * 同时用于发送（TX）和接收（RX）缓冲区
     * 注意：此值应大于单次发送的最大数据块，避免分片导致性能下降
     * 默认：4096
     */
    int buffer_size = 4096;
};

/**
 * @class AsrWebsocket
 * @brief WebSocket客户端封装类（内部使用）
 *
 * 封装ESP-IDF WebSocket客户端，提供简洁的API。
 *
 * 特性：
 * - 自动重连（指数退避算法）
 * - SSL/TLS加密
 * - 线程安全
 * - 事件驱动
 * - 消息分片处理
 */
class AsrWebsocket {
public:
    AsrWebsocket();
    ~AsrWebsocket();

    /**
     * @brief 消息接收回调类型
     * @param payload 消息内容
     * @param len 消息长度
     * @param type 消息类型（0=文本，1=二进制）
     */
    using MessageCallback = std::function<void(const uint8_t* payload, size_t len, int type)>;

    /**
     * @brief 连接事件回调类型
     * @param event 事件类型（连接/断开/错误）
     * @param message 事件描述
     */
    using EventCallback = std::function<void(int event, const std::string& message)>;

    /**
     * @brief 建立WebSocket连接
     * @param config 配置参数
     * @return bool 连接是否启动成功
     *
     * 注意：成功返回只表示开始连接，实际连接状态通过回调通知。
     */
    bool connect(const AsrWebsocketConfig& config);

    /**
     * @brief 发送文本消息
     * @param text 文本内容（通常为JSON）
     * @return bool 是否发送成功
     */
    bool sendText(const std::string& text);

    /**
     * @brief 发送二进制数据
     * @param data 数据缓冲区
     * @param len 数据长度
     * @return bool 是否发送成功
     *
     * 用于发送PCM音频数据。
     */
    bool sendBinary(const uint8_t* data, size_t len);

    /**
     * @brief 关闭连接
     */
    void disconnect();

    /**
     * @brief 设置消息回调
     */
    void setMessageCallback(MessageCallback callback);

    /**
     * @brief 设置事件回调
     */
    void setEventCallback(EventCallback callback);

    /**
     * @brief 检查连接状态
     */
    bool isConnected() const;

    /**
     * @brief 检查是否正在连接
     */
    bool isConnecting() const;

private:
    esp_websocket_client_handle_t client_ = nullptr;  ///< WebSocket客户端句柄
    bool is_connected_ = false;                        ///< 连接状态标志
    bool is_connecting_ = false;                       ///< 连接中标志
    MessageCallback message_callback_;                 ///< 消息回调
    EventCallback event_callback_;                     ///< 事件回调
    SemaphoreHandle_t connection_mutex_;              ///< 连接互斥锁

    /**
     * @brief 消息缓冲区
     * 用于累积分片消息，直到收到完整消息后再回调
     *
     * ESP WebSocket 客户端在接收大消息时会分片触发多次 WEBSOCKET_EVENT_DATA 事件，
     * 需要缓冲累积直到收到完整消息。
     */
    std::vector<uint8_t> message_buffer_;

    /**
     * @brief 整条消息首帧的 op_code
     * 多帧消息续帧 op_code=0，需用首帧值作为整条消息类型
     */
    uint8_t message_op_code_ = 0;

    /**
     * @brief 静态事件处理函数
     * 作为ESP-IDF事件循环的回调入口
     */
    static void EventHandler(void* event_handler_arg,
                           esp_event_base_t event_base,
                           int32_t event_id,
                           void* event_data);
};

}  // namespace ai_sdk

