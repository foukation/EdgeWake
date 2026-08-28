/**
 * @file report_client.h
 * @brief 上报客户端类（内部头文件）
 *
 * ============================================================================
 * ⚠️ 内部头文件 - 请勿在 SDK 外部使用
 * ============================================================================
 *
 * 此文件是 AI SDK 的内部实现细节，不属于公开 API。
 * 上层业务代码不应直接引用此头文件。
 *
 * 如需进行数据上报，请使用公开 API：
 * - GateWay::dataReport()
 *
 * 功能说明：
 * - 实现设备数据上报的 HTTP 通信
 * - 支持即时上报设备状态和心跳
 * - 自动添加 SDK 版本号到上报参数
 */
#pragma once

#include "ai_sdk/types.h"
#include <functional>

namespace ai_sdk {

/**
 * @class ReportClient
 * @brief 上报客户端类（内部使用）
 *
 * 用于设备数据上报，提供即时上报功能。
 *
 * 设计说明：
 * - 本类只提供即时上报能力（dataReport），不提供定时上报功能
 * - 定时上报是业务层的功能，SDK 只提供基础的 HTTP 上报接口
 *
 * 注意：此类由 GateWay 内部调用，上层业务应使用 GateWay API。
 */
class ReportClient {
public:
    /**
     * @brief 上报成功回调类型
     * @param response 上报响应，包含完整的响应信息
     */
    using ReportCallback = std::function<void(const DeviceReportResponse&)>;

    /**
     * @brief 上报错误回调类型
     * @param error 错误信息
     */
    using ErrorCallback = std::function<void(const std::string&)>;

    /**
     * @brief 设备数据上报接口
     *
     * 向云端上报设备信息或心跳数据。
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
     * @param request 上报请求
     * @param onSuccess 成功回调
     * @param onError 错误回调
     */
    void dataReport(
        const DeviceReportRequest& request,
        ReportCallback onSuccess,
        ErrorCallback onError
    );
};

} // namespace ai_sdk

