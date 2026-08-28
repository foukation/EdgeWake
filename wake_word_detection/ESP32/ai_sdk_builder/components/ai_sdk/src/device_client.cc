/**
 * @file device_client.cc
 * @brief 设备客户端实现
 *
 * ============================================================================
 * 内存安全改造说明（2025-01）
 * ============================================================================
 *
 * 问题：lambda 中捕获 this 指针，如果 DeviceClient 在回调执行前被销毁，
 *       this 成为悬空指针，调用成员函数会导致崩溃
 *
 * 修复方案：
 * 1. 将 onResponse 改为静态函数 parseDeviceInfoResponse，不依赖 this
 * 2. lambda 中不再捕获 this，只捕获必要的回调函数
 * 3. 响应解析逻辑完全在静态函数中完成
 */

// 内部头文件（位于 src/include/）
#include "device_client.h"
#include "api_config.h"
#include "http_client.h"
#include "cjson_guard.h"  // RAII 封装，防止 cJSON 内存泄漏

// 公开头文件（位于 include/ai_sdk/）
#include "ai_sdk/ai_assistant_manager.h"
#include "cJSON.h"
#include "esp_log.h"
#include <cstdlib>

namespace ai_sdk {

static const char* TAG = "DeviceClient";

/**
 * @brief 获取设备信息
 *
 * 通过产品信息和设备号向云端发起设备注册认证请求。
 *
 * 请求结构：
 * {
 *   "deviceNoType": "MAC|SN|IMEI",  // 设备号类型
 *   "deviceNo": "设备唯一序列号",      // 设备号，产品内唯一
 *   "productId": "产品ID",           // 平台创建产品时生成
 *   "productKey": "产品密钥"         // 平台创建产品时生成
 * }
 *
 * 响应结构：
 * {
 *   "status": 200,                   // 成功=200，其他值为异常
 *   "message": "响应消息",           // 错误提示信息
 *   "data": {
 *     "deviceId": "设备ID",         // 平台上唯一设备标识
 *     "deviceNo": "设备号",         // 产品内唯一标识设备的序列号
 *     "productId": "产品ID",        // 产品ID，平台创建产品时生成
 *     "deviceSecret": "设备密钥"    // 设备密钥，平台创建产品时生成
 *   }
 * }
 *
 * 流程：
 * 1. 构建JSON请求体，包含设备类型、设备号、产品ID和密钥
 * 2. 发送HTTP POST请求到设备认证接口
 * 3. 解析完整JSON响应，提取所有设备信息
 * 4. 如果成功，自动更新AIAssistantManager配置
 *
 * @param request 设备信息请求结构
 * @param onSuccess 成功回调，返回设备信息
 * @param onError 错误回调，返回错误信息
 */
void DeviceClient::obtainDeviceInformation(
    const DeviceInfoRequest& request,
    DeviceInfoSuccessCallback onSuccess,
    DeviceInfoErrorCallback onError
) {
    ESP_LOGI(TAG, "API: obtainDeviceInformation");

    // 构建JSON请求体
    // 使用 RAII 自动管理 cJSON 内存，防止泄漏
    cJSON* root = cJSON_CreateObject();
    cJSONGuard root_guard(root);
    cJSON_AddStringToObject(root, "deviceNoType", request.deviceNoType.c_str());  // 设备号类型：MAC、SN、IMEI
    cJSON_AddStringToObject(root, "deviceNo", request.deviceNo.c_str());          // 设备号，产品唯一标识设备的序列号
    cJSON_AddStringToObject(root, "productId", request.productId.c_str());        // 产品ID，平台创建产品时生成
    cJSON_AddStringToObject(root, "productKey", request.productKey.c_str());      // 产品密钥，平台创建产品时生成

    // 转换为无格式化的JSON字符串
    char* jsonStr = cJSON_PrintUnformatted(root);
    std::string jsonBody(jsonStr);
    free(jsonStr);  // 释放cJSON分配的内存
    // root_guard 会在作用域结束时自动释放 root

    // 创建请求头
    std::map<std::string, std::string> headers;
    headers["Content-Type"] = "application/json; charset=utf-8";
    // TODO: Add signature authentication headers

    // 输出请求参数日志（包含敏感信息，仅 DEBUG 级别输出）
    ESP_LOGD(TAG, "request params: %s", jsonBody.c_str());

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
    // - lambda 中不再捕获 this，避免 DeviceClient 销毁后访问悬空指针
    // - 使用静态函数 parseDeviceInfoResponse() 解析响应
    // - http_client 使用 shared_ptr，确保在回调期间 HTTPClient 有效
    auto http_client = std::make_shared<HTTPClient>();
    http_client->postAsync(
        std::string(ApiConfig::TERMINAL_INTELLIGENT_SERVICE_PLATFORM_BASE_URL_TEST) + ApiConfig::OBTAIN_DEVICE_INFORMATION_API,
        jsonBody,
        headers,
        // 成功回调：不捕获 this，使用静态函数解析响应
        [onSuccess, onError, http_client](const std::string& response) {
            // 请求成功，使用静态函数解析响应
            parseDeviceInfoResponse(response, onSuccess, onError);
        },
        // 错误回调：不捕获 this，直接调用用户回调
        [onError, http_client](const std::string& error) {
            ESP_LOGE(TAG, "obtainDeviceInformation failed: %s", error.c_str());
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
 * 解析云端返回的JSON数据，提取完整的设备认证信息。
 *
 * ============================================================================
 * 内存安全改造说明（2025-01）
 * ============================================================================
 *
 * 将原成员函数 onResponse() 改为静态函数 parseDeviceInfoResponse()
 * 
 * 原因：
 * - 原 onResponse() 是成员函数，lambda 中需要捕获 this 来调用
 * - 如果 DeviceClient 在回调执行前被销毁，this 成为悬空指针
 * - 改为静态函数后，lambda 不再需要捕获 this，避免悬空指针问题
 *
 * 响应解析流程：
 * 1. 解析JSON响应字符串
 * 2. 提取status字段（状态码，成功=200）
 * 3. 提取message字段（响应消息或错误信息）
 * 4. 提取data对象中的所有设备信息：
 *    - deviceId：平台上唯一设备标识
 *    - deviceNo：产品内唯一标识设备的序列号
 *    - productId：产品ID
 *    - deviceSecret：设备密钥
 * 5. 释放JSON内存
 * 6. 如果成功，自动更新AIAssistantManager配置
 * 7. 调用用户提供的回调函数
 *
 * 自动配置更新：如果云端返回status=200，SDK会自动更新
 * AIAssistantManager中的设备认证信息，包括deviceId和deviceSecret，
 * 确保后续请求可以使用正确的认证信息。
 *
 * @param response HTTP响应字符串
 * @param onSuccess 设备信息解析成功后的回调
 * @param onError 设备信息解析失败后的回调
 */
void DeviceClient::parseDeviceInfoResponse(const std::string& response, DeviceInfoSuccessCallback onSuccess, DeviceInfoErrorCallback onError) {
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

    DeviceInfoResponse deviceInfoResponse;

    // 解析code字段 - 请求状态码（成功=200，其他值为异常）
    // 服务器返回的code字段是字符串类型，需要转换为整数
    cJSON* codeJson = cJSON_GetObjectItem(root, "code");
    if (codeJson && cJSON_IsString(codeJson)) {
        deviceInfoResponse.code = std::atoi(codeJson->valuestring);
    } else if (codeJson && cJSON_IsNumber(codeJson)) {
        // 兼容数字类型的code字段
        deviceInfoResponse.code = codeJson->valueint;
    }

    // 解析message字段 - 响应结果，错误提示信息
    cJSON* messageJson = cJSON_GetObjectItem(root, "message");
    if (messageJson && cJSON_IsString(messageJson)) {
        deviceInfoResponse.message = messageJson->valuestring;
    }

    // 解析data对象 - 包含完整的设备认证信息
    cJSON* dataJson = cJSON_GetObjectItem(root, "data");
    if (dataJson && cJSON_IsObject(dataJson)) {
        // 解析deviceId - 设备ID，平台上唯一设备标识
        cJSON* deviceIdJson = cJSON_GetObjectItem(dataJson, "deviceId");
        if (deviceIdJson && cJSON_IsString(deviceIdJson)) {
            deviceInfoResponse.data.deviceId = deviceIdJson->valuestring;
        }

        // 解析deviceNo - 设备号，产品内唯一标识设备的序列号
        cJSON* deviceNoJson = cJSON_GetObjectItem(dataJson, "deviceNo");
        if (deviceNoJson && cJSON_IsString(deviceNoJson)) {
            deviceInfoResponse.data.deviceNo = deviceNoJson->valuestring;
        }

        // 解析productId - 产品ID，平台创建产品时生成
        cJSON* productIdJson = cJSON_GetObjectItem(dataJson, "productId");
        if (productIdJson && cJSON_IsString(productIdJson)) {
            deviceInfoResponse.data.productId = productIdJson->valuestring;
        }

        // 解析deviceSecret - 设备密钥，平台创建产品时生成（用于后续请求签名）
        cJSON* deviceSecretJson = cJSON_GetObjectItem(dataJson, "deviceSecret");
        if (deviceSecretJson && cJSON_IsString(deviceSecretJson)) {
            deviceInfoResponse.data.deviceSecret = deviceSecretJson->valuestring;
        }
    }

    // 释放JSON对象内存
    cJSON_Delete(root);

    // 如果成功获取设备信息，自动更新AIAssistantManager的配置
    if (deviceInfoResponse.code == 200) {  // 成功状态码，与服务器返回一致
        if (!deviceInfoResponse.data.deviceId.empty() &&
            !deviceInfoResponse.data.deviceSecret.empty()) {

            // 获取AIAssistantManager的可变配置引用
            auto& config = AIAssistantManager::getInstance().config();

            // 直接赋值更新设备认证信息
            config.deviceId = deviceInfoResponse.data.deviceId;
            config.deviceSecret = deviceInfoResponse.data.deviceSecret;

            ESP_LOGD(TAG, "Device configuration updated in AIAssistantManager:");
            ESP_LOGD(TAG, "  deviceId: %s", deviceInfoResponse.data.deviceId.c_str());
            ESP_LOGD(TAG, "  deviceNo: %s", deviceInfoResponse.data.deviceNo.c_str());
            ESP_LOGD(TAG, "  productId: %s", deviceInfoResponse.data.productId.c_str());
            // 出于安全考虑，不打印deviceSecret到日志
        }
    }

    // 调用成功回调
    if (onSuccess) {
        onSuccess(deviceInfoResponse);
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
 *
 * @param error 错误信息
 * @param onError 用户提供的错误回调函数
 */
void DeviceClient::onError(const std::string& error, DeviceInfoErrorCallback onError) {
    // 记录错误日志
    ESP_LOGE(TAG, "obtainDeviceInformation failed: %s", error.c_str());
    // 调用用户提供的错误回调
    if (onError) {
        onError(error);
    }
}

} // namespace ai_sdk