/**
 * @file gate_way.h
 * @brief 网关管理类定义
 *
 * 此文件定义了 GateWay 类，用于管理设备与云端之间的通信。
 * 网关作为设备和云端之间的桥梁，提供以下功能：
 * - 设备注册认证
 * - 网关/代理配置获取
 * - 设备数据上报（心跳）
 *
 * 使用示例：
 *
 *   #include "ai_sdk/gate_way.h"
 *   // 或通过 ai_assistant_manager.h 自动包含
 *
 *   // 通过 AIAssistantManager 获取实例
 *   auto& gateway = AIAssistantManager::getInstance().gateWayHelp();
 *
 *   // 1. 设备注册
 *   gateway.obtainDeviceInformation(
 *       [](const DeviceInfoResponse& resp) {
 *           // 保存 deviceId 和 deviceSecret
 *       },
 *       [](const std::string& error) { }  // 错误处理
 *   );
 *
 *   // 2. 获取网关配置（可选）
 *   gateway.getGateWay(
 *       [](const GatewayInfo& info, const std::string&) {
 *           // 处理代理配置
 *       },
 *       [](const std::string& error) { }  // 错误处理
 *   );
 *
 *   // 3. 数据上报（心跳）
 *   DeviceReportRequest request;
 *   request.deviceId = "...";
 *   // ... 填充其他字段
 *   gateway.dataReport(request,
 *       [](const DeviceReportResponse& resp) { },  // 成功
 *       [](const std::string& error) { }           // 失败
 *   );
 */
#pragma once

#include "ai_sdk/types/gateway.h"
#include "ai_sdk/types/device.h"
#include "ai_sdk/types/report.h"
#include <functional>

namespace ai_sdk {

/**
 * @brief 网关管理类
 *
 * 用于管理和处理与网关相关的操作和数据。
 * 网关作为不同设备和云端之间的桥梁，实现通信和数据传输。
 *
 * 设计说明：
 * - 通过 AIAssistantManager::gateWayHelp() 获取实例
 * - 所有方法采用异步回调模式
 * - 内部使用 HTTPClient 进行网络通信
 */
class GateWay {
public:
    // =========================================================================
    // 回调类型定义
    // =========================================================================

    /**
     * 设备信息获取成功回调类型
     * 参数 response: 设备信息响应
     */
    using DeviceInfoSuccessCallback = std::function<void(const DeviceInfoResponse&)>;

    /**
     * 设备信息获取错误回调类型
     * 参数 error: 错误信息
     */
    using DeviceInfoErrorCallback = std::function<void(const std::string&)>;

    /**
     * 网关信息获取成功回调类型
     * 参数 info: 网关信息
     * 参数 extra: 额外信息（保留参数）
     */
    using GatewaySuccessCallback = std::function<void(const GatewayInfo&, const std::string&)>;

    /**
     * 网关信息获取错误回调类型
     * 参数 error: 错误信息
     */
    using GatewayErrorCallback = std::function<void(const std::string&)>;

    /**
     * 数据上报成功回调类型
     * 参数 response: 上报响应
     */
    using ReportSuccessCallback = std::function<void(const DeviceReportResponse&)>;

    /**
     * 数据上报错误回调类型
     * 参数 error: 错误信息
     */
    using ReportErrorCallback = std::function<void(const std::string&)>;

    // =========================================================================
    // 构造与析构
    // =========================================================================

    /**
     * 构造函数
     */
    GateWay() = default;

    /**
     * 析构函数
     */
    ~GateWay() = default;

    // =========================================================================
    // 公开方法
    // =========================================================================

    /**
     * @brief 获取网关信息
     *
     * 从云端获取网关配置，包括代理服务器地址和认证令牌。
     * 使用回调函数处理成功和错误情况，支持异步操作。
     *
     * 成功后，SDK 会自动更新全局代理配置：
     * - ApiConfig::useAgent
     * - ApiConfig::agentBaseUrl
     * - ApiConfig::apiToken
     *
     * @param onSuccess 成功回调，接收 GatewayInfo 对象
     * @param onError 错误回调，接收错误信息字符串
     */
    void getGateWay(
        GatewaySuccessCallback onSuccess,
        GatewayErrorCallback onError
    );

    /**
     * @brief 获取设备信息
     *
     * 设备通过产品信息及设备号从云端获取设备信息。
     * 前提是设备号已录入到云端平台。
     *
     * 成功后，SDK 会自动更新配置中的 deviceId 和 deviceSecret。
     *
     * 使用回调函数处理成功和错误情况，支持异步操作。
     *
     * @param onSuccess 成功回调，接收 DeviceInfoResponse 对象
     * @param onError 错误回调，接收错误信息字符串
     */
    void obtainDeviceInformation(
        DeviceInfoSuccessCallback onSuccess,
        DeviceInfoErrorCallback onError
    );

    /**
     * @brief 数据上报接口（心跳）
     *
     * 设备向云端上报信息，更新最后活动时间。
     * 设备至少每 24 小时上报一次。
     *
     * 建议上报策略：
     * 1. 设备启动时立即上报
     * 2. 每隔 12 小时上报一次
     * 3. 添加 ±15 分钟随机偏移，避免服务器压力
     *
     * @param deviceReportRequest 上报请求数据
     * @param onSuccess 成功回调，接收 DeviceReportResponse 对象
     * @param onError 错误回调，接收错误信息字符串
     */
    void dataReport(
        const DeviceReportRequest& deviceReportRequest,
        ReportSuccessCallback onSuccess,
        ReportErrorCallback onError
    );

private:
    // =========================================================================
    // 内部方法（响应处理）
    // =========================================================================

    /**
     * HTTP 响应处理 - 设备信息
     */
    void onDeviceInfoResponse(const std::string& response,
                              DeviceInfoSuccessCallback onSuccess,
                              DeviceInfoErrorCallback onError);

    /**
     * HTTP 响应处理 - 网关信息
     */
    void onGatewayResponse(const std::string& response,
                           GatewaySuccessCallback onSuccess,
                           GatewayErrorCallback onError);

    /**
     * HTTP 响应处理 - 数据上报
     */
    void onReportResponse(const std::string& response,
                          ReportSuccessCallback onSuccess,
                          ReportErrorCallback onError);
};

}  // namespace ai_sdk
