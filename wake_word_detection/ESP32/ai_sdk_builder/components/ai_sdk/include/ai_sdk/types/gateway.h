/**
 * @file gateway.h
 * @brief 网关相关数据结构定义
 *
 * 此文件定义了与网关/代理服务相关的数据结构。
 * 网关用于在设备和云端之间建立通信桥梁，可选择性地通过代理服务器进行中转。
 *
 * 使用示例：
 *
 *   #include "ai_sdk/types/gateway.h"
 *   // 或通过聚合头文件
 *   #include "ai_sdk/types.h"
 *
 *   manager.gateWayHelp().getGateWay(
 *       [](const ai_sdk::GatewayInfo& info, const std::string&) {
 *           if (info.status == ai_sdk::AgentUseCode::USE) {
 *               // 使用代理服务器
 *               std::string http_proxy = info.data.http;
 *               std::string ws_proxy = info.data.ws;
 *           }
 *       },
 *       [](const std::string& error) { }  // 错误处理
 *   );
 */
#pragma once

#include <string>

namespace ai_sdk {

/**
 * @brief 网关代理使用状态码
 *
 * 用于判断是否需要使用代理服务器。
 * 在 GatewayInfo.status 字段中使用。
 */
namespace AgentUseCode {
    /** 使用代理服务器 */
    constexpr int USE = 1;

    /** 不使用代理服务器（直连） */
    constexpr int NOT = 0;
}

/**
 * @brief 网关代理服务地址结构
 *
 * 包含 HTTP 和 WebSocket 两种协议的代理地址。
 *
 * 当 GatewayInfo.status == AgentUseCode::USE 时，
 * 应使用此结构中的地址进行通信。
 */
struct AgentServeData {
    /**
     * HTTP 代理地址
     * 用于 HTTP/HTTPS 请求的代理服务器地址
     * 示例："https://proxy.example.com:8443"
     */
    std::string http;

    /**
     * WebSocket 代理地址
     * 用于 WebSocket 连接的代理服务器地址
     * 示例："wss://proxy.example.com:8443"
     */
    std::string ws;
};

/**
 * @brief 网关服务响应结构
 *
 * 包含网关配置的完整信息，由 GateWay::getGateWay() 返回。
 *
 * 使用流程：
 * 1. 调用 getGateWay() 获取网关配置
 * 2. 检查 status 字段判断是否使用代理
 * 3. 如果使用代理，从 data 字段获取代理地址
 * 4. token 用于代理认证（如果需要）
 */
struct GatewayInfo {
    /**
     * 网关验证令牌
     * 用于代理服务器的身份认证（可为空）
     */
    std::string token;

    /**
     * 代理服务数据
     * 包含 HTTP 和 WebSocket 代理地址
     */
    AgentServeData data;

    /**
     * 代理有效期（单位：秒）
     * 超过有效期后需要重新获取网关配置
     */
    int expires = 0;

    /**
     * 代理使用状态码
     * 1 = 使用代理（AgentUseCode::USE）
     * 0 = 不使用代理（AgentUseCode::NOT）
     */
    int status = 0;
};

}  // namespace ai_sdk
