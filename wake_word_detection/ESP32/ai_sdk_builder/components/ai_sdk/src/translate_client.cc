/**
 * @file translate_client.cc
 * @brief 文本翻译客户端实现
 *
 * 处理文本翻译 API 请求，支持机器翻译 (v1) 和模型翻译 (v2)。
 *
 * ============================================================================
 * 内存安全改造说明（2025-01）
 * ============================================================================
 *
 * 问题：lambda 中捕获 this 指针，如果 TranslateClient 在回调执行前被销毁，
 *       this 成为悬空指针，调用成员函数会导致崩溃
 *
 * 修复方案：
 * 1. 将 parseResponse 改为静态函数，不依赖 this
 * 2. lambda 中不再捕获 this，只捕获必要的回调函数
 * 3. 响应解析逻辑完全在静态函数中完成
 *
 * 风险评估：
 * - TranslateClient 是 AIFoundationKitImpl 的成员，生命周期较长
 * - 但如果 AIFoundationKit 被销毁，TranslateClient 也会被销毁
 * - 此时如果有异步回调正在执行，访问 this 会导致崩溃
 */

#include "translate_client.h"
#include "api_config.h"
#include "assist_utils.h"
#include "http_client.h"
#include "ai_sdk/ai_assistant_manager.h"
#include "cjson_guard.h"
#include "esp_log.h"
#include <sstream>

static const char* TAG = "TranslateClient";

namespace ai_sdk {

// ============================================================================
// 构造与析构
// ============================================================================

TranslateClient::TranslateClient() = default;

TranslateClient::~TranslateClient() = default;

// ============================================================================
// 公开方法
// ============================================================================

void TranslateClient::sendRequest(
    const TranslationRequest& request,
    TranslateMode mode,
    SuccessCallback onSuccess,
    ErrorCallback onError) {
    
    // 参数校验
    if (request.targetLanguage.empty()) {
        if (onError) {
            onError("targetLanguage is required");
        }
        return;
    }
    
    if (request.originText.empty()) {
        if (onError) {
            onError("originText is required");
        }
        return;
    }
    
    if (request.targetLanguage == "auto") {
        if (onError) {
            onError("targetLanguage cannot be 'auto'");
        }
        return;
    }
    
    std::string url = buildUrl(mode);
    std::string body = buildBody(request);
    auto headers = buildHeaders();
    
    ESP_LOGI(TAG, "Sending translate request to: %s", url.c_str());
    
    // ========================================================================
    // 使用异步版本 postAsync()
    // ========================================================================
    // 改用 postAsync() 替代 post()，实现真正的异步非阻塞请求
    //
    // 改动说明：
    // - 原 post() 是同步阻塞的，会阻塞调用线程直到请求完成
    // - postAsync() 立即返回，请求在独立的 FreeRTOS 任务中执行
    // - 回调函数在独立任务中被调用，注意线程安全
    //
    // 内存安全改造：
    // - lambda 中不再捕获 this，避免 TranslateClient 销毁后访问悬空指针
    // - 使用静态函数 parseResponse() 解析响应
    // - httpClient 使用 shared_ptr，确保在回调期间 HTTPClient 有效
    auto httpClient = std::make_shared<HTTPClient>();
    
    httpClient->postAsync(url, body, headers,
        // 成功回调：不捕获 this，使用静态函数解析响应
        [onSuccess, onError, httpClient](const std::string& response) {
            // ================================================================
            // 错误响应检测
            // ================================================================
            // 服务器可能返回两种格式的响应：
            // 
            // 1. 正常响应（翻译成功）：
            //    {"code":0,"data":{"translateText":"...","sourceLanguage":"en","targetLanguage":"zh"},"msg":""}
            // 
            // 2. 错误响应（服务端业务错误）：
            //    {"code":500,"data":null,"msg":"系统异常"}
            // 
            // 检测逻辑：
            // - 如果响应中存在 "code" 字段且值不为 0 和 200，视为服务端业务错误
            // - 此时应调用 onError 回调，传递服务端返回的完整响应
            // - 如果是正常响应（code 为 0 或 200），则继续解析翻译结果
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
                    
                    // code 存在且不为 0 和 200，视为服务端业务错误
                    if (codeValue != 0 && codeValue != 200) {
                        // 提取错误信息用于日志输出
                        cJSON* msg = cJSON_GetObjectItem(root, "msg");
                        const char* msgStr = (msg && cJSON_IsString(msg)) ? msg->valuestring : "Unknown server error";
                        ESP_LOGE(TAG, "Server error: code=%d, msg=%s", codeValue, msgStr);
                        cJSON_Delete(root);
                        if (onError) {
                            // 返回服务端完整响应给业务层，便于业务层自行解析处理
                            onError(response);
                        }
                        return;
                    }
                }
                cJSON_Delete(root);
            }
            
            // 正常响应处理：使用静态函数解析翻译结果
            TranslateResponse resp;
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

// ============================================================================
// 私有方法
// ============================================================================

std::string TranslateClient::buildUrl(TranslateMode mode) {
    std::string baseUrl = ApiConfig::TERMINAL_INTELLIGENT_SERVICE_PLATFORM_BASE_URL_TEST;
    
    if (mode == TranslateMode::MACHINE) {
        return baseUrl + "/device-api/ai/v1/text-translate";
    } else {
        return baseUrl + "/device-api/ai/v2/text-translate";
    }
}

std::map<std::string, std::string> TranslateClient::buildHeaders() {
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

std::string TranslateClient::buildBody(const TranslationRequest& request) {
    cJSON* root = cJSON_CreateObject();
    cJSONGuard guard(root);
    
    cJSON_AddStringToObject(root, "targetLanguage", request.targetLanguage.c_str());
    cJSON_AddStringToObject(root, "originText", request.originText.c_str());
    cJSON_AddStringToObject(root, "sourceLanguage", request.sourceLanguage.c_str());
    
    char* json_str = cJSON_PrintUnformatted(root);
    std::string result(json_str);
    cJSON_free(json_str);
    
    return result;
}

bool TranslateClient::parseResponse(const std::string& json, TranslateResponse& response) {
    cJSON* root = cJSON_Parse(json.c_str());
    if (root == nullptr) {
        ESP_LOGE(TAG, "Failed to parse JSON: %s", json.c_str());
        return false;
    }
    cJSONGuard guard(root);
    
    // code
    cJSON* code = cJSON_GetObjectItem(root, "code");
    if (cJSON_IsNumber(code)) {
        response.code = code->valueint;
    }
    
    // msg
    cJSON* msg = cJSON_GetObjectItem(root, "msg");
    if (cJSON_IsString(msg)) {
        response.msg = msg->valuestring;
    }
    
    // data
    cJSON* data = cJSON_GetObjectItem(root, "data");
    if (cJSON_IsObject(data)) {
        // translateText
        cJSON* translateText = cJSON_GetObjectItem(data, "translateText");
        if (cJSON_IsString(translateText)) {
            response.data.translateText = translateText->valuestring;
        }
        
        // sourceLanguage
        cJSON* sourceLanguage = cJSON_GetObjectItem(data, "sourceLanguage");
        if (cJSON_IsString(sourceLanguage)) {
            response.data.sourceLanguage = sourceLanguage->valuestring;
        }
        
        // targetLanguage
        cJSON* targetLanguage = cJSON_GetObjectItem(data, "targetLanguage");
        if (cJSON_IsString(targetLanguage)) {
            response.data.targetLanguage = targetLanguage->valuestring;
        }
    }
    
    return true;
}

}  // namespace ai_sdk
