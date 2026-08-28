/**
 * @file ai_sdk_log.cc
 * @brief AI SDK 日志控制接口实现
 *
 * 实现 ai_sdk_log.h 中定义的日志控制接口。
 * 内部维护所有 AI SDK 模块的 TAG 列表，对外部完全隐藏。
 *
 * @note 日志级别设置通过 ESP-IDF 的 esp_log_level_set() 实现，
 *       每个模块的 TAG 独立设置，确保精确控制。
 *
 * @copyright Copyright (c) 2024
 */

#include "ai_sdk/ai_sdk_log.h"

#include <cstddef>

namespace ai_sdk {

// ============================================================================
// 内部常量定义
// ============================================================================

/**
 * @brief AI SDK 所有内部模块的 TAG 列表
 *
 * 该列表包含所有 AI SDK 内部模块使用的日志 TAG。
 * 当调用 Log::setLevel() 时，会遍历此列表设置每个模块的日志级别。
 *
 * @note 此列表对外部用户完全隐藏，用户无需了解内部模块结构。
 * @note 新增模块时，需要在此列表中添加对应的 TAG。
 *
 * TAG 命名规范：
 * - 与源文件中定义的 static const char* TAG 保持一致
 * - 使用 PascalCase 命名风格
 */
static const char* const AI_SDK_INTERNAL_TAGS[] = {
    // ========================================================================
    // 核心管理模块
    // ========================================================================
    "AIAssistantManager",  // SDK 入口，单例管理器

    // ========================================================================
    // 语音识别与对话模块
    // ========================================================================
    "AsrIntelligentDialogue",       // ASR 智能对话
    "AsrWebsocket",                 // WebSocket 客户端（ASR 通信）
    "SpeechRecognitionPersistent",  // 持续识别（v0.9.6 新增，补登记）

    // ========================================================================
    // 网关与设备模块
    // ========================================================================
    "GateWay",        // 网关转发层（门面模式）
    "GatewayClient",  // 网关客户端（代理配置获取）
    "DeviceClient",   // 设备客户端（设备注册认证）
    "ReportClient",   // 上报客户端（数据上报和心跳）

    // ========================================================================
    // 基础设施模块
    // ========================================================================
    "HTTPClient",   // HTTP 客户端封装
    "AssistUtils",  // 工具函数（URL 构建、签名生成）

    // ========================================================================
    // AIFoundationKit 模块（大模型闲聊、翻译等）
    // ========================================================================
    "AIFoundationKit",   // AI 功能基础工具包
    "ChatbotClient",     // Chatbot 闲聊客户端
    "SSEClient",         // SSE 流式客户端
    "TranslateClient",   // 文本翻译客户端
    "ContentSummaryClient",  // 内容摘要客户端（补登记）
    "InsideRcChatClient",    // 文本链路智能问答客户端（补登记）
};

/**
 * @brief 内部模块 TAG 数量
 */
static constexpr size_t AI_SDK_TAG_COUNT =
    sizeof(AI_SDK_INTERNAL_TAGS) / sizeof(AI_SDK_INTERNAL_TAGS[0]);

// ============================================================================
// 静态成员变量
// ============================================================================

/**
 * @brief 当前日志级别
 *
 * 用于跟踪通过 setLevel() 设置的日志级别。
 * 初始值为 INFO，与 ESP-IDF 默认行为一致。
 */
static LogLevel s_current_level = LogLevel::INFO;

// ============================================================================
// Log 类实现
// ============================================================================

void Log::setLevel(LogLevel level) {
    // 保存当前日志级别
    s_current_level = level;

    // 转换为 ESP-IDF 日志级别
    esp_log_level_t esp_level = static_cast<esp_log_level_t>(level);

    // 遍历所有内部模块，设置日志级别
    for (size_t i = 0; i < AI_SDK_TAG_COUNT; ++i) {
        esp_log_level_set(AI_SDK_INTERNAL_TAGS[i], esp_level);
    }
}

LogLevel Log::getLevel() {
    // 返回通过 setLevel() 设置的当前日志级别
    return s_current_level;
}

void Log::disable() {
    setLevel(LogLevel::NONE);
}

void Log::reset() {
    setLevel(LogLevel::INFO);
}

}  // namespace ai_sdk

