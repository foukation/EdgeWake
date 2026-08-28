/**
 * @file http_client.h
 * @brief HTTP客户端封装类（内部头文件）
 *
 * ============================================================================
 * ⚠️ 内部头文件 - 请勿在 SDK 外部使用
 * ============================================================================
 *
 * 此文件是 AI SDK 的内部实现细节，不属于公开 API。
 * 上层业务代码不应直接引用此头文件。
 *
 * 功能说明：
 * - 封装 ESP-IDF esp_http_client 组件
 * - 提供异步 HTTP GET/POST 请求能力
 * - 使用智能指针管理请求上下文生命周期
 *
 * ============================================================================
 * 异步化改造说明（2025-01）
 * ============================================================================
 *
 * 问题背景：
 *   原 get()/post() 方法使用 esp_http_client_perform()，是同步阻塞的。
 *   虽然使用了事件回调，但调用线程会被阻塞直到请求完成。
 *
 * 解决方案：
 *   1. 新增 getAsync()/postAsync() - 异步版本，立即返回 requestId
 *   2. 保留 get()/post() - 同步版本，向后兼容
 *   3. 网络请求在独立的 FreeRTOS 任务中执行
 *   4. 支持通过 cancelRequest() 取消进行中的请求
 *
 * 架构对比：
 *
 *   [优化前 - 同步阻塞]
 *   调用线程: post() ──► esp_http_client_perform() ──► 完成 ──► return
 *                              (阻塞整个线程)
 *
 *   [优化后 - 异步非阻塞]
 *   调用线程: postAsync() ──► 创建任务 ──► return requestId (立即返回)
 *   独立任务:                        └──► esp_http_client_perform() ──► 回调
 */
#pragma once

#include <string>
#include <map>
#include <functional>
#include <memory>
#include <atomic>
#include <mutex>
#include "esp_http_client.h"
#include "esp_err.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "freertos/semphr.h"

namespace ai_sdk {

// 前向声明
class HTTPClient;

/**
 * @struct HTTPContext
 * @brief HTTP请求上下文结构（内部使用）
 *
 * 内存管理说明：
 * - 使用 shared_ptr 自动管理生命周期，避免手动 delete 导致的重复释放问题
 * - 在 HTTP 事件处理器中不再需要手动删除 ctx 对象
 * - shared_ptr 会在最后一个引用消失时自动释放内存
 */
struct HTTPContext {
    std::function<void(const std::string&)> on_success;  ///< 成功回调函数
    std::function<void(const std::string&)> on_error;    ///< 错误回调函数
    std::string response_data;                           ///< 响应数据缓存
    HTTPClient* client;                                  ///< HTTP客户端实例指针
    std::atomic<bool> cancelled{false};                  ///< 【新增】取消标志
    void* client_handle = nullptr;                       ///< 【新增】HTTP 客户端句柄
    std::string request_id;                              ///< 【新增】请求 ID
};

/**
 * @class HTTPClient
 * @brief HTTP客户端封装类（支持异步非阻塞）
 *
 * 封装 ESP-IDF HTTP 客户端，提供简洁的异步请求接口。
 *
 * 特性：
 * - 支持 HTTP/HTTPS（HTTPS 证书验证可配置）
 * - 支持异步非阻塞请求（getAsync/postAsync）
 * - 支持同步阻塞请求（get/post，向后兼容）
 * - 支持请求取消
 * - 自动管理请求生命周期
 * - 线程安全
 */
class HTTPClient {
public:
    using ResponseCallback = std::function<void(const std::string&)>;
    using ErrorCallback = std::function<void(const std::string&)>;

    // ========================================================================
    // 构造与析构
    // ========================================================================

    /**
     * @brief 构造函数
     */
    HTTPClient();

    /**
     * @brief 析构函数
     *
     * 析构时会取消所有活跃请求
     */
    ~HTTPClient();

    // 禁止拷贝
    HTTPClient(const HTTPClient&) = delete;
    HTTPClient& operator=(const HTTPClient&) = delete;

    // ========================================================================
    // 异步方法（推荐）
    // ========================================================================

    /**
     * @brief 【推荐】异步发送 GET 请求
     *
     * 立即返回 requestId，不会阻塞调用线程。
     * 网络请求在独立的 FreeRTOS 任务中执行。
     *
     * @param url       请求 URL
     * @param headers   请求头（键值对）
     * @param onSuccess 成功回调，参数为响应体字符串
     * @param onError   错误回调，参数为错误信息
     *
     * @return 请求 ID，用于取消请求；失败时返回空字符串
     *
     * @note 回调函数在独立任务中执行，请注意线程安全
     */
    std::string getAsync(
        const std::string& url,
        const std::map<std::string, std::string>& headers,
        ResponseCallback onSuccess,
        ErrorCallback onError
    );

