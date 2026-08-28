/**
 * @file http_client.cc
 * @brief HTTP 客户端实现（异步非阻塞版本）
 *
 * ============================================================================
 * 异步化改造说明（2025-01）
 * ============================================================================
 *
 * 核心改动：
 * 1. 新增 getAsync()/postAsync() - 异步版本，立即返回 requestId
 * 2. 新增 httpTaskFunction() - FreeRTOS 任务函数
 * 3. 新增 executeHttpRequest() - 核心请求逻辑
 * 4. 新增 cancelRequest() - 支持取消请求
 * 5. 保留 get()/post() - 同步版本，向后兼容
 * 6. 新增构造函数/析构函数
 * 7. 新增请求管理方法
 *
 * 工作流程：
 *
 *   postAsync()
 *       │
 *       ├─► 生成 requestId
 *       ├─► 创建 HTTPContext
 *       ├─► 创建 HttpTaskParams
 *       ├─► xTaskCreate(httpTaskFunction)
 *       └─► return requestId （立即返回）
 *
 *   httpTaskFunction() [独立任务]
 *       │
 *       ├─► 调用 executeHttpRequest()
 *       ├─► delete params
 *       └─► vTaskDelete(NULL)
 */

// 内部头文件（位于 src/include/）
#include "http_client.h"
#include "esp_log.h"
#include "esp_crt_bundle.h"
#include <cstring>
#include <sstream>
#include <chrono>

