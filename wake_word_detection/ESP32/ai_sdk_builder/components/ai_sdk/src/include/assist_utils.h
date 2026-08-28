/**
 * @file assist_utils.h
 * @brief 辅助工具类（内部头文件）
 *
 * ============================================================================
 * ⚠️ 内部头文件 - 请勿在 SDK 外部使用
 * ============================================================================
 *
 * 此文件是 AI SDK 的内部实现细节，不属于公开 API。
 * 上层业务代码不应直接引用此头文件。
 *
 * 功能说明：
 * - 提供WebSocket URL参数构建功能
 * - 提供MD5签名生成功能
 */
#pragma once

#include <string>
#include <cstdint>

namespace ai_sdk {

/**
 * @class AssistUtils
 * @brief 辅助工具类（内部使用）
 *
 * 提供URL参数构建、签名生成等工具函数。
 *
 * 注意：此类依赖 AIAssistantManager 的配置，
 * 必须在 AIAssistantManager::initialize() 之后使用。
 */
class AssistUtils {
public:
    /**
     * @brief 构建WebSocket URL参数
     * @param uri 基础URI（如 "wss://ivs.chinamobiledevice.com:11443/app-ws/v2/asr"）
     * @return 带参数的完整URL
     *
     * 构建的参数包括：
     * - sn: 会话唯一标识（基于MAC地址+时间戳+随机数）
     * - deviceNo: 设备编号
     * - productKey: 产品密钥
     * - productId: 产品ID
     * - ts: 时间戳（毫秒）
     * - sign: MD5签名
     * - deviceId: 设备ID
     */
    static std::string wssParameter(const std::string& uri);

    /**
     * @brief 生成MD5签名
     * @param timestamp 时间戳（毫秒）
     * @return MD5签名字符串（32位小写十六进制）
     *
     * 签名算法：MD5(deviceSecret + timestamp)
     * 使用 mbedtls 库计算 MD5 哈希值。
     *
     * 如果 deviceSecret 为空，会使用 productKey 作为备选。
     */
    static std::string signMd5(int64_t timestamp);

    /**
     * @brief 获取当前时间戳
     * @return 当前时间戳（毫秒）
     *
     * 使用 esp_log_timestamp() 获取系统运行时间。
     */
    static int64_t timestamp();

};

} // namespace ai_sdk

