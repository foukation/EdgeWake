/**
 * @file sse_client.h
 * @brief SSE (Server-Sent Events) 流式客户端
 *
 * 此文件定义了 SSE 流式客户端，用于处理流式 HTTP 响应。
 * 基于 esp_http_client 的 open/read/close 模式实现。
 *
 * ============================================================================
 * 异步化改造说明（2025-01）
 * ============================================================================
 *
 * 问题背景：
 *   原 postStream() 方法是同步阻塞的，请求完成后才返回 requestId，
 *   导致调用者无法在请求进行中取消操作。
 *
 * 解决方案：
 *   1. 新增 postStreamAsync() - 异步版本，立即返回 requestId
 *   2. 保留 postStream() - 同步版本，向后兼容
 *   3. 网络请求在独立的 FreeRTOS 任务中执行
 *   4. 支持通过 cancelRequest() 取消进行中的请求
 *
 * 架构对比：
 *
 *   [优化前 - 同步阻塞]
 *   调用线程: postStream() ──► while循环读取 ──► 完成 ──► return requestId
 *                              (阻塞整个线程)
 *
 *   [优化后 - 异步非阻塞]
 *   调用线程: postStreamAsync() ──► 创建任务 ──► return requestId (立即返回)
 *   独立任务:                              └──► while循环读取 ──► 完成/取消
 *
 * @note 此为内部头文件，仅供 SDK 内部使用
 */
#pragma once

#include <string>
#include <functional>
#include <map>
#include <memory>
#include <atomic>
#include <mutex>
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "freertos/semphr.h"

namespace ai_sdk {

/**
 * @class SSEClient
 * @brief SSE 流式客户端（支持异步非阻塞）
 *
 * 用于处理 Server-Sent Events 流式响应。
 * 支持 POST 请求和流式数据读取。
 *
 * 特性：
 * - 支持异步非阻塞请求（postStreamAsync）
 * - 支持同步阻塞请求（postStream，向后兼容）
 * - 支持请求取消
 * - 线程安全
 */
class SSEClient {
public:
    // ========================================================================
    // 回调类型定义
    // ========================================================================

    /**
     * @brief 数据回调类型
     *
     * @param data SSE 数据内容（已去除 "data: " 前缀）
     *
     * 每收到一条 SSE 数据时调用。
     *
     * @note 回调在独立任务中执行，注意线程安全
     */
    using DataCallback = std::function<void(const std::string& data)>;

    /**
     * @brief 错误回调类型
     *
     * @param error 错误信息
     *
     * 此回调在以下场景被调用：
     * 1. 网络错误、HTTP 错误等真正的错误情况
     * 2. 用户主动取消请求（error = "Request cancelled"）
     *
     * 业务层判断取消的方式：
     * @code
     * [](const std::string& error) {
     *     if (error == "Request cancelled") {
     *         // 用户主动取消，更新 UI 状态
     *     } else {
     *         // 真正的错误，显示错误提示
     *     }
     * }
     * @endcode
     *
     * @note 回调在独立任务中执行，注意线程安全
     */
    using ErrorCallback = std::function<void(const std::string& error)>;

    /**
     * @brief 完成回调类型
     *
     * 当收到 "[DONE]" 信号或连接正常关闭时调用。
     *
     * @note 回调在独立任务中执行，注意线程安全
     */
    using DoneCallback = std::function<void()>;

    // ========================================================================
    // 构造与析构
    // ========================================================================

    /**
     * @brief 构造函数
     */
    SSEClient();

    /**
     * @brief 析构函数
     *
     * 析构时会取消所有活跃请求，但不会强制删除任务。
     * 任务会在检测到取消标志后自行退出。
     */
    ~SSEClient();

    // 禁止拷贝
    SSEClient(const SSEClient&) = delete;
    SSEClient& operator=(const SSEClient&) = delete;

    // ========================================================================
    // 公开方法
    // ========================================================================

    /**
     * @brief 【推荐】异步发送流式 POST 请求
     *
     * 发送 POST 请求并以流式方式读取响应。
     * 此方法立即返回，不会阻塞调用线程。
     * 网络请求在独立的 FreeRTOS 任务中执行。
     *
     * @param url       请求 URL
     * @param body      请求体（JSON 字符串）
     * @param headers   请求头
     * @param onData    数据回调，每收到一条 SSE 数据时调用
     * @param onError   错误回调
     * @param onDone    完成回调
     *
     * @return 请求 ID，用于取消请求；失败时返回空字符串
     *
     * @note 回调函数在独立任务中执行，请注意线程安全
     *
     * @example
     * @code
     * SSEClient client;
     * std::string requestId = client.postStreamAsync(
     *     "https://api.example.com/stream",
     *     "{\"prompt\": \"Hello\"}",
     *     {{"Authorization", "Bearer xxx"}},
     *     [](const std::string& data) {
     *         // 处理数据（在任务线程中执行）
     *         printf("Data: %s\n", data.c_str());
     *     },
     *     [](const std::string& error) {
     *         printf("Error: %s\n", error.c_str());
     *     },
     *     []() {
     *         printf("Done!\n");
     *     }
     * );
     *
     * // requestId 立即返回，可用于取消请求
     * // client.cancelRequest(requestId);
     * @endcode
     */
    std::string postStreamAsync(
        const std::string& url,
        const std::string& body,
        const std::map<std::string, std::string>& headers,
        DataCallback onData,
        ErrorCallback onError,
        DoneCallback onDone);

