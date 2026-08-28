/**
 * @file sse_client.cc
 * @brief SSE (Server-Sent Events) 流式客户端实现
 *
 * 基于 esp_http_client 的 open/read/close 模式实现 SSE 流式请求。
 *
 * ============================================================================
 * 异步化改造说明（2025-01）
 * ============================================================================
 *
 * 核心改动：
 * 1. 新增 postStreamAsync() - 异步版本，立即返回 requestId
 * 2. 新增 streamTaskFunction() - FreeRTOS 任务函数
 * 3. 新增 executeStreamRequest() - 核心请求逻辑（从原 postStream 提取）
 * 4. 修改 cancelRequest() - 支持 esp_http_client_cancel_request()
 * 5. 保留 postStream() - 同步版本，向后兼容
 *
 * 工作流程：
 *
 *   postStreamAsync()
 *       │
 *       ├─► 生成 requestId
 *       ├─► 创建 RequestContext
 *       ├─► 创建 StreamTaskParams
 *       ├─► xTaskCreate(streamTaskFunction)
 *       └─► return requestId （立即返回）
 *
 *   streamTaskFunction() [独立任务]
 *       │
 *       ├─► 调用 executeStreamRequest()
 *       ├─► delete params
 *       └─► vTaskDelete(NULL)
 *
 *   executeStreamRequest()
 *       │
 *       ├─► esp_http_client_init()
 *       ├─► esp_http_client_open()
 *       ├─► esp_http_client_write()
 *       ├─► while (!cancelled) { esp_http_client_read() }
 *       ├─► 调用回调
 *       └─► 清理资源
 */

#include "sse_client.h"
#include "esp_http_client.h"
#include "esp_crt_bundle.h"
#include "esp_log.h"
#include <cstring>
#include <sstream>
#include <chrono>
#include <cinttypes>

static const char* TAG = "SSEClient";

