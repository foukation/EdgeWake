/**
 * @file chatbot_client.cc
 * @brief Chatbot 闲聊客户端实现
 *
 * 处理 Chatbot 闲聊 API 请求，支持流式和非流式模式。
 *
 * ============================================================================
 * 内存安全改造说明（2025-01）
 * ============================================================================
 *
 * 问题：lambda 中捕获 this 指针，如果 ChatbotClient 在回调执行前被销毁，
 *       this 成为悬空指针，调用成员函数会导致崩溃
 *
 * 修复方案：
 * 1. 将 parseResponse 改为静态函数，不依赖 this
 * 2. lambda 中不再捕获 this，只捕获必要的回调函数
 * 3. 响应解析逻辑完全在静态函数中完成
 *
 * 风险评估：
 * - ChatbotClient 是 AIFoundationKitImpl 的成员，生命周期较长
 * - 但如果 AIFoundationKit 被销毁，ChatbotClient 也会被销毁
 * - 此时如果有异步回调正在执行，访问 this 会导致崩溃
 */

#include "chatbot_client.h"
#include "api_config.h"
#include "assist_utils.h"
#include "http_client.h"
#include "ai_sdk/ai_assistant_manager.h"
#include "cjson_guard.h"
#include "esp_log.h"
#include <sstream>

static const char* TAG = "ChatbotClient";

