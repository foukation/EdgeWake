/**
 * @file inside_rc_chat_client.cc
 * @brief 文本链路智能问答客户端实现
 *
 * 处理 insideRcChat API 请求，支持流式（SSE）和非流式（HTTP POST）模式。
 * 响应解析逻辑参考 asr_intelligent_dialogue.cc 中 inside_rc 消息的处理方式。
 *
 * ============================================================================
 * 内存安全说明
 * ============================================================================
 *
 * 问题：lambda 中捕获 this 指针，如果 InsideRcChatClient 在回调执行前被销毁，
 *       this 成为悬空指针，调用成员函数会导致崩溃
 *
 * 修复方案：
 * 1. 将 parseResponse 改为静态函数，不依赖 this
 * 2. lambda 中不再捕获 this，只捕获必要的回调函数
 * 3. 响应解析逻辑完全在静态函数中完成
 */

#include "inside_rc_chat_client.h"
#include "api_config.h"
#include "assist_utils.h"
#include "http_client.h"
#include "ai_sdk/ai_assistant_manager.h"
#include "cjson_guard.h"
#include "esp_log.h"
#include <sstream>

static const char* TAG = "InsideRcChatClient";

namespace ai_sdk {

// ============================================================================
// 构造与析构
// ============================================================================

InsideRcChatClient::InsideRcChatClient()
    : sse_client_(std::make_unique<SSEClient>()) {
}

InsideRcChatClient::~InsideRcChatClient() = default;

// ============================================================================
// 公开方法
// ============================================================================

std::string InsideRcChatClient::sendRequest(
    const InsideRcChatRequest& request,
    SuccessCallback onSuccess,
    ErrorCallback onError) {

    if (request.stream) {
        return sendStreamRequest(request, onSuccess, onError);
    } else {
        sendNonStreamRequest(request, onSuccess, onError);
        return "";
    }
}

bool InsideRcChatClient::cancelRequest(const std::string& requestId) {
    if (sse_client_) {
        return sse_client_->cancelRequest(requestId);
    }
    return false;
}

// ============================================================================
// 私有方法
// ============================================================================

std::string InsideRcChatClient::buildUrl() {
    std::string baseUrl = ApiConfig::TERMINAL_INTELLIGENT_SERVICE_PLATFORM_BASE_URL_TEST;
    return baseUrl + ApiConfig::INSIDE_RC_CHAT_API;
}

std::map<std::string, std::string> InsideRcChatClient::buildHeaders() {
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

    // 注意：insideRcChat 不使用 Bearer token（与 Chatbot 不同）

    return headers;
}

std::string InsideRcChatClient::buildBody(const InsideRcChatRequest& request) {
    auto& config = AIAssistantManager::getInstance().config();

    cJSON* root = cJSON_CreateObject();
    cJSONGuard guard(root);

    // ========================================================================
    // 必选参数
    // ========================================================================

    cJSON_AddStringToObject(root, "qid", request.qid.c_str());
    cJSON_AddStringToObject(root, "third_user_id", request.third_user_id.c_str());
    cJSON_AddStringToObject(root, "cuid", request.cuid.c_str());

    // messages 数组
    cJSON* messages = cJSON_CreateArray();
    for (const auto& msg : request.messages) {
        cJSON* msgObj = cJSON_CreateObject();
        cJSON_AddStringToObject(msgObj, "role", msg.role.c_str());
        cJSON_AddStringToObject(msgObj, "content", msg.content.c_str());
        cJSON_AddItemToArray(messages, msgObj);
    }
    cJSON_AddItemToObject(root, "messages", messages);

    // ========================================================================
    // SDK 自动注入字段
    // ========================================================================

    // version / rc_version：从 AIAssistConfig::centralConfigVersion 取值
    if (!config.centralConfigVersion.empty()) {
        cJSON_AddStringToObject(root, "version", config.centralConfigVersion.c_str());

        // rc_version 为 int 类型
        int rc_version = 0;
        try {
            rc_version = std::stoi(config.centralConfigVersion);
        } catch (...) {
            // centralConfigVersion 非数字时使用默认值 0
        }
        cJSON_AddNumberToObject(root, "rc_version", rc_version);
    }

    // client_context：自动注入 SpeechState（TTS 配置）
    cJSON* client_context = cJSON_CreateArray();

    // 构建 SpeechState 对象
    cJSON* speech_state = cJSON_CreateObject();

    // header
    cJSON* ctx_header = cJSON_CreateObject();
    cJSON_AddStringToObject(ctx_header, "namespace", "ai.dueros.device_interface.voice_output");
    cJSON_AddStringToObject(ctx_header, "name", "SpeechState");
    cJSON_AddItemToObject(speech_state, "header", ctx_header);

    // payload（TTS 配置参数）
    cJSON* ctx_payload = cJSON_CreateObject();
    cJSON_AddNumberToObject(ctx_payload, "voiceId", config.dialogueTtsConfig.voiceId);
    cJSON_AddStringToObject(ctx_payload, "source", "baidu_tsn");

    cJSON* baidu_tsn = cJSON_CreateObject();
    cJSON_AddNumberToObject(baidu_tsn, "speed", config.dialogueTtsConfig.speed);
    cJSON_AddNumberToObject(baidu_tsn, "pitch", config.dialogueTtsConfig.pitch);
    cJSON_AddNumberToObject(baidu_tsn, "volume", config.dialogueTtsConfig.volume);
    cJSON_AddItemToObject(ctx_payload, "baidu_tsn", baidu_tsn);

    cJSON_AddItemToObject(speech_state, "payload", ctx_payload);
    cJSON_AddItemToArray(client_context, speech_state);

    cJSON_AddItemToObject(root, "client_context", client_context);

    // ========================================================================
    // 可选参数
    // ========================================================================

    // stream
    cJSON_AddBoolToObject(root, "stream", request.stream);

    // is_debug
    cJSON_AddNumberToObject(root, "is_debug", request.is_debug);

    // url（指定 bot 路径）
    if (!request.url.empty()) {
        cJSON_AddStringToObject(root, "url", request.url.c_str());
    }

    // params（透传参数）
    if (!request.params.empty()) {
        cJSON* params = cJSON_CreateObject();
        for (const auto& pair : request.params) {
            cJSON_AddStringToObject(params, pair.first.c_str(), pair.second.c_str());
        }
        cJSON_AddItemToObject(root, "params", params);
    }

    // client_ip
    if (!request.client_ip.empty()) {
        cJSON_AddStringToObject(root, "client_ip", request.client_ip.c_str());
    }

    // dialog_request_id
    if (!request.dialog_request_id.empty()) {
        cJSON_AddStringToObject(root, "dialog_request_id", request.dialog_request_id.c_str());
    }

    char* json_str = cJSON_PrintUnformatted(root);
    std::string result(json_str);
    cJSON_free(json_str);

    return result;
}

void InsideRcChatClient::sendNonStreamRequest(
    const InsideRcChatRequest& request,
    SuccessCallback onSuccess,
    ErrorCallback onError) {

    std::string url = buildUrl();
    std::string body = buildBody(request);
    auto headers = buildHeaders();

    ESP_LOGI(TAG, "发送非流式请求: %s", url.c_str());

    // ========================================================================
    // 内存安全改造：lambda 中不捕获 this，使用静态函数解析响应
    // httpClient 使用 shared_ptr，确保在回调期间 HTTPClient 有效
    // ========================================================================
    auto httpClient = std::make_shared<HTTPClient>();

    httpClient->postAsync(url, body, headers,
        // 成功回调：不捕获 this，使用静态函数解析响应
        [onSuccess, onError, httpClient](const std::string& response) {
            // 服务端对此接口的非流式响应仍以 SSE 格式返回（"data:{...}"），
            // 而流式路径由 SSEClient 负责剥离前缀，parseResponse 始终收到干净 JSON。
            // 此处手动剥离，与 Android 实现保持一致：
            //   AIFoundationKitRequestApi.kt: if (it.startsWith("data:")) substring(5).trim()
            const std::string kSSEPrefix = "data:";
            if (response.size() > kSSEPrefix.size() &&
                response.compare(0, kSSEPrefix.size(), kSSEPrefix) == 0) {
                std::string json = response.substr(kSSEPrefix.size());
                size_t start = json.find_first_not_of(" \t\r\n");
                parseResponse(start == std::string::npos ? "" : json.substr(start),
                              onSuccess, onError);
            } else {
                parseResponse(response, onSuccess, onError);
            }
        },
        // 错误回调：不捕获 this
        [onError, httpClient](const std::string& error) {
            ESP_LOGE(TAG, "请求失败: %s", error.c_str());
            if (onError) {
                onError(error);
            }
        });
}

std::string InsideRcChatClient::sendStreamRequest(
    const InsideRcChatRequest& request,
    SuccessCallback onSuccess,
    ErrorCallback onError) {

    std::string url = buildUrl();
    std::string body = buildBody(request);
    auto headers = buildHeaders();

    ESP_LOGI(TAG, "发送流式请求: %s", url.c_str());

    // ========================================================================
    // 内存安全改造：lambda 中不捕获 this，使用静态函数解析响应
    // ========================================================================
    return sse_client_->postStreamAsync(url, body, headers,
        // 数据回调：不捕获 this，使用静态函数解析每条 SSE 数据
        [onSuccess, onError](const std::string& data) {
            ESP_LOGD(TAG, "SSE 数据: %s", data.c_str());
            parseResponse(data, onSuccess, onError);
        },
        // 错误回调：不捕获 this
        [onError](const std::string& error) {
            ESP_LOGE(TAG, "流式请求错误: %s", error.c_str());
            if (onError) {
                onError(error);
            }
        },
        // 完成回调：不捕获 this
        []() {
            ESP_LOGI(TAG, "流式请求完成");
        });
}

// ============================================================================
// 静态解析方法
// ============================================================================

void InsideRcChatClient::parseResponse(
    const std::string& json,
    SuccessCallback onSuccess,
    ErrorCallback onError) {

    cJSON* root = cJSON_Parse(json.c_str());
    if (root == nullptr) {
        ESP_LOGE(TAG, "JSON 解析失败: %s", json.c_str());
        if (onError) {
            onError("JSON 解析失败");
        }
        return;
    }
    cJSONGuard guard(root);

    // ========================================================================
    // 错误响应检测
    // ========================================================================
    // 服务端返回 code 字段时，非 0 且非 200 视为业务错误
    cJSON* code = cJSON_GetObjectItem(root, "code");
    if (code != nullptr) {
        int codeValue = 0;
        if (cJSON_IsNumber(code)) {
            codeValue = code->valueint;
        } else if (cJSON_IsString(code)) {
            codeValue = atoi(code->valuestring);
        }

        if (codeValue != 0 && codeValue != 200) {
            cJSON* msg = cJSON_GetObjectItem(root, "msg");
            const char* msgStr = (msg && cJSON_IsString(msg)) ? msg->valuestring : "服务端错误";
            ESP_LOGE(TAG, "服务端错误: code=%d, msg=%s", codeValue, msgStr);
            if (onError) {
                onError(json);
            }
            return;
        }
    }

    // ========================================================================
    // 解析公共字段
    // ========================================================================
    std::string qid_str;
    int is_end_val = 0;

    cJSON* qid = cJSON_GetObjectItem(root, "qid");
    if (qid && cJSON_IsString(qid)) {
        qid_str = qid->valuestring;
    }

    cJSON* is_end = cJSON_GetObjectItem(root, "is_end");
    if (is_end && cJSON_IsNumber(is_end)) {
        is_end_val = is_end->valueint;
    }

    // ========================================================================
    // 遍历 data[] 指令数组
    // ========================================================================
    // 每个 Directive 构建一个 DialogueResult 并回调 onSuccess
    // 参考 asr_intelligent_dialogue.cc 中 inside_rc 消息的处理方式
    cJSON* directives = cJSON_GetObjectItem(root, "data");
    if (directives && cJSON_IsArray(directives)) {
        int directives_count = cJSON_GetArraySize(directives);
        for (int i = 0; i < directives_count; i++) {
            cJSON* directive = cJSON_GetArrayItem(directives, i);
            if (!directive) continue;

            cJSON* header = cJSON_GetObjectItem(directive, "header");
            cJSON* payload_obj = cJSON_GetObjectItem(directive, "payload");

            // 构建 DialogueResult
            DialogueResult dialogue_result;
            dialogue_result.qid = qid_str;
            dialogue_result.is_end = 0;  // 指令级别不设 is_end

            // 序列化 header 为 JSON 字符串
            if (header) {
                char* header_str = cJSON_PrintUnformatted(header);
                if (header_str) {
                    dialogue_result.header = header_str;
                    cJSON_free(header_str);
                }

                // 提取 name 作为便捷字段 directive
                cJSON* name = cJSON_GetObjectItem(header, "name");
                if (name && cJSON_IsString(name)) {
                    dialogue_result.directive = name->valuestring;
                }
            }

            // 序列化 payload 为 JSON 字符串
            if (payload_obj) {
                char* payload_str = cJSON_PrintUnformatted(payload_obj);
                if (payload_str) {
                    dialogue_result.payload = payload_str;
                    cJSON_free(payload_str);
                }
            }

            // 回调每个指令
            if (onSuccess) {
                onSuccess(dialogue_result);
            }
        }
    }

    // ========================================================================
    // is_end == 1 时解析 assistant_answer 并额外回调
    // ========================================================================
    if (is_end_val == 1) {
        std::string answer_content;

        cJSON* assistant_answer = cJSON_GetObjectItem(root, "assistant_answer");
        if (assistant_answer && cJSON_IsString(assistant_answer)) {
            std::string raw_answer = assistant_answer->valuestring;

            // assistant_answer 是 JSON 字符串，尝试提取其中的 content 字段
            cJSON* answer_json = cJSON_Parse(raw_answer.c_str());
            if (answer_json) {
                cJSON* content = cJSON_GetObjectItem(answer_json, "content");
                if (content && cJSON_IsString(content)) {
                    answer_content = content->valuestring;
                } else {
                    // 没有 content 字段，使用原始值
                    answer_content = raw_answer;
                }
                cJSON_Delete(answer_json);
            } else {
                // 不是 JSON 格式，直接使用原始值
                answer_content = raw_answer;
            }
        }

        ESP_LOGD(TAG, "is_end=1, answer: %s", answer_content.c_str());

        // 发送 is_end=1 的结果回调
        DialogueResult end_result;
        end_result.qid = qid_str;
        end_result.is_end = 1;
        end_result.assistant_answer_content = answer_content;

        if (onSuccess) {
            onSuccess(end_result);
        }
    }
}

}  // namespace ai_sdk