    /**
     * @brief 【推荐】异步发送 POST 请求
     *
     * 立即返回 requestId，不会阻塞调用线程。
     * 网络请求在独立的 FreeRTOS 任务中执行。
     *
     * @param url       请求 URL
     * @param body      请求体（通常为 JSON 字符串）
     * @param headers   请求头（键值对）
     * @param onSuccess 成功回调，参数为响应体字符串
     * @param onError   错误回调，参数为错误信息
     *
     * @return 请求 ID，用于取消请求；失败时返回空字符串
     *
     * @note 回调函数在独立任务中执行，请注意线程安全
     */
    std::string postAsync(
        const std::string& url,
        const std::string& body,
        const std::map<std::string, std::string>& headers,
        ResponseCallback onSuccess,
        ErrorCallback onError
    );

    // ========================================================================
    // 同步方法（向后兼容）
    // ========================================================================

    /**
     * @brief 【向后兼容】同步发送 GET 请求
     *
     * 此方法会阻塞直到请求完成。
     *
     * @param url       请求 URL
     * @param headers   请求头（键值对）
     * @param onSuccess 成功回调，参数为响应体字符串
     * @param onError   错误回调，参数为错误信息
     *
     * @deprecated 建议使用 getAsync() 替代
     */
    void get(
        const std::string& url,
        const std::map<std::string, std::string>& headers,
        ResponseCallback onSuccess,
        ErrorCallback onError
    );

    /**
     * @brief 【向后兼容】同步发送 POST 请求
     *
     * 此方法会阻塞直到请求完成。
     *
     * @param url       请求 URL
     * @param body      请求体（通常为 JSON 字符串）
     * @param headers   请求头（键值对）
     * @param onSuccess 成功回调，参数为响应体字符串
     * @param onError   错误回调，参数为错误信息
     *
     * @deprecated 建议使用 postAsync() 替代
     */
    void post(
        const std::string& url,
        const std::string& body,
        const std::map<std::string, std::string>& headers,
        ResponseCallback onSuccess,
        ErrorCallback onError
    );

    // ========================================================================
    // 请求管理
    // ========================================================================

    /**
     * @brief 取消请求
     *
     * 设置取消标志，并调用 esp_http_client_cancel_request() 中断底层连接。
     *
     * @param requestId 请求 ID
     *
     * @return true 取消成功，false 请求不存在
     */
    bool cancelRequest(const std::string& requestId);

    /**
     * @brief 生成唯一请求 ID
     *
     * @return 唯一的请求 ID，格式：http_<时间戳>_<随机数>
     */
    static std::string generateRequestId();

private:
    // ========================================================================
    // 内部数据结构
    // ========================================================================

    /**
     * @struct HttpTaskParams
     * @brief HTTP 任务参数
     *
     * 传递给 FreeRTOS 任务的参数结构体。
     */
    struct HttpTaskParams {
        std::string url;                                    ///< 请求 URL
        std::string body;                                   ///< 请求体（POST 用）
        std::map<std::string, std::string> headers;         ///< 请求头
        ResponseCallback onSuccess;                         ///< 成功回调
        ErrorCallback onError;                              ///< 错误回调
        std::shared_ptr<HTTPContext> context;               ///< 请求上下文
        HTTPClient* http_client;                            ///< HTTPClient 实例
        bool is_post;                                       ///< 是否为 POST 请求
    };

    std::map<std::string, std::shared_ptr<HTTPContext>> active_requests_;  ///< 活跃请求映射表
    std::mutex mutex_;  ///< 互斥锁，保护 active_requests_

    // ========================================================================
    // 私有方法
    // ========================================================================

    /**
     * @brief FreeRTOS 任务函数
     *
     * 在独立任务中执行 HTTP 请求。
     *
     * @param arg HttpTaskParams* 参数指针
     */
    static void httpTaskFunction(void* arg);

    /**
     * @brief 执行 HTTP 请求的核心逻辑
     *
     * @param params 任务参数
     */
    void executeHttpRequest(HttpTaskParams* params);

    /**
     * @brief HTTP事件处理器（静态）
     *
     * 处理ESP-IDF HTTP客户端的所有事件，包括：
     * - 连接建立/断开
     * - 响应头接收
     * - 响应体数据接收
     * - 请求完成
     * - 错误处理
     *
     * @param evt HTTP事件结构体，包含事件类型和相关数据
     * @return ESP_OK表示成功，ESP_FAIL表示失败
     */
    static esp_err_t http_event_handler(esp_http_client_event_t *evt);

    /**
     * @brief 添加请求上下文
     */
    void addRequest(const std::string& requestId, std::shared_ptr<HTTPContext> context);

    /**
     * @brief 移除请求上下文
     */
    void removeRequest(const std::string& requestId);

    /**
     * @brief 获取请求上下文
     */
    std::shared_ptr<HTTPContext> getRequest(const std::string& requestId);
};

} // namespace ai_sdk

