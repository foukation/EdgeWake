// 内部头文件（位于 src/include/）
#include "api_config.h"

namespace ai_sdk {

// ===== 基础URL配置 =====
// 这些配置在编译时确定，提供云端服务的基础信息

/**
 * 终端智能服务平台基础URL
 * 中国移动物联网平台，提供设备管理和通信服务
 * 使用HTTPS协议，端口11443
 */
const char* ApiConfig::TERMINAL_INTELLIGENT_SERVICE_PLATFORM_BASE_URL =
    "https://ivs.chinamobiledevice.com:11443";

/**
 * 终端智能服务平台基础URL（测试环境）
 * 备用地址，默认不使用；连测试服时手动把请求代码里用的常量换成它
 */
const char* ApiConfig::TERMINAL_INTELLIGENT_SERVICE_PLATFORM_BASE_URL_TEST =
    "https://62b98tux.cxzfdm.com:30101";

/**
 * 网关API路径
 * 用于获取代理服务器配置
 * 完整URL: {BASE_URL}/apgp/pl
 */
const char* ApiConfig::GATEWAY_API = "/apgp/pl";

/**
 * 设备信息获取API路径
 * 设备注册认证接口，通过设备号获取设备凭证
 * 完整URL: {BASE_URL}/v2/customer/device/secret/info
 */
const char* ApiConfig::OBTAIN_DEVICE_INFORMATION_API = "/v2/customer/device/secret/info";

/**
 * 设备数据上报API路径
 * 心跳/数据上报接口，报告设备状态和活跃信息
 * 完整URL: {BASE_URL}/v2/customer/device/report
 */
const char* ApiConfig::DEVICE_DATA_REPORT_API = "/v2/customer/device/report";

/**
 * WebSocket ASR基础URL
 * WebSocket语音识别服务基础地址
 * 使用WebSocket安全连接，端口11443
 */
const char* ApiConfig::WSS_WEBSOCKET_ASR_BASE_URL = "wss://ivs.chinamobiledevice.com:11443";

/**
 * WebSocket ASR基础URL（测试环境）
 * 备用地址，默认不使用；连测试服时手动把请求代码里用的常量换成它
 */
const char* ApiConfig::WSS_WEBSOCKET_ASR_BASE_URL_TEST = "wss://62b98tux.cxzfdm.com:30101";

/**
 * ASR智能对话API路径（v2版本）
 * 实时ASR识别和智能对话
 * 完整URL: {WSS_URL}/app-ws/v2/asr
 */
const char* ApiConfig::ASR_INTELLIGENT_DIALOGUE_API = "/app-ws/v2/asr";

/**
 * ASR智能对话API路径（v2版本）
 * 实时ASR识别和智能对话
 * 完整URL: {WSS_URL}/app-ws/v2/asr
 * 测试接口
 */
const char* ApiConfig::ASR_INTELLIGENT_DIALOGUE_API_TEST = "/ai-admin-beta/app-ws/v2/asr";

/**
 * 自动语音识别API路径（v1版本）
 * 标准ASR识别接口
 * 完整URL: {WSS_URL}/app-ws/v1/asr
 */
const char* ApiConfig::AUTOMATIC_SPEECH_RECOGNITION_API = "/app-ws/v1/asr";

/**
 * 长语音ASR识别API路径
 * 支持长语音的持久化ASR识别
 * 完整URL: {WSS_URL}/app-ws/v1/long-asr
 */
const char* ApiConfig::AUTOMATIC_SPEECH_RECOGNITION_PERSISTENT_API = "/app-ws/v1/long-asr";

/**
 * ASR实时翻译API路径
 * 语音识别并实时翻译
 * 完整URL: {WSS_URL}/app-ws/v1/realtime_speech_trans
 */
const char* ApiConfig::ASR_TRANSLATION_API = "/app-ws/v1/realtime_speech_trans";

// ===== AIFoundationKit API 端点 =====

/**
 * Chatbot 闲聊 API 路径 (v2)
 * 大模型闲聊接口，支持流式和非流式请求
 * 完整URL: {BASE_URL}/device-api/ai/v2/chat/completions
 */
const char* ApiConfig::CHAT_BOT_COMPLETIONS_API = "/device-api/ai/v2/chat/completions";

/**
 * 文本翻译 API 路径 (v1)
 * 机器翻译接口，支持 200+ 种语言
 * 完整URL: {BASE_URL}/device-api/ai/v1/text-translate
 */
const char* ApiConfig::TEXT_TRANSLATE_API = "/device-api/ai/v1/text-translate";

/**
 * 文本翻译 API 路径 (v2)
 * 模型翻译接口，使用大模型进行翻译，支持约 90 种语言
 * 完整URL: {BASE_URL}/device-api/ai/v2/text-translate
 */
const char* ApiConfig::TEXT_TRANSLATE_MODEL_API = "/device-api/ai/v2/text-translate";

/**
 * 内容摘要 API 路径
 * 用于对长文本进行智能摘要处理
 * 完整URL: {BASE_URL}/device-api/ai/v1/note-summary
 */
const char* ApiConfig::NOTE_SUMMARY_API = "/device-api/ai/v1/note-summary";

/**
 * 文本链路智能问答 API 路径 (v1)
 * 设备端 NLU 及聊天接口，文本输入走智能对话后端
 * 完整URL: {BASE_URL}/device-api/ai/v1/rc-chat
 */
const char* ApiConfig::INSIDE_RC_CHAT_API = "/device-api/ai/v1/rc-chat";

/**
 * HTTP请求超时时间（毫秒）
 * 设置为15秒，考虑网络环境和设备性能
 * 可根据实际需求调整：
 * - 降低：提高响应速度，但可能增加超时错误
 * - 提高：减少超时错误，但会增加等待时间
 */
const long ApiConfig::TIMEOUT = 15000L;

// ===== 运行时配置 =====
// 这些配置在程序运行时动态设置

/**
 * 是否使用代理（默认：false）
 * 由GatewayClient在启动时从云端获取代理配置后设置
 * true: 后续请求通过代理服务器转发
 * false: 直接连接云端服务器
 */
bool ApiConfig::useAgent = false;

/**
 * 代理服务器基础URL（默认：空）
 * 当useAgent为true时使用此地址
 * 格式: http(s)://proxy.example.com:port
 */
std::string ApiConfig::agentBaseUrl = "";

/**
 * API访问令牌（默认：空）
 * 用于代理服务器认证
 * 由GatewayClient获取并设置
 */
std::string ApiConfig::apiToken = "";

/**
 * 设备认证令牌（默认：空）
 * 设备注册后获得的认证信息
 * TODO: 目前未使用，需要实现设备签名逻辑
 * 计划用途：为每个API请求生成签名，防止伪造请求
 */
std::string ApiConfig::auth_token = "";

} // namespace ai_sdk