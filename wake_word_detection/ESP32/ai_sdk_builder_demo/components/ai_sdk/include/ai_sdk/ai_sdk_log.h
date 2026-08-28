/**
 * @file ai_sdk_log.h
 * @brief AI SDK 日志控制接口
 *
 * 提供统一的日志级别控制接口，允许第三方业务在运行时动态调整 AI SDK 内部日志输出。
 * 用户无需了解 SDK 内部模块的 TAG 名称，通过简单的接口即可控制所有模块的日志级别。
 *
 * @note 日志级别设置会影响所有 AI SDK 内部模块，包括：
 *       - AIAssistantManager（SDK 核心管理器）
 *       - AsrIntelligentDialogue（语音识别与智能对话）
 *       - WebSocket 通信模块
 *       - HTTP 客户端模块
 *       - 网关与设备认证模块
 *
 * @example 基本用法
 * @code
 * #include "ai_sdk/ai_sdk_log.h"
 *
 * // 禁用所有 AI SDK 日志（生产环境推荐）
 * ai_sdk::Log::disable();
 *
 * // 只显示错误和警告
 * ai_sdk::Log::setLevel(ai_sdk::LogLevel::WARN);
 *
 * // 启用调试模式（开发调试时使用）
 * ai_sdk::Log::setLevel(ai_sdk::LogLevel::DEBUG);
 *
 * // 恢复默认日志级别（INFO）
 * ai_sdk::Log::reset();
 * @endcode
 *
 * @copyright Copyright (c) 2024
 */

#pragma once

#include "esp_log.h"

namespace ai_sdk {

// ============================================================================
// LogLevel - 日志级别枚举
// ============================================================================

/**
 * @brief AI SDK 日志级别枚举
 *
 * 封装 ESP-IDF 日志级别，提供类型安全的日志级别控制。
 * 级别从高到低排列：NONE > ERROR > WARN > INFO > DEBUG > VERBOSE
 *
 * 设置某个级别后，只有该级别及更高级别的日志会被输出。
 * 例如：设置为 WARN 级别，则只输出 WARN 和 ERROR 级别的日志。
 */
enum class LogLevel {
    /**
     * @brief 禁用所有日志输出
     *
     * 完全静默模式，不输出任何日志。
     * 适用于对日志输出有严格限制的生产环境。
     */
    NONE = ESP_LOG_NONE,

    /**
     * @brief 仅输出错误日志
     *
     * 只显示严重错误，如初始化失败、连接断开等。
     * 适用于生产环境的最小日志输出。
     */
    ERROR = ESP_LOG_ERROR,

    /**
     * @brief 输出警告和错误日志
     *
     * 显示可能影响功能的警告信息和错误。
     * 推荐的生产环境日志级别。
     */
    WARN = ESP_LOG_WARN,

    /**
     * @brief 输出信息、警告和错误日志（默认级别）
     *
     * 显示关键状态变化信息，如初始化成功、连接建立等。
     * 适用于日常运行监控。
     */
    INFO = ESP_LOG_INFO,

    /**
     * @brief 输出调试日志及以上所有级别
     *
     * 显示详细的调试信息，包括请求/响应内容、消息解析等。
     * 适用于开发调试阶段。
     */
    DEBUG = ESP_LOG_DEBUG,

    /**
     * @brief 输出所有日志（最详细）
     *
     * 显示所有可能的日志信息，包括非常详细的追踪信息。
     * 仅用于深度调试问题。
     */
    VERBOSE = ESP_LOG_VERBOSE
};

// ============================================================================
// Log - 日志控制类
// ============================================================================

/**
 * @brief AI SDK 日志控制类
 *
 * 提供统一的日志级别控制接口，用户无需了解 SDK 内部模块的 TAG 名称。
 * 所有方法均为静态方法，无需实例化即可使用。
 *
 * @note 日志级别设置是全局的，会影响所有 AI SDK 模块。
 * @note 日志级别可以在任何时候动态调整，立即生效。
 * @note 建议在调用 AIAssistantManager::initialize() 之前设置日志级别。
 *
 * @example 典型使用场景
 * @code
 * // 场景1：生产环境 - 禁用日志
 * ai_sdk::Log::disable();
 *
 * // 场景2：生产环境 - 只看错误
 * ai_sdk::Log::setLevel(ai_sdk::LogLevel::ERROR);
 *
 * // 场景3：测试环境 - 看警告和错误
 * ai_sdk::Log::setLevel(ai_sdk::LogLevel::WARN);
 *
 * // 场景4：开发环境 - 开启调试
 * ai_sdk::Log::setLevel(ai_sdk::LogLevel::DEBUG);
 *
 * // 场景5：问题排查 - 临时开启调试后恢复
 * ai_sdk::Log::setLevel(ai_sdk::LogLevel::DEBUG);
 * // ... 排查问题 ...
 * ai_sdk::Log::reset();  // 恢复为 INFO 级别
 * @endcode
 */
class Log {
public:
    /**
     * @brief 设置 AI SDK 所有模块的日志级别
     *
     * 统一设置所有 AI SDK 内部模块的日志输出级别。
     * 设置后立即生效，无需重新初始化 SDK。
     *
     * @param level 要设置的日志级别
     *
     * @example
     * @code
     * // 设置为只显示错误
     * ai_sdk::Log::setLevel(ai_sdk::LogLevel::ERROR);
     *
     * // 设置为显示调试信息
     * ai_sdk::Log::setLevel(ai_sdk::LogLevel::DEBUG);
     * @endcode
     */
    static void setLevel(LogLevel level);

    /**
     * @brief 获取当前日志级别
     *
     * 返回通过 setLevel() 设置的当前日志级别。
     * 如果从未调用过 setLevel()，返回默认级别 INFO。
     *
     * @return 当前设置的日志级别
     *
     * @example
     * @code
     * LogLevel current = ai_sdk::Log::getLevel();
     * if (current == LogLevel::DEBUG) {
     *     // 当前处于调试模式
     * }
     * @endcode
     */
    static LogLevel getLevel();

    /**
     * @brief 禁用所有日志输出
     *
     * 便捷方法，等同于 setLevel(LogLevel::NONE)。
     * 完全禁止 AI SDK 的所有日志输出。
     *
     * @note 禁用日志后，即使发生错误也不会有任何输出。
     *       如需在生产环境保留错误日志，建议使用 setLevel(LogLevel::ERROR)。
     *
     * @example
     * @code
     * // 生产环境禁用日志
     * ai_sdk::Log::disable();
     * @endcode
     */
    static void disable();

    /**
     * @brief 恢复默认日志级别（INFO）
     *
     * 将日志级别重置为默认的 INFO 级别。
     * 适用于临时调整日志级别后恢复正常状态。
     *
     * @example
     * @code
     * // 临时开启调试
     * ai_sdk::Log::setLevel(ai_sdk::LogLevel::DEBUG);
     * // ... 排查问题 ...
     *
     * // 恢复正常
     * ai_sdk::Log::reset();
     * @endcode
     */
    static void reset();

private:
    // 禁止实例化（静态类）
    Log() = delete;
    Log(const Log&) = delete;
    Log& operator=(const Log&) = delete;
};

}  // namespace ai_sdk

