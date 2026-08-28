/**
 * @file device.h
 * @brief 设备注册相关数据结构定义
 *
 * 此文件定义了设备注册和认证相关的数据结构。
 * 设备需要先向云端注册，获取 deviceId 和 deviceSecret 后才能使用其他 API。
 *
 * 使用示例：
 *
 *   #include "ai_sdk/types/device.h"
 *   // 或通过聚合头文件
 *   #include "ai_sdk/types.h"
 *
 *   manager.gateWayHelp().obtainDeviceInformation(
 *       [](const ai_sdk::DeviceInfoResponse& resp) {
 *           if (resp.code == 200) {
 *               std::string deviceId = resp.data.deviceId;
 *               std::string deviceSecret = resp.data.deviceSecret;
 *               // 保存设备凭证
 *           }
 *       },
 *       [](const std::string& error) { }  // 错误处理
 *   );
 */
#pragma once

#include <string>

namespace ai_sdk {

/**
 * @brief 设备信息请求结构
 *
 * 用于向云端请求设备注册，前提是设备号已录入到云端平台。
 *
 * 注意：此结构通常不需要手动创建，SDK 会从 AIAssistConfig 中自动构建。
 */
struct DeviceInfoRequest {
    /**
     * 设备号类型
     * 支持的值：
     * - "MAC"：使用 MAC 地址作为设备标识
     * - "SN"：使用序列号作为设备标识
     * - "IMEI"：使用 IMEI 号作为设备标识
     */
    std::string deviceNoType;

    /**
     * 设备号
     * 产品内唯一标识设备的序列号
     * 示例："YM00GCDCK01896"
     */
    std::string deviceNo;

    /**
     * 产品 ID
     * 平台创建产品时生成的唯一标识
     * 示例："1889495584410234882"
     */
    std::string productId;

    /**
     * 产品密钥
     * 平台创建产品时生成的认证密钥
     * 示例："riAtcQzVmPLQprAL"
     */
    std::string productKey;
};

/**
 * @brief 设备信息响应数据结构
 *
 * 包含设备注册成功后返回的完整设备信息。
 *
 * 重要字段：
 * - deviceId：后续 API 调用必需
 * - deviceSecret：用于生成签名，后续 API 调用必需
 */
struct DeviceData {
    /**
     * 设备 ID
     * 平台上唯一标识设备的 ID
     * 由云端生成，设备注册成功后返回
     */
    std::string deviceId;

    /**
     * 设备号
     * 产品内唯一标识设备的序列号（与请求中的 deviceNo 一致）
     */
    std::string deviceNo;

    /**
     * 产品 ID
     * 平台创建产品时生成（与请求中的 productId 一致）
     */
    std::string productId;

    /**
     * 设备密钥
     * 用于后续 API 调用的签名生成
     * 需要安全保存，不可泄露
     */
    std::string deviceSecret;
};

/**
 * @brief 设备信息响应结构
 *
 * 包含设备注册接口的完整响应信息。
 *
 * 响应判断：
 * - code == 200：注册成功，从 data 字段获取设备信息
 * - code != 200：注册失败，从 message 字段获取错误原因
 */
struct DeviceInfoResponse {
    /**
     * 响应状态码
     * 200 表示成功，其他值表示异常
     *
     * 常见错误码：
     * - 400：参数错误
     * - 401：认证失败
     * - 404：设备未在平台注册
     * - 500：服务器内部错误
     */
    int code;

    /**
     * 响应消息
     * 成功时为 "success" 或空
     * 失败时包含错误描述
     */
    std::string message;

    /**
     * 设备信息数据
     * 仅在 code == 200 时有效
     */
    DeviceData data;
};

}  // namespace ai_sdk
