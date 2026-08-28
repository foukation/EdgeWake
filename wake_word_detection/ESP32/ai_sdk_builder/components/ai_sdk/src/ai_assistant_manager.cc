/**
 * @file ai_assistant_manager.cc
 * @brief AI助手管理器实现文件
 *
 * 该文件实现了 AIAssistantManager 类的所有方法。
 * 提供设备注册、网关管理、数据上报等核心功能。
 *
 * 设计说明：
 * - AIAssistantManager 仅作为配置管理器和入口点
 * - 网关操作统一通过 gateWayHelp() 返回的 GateWay 对象进行
 * - 使用方式：manager.gateWayHelp().xxx()
 */

#include "ai_sdk/ai_assistant_manager.h"
#include "ai_sdk/asr_intelligent_dialogue.h"
#include "ai_sdk/speech_recognition_persistent.h"
#include "esp_log.h"
#include "esp_random.h"  // for esp_random() - 硬件随机数
#include <stdexcept>
#include <cstdio>
#include <cstdlib>  // for srand()

namespace ai_sdk {

/**
 * @brief 日志标签
 * 用于 ESP-IDF 日志系统标识日志来源
 */
static const char* TAG = "AIAssistantManager";

/**
 * @brief 单例实例指针
 * 指向唯一的 AIAssistantManager 实例，nullptr 表示未初始化
 */
AIAssistantManager* AIAssistantManager::instance_ = nullptr;

// ===== 静态方法实现 =====

/**
 * @brief 初始化 AI 助手管理器
 *
 * 创建管理器单例实例，保存配置并初始化内部组件。
 * packageName 默认在 AIAssistConfig 构造函数中设置为 "esp32.ai.assistant"。
 *
 * 线程安全性：非线程安全，应在程序启动时单线程调用
 * 幂等性：多次调用会被忽略（仅第一次生效）
 *
 * @param config AI助手配置对象（必须有效）
 */
void AIAssistantManager::initialize(std::unique_ptr<AIAssistConfig> config) {
    // 检查是否已初始化，防止重复创建
    if (!instance_) {
        // 创建新实例，转移配置 ownership
        instance_ = new AIAssistantManager(std::move(config));
        ESP_LOGI(TAG, "AIAssistantManager initialized");
    } else {
        // 已初始化，记录警告
        ESP_LOGW(TAG, "AIAssistantManager already initialized, ignoring");
    }
}

/**
 * @brief 获取 AI 助手管理器单例实例
 *
 * ESP32 优化说明：
 * - ESP-IDF 默认禁用 C++ 异常，throw 会导致 abort() 和设备重启
 * - 本实现采用"安全失败"模式：返回空实例 + 详细错误日志
 * - 程序不会 crash，但功能不会正常工作
 *
 * @return AIAssistantManager 实例的引用（如果未初始化，返回空的 dummy 实例）
 */
AIAssistantManager& AIAssistantManager::getInstance() {
    // 检查实例是否存在
    if (!instance_) {
        // 输出详细错误日志，帮助开发者定位问题
        ESP_LOGE(TAG, "");
        ESP_LOGE(TAG, "╔══════════════════════════════════════════════════════════════════╗");
        ESP_LOGE(TAG, "║  FATAL: AIAssistantManager not initialized!                      ║");
        ESP_LOGE(TAG, "╠══════════════════════════════════════════════════════════════════╣");
        ESP_LOGE(TAG, "║  You MUST call initialize() before using AI SDK.                 ║");
        ESP_LOGE(TAG, "║                                                                  ║");
        ESP_LOGE(TAG, "║  Example:                                                        ║");
        ESP_LOGE(TAG, "║    auto builder = std::make_unique<AIAssistConfig::Builder>();   ║");
        ESP_LOGE(TAG, "║    auto config = builder->deviceNo(\"YourDeviceSN\")               ║");
        ESP_LOGE(TAG, "║                          .deviceNoType(\"SN\")                     ║");
        ESP_LOGE(TAG, "║                          .productId(\"YourProductId\")             ║");
        ESP_LOGE(TAG, "║                          .productKey(\"YourProductKey\")           ║");
        ESP_LOGE(TAG, "║                          .build();                               ║");
        ESP_LOGE(TAG, "║    AIAssistantManager::initialize(std::move(config));            ║");
        ESP_LOGE(TAG, "╚══════════════════════════════════════════════════════════════════╝");
        ESP_LOGE(TAG, "");
        
        // 创建一个静态的空配置实例，避免返回 null 导致的 crash
        // 这是一个"安全失败"设计：程序不会 crash，但功能不会正常工作
        // 使用局部静态变量确保只创建一次，且线程安全（C++11保证）
        static AIAssistantManager dummy_instance(std::make_unique<AIAssistConfig>());
        
        // 标记为未正确初始化，业务可以通过 isInitialized() 检查
        dummy_instance.initialized_ = false;
        
        return dummy_instance;  // 返回空实例，业务可以继续，但功能失效
    }
    return *instance_;
}

/**
 * @brief 检查 AI 助手管理器是否已初始化
 *
 * 提供给业务层检查 SDK 是否已正确初始化的方法。
 * 建议在使用 SDK 功能前调用此方法检查状态。
 *
 * @return true 已初始化，false 未初始化
 */
bool AIAssistantManager::isInitialized() {
    return instance_ != nullptr && instance_->initialized_;
}

/**
 * @brief 销毁 AI 助手管理器实例
 *
 * 清理所有资源，释放内存，将实例指针置为 nullptr。
 *
 * 调用时机：
 * - 程序退出时
 * - 需要重置 SDK 状态时
 * - 单元测试结束时
 *
 * 注意事项：
 * - 销毁后必须重新调用 initialize() 才能使用
 * - 线程不安全，应在单线程环境调用
 */
void AIAssistantManager::destroyInstance() {
    // 检查是否有实例
    if (instance_) {
        // 删除实例，释放内存
        delete instance_;
        instance_ = nullptr;  // 置空指针
        ESP_LOGD(TAG, "AIAssistantManager destroyed");
    }
}

// ===== 构造函数和析构函数 =====

/**
 * @brief 私有构造函数
 *
 * 单例模式，只能通过 initialize() 静态方法创建实例。
 *
 * 构造流程：
 * 1. 保存配置（转移配置所有权）
 * 2. 初始化标记设为 false
 * 3. 调用 initWithConfig() 初始化内部组件
 *
 * @param config AI助手配置（唯一指针，所有权转移）
 */
AIAssistantManager::AIAssistantManager(std::unique_ptr<AIAssistConfig> config)
    : config_(std::move(config)),  // 转移所有权
      initialized_(false) {
    // 初始化内部组件
    initWithConfig();
}

/**
 * @brief 析构函数
 *
 * 清理所有资源，释放内存。
 *
 * 析构流程：
 * 1. 释放配置对象
 * 2. 记录销毁日志
 */
AIAssistantManager::~AIAssistantManager() {
    ESP_LOGD(TAG, "AIAssistantManager destroyed");
}

// ===== 内部初始化方法 =====

/**
 * @brief 使用配置初始化各个组件
 *
 * 初始化内部状态和各个客户端组件。
 *
 * 初始化流程：
 * 1. 验证配置的完整性
 * 2. 设置包名
 * 3. 标记初始化完成
 *
 * 注意事项：
 * - 此方法在构造函数中调用
 * - 失败不会抛出异常，但会记录错误日志
 */
void AIAssistantManager::initWithConfig() {
    // 验证配置是否存在
    if (!config_) {
        ESP_LOGE(TAG, "Config is null during initialization");
        lastError_ = "Config is null during initialization";
        return;
    }

    // ========================================================================
    // 初始化随机数生成器
    // ========================================================================
    // 使用 ESP32 硬件真随机数生成器初始化 rand() 种子
    // 这确保了 assist_utils.cc 中的 generateSessionId() 每次启动都生成不同的随机数
    // 如果不初始化，rand() 序列在每次启动后相同
    srand(esp_random());

    // packageName 默认在 AIAssistConfig 构造函数中设置
    // 这里仅记录日志，不需要额外设置
    ESP_LOGD(TAG, "Package name: %s", config_->packageName.c_str());

    // 记录配置信息（脱敏处理）
    ESP_LOGD(TAG, "Configuration initialized:");
    ESP_LOGD(TAG, "  Product ID: %s", config_->productId.c_str());
    ESP_LOGD(TAG, "  Device No: %s", config_->deviceNo.c_str());
    ESP_LOGD(TAG, "  Gateway enabled: %s", config_->enableGateway ? "yes" : "no");

    // 标记初始化完成
    // 注意：GateWay 完全无状态，每次调用时从 AIAssistantManager 获取配置
    initialized_ = true;
    ESP_LOGD(TAG, "AIAssistantManager internal components initialized");
    ESP_LOGD(TAG, "GateWay is stateless (no setConfig needed)");
}

// ===== 核心管理方法实现 =====

/**
 * @brief 获取网关对象
 *
 * 提供对 GateWay 的访问，用于管理网关相关的操作。
 *
 * 设计模式：单例模式（返回同一个 GateWay 实例）
 *
 * @return GateWay 实例的引用
 *
 * 使用示例：
 * auto& gateway = manager.gateWayHelp();
 * gateway.obtainDeviceInformation(onSuccess, onError);  // ✓ 设备注册
 * gateway.getGateWay(onSuccess, onError);               // ✓ 获取网关
 * gateway.dataReport(request, onSuccess, onError);      // ✓ 数据上报
 */
GateWay& AIAssistantManager::gateWayHelp() {
    return gateWay_;
}

/**
 * @brief 获取 ASR 智能对话实例
 *
 * ESP32 实现：
 * - 返回单例引用（AsrIntelligentDialogue 使用单例模式）
 * - 嵌入式系统资源优化
 * - 通过 WebSocket 与云端服务器通信的完整 ASR 功能
 *
 * @return AsrIntelligentDialogue& ASR对话单例实例的引用
 */
AsrIntelligentDialogue& AIAssistantManager::asrIntelligentDialogueHelp() {
    // 通过友元类访问返回单例实例
    return AsrIntelligentDialogue::getInstance();
}

/**
 * @brief 获取持续语音识别实例
 *
 * 通过友元访问私有单例，与 asrIntelligentDialogueHelp() 模式一致。
 *
 * @return SpeechRecognitionPersistent& 单例引用
 */
SpeechRecognitionPersistent& AIAssistantManager::speechRecognitionPersistentHelp() {
    return SpeechRecognitionPersistent::getInstance();
}

/**
 * @brief 提供 ASR 翻译帮助
 *
 * 提供语音识别翻译功能。
 *
 * @return 翻译实例指针，ESP32 上返回 nullptr
 */
void* AIAssistantManager::asrTranslationHelp() {
    ESP_LOGW(TAG, "ASR Translation not implemented on ESP32");
    return nullptr;
}

/**
 * @brief 获取 AI 功能基础工具包实例
 *
 * 提供大模型闲聊（Chatbot）和文本翻译功能。
 *
 * @return AIFoundationKit 实例的引用
 */
AIFoundationKit& AIAssistantManager::aiFoundationKit() {
    return aiFoundationKit_;
}

/**
 * @brief 修改 TTS 配置信息
 *
 * 动态更新文本转语音的配置参数。
 * 
 * 新配置会在下一次智能对话的 start signal 中生效。
 * TTS 配置会被发送到服务器的 SpeechState 中，控制语音合成效果。
 * 
 * 使用位置: AsrIntelligentDialogue::sendStartSignal() 中的 SpeechState
 *
 * @param tts TTS 配置对象，包含 voiceId、speed、pitch、volume
 */
void AIAssistantManager::changeTtsConfig(const TtsConfig& tts) {
    // ========================================================================
    // 验证配置参数是否在有效范围内
    // ========================================================================
    // speed, pitch, volume 的取值范围都是 0-15
    if (!tts.isValid()) {
        ESP_LOGW(TAG, "Invalid TTS config: speed=%d, pitch=%d, volume=%d "
                      "(valid range: 0-15)", 
                 tts.speed, tts.pitch, tts.volume);
        return;
    }

    // ========================================================================
    // 保存配置到 AIAssistConfig
    // ========================================================================
    if (config_) {
        config_->dialogueTtsConfig = tts;
        ESP_LOGD(TAG, "TTS configuration updated: voiceId=%d, speed=%d, pitch=%d, volume=%d",
                 tts.voiceId, tts.speed, tts.pitch, tts.volume);
    } else {
        ESP_LOGE(TAG, "Cannot change TTS config: config_ is null");
    }
}

} // namespace ai_sdk
