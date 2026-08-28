/**
 * @file gateway_client.cc
 * @brief 网关客户端实现
 *
 * ============================================================================
 * 内存安全改造说明（2025-01）
 * ============================================================================
 *
 * 问题：lambda 中捕获 this 指针，如果 GatewayClient 在回调执行前被销毁，
 *       this 成为悬空指针，调用成员函数会导致崩溃
 *
 * 修复方案：
 * 1. 将 onResponse 改为静态函数，不依赖 this
 * 2. lambda 中不再捕获 this，只捕获必要的回调函数
 * 3. 响应解析逻辑完全在静态函数中完成
 */

// 内部头文件（位于 src/include/）
#include "gateway_client.h"
#include "api_config.h"
#include "http_client.h"

// 公开头文件（位于 include/ai_sdk/）
#include "ai_sdk/ai_assistant_manager.h"
#include "cJSON.h"
#include "esp_log.h"

namespace ai_sdk {

static const char* TAG = "GatewayClient";

/**
 * 获取网关信息
 *
 * 向云端发起HTTP GET请求获取代理配置。
 *
 * 工作原理：
 * 1. 创建HTTP GET请求到网关接口
 * 2. 设置Content-Type请求头
 * 3. 解析JSON响应，提取代理配置
 * 4. 如果status=1，自动更新全局ApiConfig配置
 *
 * 配置更新：
 * - 如果云端返回status=1（使用代理），自动设置：
 *   - ApiConfig::useAgent = true
 *   - ApiConfig::agentBaseUrl = 代理服务器地址
 *   - ApiConfig::apiToken = 认证令牌
 * - 后续的所有API请求会自动走代理
 *
 * 认证headers：
 * - 请求中会添加设备认证信息：
 *   - X-AI-VID: 产品ID (从AIAssistantManager获取)
 *   - X-AI-UID: 设备ID (从AIAssistantManager获取)
 *
 * ============================================================================
 * 内存安全说明
 * ============================================================================
 * 
 * lambda 中不再捕获 this，避免悬空指针问题：
 * - 成功回调：直接调用静态函数 parseGatewayResponse() 解析响应
 * - 错误回调：直接调用用户回调，无需访问成员
 *
 * @param onSuccess 成功回调，返回网关配置
 * @param onError 错误回调
 */
void GatewayClient::getGateWay(GatewayCallback onSuccess, std::function<void(const std::string&)> onError) {
    ESP_LOGI(TAG, "API: getGateWay");

    // 设置请求头
    std::map<std::string, std::string> headers;
    headers["Content-Type"] = "application/json; charset=utf-8";

    // 添加设备认证headers
    // 从AIAssistantManager获取设备配置
    const auto& config = AIAssistantManager::getInstance().config();
    if (!config.productId.empty()) {
        headers["X-AI-VID"] = config.productId;
    }
    if (!config.deviceId.empty()) {
        headers["X-AI-UID"] = config.deviceId;
    }

    // ========================================================================
    // 使用异步版本 getAsync()
    // ========================================================================
    // 改用 getAsync() 替代 get()，实现真正的异步非阻塞请求
    //
    // 改动说明：
    // - 原 get() 是同步阻塞的，会阻塞调用线程直到请求完成
    // - getAsync() 立即返回，请求在独立的 FreeRTOS 任务中执行
    // - 回调函数在独立任务中被调用，注意线程安全
    //
    // 内存安全改造：
    // - lambda 中不再捕获 this，避免 GatewayClient 销毁后访问悬空指针
    // - 使用静态函数 parseGatewayResponse() 解析响应
    // - http_client 使用 shared_ptr，确保在回调期间 HTTPClient 有效
    auto http_client = std::make_shared<HTTPClient>();
    http_client->getAsync(
        std::string(ApiConfig::TERMINAL_INTELLIGENT_SERVICE_PLATFORM_BASE_URL_TEST) + ApiConfig::GATEWAY_API,
        headers,
        // 成功回调：不捕获 this，使用静态函数解析响应
        [onSuccess, onError, http_client](const std::string& response) {
            // 输出响应日志（包含敏感信息，仅 DEBUG 级别输出）
            ESP_LOGD(TAG, "response: %s", response.c_str());
            // 请求成功，使用静态函数解析响应
            parseGatewayResponse(response, onSuccess, onError);
        },
        // 错误回调：不捕获 this，直接调用用户回调
        [onError, http_client](const std::string& error) {
            ESP_LOGE(TAG, "error: %s", error.c_str());
            // 请求失败，调用错误回调
            if (onError) {
                onError(error);
            }
        }
    );
}

/**
 * @brief HTTP响应解析函数（静态函数）
 *
 * 解析云端返回的网关配置JSON数据。
 *
 * ============================================================================
 * 内存安全改造说明（2025-01）
 * ============================================================================
 *
 * 将原成员函数 onResponse() 改为静态函数 parseGatewayResponse()
 * 
 * 原因：
 * - 原 onResponse() 是成员函数，lambda 中需要捕获 this 来调用
 * - 如果 GatewayClient 在回调执行前被销毁，this 成为悬空指针
 * - 改为静态函数后，lambda 不再需要捕获 this，避免悬空指针问题
 *
 * 响应结构：
 * {
 *   "status": 1,              // 是否使用代理（1：使用，0：不使用）
 *   "token": "xxx",           // 认证令牌（可为空）
 *   "data": {
 *     "http": "http://...",   // HTTP代理地址
 *     "ws": "ws://..."        // WebSocket代理地址
 *   },
 *   "expires": 86400          // 有效期（秒）
 * }
 *
 * 解析流程：
 * 1. 解析status字段，判断是否需要使用代理（AgentUseCode.USE或AgentUseCode.NOT）
 * 2. 解析token字段，获取认证令牌
 * 3. 解析data对象中的http和ws代理地址（使用新的字段名）
 * 4. 解析expires字段，获取代理有效期
 * 5. 如果status=AgentUseCode.USE，更新全局ApiConfig配置
 * 6. 调用用户回调
 *
 * 自动配置：如果云端返回status=AgentUseCode.USE，SDK会自动设置全局代理配置，
 * 后续的所有API请求会自动走代理服务器。
 *
 * @param response HTTP响应字符串
 * @param onSuccess 网关信息解析成功后的回调
 * @param onError 网关信息解析失败后的回调
 */
void GatewayClient::parseGatewayResponse(const std::string& response, GatewayCallback onSuccess, std::function<void(const std::string&)> onError) {
    ESP_LOGD(TAG, "response: %s", response.c_str());

    // 解析JSON响应
    cJSON* root = cJSON_Parse(response.c_str());
    if (!root) {
        // JSON解析失败
        if (onError) {
            onError("Failed to parse JSON response");
        }
        return;
    }

    GatewayInfo gatewayInfo;

    // 解析status字段 - 指示是否使用代理（1：使用，0：不使用）
    cJSON* statusJson = cJSON_GetObjectItem(root, "status");
    if (statusJson && cJSON_IsNumber(statusJson)) {
        gatewayInfo.status = statusJson->valueint;
    }

    // 解析token字段 - 网关认证令牌（可为空）
    cJSON* tokenJson = cJSON_GetObjectItem(root, "token");
    if (tokenJson && cJSON_IsString(tokenJson)) {
        gatewayInfo.token = tokenJson->valuestring;
    }

    // 解析data对象 - 包含代理服务器地址
    cJSON* dataJson = cJSON_GetObjectItem(root, "data");
    if (dataJson && cJSON_IsObject(dataJson)) {
        // 解析HTTP代理地址
        cJSON* httpJson = cJSON_GetObjectItem(dataJson, "http");
        if (httpJson && cJSON_IsString(httpJson)) {
            gatewayInfo.data.http = httpJson->valuestring;
        }

        // 解析WebSocket代理地址
        cJSON* wsJson = cJSON_GetObjectItem(dataJson, "ws");
        if (wsJson && cJSON_IsString(wsJson)) {
            gatewayInfo.data.ws = wsJson->valuestring;
        }
    }

    // 解析expires字段 - 代理有效期（秒）- 使用新的字段名
    cJSON* expiresJson = cJSON_GetObjectItem(root, "expires");
    if (expiresJson && cJSON_IsNumber(expiresJson)) {
        gatewayInfo.expires = expiresJson->valueint;
    }

    // 如果status为AgentUseCode.USE，更新全局ApiConfig配置
    // 这样后续请求会自动使用代理
    if (gatewayInfo.status == AgentUseCode::USE) {
        ApiConfig::useAgent = true;
        ApiConfig::agentBaseUrl = gatewayInfo.data.http;
        ApiConfig::apiToken = gatewayInfo.token;

        ESP_LOGD(TAG, "Gateway configuration updated:");
        ESP_LOGD(TAG, "  Use agent: true");
        ESP_LOGD(TAG, "  Agent URL: %s", gatewayInfo.data.http.c_str());
        ESP_LOGD(TAG, "  Token: %s", gatewayInfo.token.empty() ? "(empty)" : "***");
    } else {
        // 明确禁用代理
        ApiConfig::useAgent = false;
        ApiConfig::agentBaseUrl = "";
        ApiConfig::apiToken = "";

        ESP_LOGD(TAG, "Gateway configuration updated:");
        ESP_LOGD(TAG, "  Use agent: false");
    }

    // 释放JSON对象内存
    cJSON_Delete(root);

    // 调用成功回调
    if (onSuccess) {
        onSuccess(gatewayInfo, "");
    }
}

/**
 * 错误处理回调
 *
 * 记录错误日志并调用用户提供的错误回调函数。
 *
 * 错误可能发生在：
 * - HTTP请求失败（网络问题、服务器错误）
 * - JSON解析失败（响应格式错误）
 * - 响应数据不完整（缺少必要字段）
 * - 无法连接到网关服务器
 *
 * 错误处理策略：
 * - 记录详细的错误日志，方便排查问题
 * - 调用用户提供的错误回调，让上层应用决定如何处理
 * - 不自动重试，由上层应用根据业务需求决定是否重试获取
 *
 * @param error 错误信息
 * @param onError 用户提供的错误回调函数
 */
void GatewayClient::onError(const std::string& error, std::function<void(const std::string&)> onError) {
    // 记录错误日志
    ESP_LOGE(TAG, "getGateWay failed: %s", error.c_str());
    // 调用用户提供的错误回调
    if (onError) {
        onError(error);
    }
}

} // namespace ai_sdk