namespace ai_sdk {

static const char* TAG = "HTTPClient";

// ============================================================================
// 构造与析构
// ============================================================================

HTTPClient::HTTPClient() = default;

HTTPClient::~HTTPClient() {
    // 取消所有活跃请求
    std::lock_guard<std::mutex> lock(mutex_);
    for (auto& pair : active_requests_) {
        pair.second->cancelled = true;
        if (pair.second->client_handle) {
            esp_http_client_cancel_request(
                static_cast<esp_http_client_handle_t>(pair.second->client_handle)
            );
        }
    }
}

// ============================================================================
// 请求 ID 生成
// ============================================================================

std::string HTTPClient::generateRequestId() {
    auto now = std::chrono::system_clock::now();
    auto timestamp = std::chrono::duration_cast<std::chrono::milliseconds>(
        now.time_since_epoch()).count();
    
    std::stringstream ss;
    ss << "http_" << timestamp << "_" << (rand() % 10000);
    return ss.str();
}

// ============================================================================
// HTTP 事件处理器
// ============================================================================

esp_err_t HTTPClient::http_event_handler(esp_http_client_event_t *evt) {
    HTTPContext* ctx = static_cast<HTTPContext*>(evt->user_data);

    if (!ctx) {
        return ESP_FAIL;
    }

    // 检查是否已取消
    if (ctx->cancelled) {
        return ESP_FAIL;
    }

    switch(evt->event_id) {
        case HTTP_EVENT_ERROR:
            // HTTP连接错误
            ESP_LOGE(TAG, "HTTP_EVENT_ERROR");
            if (ctx->on_error && !ctx->cancelled) {
                ctx->on_error("HTTP_EVENT_ERROR");
            }
            return ESP_FAIL;

        case HTTP_EVENT_ON_CONNECTED:
            // HTTP连接成功
            ESP_LOGD(TAG, "HTTP_EVENT_ON_CONNECTED");
            break;

        case HTTP_EVENT_ON_HEADER:
            // 接收到HTTP响应头
            ESP_LOGD(TAG, "HTTP_EVENT_ON_HEADER, key=%s, value=%s",
                     evt->header_key, evt->header_value);
            break;

        case HTTP_EVENT_ON_DATA:
            // 接收到HTTP响应体数据
            // 将数据追加到response_data中，支持分块接收
            if (evt->data_len > 0 && !ctx->cancelled) {
                ctx->response_data.append(static_cast<char*>(evt->data), evt->data_len);
            }
            break;

        case HTTP_EVENT_ON_FINISH:
            // HTTP请求完成
            ESP_LOGD(TAG, "HTTP_EVENT_ON_FINISH");
            ESP_LOGD(TAG, "response: %s", ctx->response_data.c_str());
            if (ctx->on_success && !ctx->cancelled) {
                ctx->on_success(ctx->response_data);
            }
            break;

        case HTTP_EVENT_DISCONNECTED:
            // HTTP连接断开
            ESP_LOGD(TAG, "HTTP_EVENT_DISCONNECTED");
            // 如果没有接收到任何数据且未取消，调用错误回调
            if (ctx->on_error && ctx->response_data.empty() && !ctx->cancelled) {
                ctx->on_error("HTTP_EVENT_DISCONNECTED");
            }
            break;

        case HTTP_EVENT_HEADERS_SENT:
            break;

        case HTTP_EVENT_REDIRECT:
            break;

        default:
            break;
    }

    return ESP_OK;
}

// ============================================================================
// 【新增】异步发送 GET 请求
// ============================================================================

std::string HTTPClient::getAsync(
    const std::string& url,
    const std::map<std::string, std::string>& headers,
    ResponseCallback onSuccess,
    ErrorCallback onError
) {
    // 生成唯一请求 ID
    std::string requestId = generateRequestId();
    
    // 创建请求上下文
    auto ctx = std::make_shared<HTTPContext>();
    ctx->on_success = onSuccess;
    ctx->on_error = onError;
    ctx->client = this;
    ctx->request_id = requestId;
    addRequest(requestId, ctx);
    
    ESP_LOGI(TAG, "[Async] Starting GET request: %s", requestId.c_str());
    
    // 创建任务参数
    auto* params = new HttpTaskParams{
        url,                    // URL
        "",                     // body（GET 请求为空）
        headers,                // 请求头
        onSuccess,              // 成功回调
        onError,                // 错误回调
        ctx,                    // 上下文
        this,                   // HTTPClient 实例
        false                   // 不是 POST
    };
    
    // 创建 FreeRTOS 任务
    TaskHandle_t task_handle = nullptr;
    BaseType_t ret = xTaskCreate(
        httpTaskFunction,       // 任务函数
        "http_get",             // 任务名称
        8192,                   // 栈大小
        params,                 // 参数
        5,                      // 优先级
        &task_handle            // 任务句柄
    );
    
    if (ret != pdPASS) {
        ESP_LOGE(TAG, "[Async] Failed to create HTTP GET task");
        delete params;
        removeRequest(requestId);
        if (onError) {
            onError("Failed to create HTTP task");
        }
        return "";
    }
    
    ESP_LOGI(TAG, "[Async] GET task created, requestId: %s", requestId.c_str());
    return requestId;
}

// ============================================================================
// 【新增】异步发送 POST 请求
// ============================================================================

std::string HTTPClient::postAsync(
    const std::string& url,
    const std::string& body,
    const std::map<std::string, std::string>& headers,
    ResponseCallback onSuccess,
    ErrorCallback onError
) {
    // 生成唯一请求 ID
    std::string requestId = generateRequestId();
    
    // 创建请求上下文
    auto ctx = std::make_shared<HTTPContext>();
    ctx->on_success = onSuccess;
    ctx->on_error = onError;
    ctx->client = this;
    ctx->request_id = requestId;
    addRequest(requestId, ctx);
    
    ESP_LOGI(TAG, "[Async] Starting POST request: %s", requestId.c_str());
    
    // 创建任务参数
    auto* params = new HttpTaskParams{
        url,                    // URL
        body,                   // 请求体
        headers,                // 请求头
        onSuccess,              // 成功回调
        onError,                // 错误回调
        ctx,                    // 上下文
        this,                   // HTTPClient 实例
        true                    // 是 POST
    };
    
    // 创建 FreeRTOS 任务
    TaskHandle_t task_handle = nullptr;
    BaseType_t ret = xTaskCreate(
        httpTaskFunction,       // 任务函数
        "http_post",            // 任务名称
        8192,                   // 栈大小
        params,                 // 参数
        5,                      // 优先级
        &task_handle            // 任务句柄
    );
    
    if (ret != pdPASS) {
        ESP_LOGE(TAG, "[Async] Failed to create HTTP POST task");
        delete params;
        removeRequest(requestId);
        if (onError) {
            onError("Failed to create HTTP task");
        }
        return "";
    }
    
    ESP_LOGI(TAG, "[Async] POST task created, requestId: %s", requestId.c_str());
    return requestId;
}

// ============================================================================
// 【保留】同步发送 GET 请求（向后兼容）
// ============================================================================

void HTTPClient::get(
    const std::string& url,
    const std::map<std::string, std::string>& headers,
    ResponseCallback onSuccess,
    ErrorCallback onError
) {
    // 使用信号量等待任务完成
    SemaphoreHandle_t done_sem = xSemaphoreCreateBinary();
    if (done_sem == nullptr) {
        ESP_LOGE(TAG, "[Sync] Failed to create semaphore");
        if (onError) {
            onError("Failed to create semaphore");
        }
        return;
    }
    
    // 包装成功回调
    auto wrappedSuccess = [done_sem, onSuccess](const std::string& response) {
        if (onSuccess) {
            onSuccess(response);
        }
        xSemaphoreGive(done_sem);
    };
    
    // 包装错误回调
    auto wrappedError = [done_sem, onError](const std::string& error) {
        if (onError) {
            onError(error);
        }
        xSemaphoreGive(done_sem);
    };
    
    // 调用异步版本
    std::string requestId = getAsync(url, headers, wrappedSuccess, wrappedError);
    
    if (!requestId.empty()) {
        // 等待完成（最长 30 秒）
        xSemaphoreTake(done_sem, pdMS_TO_TICKS(30000));
    }
    
    vSemaphoreDelete(done_sem);
}

// ============================================================================
// 【保留】同步发送 POST 请求（向后兼容）
// ============================================================================

void HTTPClient::post(
    const std::string& url,
    const std::string& body,
    const std::map<std::string, std::string>& headers,
    ResponseCallback onSuccess,
    ErrorCallback onError
) {
    // 使用信号量等待任务完成
    SemaphoreHandle_t done_sem = xSemaphoreCreateBinary();
    if (done_sem == nullptr) {
        ESP_LOGE(TAG, "[Sync] Failed to create semaphore");
        if (onError) {
            onError("Failed to create semaphore");
        }
        return;
    }
    
    // 包装成功回调
    auto wrappedSuccess = [done_sem, onSuccess](const std::string& response) {
        if (onSuccess) {
            onSuccess(response);
        }
        xSemaphoreGive(done_sem);
    };
    
    // 包装错误回调
    auto wrappedError = [done_sem, onError](const std::string& error) {
        if (onError) {
            onError(error);
        }
        xSemaphoreGive(done_sem);
    };
    
    // 调用异步版本
    std::string requestId = postAsync(url, body, headers, wrappedSuccess, wrappedError);
    
    if (!requestId.empty()) {
        // 等待完成（最长 30 秒）
        xSemaphoreTake(done_sem, pdMS_TO_TICKS(30000));
    }
    
    vSemaphoreDelete(done_sem);
}

// ============================================================================
// 【新增】取消请求
// ============================================================================

bool HTTPClient::cancelRequest(const std::string& requestId) {
    auto ctx = getRequest(requestId);
    if (ctx) {
        // 设置取消标志
        ctx->cancelled = true;
        
        // 调用官方 API 取消请求
        if (ctx->client_handle) {
            esp_http_client_cancel_request(
                static_cast<esp_http_client_handle_t>(ctx->client_handle)
            );
        }
        
        ESP_LOGI(TAG, "Request cancelled: %s", requestId.c_str());
        return true;
    }
    
    ESP_LOGW(TAG, "Cancel failed, request not found: %s", requestId.c_str());
    return false;
}

// ============================================================================
// 【新增】FreeRTOS 任务函数
// ============================================================================

void HTTPClient::httpTaskFunction(void* arg) {
    auto* params = static_cast<HttpTaskParams*>(arg);
    
    ESP_LOGI(TAG, "[Task] HTTP task started: %s", 
             params->context->request_id.c_str());
    
    // 执行核心请求逻辑
    params->http_client->executeHttpRequest(params);
    
    // 清理参数内存
    delete params;
    
    ESP_LOGI(TAG, "[Task] HTTP task exiting");
    
    // 删除任务自身
    vTaskDelete(NULL);
}

// ============================================================================
// 【新增】执行 HTTP 请求的核心逻辑
// ============================================================================

void HTTPClient::executeHttpRequest(HttpTaskParams* params) {
    const std::string& url = params->url;
    const std::string& body = params->body;
    const auto& headers = params->headers;
    auto& ctx = params->context;
    const std::string& requestId = ctx->request_id;
    bool is_post = params->is_post;
    
    // 输出请求日志
    ESP_LOGD(TAG, "request httpUrl: %s", url.c_str());
    ESP_LOGD(TAG, "request headers:");
    for (const auto& header : headers) {
        ESP_LOGD(TAG, "  %s: %s", header.first.c_str(), header.second.c_str());
    }
    if (is_post) {
        ESP_LOGD(TAG, "request body: %s", body.c_str());
    }
    
    // ========================================================================
    // 配置 HTTP 客户端
    // ========================================================================
    esp_http_client_config_t config = {};
    config.url = url.c_str();
    config.event_handler = http_event_handler;
    config.timeout_ms = 15000;
    config.user_data = ctx.get();
    
    // TLS 严格校验配置：
    // - 使用 ESP-IDF 系统证书包校验服务器证书链
    // - 保留主机名校验（防止证书与域名不匹配）
    // 这样在 CONFIG_ESP_TLS_INSECURE=n / SKIP_SERVER_CERT_VERIFY=n 下可正常工作。
    config.transport_type = HTTP_TRANSPORT_OVER_SSL;
    config.crt_bundle_attach = esp_crt_bundle_attach;
    config.cert_pem = NULL;
    config.skip_cert_common_name_check = false;
    config.use_global_ca_store = false;
    
    // 初始化 HTTP 客户端
    esp_http_client_handle_t client = esp_http_client_init(&config);
    if (client == nullptr) {
        ESP_LOGE(TAG, "Failed to init HTTP client");
        if (ctx->on_error) {
            ctx->on_error("Failed to init HTTP client");
        }
        removeRequest(requestId);
        return;
    }
    
    // 保存句柄用于取消
    ctx->client_handle = client;
    
    // 设置请求头
    for (const auto& header : headers) {
        esp_http_client_set_header(client, header.first.c_str(), header.second.c_str());
    }
    
    // 设置请求方法和请求体
    if (is_post) {
        esp_http_client_set_method(client, HTTP_METHOD_POST);
        esp_http_client_set_post_field(client, body.c_str(), body.length());
    } else {
        esp_http_client_set_method(client, HTTP_METHOD_GET);
    }
    
    // ========================================================================
    // 执行 HTTP 请求
    // 注意：这里使用 esp_http_client_perform()，它是阻塞的
    // 但因为我们在独立任务中执行，不会阻塞调用者
    // ========================================================================
    esp_err_t err = esp_http_client_perform(client);
    
    if (err != ESP_OK && !ctx->cancelled) {
        ESP_LOGE(TAG, "HTTP perform failed: %s", esp_err_to_name(err));
        // 注意：错误已经通过事件处理器回调了
    }
    
    // 清理
    esp_http_client_cleanup(client);
    removeRequest(requestId);
    
    ESP_LOGI(TAG, "HTTP request completed: %s, cancelled: %s",
             requestId.c_str(), ctx->cancelled ? "yes" : "no");
}

// ============================================================================
// 请求管理方法
// ============================================================================

void HTTPClient::addRequest(const std::string& requestId, std::shared_ptr<HTTPContext> context) {
    std::lock_guard<std::mutex> lock(mutex_);
    active_requests_[requestId] = context;
}

void HTTPClient::removeRequest(const std::string& requestId) {
    std::lock_guard<std::mutex> lock(mutex_);
    active_requests_.erase(requestId);
}

std::shared_ptr<HTTPContext> HTTPClient::getRequest(const std::string& requestId) {
    std::lock_guard<std::mutex> lock(mutex_);
    auto it = active_requests_.find(requestId);
    if (it != active_requests_.end()) {
        return it->second;
    }
    return nullptr;
}

} // namespace ai_sdk