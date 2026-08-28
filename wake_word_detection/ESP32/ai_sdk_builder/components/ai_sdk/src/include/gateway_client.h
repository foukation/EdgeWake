/**
 * @file gateway_client.h
 * @brief 网关客户端类（内部头文件）
 *
 * ============================================================================
 * ⚠️ 内部头文件 - 请勿在 SDK 外部使用
 * ============================================================================
 *
 * 此文件是 AI SDK 的内部实现细节，不属于公开 API。
 * 上层业务代码不应直接引用此头文件。
 *
 * 如需获取网关信息，请使用公开 API：
 * - GateWay::getGateWay()
 *
 * 功能说明：
 * - 实现网关/代理配置获取的 HTTP 通信
 * - 解析云端返回的代理配置
 * - 自动更新 ApiConfig 中的代理设置
 *
 * ============================================================================
 * 内存安全改造说明（2025-01）
 * ============================================================================
 *
 * 将响应解析函数改为静态函数，避免在 lambda 中捕获 this 指针：
 * - parseGatewayResponse() - 静态函数，解析网关响应
 * - 原 onResponse() 已移除
 */
#pragma once

#include "ai_sdk/types.h"
#include <functional>

namespace ai_sdk {

/**
 * @class GatewayClient
 * @brief 网关客户端类（内部使用）
 *
 * 用于获取网关信息和代理配置。
 *
 * 注意：此类由 GateWay 内部调用，上层业务应使用 GateWay API。
 */
class GatewayClient {
public:
    /**
     * @brief 网关信息获取成功回调类型
     * @param gatewayInfo 网关信息，包含代理地址和认证令牌
     * @param emptyStr 空字符串（用于兼容性）
     */
    using GatewayCallback = std::function<void(const GatewayInfo&, const std::string&)>;

    GatewayClient() = default;
    ~GatewayClient() = default;

    /**
     * @brief 获取网关信息
     *
     * 从云端获取网关配置，包括代理服务器地址和认证令牌。
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
     * 成功后，SDK会自动更新全局 ApiConfig 配置：
     * - ApiConfig::useAgent
     * - ApiConfig::agentBaseUrl
     * - ApiConfig::apiToken
     *
     * @param onSuccess 成功回调
     * @param onError 错误回调
     */
    void getGateWay(GatewayCallback onSuccess, std::function<void(const std::string&)> onError);

private:
    /**
     * @brief HTTP响应解析（静态函数）
     *
     * 解析云端返回的网关配置JSON数据。
     * 
     * 内存安全说明：
     * - 使用静态函数，避免在 lambda 中捕获 this 指针
     * - lambda 只需捕获回调函数，不依赖 GatewayClient 实例
     *
     * @param response HTTP响应字符串
     * @param onSuccess 成功回调
     * @param onError 错误回调
     */
    static void parseGatewayResponse(const std::string& response, GatewayCallback onSuccess, std::function<void(const std::string&)> onError);

    /**
     * @brief 错误处理
     */
    void onError(const std::string& error, std::function<void(const std::string&)> onError);
};

} // namespace ai_sdk

