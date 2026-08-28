/**
 * @file content_summary_client.cc
 * @brief 内容摘要客户端实现
 *
 * 处理内容摘要 API 请求，支持流式和非流式模式。
 *
 * ============================================================================
 * 内存安全改造说明（2025-01）
 * ============================================================================
 *
 * 问题：lambda 中捕获 this 指针，如果 ContentSummaryClient 在回调执行前被销毁，
 *       this 成为悬空指针，调用成员函数会导致崩溃
 *
 * 修复方案：
 * 1. 将 parseResponse 改为静态函数，不依赖 this
 * 2. lambda 中不再捕获 this，只捕获必要的回调函数
 * 3. 响应解析逻辑完全在静态函数中完成
 */

#include "content_summary_client.h"
#include "api_config.h"
#include "assist_utils.h"
#include "http_client.h"
#include "ai_sdk/ai_assistant_manager.h"
#include "cjson_guard.h"
#include "esp_log.h"

static const char* TAG = "ContentSummaryClient";

namespace ai_sdk {

// ============================================================================
// 构造与析构
// ============================================================================

ContentSummaryClient::ContentSummaryClient()
    : sse_client_(std::make_unique<SSEClient>()) {
}

ContentSummaryClient::~ContentSummaryClient() = default;

// ============================================================================
// 公开方法
// ============================================================================

std::string ContentSummaryClient::sendRequest(
    const ContentSummaryRequest& request,
    SuccessCallback onSuccess,
    ErrorCallback onError) {
    
    if (request.stream) {
        return sendStreamRequest(request, onSuccess, onError);
    } else {
        sendNonStreamRequest(request, onSuccess, onError);
        return "";
    }
}

bool ContentSummaryClient::cancelRequest(const std::string& requestId) {
    if (sse_client_) {
        return sse_client_->cancelRequest(requestId);
    }
    return false;
}

// ============================================================================
// 私有方法
// ============================================================================

std::string ContentSummaryClient::buildUrl() {
    std::string baseUrl = ApiConfig::TERMINAL_INTELLIGENT_SERVICE_PLATFORM_BASE_URL_TEST;
    return baseUrl + ApiConfig::NOTE_SUMMARY_API;
}

std::map<std::string, std::string> ContentSummaryClient::buildHeaders() {
    std::map<std::string, std::string> headers;
    
    auto& config = AIAssistantManager::getInstance().config();
    int64_t ts = AssistUtils::timestamp();
    std::string sign = AssistUtils::signMd5(ts);
    
    headers["Content-Type"] = "application/json; charset=utf-8";
    headers["sign"] = sign;
    headers["deviceNo"] = config.deviceNo;
    headers["productId"] = config.productId;
    headers["productKey"] = config.productKey;
    headers["ts"] = std::to_string(ts);
    headers["deviceId"] = config.deviceId;
    headers["client-id"] = config.clientID;
    headers["packageName"] = config.packageName;
    
    return headers;
}

std::string ContentSummaryClient::buildBody(const ContentSummaryRequest& request) {
    cJSON* root = cJSON_CreateObject();
    cJSONGuard guard(root);
    
    // content - 必选参数
    cJSON_AddStringToObject(root, "content", request.content.c_str());
    
    // stream - 可选参数
    cJSON_AddBoolToObject(root, "stream", request.stream);
    
    // language - 可选参数
    if (!request.language.empty()) {
        cJSON_AddStringToObject(root, "language", request.language.c_str());
    }
    
    char* json_str = cJSON_PrintUnformatted(root);
    std::string result(json_str);
    cJSON_free(json_str);
    
    return result;
}

void ContentSummaryClient::sendNonStreamRequest(
    const ContentSummaryRequest& request,
    SuccessCallback onSuccess,
    ErrorCallback onError) {
    
    std::string url = buildUrl();
    std::string body = buildBody(request);
    auto headers = buildHeaders();
    
    ESP_LOGI(TAG, "Sending non-stream request to: %s", url.c_str());
    
    // ========================================================================
    // 使用异步版本 postAsync()
    // ========================================================================
    // 内存安全改造：lambda 中不捕获 this
    // httpClient 使用 shared_ptr，确保在回调期间 HTTPClient 有效
    auto httpClient = std::make_shared<HTTPClient>();
    
    httpClient->postAsync(url, body, headers,
        // 成功回调：不捕获 this，使用静态函数解析响应
        [onSuccess, onError, httpClient](const std::string& response) {
            // ================================================================
            // 错误响应检测
            // ================================================================
            // 服务器可能返回两种格式的响应：
            // 
            // 1. 正常响应：
            //    {"msg":"success","data":{"content":"..."},"logId":"...","status":0}
            // 
            // 2. 错误响应（服务端业务错误）：
            //    {"code":500,"data":null,"msg":"系统异常"}
            // ================================================================
            cJSON* root = cJSON_Parse(response.c_str());
            if (root != nullptr) {
                cJSON* code = cJSON_GetObjectItem(root, "code");
                if (code != nullptr) {
                    // 获取 code 值（支持数字和字符串两种格式）
                    int codeValue = 0;
                    if (cJSON_IsNumber(code)) {
                        codeValue = code->valueint;
                    } else if (cJSON_IsString(code)) {
                        codeValue = atoi(code->valuestring);
                    }
                    
                    // code 存在且不为 200，视为服务端业务错误
                    if (codeValue != 0 && codeValue != 200) {
                        cJSON* msg = cJSON_GetObjectItem(root, "msg");
                        const char* msgStr = (msg && cJSON_IsString(msg)) ? msg->valuestring : "Unknown server error";
                        ESP_LOGE(TAG, "Server error: code=%d, msg=%s", codeValue, msgStr);
                        cJSON_Delete(root);
                        if (onError) {
                            // 返回服务端完整响应给业务层
                            onError(response);
                        }
                        return;
                    }
                }
                cJSON_Delete(root);
            }
            
            // 正常响应处理：使用静态函数解析
            ContentSummaryResponse resp;
            if (parseResponse(response, resp)) {
                if (onSuccess) {
                    onSuccess(resp);
                }
            } else {
                if (onError) {
                    onError("Failed to parse response");
                }
            }
        },
        // 错误回调：不捕获 this
        [onError, httpClient](const std::string& error) {
            ESP_LOGE(TAG, "Request failed: %s", error.c_str());
            if (onError) {
                onError(error);
            }
        });
}

std::string ContentSummaryClient::sendStreamRequest(
    const ContentSummaryRequest& request,
    SuccessCallback onSuccess,
    ErrorCallback onError) {
    
    std::string url = buildUrl();
    std::string body = buildBody(request);
    auto headers = buildHeaders();
    
    ESP_LOGI(TAG, "Sending stream request to: %s", url.c_str());
    
    // ========================================================================
    // 使用异步版本 postStreamAsync()
    // ========================================================================
    // 内存安全改造：lambda 中不捕获 this，使用静态函数解析响应
    return sse_client_->postStreamAsync(url, body, headers,
        // 数据回调：不捕获 this，使用静态函数解析响应
        [onSuccess, onError](const std::string& data) {
            // 打印原始 SSE 数据（DEBUG 级别）
            ESP_LOGD(TAG, "Raw SSE data: %s", data.c_str());
            
            // 使用静态函数解析每一条 SSE 数据
            ContentSummaryResponse resp;
            if (parseResponse(data, resp)) {
                if (onSuccess) {
                    onSuccess(resp);
                }
            }
        },
        // 错误回调：不捕获 this
        [onError](const std::string& error) {
            ESP_LOGE(TAG, "Stream error: %s", error.c_str());
            if (onError) {
                onError(error);
            }
        },
        // 完成回调：不捕获 this
        []() {
            ESP_LOGI(TAG, "Stream completed");
        });
}

bool ContentSummaryClient::parseResponse(const std::string& json, ContentSummaryResponse& response) {
    cJSON* root = cJSON_Parse(json.c_str());
    if (root == nullptr) {
        ESP_LOGE(TAG, "Failed to parse JSON: %s", json.c_str());
        return false;
    }
    cJSONGuard guard(root);
    
    // msg
    cJSON* msg = cJSON_GetObjectItem(root, "msg");
    if (cJSON_IsString(msg)) {
        response.msg = msg->valuestring;
    }
    
    // logId
    cJSON* logId = cJSON_GetObjectItem(root, "logId");
    if (cJSON_IsString(logId)) {
        response.logId = logId->valuestring;
    }
    
    // status
    cJSON* status = cJSON_GetObjectItem(root, "status");
    if (cJSON_IsNumber(status)) {
        response.status = status->valueint;
    }
    
    // data
    cJSON* data = cJSON_GetObjectItem(root, "data");
    if (cJSON_IsObject(data)) {
        cJSON* content = cJSON_GetObjectItem(data, "content");
        if (cJSON_IsString(content)) {
            response.data.content = content->valuestring;
        }
    }
    
    return true;
}

}  // namespace ai_sdk