namespace ai_sdk {

// ============================================================================
// 构造与析构
// ============================================================================

SSEClient::SSEClient() = default;

SSEClient::~SSEClient() {
    // 取消所有活跃请求
    // 注意：不在这里删除任务，让任务检测到 cancelled 后自行退出
    std::lock_guard<std::mutex> lock(mutex_);
    for (auto& pair : active_requests_) {
        pair.second->cancelled = true;
        // 调用取消 API 中断底层连接
        if (pair.second->client_handle) {
            esp_http_client_cancel_request(
                static_cast<esp_http_client_handle_t>(pair.second->client_handle)
            );
        }
    }
}

// ============================================================================
// 公开方法
// ============================================================================

std::string SSEClient::generateRequestId() {
    // 使用时间戳和随机数生成唯一 ID
    auto now = std::chrono::system_clock::now();
    auto timestamp = std::chrono::duration_cast<std::chrono::milliseconds>(
        now.time_since_epoch()).count();
    
    std::stringstream ss;
    ss << "sse_" << timestamp << "_" << (rand() % 10000);
    return ss.str();
}

// ============================================================================
// 【新增】异步发送流式 POST 请求
// ============================================================================

std::string SSEClient::postStreamAsync(
    const std::string& url,
    const std::string& body,
    const std::map<std::string, std::string>& headers,
    DataCallback onData,
    ErrorCallback onError,
    DoneCallback onDone) {
    
    // ========================================================================
    // 步骤 1：生成唯一请求 ID
    // ========================================================================
    std::string requestId = generateRequestId();
    
    // ========================================================================
    // 步骤 2：创建请求上下文
    // ========================================================================
    auto context = std::make_shared<RequestContext>();
    context->request_id = requestId;
    addRequest(requestId, context);
    
    ESP_LOGI(TAG, "[Async] Starting SSE request: %s", requestId.c_str());
    
    // ========================================================================
    // 步骤 3：创建任务参数
    // 注意：使用 new 分配内存，由任务函数负责 delete
    // ========================================================================
    auto* params = new StreamTaskParams{
        url,                    // 复制 URL（避免引用悬空）
        body,                   // 复制请求体
        headers,                // 复制请求头
        onData,                 // 数据回调
        onError,                // 错误回调
        onDone,                 // 完成回调
        context,                // 共享上下文（shared_ptr）
        this                    // SSEClient 实例指针
    };
    
    // ========================================================================
    // 步骤 4：创建 FreeRTOS 任务
    // ========================================================================
    BaseType_t ret = xTaskCreate(
        streamTaskFunction,     // 任务函数
        "sse_stream",           // 任务名称（调试用）
        8192,                   // 栈大小（8KB，HTTPS 需要较大栈）
        params,                 // 任务参数
        5,                      // 优先级（中等）
        &context->task_handle   // 任务句柄（用于管理）
    );
    
    if (ret != pdPASS) {
        ESP_LOGE(TAG, "[Async] Failed to create SSE stream task");
        delete params;
        removeRequest(requestId);
        if (onError) {
            onError("Failed to create stream task");
        }
        return "";
    }
    
    // ========================================================================
    // 步骤 5：立即返回 requestId（核心改动！）
    // 调用者可以用这个 ID 来取消请求
    // ========================================================================
    ESP_LOGI(TAG, "[Async] Task created successfully, requestId: %s", requestId.c_str());
    return requestId;
}

// ============================================================================
// 【保留】同步发送流式 POST 请求（向后兼容）
// ============================================================================

std::string SSEClient::postStream(
    const std::string& url,
    const std::string& body,
    const std::map<std::string, std::string>& headers,
    DataCallback onData,
    ErrorCallback onError,
    DoneCallback onDone) {
    
    // 使用信号量等待任务完成
    SemaphoreHandle_t done_sem = xSemaphoreCreateBinary();
    if (done_sem == nullptr) {
        ESP_LOGE(TAG, "[Sync] Failed to create semaphore");
        if (onError) {
            onError("Failed to create semaphore");
        }
        return "";
    }
    
    // ========================================================================
    // 包装完成回调：在原回调后释放信号量
    // ========================================================================
    auto wrappedDone = [done_sem, onDone]() {
        if (onDone) {
            onDone();
        }
        xSemaphoreGive(done_sem);
    };
    
    // ========================================================================
    // 包装错误回调：在原回调后也需要释放信号量
    // 否则同步调用会永远阻塞
    // ========================================================================
    auto wrappedError = [done_sem, onError](const std::string& error) {
        if (onError) {
            onError(error);
        }
        xSemaphoreGive(done_sem);
    };
    
    // ========================================================================
    // 调用异步版本
    // ========================================================================
    std::string requestId = postStreamAsync(url, body, headers, onData, wrappedError, wrappedDone);
    
    if (!requestId.empty()) {
        // 等待完成（最长 5 分钟）
        // 注意：这里会阻塞调用线程
        xSemaphoreTake(done_sem, pdMS_TO_TICKS(300000));
    }
    
    vSemaphoreDelete(done_sem);
    return requestId;
}

// ============================================================================
// 【新增】FreeRTOS 任务函数
// ============================================================================

void SSEClient::streamTaskFunction(void* arg) {
    auto* params = static_cast<StreamTaskParams*>(arg);
    
    ESP_LOGI(TAG, "[Task] Stream task started: %s", 
             params->context->request_id.c_str());
    
    // 执行核心请求逻辑
    params->sse_client->executeStreamRequest(params);
    
    // 清理参数内存
    delete params;
    
    ESP_LOGI(TAG, "[Task] Stream task exiting");
    
    // 删除任务自身
    vTaskDelete(NULL);
}

// ============================================================================
// 【新增】执行流式请求的核心逻辑
// 从原 postStream() 中提取，主要改动：
// 1. 参数通过 StreamTaskParams 传入
// 2. 增加取消检测
// ============================================================================

void SSEClient::executeStreamRequest(StreamTaskParams* params) {
    // 从参数中提取所需变量
    const std::string& url = params->url;
    const std::string& body = params->body;
    const auto& headers = params->headers;
    auto& onData = params->onData;
    auto& onError = params->onError;
    auto& onDone = params->onDone;
    auto& context = params->context;
    const std::string& requestId = context->request_id;

    // ========================================================================
    // 输出请求日志（与 HTTPClient 保持一致）
    // ========================================================================
    ESP_LOGD(TAG, "request httpUrl: %s", url.c_str());
    ESP_LOGD(TAG, "request headers:");
    for (const auto& header : headers) {
        ESP_LOGD(TAG, "  %s: %s", header.first.c_str(), header.second.c_str());
    }
    ESP_LOGD(TAG, "request body: %s", body.c_str());

    // ========================================================================
    // 配置 HTTP 客户端
    // ========================================================================
    esp_http_client_config_t config = {};
    config.url = url.c_str();
    config.method = HTTP_METHOD_POST;
    config.timeout_ms = 60000;  // 60 秒超时
    config.buffer_size = 2048;
    config.buffer_size_tx = 2048;
    // TLS 严格校验配置（与 HTTPClient 保持一致）：
    // - 使用系统证书包验证证书链
    // - 保持主机名校验开启
    config.transport_type = HTTP_TRANSPORT_OVER_SSL;
    config.crt_bundle_attach = esp_crt_bundle_attach;
    config.skip_cert_common_name_check = false;

    esp_http_client_handle_t client = esp_http_client_init(&config);
    if (client == nullptr) {
        ESP_LOGE(TAG, "Failed to init HTTP client");
        if (onError) {
            onError("Failed to init HTTP client");
        }
        removeRequest(requestId);
        return;
    }

    // 保存句柄用于取消
    context->client_handle = client;

    // ========================================================================
    // 设置请求头
    // ========================================================================
    for (const auto& header : headers) {
        esp_http_client_set_header(client, header.first.c_str(), header.second.c_str());
    }
    esp_http_client_set_header(client, "Accept", "text/event-stream");
    esp_http_client_set_header(client, "Cache-Control", "no-cache");

    // ========================================================================
    // 打开连接
    // ========================================================================
    int body_len = static_cast<int>(body.length());
    esp_err_t err = esp_http_client_open(client, body_len);
    if (err != ESP_OK) {
        ESP_LOGE(TAG, "Failed to open HTTP connection: %s", esp_err_to_name(err));
        if (onError) {
            onError(std::string("Failed to open connection: ") + esp_err_to_name(err));
        }
        esp_http_client_cleanup(client);
        removeRequest(requestId);
        return;
    }

    ESP_LOGD(TAG, "HTTP_EVENT_ON_CONNECTED");

    // ========================================================================
    // 发送请求体
    // ========================================================================
    int wlen = esp_http_client_write(client, body.c_str(), body_len);
    if (wlen < 0) {
        ESP_LOGE(TAG, "Failed to write request body");
        if (onError) {
            onError("Failed to write request body");
        }
        esp_http_client_close(client);
        esp_http_client_cleanup(client);
        removeRequest(requestId);
        return;
    }

    // ========================================================================
    // 获取响应头
    // ========================================================================
    int64_t content_len = esp_http_client_fetch_headers(client);
    int status_code = esp_http_client_get_status_code(client);
    ESP_LOGI(TAG, "HTTP status: %d, content_len: %" PRId64, status_code, content_len);

    // 输出响应头日志
    char* header_value = nullptr;
    if (esp_http_client_get_header(client, "Content-Type", &header_value) == ESP_OK && header_value) {
        ESP_LOGD(TAG, "HTTP_EVENT_ON_HEADER, key=Content-Type, value=%s", header_value);
    }
    if (esp_http_client_get_header(client, "Connection", &header_value) == ESP_OK && header_value) {
        ESP_LOGD(TAG, "HTTP_EVENT_ON_HEADER, key=Connection, value=%s", header_value);
    }
    if (esp_http_client_get_header(client, "Date", &header_value) == ESP_OK && header_value) {
        ESP_LOGD(TAG, "HTTP_EVENT_ON_HEADER, key=Date, value=%s", header_value);
    }
    if (esp_http_client_get_header(client, "Server", &header_value) == ESP_OK && header_value) {
        ESP_LOGD(TAG, "HTTP_EVENT_ON_HEADER, key=Server, value=%s", header_value);
    }
    if (esp_http_client_get_header(client, "X-Request-Id", &header_value) == ESP_OK && header_value) {
        ESP_LOGD(TAG, "HTTP_EVENT_ON_HEADER, key=X-Request-Id, value=%s", header_value);
    }

    if (status_code != 200) {
        ESP_LOGE(TAG, "HTTP error: %d", status_code);
        if (onError) {
            std::stringstream ss;
            ss << "HTTP error: " << status_code;
            onError(ss.str());
        }
        esp_http_client_close(client);
        esp_http_client_cleanup(client);
        removeRequest(requestId);
        return;
    }

    // ========================================================================
    // 核心：流式读取数据
    // ========================================================================
    const size_t buffer_size = 1024;
    char* buffer = new char[buffer_size];
    std::string accumulated_data;
    bool done = false;

    while (!done && !context->cancelled) {
        int read_len = esp_http_client_read(client, buffer, buffer_size - 1);
        
        if (read_len < 0) {
            // 检查是否是因为取消导致的错误
            if (context->cancelled) {
                ESP_LOGI(TAG, "Request was cancelled: %s", requestId.c_str());
            } else {
                ESP_LOGE(TAG, "Read error");
                if (onError) {
                    onError("Read error");
                }
            }
            break;
        } else if (read_len == 0) {
            // 数据读取完成，流结束
            ESP_LOGI(TAG, "Stream ended");
            
            // 检查是否有未处理的数据
            if (!accumulated_data.empty()) {
                ESP_LOGW(TAG, "Unparsed data remaining: %s", accumulated_data.c_str());
                if (onError) {
                    onError("Unexpected response: " + accumulated_data);
                }
            }
            break;
        }

        buffer[read_len] = '\0';
        
        // ========================================================================
        // 调试日志：打印原始接收数据（用于分析服务器返回格式）
        // ========================================================================
        ESP_LOGD(TAG, "Raw data received (%d bytes): %s", read_len, buffer);
        
        // 打印数据的十六进制表示（用于检查特殊字符如 \n）
        if (read_len > 0 && read_len <= 256) {
            std::string hex_dump;
            for (int i = 0; i < read_len && i < 64; i++) {
                char hex[4];
                snprintf(hex, sizeof(hex), "%02X ", (unsigned char)buffer[i]);
                hex_dump += hex;
            }
            ESP_LOGD(TAG, "Hex dump (first 64 bytes): %s", hex_dump.c_str());
        }
        
        accumulated_data += buffer;
        
        // 打印累积数据状态
        ESP_LOGD(TAG, "Accumulated data size: %zu bytes", accumulated_data.size());

        // 解析 SSE 数据
        done = parseSSEData(accumulated_data, onData);
    }

    delete[] buffer;

    // ========================================================================
    // 清理资源
    // ========================================================================
    ESP_LOGD(TAG, "HTTP_EVENT_DISCONNECTED");

    esp_http_client_close(client);
    esp_http_client_cleanup(client);
    removeRequest(requestId);

    // ========================================================================
    // 业务层回调通知
    // ========================================================================
    // 
    // 回调策略：
    // 1. 正常完成：调用 onDone() 通知业务层流已结束
    // 2. 主动取消：调用 onError() 传递特殊错误信息 "Request cancelled"
    //    - 业务层可通过检查 error 内容判断是用户主动取消还是真正错误
    //    - 这样业务层能及时更新 UI 状态（如隐藏"生成中..."提示）
    // 3. 网络错误：已在上方 read_len < 0 时调用 onError()
    //
    // 设计说明：
    // - 复用 onError 回调而非新增 onCancelled 回调，保持 API 向后兼容
    // - 特殊错误信息格式固定为 "Request cancelled"，便于业务层判断
    // ========================================================================
    if (context->cancelled) {
        // 用户主动取消：通过 onError 通知业务层
        if (onError) {
            onError("Request cancelled");
        }
    } else if (onDone) {
        // 正常完成：调用 onDone
        onDone();
    }

    ESP_LOGI(TAG, "SSE request completed: %s, cancelled: %s", 
             requestId.c_str(), context->cancelled ? "yes" : "no");
}

bool SSEClient::cancelRequest(const std::string& requestId) {
    auto context = getRequest(requestId);
    if (context) {
        // ====================================================================
        // 步骤 1：设置取消标志
        // 任务会在下次循环检测到此标志后退出
        // ====================================================================
        context->cancelled = true;
        
        // ====================================================================
        // 步骤 2：调用官方 API 取消请求
        // 这会中断底层 socket 操作，使 esp_http_client_read() 立即返回
        // ====================================================================
        if (context->client_handle) {
            esp_http_client_cancel_request(
                static_cast<esp_http_client_handle_t>(context->client_handle)
            );
        }
        
        ESP_LOGI(TAG, "Request cancelled: %s", requestId.c_str());
        return true;
    }
    
    ESP_LOGW(TAG, "Cancel failed, request not found: %s", requestId.c_str());
    return false;
}

// ============================================================================
// 私有方法
// ============================================================================

bool SSEClient::parseSSEData(std::string& buffer, DataCallback& onData) {
    // ========================================================================
    // SSE (Server-Sent Events) 数据格式
    // ========================================================================
    // 
    // 标准格式（带空格）：
    //   data: {json}\n\n
    //   data: [DONE]\n\n
    // 
    // 非标准格式（不带空格，某些服务器使用）：
    //   data:{json}\n\n
    //   data:[DONE]\n\n
    // 
    // 参考：OkHttp ServerSentEventReader.kt 同时支持两种格式
    //   - "data: ".encodeUtf8()  // 索引 3：带空格
    //   - "data:".encodeUtf8()   // 索引 4：不带空格
    // 
    // 参考：Android AIFoundationKitRequestApi.kt 的兼容处理
    //   if (it.startsWith("data:")) {
    //       result.substring(5).trim()  // 去除 "data:" (5个字符)
    //   }
    // ========================================================================

    size_t pos;
    
    // 调试日志：检查是否能找到 \n\n 分隔符
    pos = buffer.find("\n\n");
    ESP_LOGD(TAG, "parseSSEData: buffer size=%d, found \\n\\n at pos=%d", 
             (int)buffer.size(), (pos != std::string::npos) ? (int)pos : -1);

    while ((pos = buffer.find("\n\n")) != std::string::npos) {
        std::string event = buffer.substr(0, pos);
        buffer = buffer.substr(pos + 2);  // 直接修改原 buffer，移除已处理的数据
        
        // 调试日志：打印提取的事件内容
        ESP_LOGD(TAG, "parseSSEData: extracted event (%d bytes): %s", 
                 (int)event.size(), event.c_str());

        // ====================================================================
        // 查找 "data:" 前缀（同时支持带空格和不带空格两种格式）
        // ====================================================================
        // 
        // 优先查找 "data: "（标准格式，6个字符）
        // 如果找不到，再查找 "data:"（非标准格式，5个字符）
        // 
        // 这与 OkHttp 的处理方式一致：
        //   in 3..4 -> { source.readData(data) }
        //   索引 3 = "data: "，索引 4 = "data:"
        // ====================================================================
        
        const std::string prefix_with_space = "data: ";    // 标准格式（6个字符）
        const std::string prefix_no_space = "data:";       // 非标准格式（5个字符）
        
        size_t data_pos = std::string::npos;
        size_t prefix_len = 0;
        
        // 优先查找带空格的标准格式
        data_pos = event.find(prefix_with_space);
        if (data_pos != std::string::npos) {
            prefix_len = prefix_with_space.length();  // 6
            ESP_LOGD(TAG, "parseSSEData: found 'data: ' (with space) at pos=%d", (int)data_pos);
        } else {
            // 如果找不到，查找不带空格的非标准格式
            data_pos = event.find(prefix_no_space);
            if (data_pos != std::string::npos) {
                prefix_len = prefix_no_space.length();  // 5
                ESP_LOGD(TAG, "parseSSEData: found 'data:' (no space) at pos=%d", (int)data_pos);
            }
        }
        
        if (data_pos != std::string::npos) {
            std::string data = event.substr(data_pos + prefix_len);
            
            // 去除首尾空白
            while (!data.empty() && (data.front() == ' ' || data.front() == '\n' || data.front() == '\r')) {
                data.erase(0, 1);
            }
            while (!data.empty() && (data.back() == ' ' || data.back() == '\n' || data.back() == '\r')) {
                data.pop_back();
            }

            // 检查是否为结束标志
            if (data == "[DONE]") {
                ESP_LOGI(TAG, "Received [DONE] signal");
                return true;
            }

            // 回调数据
            if (onData && !data.empty()) {
                ESP_LOGD(TAG, "parseSSEData: calling onData with: %s", data.c_str());
                onData(data);
            }
        } else {
            // 没有找到任何 data 前缀，打印警告
            ESP_LOGW(TAG, "parseSSEData: no 'data:' prefix found in event: %s", event.c_str());
        }
    }

    return false;
}

void SSEClient::addRequest(const std::string& requestId, std::shared_ptr<RequestContext> context) {
    std::lock_guard<std::mutex> lock(mutex_);
    active_requests_[requestId] = context;
}

void SSEClient::removeRequest(const std::string& requestId) {
    std::lock_guard<std::mutex> lock(mutex_);
    active_requests_.erase(requestId);
}

std::shared_ptr<SSEClient::RequestContext> SSEClient::getRequest(const std::string& requestId) {
    std::lock_guard<std::mutex> lock(mutex_);
    auto it = active_requests_.find(requestId);
    if (it != active_requests_.end()) {
        return it->second;
    }
    return nullptr;
}

}  // namespace ai_sdk
