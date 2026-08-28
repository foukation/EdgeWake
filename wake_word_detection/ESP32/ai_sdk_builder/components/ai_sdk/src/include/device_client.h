/**
 * @file device_client.h
 * @brief 设备客户端类（内部头文件）
 *
 * ============================================================================
 * ⚠️ 内部头文件 - 请勿在 SDK 外部使用
 * ============================================================================
 *
 * 此文件是 AI SDK 的内部实现细节，不属于公开 API。
 * 上层业务代码不应直接引用此头文件。
 *
 * 如需进行设备注册，请使用公开 API：
 * - GateWay::obtainDeviceInformation()
 *
 * 功能说明：
 * - 实现设备注册和认证的 HTTP 通信
 * - 解析云端返回的设备信息
 * - 自动更新 AIAssistantManager 配置中的 deviceId 和 deviceSecret
 *
 * ============================================================================
 * 内存安全改造说明（2025-01）
 * ============================================================================
 *
 * 将响应解析函数改为静态函数，避免在 lambda 中捕获 this 指针：
 * - parseDeviceInfoResponse() - 静态函数，解析设备信息响应
 * - 原 onResponse() 已移除
 */
#pragma once

#include "ai_sdk/types.h"
#include <functional>

namespace ai_sdk {

/**
 * @class DeviceClient
 * @brief 设备客户端类（内部使用）
 *
 * 用于管理与云端平台的设备注册和认证交互。
 *
 * 注意：此类由 GateWay 内部调用，上层业务应使用 GateWay API。
 */
class DeviceClient {
public:
    /**
     * @brief 设备信息获取成功回调类型
     * @param response 设备信息响应，包含完整的设备信息
     */
    using DeviceInfoSuccessCallback = std::function<void(const DeviceInfoResponse&)>;

    /**
     * @brief 设备信息获取错误回调类型
     * @param error 错误信息
     */
    using DeviceInfoErrorCallback = std::function<void(const std::string&)>;

    DeviceClient() = default;
    ~DeviceClient() = default;

    /**
     * @brief 获取设备信息
     *
     * 设备通过产品信息及设备号从云端获取设备信息。
     * 前提是设备号已录入到云端平台。
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
     *   "message": "响应消息",
     *   "data": {
     *     "deviceId": "设备ID",
     *     "deviceNo": "设备号",
     *     "productId": "产品ID",
     *     "deviceSecret": "设备密钥"
     *   }
     * }
     *
     * @param request 设备信息请求
     * @param onSuccess 成功回调
     * @param onError 错误回调
     */
    void obtainDeviceInformation(
        const DeviceInfoRequest& request,
        DeviceInfoSuccessCallback onSuccess,
        DeviceInfoErrorCallback onError
    );

private:
    /**
     * @brief HTTP响应解析（静态函数）
     *
     * 解析云端返回的JSON数据，提取设备信息。
     * 
     * 内存安全说明：
     * - 使用静态函数，避免在 lambda 中捕获 this 指针
     * - lambda 只需捕获回调函数，不依赖 DeviceClient 实例
     *
     * @param response HTTP响应字符串
     * @param onSuccess 成功回调
     * @param onError 错误回调
     */
    static void parseDeviceInfoResponse(const std::string& response, DeviceInfoSuccessCallback onSuccess, DeviceInfoErrorCallback onError);

    /**
     * @brief 错误处理
     */
    void onError(const std::string& error, DeviceInfoErrorCallback onError);
};

} // namespace ai_sdk

