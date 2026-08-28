/**
 * @file report.h
 * @brief 数据上报相关数据结构定义
 *
 * 此文件定义了设备数据上报（心跳）相关的数据结构。
 * 设备需要定期向云端上报状态信息，保持连接活跃。
 *
 * 上报策略建议：
 * - 设备启动时立即上报一次
 * - 之后每 12 小时上报一次（加上 ±15 分钟随机偏移避免服务器压力）
 * - 至少每 24 小时上报一次
 *
 * 使用示例：
 *
 *   #include "ai_sdk/types/report.h"
 *   // 或通过聚合头文件
 *   #include "ai_sdk/types.h"
 *
 *   ai_sdk::DeviceReportRequest request;
 *   request.deviceId = config.deviceId;
 *   request.deviceSecret = config.deviceSecret;
 *   request.productId = config.productId;
 *   request.productKey = config.productKey;
 *   request.params["firmwareVersion"] = std::string("1.0.0");
 *
 *   manager.gateWayHelp().dataReport(request,
 *       [](const ai_sdk::DeviceReportResponse& resp) { },  // 成功
 *       [](const std::string& error) { }                   // 失败
 *   );
 */
#pragma once

#include <string>
#include <map>
#include <any>

namespace ai_sdk {

/**
 * @brief 设备数据上报请求结构
 *
 * 用于向云端上报设备状态信息和心跳。
 */
struct DeviceReportRequest {
    /**
     * 设备 ID
     * 从设备注册接口获取
     */
    std::string deviceId;

    /**
     * 设备密钥
     * 从设备注册接口获取，用于签名认证
     */
    std::string deviceSecret;

    /**
     * 产品 ID
     * 标识设备所属的产品
     */
    std::string productId;

    /**
     * 产品密钥
     * 用于产品身份验证
     */
    std::string productKey;

    /**
     * 额外上报参数
     * 可以包含设备状态、版本信息等自定义数据
     *
     * 常用参数：
     * - "innerIp"：内网 IP 地址（std::vector<std::string>）
     * - "netSpeed"：网络速度等级（std::string）
     * - "netType"：网络类型（std::string）
     * - "platform"：操作系统（std::string）
     * - "sdkVersion"：SDK 版本（std::string）
     * - "firmwareVersion"：固件版本（std::string）
     * - "mac"：MAC 地址（std::string）
     */
    std::map<std::string, std::any> params;
};

/**
 * @brief 设备数据上报响应数据结构
 *
 * 包含上报成功后服务器返回的确认信息。
 */
struct DeviceReportData {
    /**
     * 设备 ID
     * 确认上报的设备标识
     */
    std::string deviceId;

    /**
     * 协议类型时间
     * 服务器记录的协议处理时间
     */
    std::string protocolTypeTime;
};

/**
 * @brief 设备数据上报响应结构
 *
 * 包含数据上报接口的完整响应信息。
 *
 * 服务器返回格式示例：
 * {
 *   "code": "200",
 *   "message": "成功",
 *   "success": true,
 *   "data": {
 *     "deviceId": "xxx",
 *     "protocolTypeTime": 0
 *   }
 * }
 *
 * @note 服务器返回的 "code" 可能是字符串 "200" 或数字 200
 */
struct DeviceReportResponse {
    /**
     * 响应状态码
     *
     * 对应服务器返回的 "code" 字段。
     * 200 表示成功，其他值表示错误。
     *
     * @note 服务器可能返回字符串 "200" 或数字 200，
     *       解析时会自动转换为整数。
     */
    int code = 0;

    /**
     * 请求是否成功
     *
     * 对应服务器返回的 "success" 字段。
     * true 表示请求处理成功。
     *
     * @note 可以通过 code == 200 或 success == true 判断成功
     */
    bool success = false;

    /**
     * 响应消息
     *
     * 对应服务器返回的 "message" 字段。
     * 描述处理结果，如 "成功"。
     */
    std::string message;

    /**
     * 上报响应数据
     *
     * 对应服务器返回的 "data" 字段。
     * 包含服务器确认信息。
     */
    DeviceReportData data;
};

}  // namespace ai_sdk