namespace ai_sdk {

// ============================================================================
// 构造与析构
// ============================================================================

ChatbotClient::ChatbotClient()
    : sse_client_(std::make_unique<SSEClient>()) {
}

ChatbotClient::~ChatbotClient() = default;

// ============================================================================
// 公开方法
// ============================================================================

std::string ChatbotClient::sendRequest(
    const ChatbotCompletionRequest& request,
    SuccessCallback onSuccess,
    ErrorCallback onError) {
    
    if (request.stream) {
        return sendStreamRequest(request, onSuccess, onError);
    } else {
        sendNonStreamRequest(request, onSuccess, onError);
        return "";
    }
}

bool ChatbotClient::cancelRequest(const std::string& requestId) {
    if (sse_client_) {
        return sse_client_->cancelRequest(requestId);
    }
    return false;
}

// ============================================================================
// 私有方法
// ============================================================================

std::string ChatbotClient::buildUrl() {
    std::string baseUrl = ApiConfig::TERMINAL_INTELLIGENT_SERVICE_PLATFORM_BASE_URL_TEST;
    return baseUrl + "/device-api/ai/v2/chat/completions";
}

std::map<std::string, std::string> ChatbotClient::buildHeaders() {
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
    
    // Chatbot 专用请求头：授权令牌
    // 从 AIAssistConfig::token 读取用户设置的授权令牌
    // 用于 Chatbot 闲聊功能的服务端授权验证
    if (!config.token.empty()) {
        headers["authorization"] = "Bearer " + config.token;
    }
    
    return headers;
}

std::string ChatbotClient::buildBody(const ChatbotCompletionRequest& request) {
    cJSON* root = cJSON_CreateObject();
    cJSONGuard guard(root);
    
    // messages 数组
    cJSON* messages = cJSON_CreateArray();
    for (const auto& msg : request.messages) {
        cJSON* msgObj = cJSON_CreateObject();
        cJSON_AddStringToObject(msgObj, "role", msg.role.c_str());
        cJSON_AddStringToObject(msgObj, "content", msg.content.c_str());
        cJSON_AddItemToArray(messages, msgObj);
    }
    cJSON_AddItemToObject(root, "messages", messages);
    
    // model
    cJSON_AddStringToObject(root, "model", request.model.c_str());
    
    // stream
    cJSON_AddBoolToObject(root, "stream", request.stream);
    
    // temperature (可选)
    if (request.temperature >= 0) {
        cJSON_AddNumberToObject(root, "temperature", request.temperature);
    }
    
    // top_p (可选)
    if (request.top_p >= 0) {
        cJSON_AddNumberToObject(root, "top_p", request.top_p);
    }
    
    char* json_str = cJSON_PrintUnformatted(root);
    std::string result(json_str);
    cJSON_free(json_str);
    
    return result;
}

void ChatbotClient::sendNonStreamRequest(
    const ChatbotCompletionRequest& request,
    SuccessCallback onSuccess,
    ErrorCallback onError) {
    
    std::string url = buildUrl();
    std::string body = buildBody(request);
    auto headers = buildHeaders();
    
    ESP_LOGI(TAG, "Sending non-stream request to: %s", url.c_str());
    
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
    // 注意：httpClient 使用 shared_ptr，确保在回调执行期间对象有效
    // ========================================================================
    // 内存安全改造：lambda 中不捕获 this
    // ========================================================================
    // 使用静态函数 parseResponse() 解析响应，避免悬空指针问题
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
            // 1. 正常响应（Chatbot API 标准格式）：
            //    {"id":"...","object":"chat.completion","choices":[...],"usage":{...}}
            // 
            // 2. 错误响应（服务端业务错误）：
            //    {"code":500,"data":null,"msg":"系统异常"}
            // 
            // 检测逻辑：
            // - 如果响应中存在 "code" 字段且值不为 200，视为服务端业务错误
            // - 此时应调用 onError 回调，传递服务端返回的错误信息（msg 字段）
            // - 如果是正常响应（无 code 字段），则继续解析 Chatbot 响应格式
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
            
            // 正常响应处理：使用静态函数解析 Chatbot API 标准格式
            ChatbotCompletionResponse resp;
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

std::string ChatbotClient::sendStreamRequest(
    const ChatbotCompletionRequest& request,
    SuccessCallback onSuccess,
    ErrorCallback onError) {
    
    std::string url = buildUrl();
    std::string body = buildBody(request);
    auto headers = buildHeaders();
    
    ESP_LOGI(TAG, "Sending stream request to: %s", url.c_str());
    
    // ========================================================================
    // 使用异步版本 postStreamAsync()
    // ========================================================================
    // 改用 postStreamAsync() 替代 postStream()，实现真正的异步非阻塞流式请求
    //
    // 改动说明：
    // - 原 postStream() 是同步阻塞的，会阻塞调用线程直到流结束
    // - postStreamAsync() 立即返回 requestId，请求在独立的 FreeRTOS 任务中执行
    // - 调用者可以使用返回的 requestId 调用 cancelRequest() 取消请求
    // - 回调函数在独立任务中被调用，注意线程安全
    //
    // 核心优势：
    // 1. 调用线程不会被阻塞，可以继续处理其他任务
    // 2. requestId 立即返回，支持请求取消功能
    // 3. 回调驱动，数据到达时自动触发 onSuccess
    //
    // 内存安全改造：lambda 中不捕获 this，使用静态函数解析响应
    return sse_client_->postStreamAsync(url, body, headers,
        // 数据回调：不捕获 this，使用静态函数解析响应
        [onSuccess, onError](const std::string& data) {
            // 打印原始 SSE 数据
            ESP_LOGI(TAG, "Raw SSE data: %s", data.c_str());
            
            // 使用静态函数解析每一条 SSE 数据
            ChatbotCompletionResponse resp;
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

bool ChatbotClient::parseResponse(const std::string& json, ChatbotCompletionResponse& response) {
    cJSON* root = cJSON_Parse(json.c_str());
    if (root == nullptr) {
        ESP_LOGE(TAG, "Failed to parse JSON: %s", json.c_str());
        return false;
    }
    cJSONGuard guard(root);
    
    // id
    cJSON* id = cJSON_GetObjectItem(root, "id");
    if (cJSON_IsString(id)) {
        response.id = id->valuestring;
    }
    
    // object
    cJSON* object = cJSON_GetObjectItem(root, "object");
    if (cJSON_IsString(object)) {
        response.object = object->valuestring;
    }
    
    // created
    cJSON* created = cJSON_GetObjectItem(root, "created");
    if (cJSON_IsNumber(created)) {
        response.created = static_cast<int64_t>(created->valuedouble);
    }
    
    // model
    cJSON* model = cJSON_GetObjectItem(root, "model");
    if (cJSON_IsString(model)) {
        response.model = model->valuestring;
    }
    
    // choices
    cJSON* choices = cJSON_GetObjectItem(root, "choices");
    if (cJSON_IsArray(choices)) {
        int size = cJSON_GetArraySize(choices);
        for (int i = 0; i < size; i++) {
            cJSON* choiceItem = cJSON_GetArrayItem(choices, i);
            ChatbotChoice choice;
            
            // index
            cJSON* index = cJSON_GetObjectItem(choiceItem, "index");
            if (cJSON_IsNumber(index)) {
                choice.index = index->valueint;
            }
            
            // delta (流式)
            cJSON* delta = cJSON_GetObjectItem(choiceItem, "delta");
            if (cJSON_IsObject(delta)) {
                cJSON* role = cJSON_GetObjectItem(delta, "role");
                if (cJSON_IsString(role)) {
                    choice.delta.role = role->valuestring;
                }
                cJSON* content = cJSON_GetObjectItem(delta, "content");
                if (cJSON_IsString(content)) {
                    choice.delta.content = content->valuestring;
                }
            }
            
            // message (非流式)
            cJSON* message = cJSON_GetObjectItem(choiceItem, "message");
            if (cJSON_IsObject(message)) {
                cJSON* role = cJSON_GetObjectItem(message, "role");
                if (cJSON_IsString(role)) {
                    choice.message.role = role->valuestring;
                }
                cJSON* content = cJSON_GetObjectItem(message, "content");
                if (cJSON_IsString(content)) {
                    choice.message.content = content->valuestring;
                }
            }
            
            // finish_reason
            cJSON* finish_reason = cJSON_GetObjectItem(choiceItem, "finish_reason");
            if (cJSON_IsString(finish_reason)) {
                choice.finish_reason = finish_reason->valuestring;
            }
            
            response.choices.push_back(choice);
        }
    }
    
    // usage
    cJSON* usage = cJSON_GetObjectItem(root, "usage");
    if (cJSON_IsObject(usage)) {
        cJSON* prompt_tokens = cJSON_GetObjectItem(usage, "prompt_tokens");
        if (cJSON_IsNumber(prompt_tokens)) {
            response.usage.prompt_tokens = prompt_tokens->valueint;
        }
        cJSON* completion_tokens = cJSON_GetObjectItem(usage, "completion_tokens");
        if (cJSON_IsNumber(completion_tokens)) {
            response.usage.completion_tokens = completion_tokens->valueint;
        }
        cJSON* total_tokens = cJSON_GetObjectItem(usage, "total_tokens");
        if (cJSON_IsNumber(total_tokens)) {
            response.usage.total_tokens = total_tokens->valueint;
        }
    }
    
    return true;
}

}  // namespace ai_sdk
