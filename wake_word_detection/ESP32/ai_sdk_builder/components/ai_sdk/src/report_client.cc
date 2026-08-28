// 内部头文件（位于 src/include/）
#include "report_client.h"
#include "api_config.h"
#include "http_client.h"
#include "cjson_guard.h"  // RAII 封装，防止 cJSON 内存泄漏
#include "cJSON.h"
#include "esp_log.h"
#include <vector>  // for std::vector<std::string> support

namespace ai_sdk {

static const char* TAG = "ReportClient";

/**
 * @brief 设备数据上报接口
 *
 * 向云端上报设备信息或心跳数据，用于更新设备的最后活动时间。
 *
 * 上报流程：
 * 1. 构建 JSON 请求体，包含设备认证信息和参数
 * 2. 自动添加 SDK 版本号到 params 中
 * 3. 发送 HTTP POST 请求到设备上报接口
 * 4. 解析完整响应并调用用户回调
 *
 * 请求结构：
 * {
 *   "deviceId": "设备唯一标识",
 *   "deviceSecret": "设备密钥",
 *   "productId": "产品ID",
 *   "productKey": "产品密钥",
 *   "params": {
 *     "innerIp": ["内网IP"],
 *     "netSpeed": "网络分级",
 *     "netType": "网络类型",
 *     "platform": "操作系统",
 *     "sdkVersion": "SDK版本",
 *     "firmwareVersion": "固件版本",
 *     "mac": "MAC地址"
 *   }
 * }
 *
 * 响应结构（服务器实际返回）：
 * {
 *   "code": "200",          // 状态码（可能是字符串或数字）
 *   "message": "成功",       // 响应消息
 *   "success": true,        // 是否成功
 *   "data": {
 *     "deviceId": "设备ID",
 *     "protocolTypeTime": 0  // 协议类型时间（可能是数字或字符串）
 *   }
 * }
 *
 * @param request 上报请求，包含设备认证信息和参数
 * @param onSuccess 成功回调，返回完整响应信息
 * @param onError 错误回调
 */
void ReportClient::dataReport(
    const DeviceReportRequest& request,
    ReportCallback onSuccess,
    ErrorCallback onError
) {
    ESP_LOGI(TAG, "API: dataReport");

    // 构建JSON请求体
    // 使用 RAII 自动管理 cJSON 内存，防止泄漏
    cJSON* root = cJSON_CreateObject();
    cJSONGuard root_guard(root);

    // 添加设备认证信息 - 必需字段
    cJSON_AddStringToObject(root, "deviceId", request.deviceId.c_str());
    cJSON_AddStringToObject(root, "deviceSecret", request.deviceSecret.c_str());
    cJSON_AddStringToObject(root, "productId", request.productId.c_str());
    cJSON_AddStringToObject(root, "productKey", request.productKey.c_str());

    // 创建params对象 - 用于存放自定义参数
    // 注意：paramsObj 会被 cJSON_AddItemToObject 添加到 root，由 root 管理生命周期
    cJSON* paramsObj = cJSON_CreateObject();

    // ========================================================================
    // 添加自定义参数
    // ========================================================================
    // 
    // 设计说明：
    // - 使用 std::any_cast 的指针版本，失败时返回 nullptr 而不是抛出异常
    // - 这样可以避免在「禁用 C++ 异常」的 ESP-IDF 配置下导致 abort()
    //
    // 类型映射：
    // | C++ 类型                    | JSON 类型 |
    // |-----------------------------|-----------|
    // | std::string                 | string    |
    // | const char*                 | string    |
    // | int                         | number    |
    // | int64_t                     | number    |
    // | float                       | number    |
    // | double                      | number    |
    // | bool                        | boolean   |
    // | std::vector<std::string>    | array     |
    //
    // ========================================================================
    for (const auto& param : request.params) {
        // ---- 1. 字符串类型 (std::string) ----
        // 最常用的类型，如 netType, platform, firmwareVersion 等
        if (const auto* str_ptr = std::any_cast<std::string>(&param.second)) {
            cJSON_AddStringToObject(paramsObj, param.first.c_str(), str_ptr->c_str());
        }
        // ---- 2. C 风格字符串 (const char*) ----
        // 兼容直接传入字符串字面量的情况
        else if (const auto* cstr_ptr = std::any_cast<const char*>(&param.second)) {
            cJSON_AddStringToObject(paramsObj, param.first.c_str(), *cstr_ptr);
        }
        // ---- 3. 整数类型 (int) ----
        // 如 wifi_rssi, count 等整数值
        else if (const auto* int_ptr = std::any_cast<int>(&param.second)) {
            cJSON_AddNumberToObject(paramsObj, param.first.c_str(), *int_ptr);
        }
        // ---- 4. 64位整数类型 (int64_t) ----
        // 用于时间戳、大整数等场景
        // 注意：JSON 的 number 类型是 IEEE 754 双精度浮点，精度有限
        else if (const auto* int64_ptr = std::any_cast<int64_t>(&param.second)) {
            cJSON_AddNumberToObject(paramsObj, param.first.c_str(), static_cast<double>(*int64_ptr));
        }
        // ---- 5. 双精度浮点数 (double) ----
        // 如 temperature, latitude, longitude 等
        else if (const auto* double_ptr = std::any_cast<double>(&param.second)) {
            cJSON_AddNumberToObject(paramsObj, param.first.c_str(), *double_ptr);
        }
        // ---- 6. 单精度浮点数 (float) ----
        // 转换为 double 后添加
        else if (const auto* float_ptr = std::any_cast<float>(&param.second)) {
            cJSON_AddNumberToObject(paramsObj, param.first.c_str(), static_cast<double>(*float_ptr));
        }
        // ---- 7. 布尔类型 (bool) ----
        // 如 networkConnected, isActive 等
        else if (const auto* bool_ptr = std::any_cast<bool>(&param.second)) {
            cJSON_AddBoolToObject(paramsObj, param.first.c_str(), *bool_ptr);
        }
        // ---- 8. 字符串数组 (std::vector<std::string>) ----
        // 如 innerIp 参数
        else if (const auto* vec_ptr = std::any_cast<std::vector<std::string>>(&param.second)) {
            cJSON* arr = cJSON_CreateArray();
            if (arr) {
                for (const auto& item : *vec_ptr) {
                    cJSON_AddItemToArray(arr, cJSON_CreateString(item.c_str()));
                }
                cJSON_AddItemToObject(paramsObj, param.first.c_str(), arr);
            } else {
                ESP_LOGE(TAG, "Failed to create JSON array for key: %s", param.first.c_str());
            }
        }
        // ---- 未知类型处理 ----
        // 如果用户传入了不支持的类型，记录警告并使用占位符
        else {
            ESP_LOGW(TAG, "Unknown param type for key: %s (typeid: %s)", 
                     param.first.c_str(), param.second.type().name());
            cJSON_AddStringToObject(paramsObj, param.first.c_str(), "unknown_type");
        }
    }

    // 自动添加SDK版本号
    cJSON_AddStringToObject(paramsObj, "sdkVersion", "ai_0.9.9");

    cJSON_AddItemToObject(root, "params", paramsObj);

    // 转换为无格式化的JSON字符串
    char* jsonStr = cJSON_PrintUnformatted(root);
    std::string jsonBody(jsonStr);
    free(jsonStr);  // 释放cJSON分配的内存
    // root_guard 会在作用域结束时自动释放 root

    // 输出请求参数日志（包含敏感信息，仅 DEBUG 级别输出）
    ESP_LOGD(TAG, "request params: %s", jsonBody.c_str());

    // 创建请求头
    std::map<std::string, std::string> headers;
    headers["Content-Type"] = "application/json; charset=utf-8";
    // TODO: Add signature authentication headers

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
    // 注意：使用 shared_ptr 确保在回调执行期间 HTTPClient 对象有效
    auto http_client = std::make_shared<HTTPClient>();
    http_client->postAsync(
        std::string(ApiConfig::TERMINAL_INTELLIGENT_SERVICE_PLATFORM_BASE_URL_TEST) + ApiConfig::DEVICE_DATA_REPORT_API,
        jsonBody,
        headers,
        [onSuccess, http_client](const std::string& response) {
            ESP_LOGD(TAG, "dataReport response: %s", response.c_str());
            DeviceReportResponse reportResponse;

            // ================================================================
            // 解析完整响应 - 与服务器实际返回格式对齐
            // ================================================================
            // 
            // 服务器返回格式：
            // {
            //   "code": "200",      // 状态码（字符串或数字）
            //   "message": "成功",
            //   "success": true,
            //   "data": { ... }
            // }
            // ================================================================
            cJSON* root = cJSON_Parse(response.c_str());
            cJSONGuard root_guard(root);  // RAII 自动管理内存
            if (root) {
                // ============================================================
                // 解析状态码 "code"
                // ============================================================
                // 服务器返回的 "code" 可能是字符串 "200" 或数字 200
                // 需要同时处理两种类型
                cJSON* codeJson = cJSON_GetObjectItem(root, "code");
                if (codeJson) {
                    if (cJSON_IsNumber(codeJson)) {
                        // 数字类型：直接使用
                        reportResponse.code = codeJson->valueint;
                    } else if (cJSON_IsString(codeJson)) {
                        // 字符串类型：转换为整数
                        reportResponse.code = atoi(codeJson->valuestring);
                    }
                }

                // ============================================================
                // 解析 "success" 布尔值
                // ============================================================
                cJSON* successJson = cJSON_GetObjectItem(root, "success");
                if (successJson && cJSON_IsBool(successJson)) {
                    reportResponse.success = cJSON_IsTrue(successJson);
                }

                // ============================================================
                // 解析响应消息 "message"
                // ============================================================
                cJSON* messageJson = cJSON_GetObjectItem(root, "message");
                if (messageJson && cJSON_IsString(messageJson)) {
                    reportResponse.message = messageJson->valuestring;
                }

                // ============================================================
                // 解析数据对象 "data"
                // ============================================================
                cJSON* dataJson = cJSON_GetObjectItem(root, "data");
                if (dataJson && cJSON_IsObject(dataJson)) {
                    // 解析设备 ID
                    cJSON* deviceIdJson = cJSON_GetObjectItem(dataJson, "deviceId");
                    if (deviceIdJson && cJSON_IsString(deviceIdJson)) {
                        reportResponse.data.deviceId = deviceIdJson->valuestring;
                    }

                    // 解析协议类型时间（可能是字符串或数字）
                    cJSON* protocolTypeTimeJson = cJSON_GetObjectItem(dataJson, "protocolTypeTime");
                    if (protocolTypeTimeJson) {
                        if (cJSON_IsString(protocolTypeTimeJson)) {
                            reportResponse.data.protocolTypeTime = protocolTypeTimeJson->valuestring;
                        } else if (cJSON_IsNumber(protocolTypeTimeJson)) {
                            // 数字类型转换为字符串
                            char buf[32];
                            snprintf(buf, sizeof(buf), "%d", protocolTypeTimeJson->valueint);
                            reportResponse.data.protocolTypeTime = buf;
                        }
                    }
                }

                // root_guard 会在 lambda 结束时自动释放 root
            }

            // 调用成功回调
            if (onSuccess) {
                onSuccess(reportResponse);
            }
        },
        [onError, http_client](const std::string& error) {
            ESP_LOGE(TAG, "dataReport failed: %s", error.c_str());
            // 调用错误回调
            if (onError) {
                onError(error);
            }
        }
    );
}

} // namespace ai_sdk