    /**
     * @brief 【向后兼容】同步发送流式 POST 请求
     *
     * 发送 POST 请求并以流式方式读取响应。
     * 此方法会阻塞直到流结束或被取消。
     *
     * @param url       请求 URL
     * @param body      请求体（JSON 字符串）
     * @param headers   请求头
     * @param onData    数据回调，每收到一条 SSE 数据时调用
     * @param onError   错误回调
     * @param onDone    完成回调
     *
     * @return 请求 ID
     *
     * @deprecated 建议使用 postStreamAsync() 替代
     *
     * @note 内部调用 postStreamAsync() 并等待完成
     */
    std::string postStream(
        const std::string& url,
        const std::string& body,
        const std::map<std::string, std::string>& headers,
        DataCallback onData,
        ErrorCallback onError,
        DoneCallback onDone);

    /**
     * @brief 取消请求
     *
     * 设置取消标志，并调用 esp_http_client_cancel_request() 中断底层连接。
     * 任务会在下次循环检测时退出。
     *
     * 取消后的回调行为：
     * - onError 会被调用，参数为 "Request cancelled"
     * - onDone 不会被调用
     * - 业务层可通过 error 内容判断是主动取消还是真正错误
     *
     * @param requestId 请求 ID
     *
     * @return true 取消成功，false 请求不存在
     */
    bool cancelRequest(const std::string& requestId);

    /**
     * @brief 生成唯一请求 ID
     *
     * @return 唯一的请求 ID，格式：sse_<时间戳>_<随机数>
     */
    static std::string generateRequestId();

private:
    // ========================================================================
    // 内部数据结构
    // ========================================================================

    /**
     * @struct RequestContext
     * @brief 请求上下文
     *
     * 保存请求的状态和相关句柄，用于取消和管理请求。
     */
    struct RequestContext {
        std::atomic<bool> cancelled{false};     ///< 取消标志（原子操作，线程安全）
        void* client_handle = nullptr;          ///< HTTP 客户端句柄
        TaskHandle_t task_handle = nullptr;     ///< FreeRTOS 任务句柄
        std::string request_id;                 ///< 请求 ID
    };

    /**
     * @struct StreamTaskParams
     * @brief 流式任务参数
     *
     * 传递给 FreeRTOS 任务的参数结构体。
     * 包含请求所需的所有信息和回调函数。
     *
     * @note 使用 new 分配，由任务函数负责 delete
     */
    struct StreamTaskParams {
        std::string url;                                    ///< 请求 URL
        std::string body;                                   ///< 请求体
        std::map<std::string, std::string> headers;         ///< 请求头
        DataCallback onData;                                ///< 数据回调
        ErrorCallback onError;                              ///< 错误回调
        DoneCallback onDone;                                ///< 完成回调
        std::shared_ptr<RequestContext> context;            ///< 请求上下文（共享）
        SSEClient* sse_client;                              ///< SSEClient 实例指针
    };

    std::map<std::string, std::shared_ptr<RequestContext>> active_requests_;  ///< 活跃请求映射表
    std::mutex mutex_;  ///< 互斥锁，保护 active_requests_

    // ========================================================================
    // 私有方法
    // ========================================================================

    /**
     * @brief FreeRTOS 任务函数
     *
     * 在独立任务中执行 HTTP 流式请求。
     * 任务完成后自动清理并删除自身。
     *
     * @param arg StreamTaskParams* 参数指针
     */
    static void streamTaskFunction(void* arg);

    /**
     * @brief 执行流式请求的核心逻辑
     *
     * 从原 postStream() 中提取的核心代码。
     * 包含 HTTP 连接、数据读取、SSE 解析等。
     *
     * @param params 任务参数
     */
    void executeStreamRequest(StreamTaskParams* params);

    /**
     * @brief 解析 SSE 数据
     *
     * @param buffer 原始数据缓冲区（已处理的数据会被移除）
     * @param onData 数据回调
     *
     * @return true 收到 [DONE] 信号，false 继续读取
     */
    bool parseSSEData(std::string& buffer, DataCallback& onData);

    /**
     * @brief 添加请求上下文
     *
     * @param requestId 请求 ID
     * @param context 请求上下文
     */
    void addRequest(const std::string& requestId, std::shared_ptr<RequestContext> context);

    /**
     * @brief 移除请求上下文
     *
     * @param requestId 请求 ID
     */
    void removeRequest(const std::string& requestId);

    /**
     * @brief 获取请求上下文
     *
     * @param requestId 请求 ID
     *
     * @return 请求上下文，不存在返回 nullptr
     */
    std::shared_ptr<RequestContext> getRequest(const std::string& requestId);
};

}  // namespace ai_sdk
