/**
 * @file api_config.h
 * @brief API配置管理类（内部头文件）
 *
 * ============================================================================
 * ⚠️ 内部头文件 - 请勿在 SDK 外部使用
 * ============================================================================
 *
 * 此文件是 AI SDK 的内部实现细节，不属于公开 API。
 * 上层业务代码不应直接引用此头文件。
 *
 * 如需配置 API 相关参数，请使用：
 * - AIAssistantManager::initialize() 进行 SDK 初始化
 * - AIAssistConfig::Builder 设置设备/产品信息
 */
#pragma once

#include <string>

namespace ai_sdk {

/**
 * @class ApiConfig
 * @brief API配置管理类（内部使用）
 *
 * 集中管理所有API接口的配置信息，包括：
 * - 云端服务基础URL和接口路径
 * - HTTP请求超时时间
 * - 代理配置（运行时动态调整）
 * - 认证令牌
 *
 * 特点：
 * - 使用静态成员，全局共享配置
 * - 代理配置可在运行时动态修改（通过GatewayClient获取）
 * - 认证信息需要在设备注册后设置
 */
class ApiConfig {
public:
    /**
     * 终端智能服务平台基础URL
     * 所有API请求的基础地址
     */
    static const char* TERMINAL_INTELLIGENT_SERVICE_PLATFORM_BASE_URL;

    /**
     * 终端智能服务平台基础URL（测试环境）
     * 备用地址：需要连测试服时，手动替换请求代码中使用的常量
     */
    static const char* TERMINAL_INTELLIGENT_SERVICE_PLATFORM_BASE_URL_TEST;

    /**
     * 网关API路径
     * 获取代理服务器配置
     */
    static const char* GATEWAY_API;

    /**
     * 设备信息获取API路径
     * 设备注册认证接口
     */
    static const char* OBTAIN_DEVICE_INFORMATION_API;

    /**
     * 设备数据上报API路径
     * 心跳/数据上报接口
     */
    static const char* DEVICE_DATA_REPORT_API;

    /**
     * WebSocket ASR基础URL
     * WebSocket语音识别服务基础地址
     */
    static const char* WSS_WEBSOCKET_ASR_BASE_URL;

    /**
     * WebSocket ASR基础URL（测试环境）
     * 备用地址：需要连测试服时，手动替换请求代码中使用的常量
     */
    static const char* WSS_WEBSOCKET_ASR_BASE_URL_TEST;

    /**
     * ASR智能对话API路径（v2版本）
     * 实时ASR识别和智能对话
     */
    static const char* ASR_INTELLIGENT_DIALOGUE_API;

    /**
     * ASR智能对话API路径（v2版本）
     * 实时ASR识别和智能对话
     * 测试接口
     */
    static const char* ASR_INTELLIGENT_DIALOGUE_API_TEST;

    /**
     * 自动语音识别API路径（v1版本）
     * 标准ASR识别接口
     */
    static const char* AUTOMATIC_SPEECH_RECOGNITION_API;

    /**
     * 长语音ASR识别API路径
     * 支持长语音的持久化ASR识别
     */
    static const char* AUTOMATIC_SPEECH_RECOGNITION_PERSISTENT_API;

    /**
     * ASR实时翻译API路径
     * 语音识别并实时翻译
     */
    static const char* ASR_TRANSLATION_API;

    // ========================================================================
    // AIFoundationKit API 端点
    // ========================================================================

    /**
     * Chatbot 闲聊 API 路径 (v2)
     * 大模型闲聊接口，支持流式和非流式请求
     */
    static const char* CHAT_BOT_COMPLETIONS_API;

    /**
     * 文本翻译 API 路径 (v1)
     * 机器翻译接口，支持 200+ 种语言
     */
    static const char* TEXT_TRANSLATE_API;

    /**
     * 文本翻译 API 路径 (v2)
     * 模型翻译接口，使用大模型进行翻译，支持约 90 种语言
     */
    static const char* TEXT_TRANSLATE_MODEL_API;

    /**
     * 内容摘要 API 路径
     * 用于对长文本进行智能摘要处理
     */
    static const char* NOTE_SUMMARY_API;

    /**
     * 文本链路智能问答 API 路径 (v1)
     * 设备端 NLU 及聊天接口，文本输入走智能对话后端
     * 返回与语音助手相同的 DCS 指令集（Speak/Play/Nlu 等）
     */
    static const char* INSIDE_RC_CHAT_API;

    /**
     * HTTP请求超时时间（毫秒）
     * 默认15秒
     */
    static const long TIMEOUT;

    /**
     * 是否使用代理
     * 由GatewayClient根据云端配置动态设置
     * true: 使用代理服务器，false: 直接连接
     */
    static bool useAgent;

    /**
     * 代理服务器基础URL
     * 当useAgent为true时使用此地址
     */
    static std::string agentBaseUrl;

    /**
     * API访问令牌
     * 用于代理服务器认证
     */
    static std::string apiToken;

    /**
     * 设备认证令牌
     * 设备注册后获得的认证信息
     * TODO: 目前未使用，需要实现设备签名逻辑
     */
    static std::string auth_token;
};

} // namespace ai_sdk

