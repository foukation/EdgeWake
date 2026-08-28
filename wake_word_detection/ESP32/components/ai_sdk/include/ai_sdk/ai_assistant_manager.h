#pragma once

#include "ai_sdk/types.h"
#include "ai_sdk/gate_way.h"
#include "ai_sdk/asr_intelligent_dialogue.h"
#include "ai_sdk/ai_foundation_kit.h"
#include <functional>
#include <memory>
#include <string>

namespace ai_sdk {

// ============================================================================
// TtsConfig - TTS（文本转语音）配置类
// ============================================================================
/**
 * @brief TTS（文本转语音）配置类
 * 
 * 用于统一管理语音合成的参数配置。
 * 在智能对话的 start signal 中，TTS 配置会被发送给服务器以控制语音合成效果。
 * 
 * 使用位置：
 * - AsrIntelligentDialogue::sendStartSignal() 中的 SpeechState
 * - AIAssistantManager::changeTtsConfig() 动态更新配置
 * 
 * @see AIAssistConfig::dialogueTtsConfig
 * @see AIAssistantManager::changeTtsConfig()
 */
struct TtsConfig {
    /**
     * @brief 音色 ID
     * 
     * 不同的 ID 对应不同的发音人，由服务端提供音色列表。
     * 默认值 4100 为「活力女主播」。
     */
    int voiceId;
    
    /**
     * @brief 语速
     * 
     * 取值范围 0-15，值越大语速越快。
     * - 0: 最慢
     * - 5: 中速（默认）
     * - 15: 最快
     */
    int speed;
    
    /**
     * @brief 音调
     * 
     * 取值范围 0-15，值越大音调越高。
     * - 0: 最低
     * - 5: 中音调（默认）
     * - 15: 最高
     */
    int pitch;
    
    /**
     * @brief 音量
     * 
     * 取值范围 0-15，值越大音量越大。
     * - 0: 静音
     * - 15: 最大音量（默认）
     */
    int volume;
    
    /**
     * @brief 默认构造函数
     * 
     * 创建默认 TTS 配置：
     * - voiceId: 4100（活力女主播）
     * - speed: 5（中速）
     * - pitch: 5（中音调）
     * - volume: 15（最大音量）
     */
    TtsConfig() : voiceId(4100), speed(5), pitch(5), volume(15) {}
    
    /**
     * @brief 带参数构造函数
     * 
     * @param vid 音色 ID
     * @param spd 语速 (0-15)
     * @param pit 音调 (0-15)
     * @param vol 音量 (0-15)
     */
    TtsConfig(int vid, int spd, int pit, int vol)
        : voiceId(vid), speed(spd), pitch(pit), volume(vol) {}
    
    /**
     * @brief 验证配置参数是否在有效范围内
     * 
     * @return true 如果所有参数都在有效范围内
     * @return false 如果任何参数超出范围
     */
    bool isValid() const {
        return speed >= 0 && speed <= 15 &&
               pitch >= 0 && pitch <= 15 &&
               volume >= 0 && volume <= 15;
    }
};

// ============================================================================
// AIAssistConfig - AI助手配置类
// ============================================================================
/**
 * @brief AI助手配置类
 *
 * 提供 Builder 模式进行配置构建。
 * 配置内容包括设备信息、产品信息、网关设置、TTS配置等。
 * 使用 Builder 模式可以链式调用，提高代码的可读性和可维护性。
 */
class AIAssistConfig {
public:
    /**
     * Builder 类
     *
     * 采用构建者模式，通过链式调用来构建 AIAssistConfig 对象。
     * 每个设置方法都返回 Builder 自身的引用，支持流畅的 API 调用。
     *
     * 使用示例：
     * auto builder = std::make_unique<AIAssistConfig::Builder>();
     * auto config = builder->deviceId("device-001")
     *                      ->productId("PROD001")
     *                      ->productKey("KEY001")
     *                      ->build();
     */
    class Builder {
    public:
        /**
         * 构造函数
         * 创建一个新的 AIAssistConfig 实例并开始构建
         */
        Builder() : config(std::make_unique<AIAssistConfig>()) {}

        /**
         * 设置设备ID
         *
         * 设备ID是设备在平台上注册成功后获得的唯一标识
         *
         * @param deviceId 设备唯一标识符
         * @return Builder 引用，支持链式调用
         */
        Builder& deviceId(const std::string& deviceId) {
            config->deviceId = deviceId;
            return *this;
        }

        /**
         * 设置设备密钥
         *
         * 设备密钥是设备在平台上注册成功后获得的认证密钥，用于后续请求的签名验证
         *
         * @param secret 设备密钥
         * @return Builder 引用，支持链式调用
         */
        Builder& deviceSecret(const std::string& secret) {
            config->deviceSecret = secret;
            return *this;
        }

        /**
         * 设置产品ID
         *
         * 产品ID是平台创建产品时生成的唯一标识
         *
         * @param productId 产品标识符
         * @return Builder 引用，支持链式调用
         */
        Builder& productId(const std::string& productId) {
            config->productId = productId;
            return *this;
        }

        /**
         * 设置产品密钥
         *
         * 产品密钥是平台创建产品时生成的认证密钥
         *
         * @param productKey 产品密钥
         * @return Builder 引用，支持链式调用
         */
        Builder& productKey(const std::string& productKey) {
            config->productKey = productKey;
            return *this;
        }

        /**
         * 设置设备号类型
         *
         * 设备号类型标识设备编号的类型，如 SN、MAC 等
         *
         * @param type 设备号类型，默认为 "SN"
         * @return Builder 引用，支持链式调用
         */
        Builder& deviceNoType(const std::string& type) {
            config->deviceNoType = type;
            return *this;
        }

        /**
         * 设置设备号
         *
         * 设备号是产品内唯一标识设备的序列号
         *
         * @param deviceNo 设备序列号
         * @return Builder 引用，支持链式调用
         */
        Builder& deviceNo(const std::string& deviceNo) {
            config->deviceNo = deviceNo;
            return *this;
        }

        /**
         * 启用或禁用网关代理
         *
         * 如果启用，SDK 会自动从云端获取网关配置并使用代理服务器
         *
         * @param enable 是否启用网关代理，默认为 true
         * @return Builder 引用，支持链式调用
         */
        Builder& enableGateway(bool enable) {
            config->enableGateway = enable;
            return *this;
        }

        /**
         * 设置中控配置版本
         *
         * 用于智能对话（inside_rc）请求中的 rc_version 字段
         * 服务器根据此版本号返回对应的对话配置
         *
         * @param version 中控配置版本号，如 "9"
         * @return Builder 引用，支持链式调用
         */
        Builder& centralConfigVersion(const std::string& version) {
            config->centralConfigVersion = version;
            return *this;
        }

        /**
         * @brief 设置 Chatbot 闲聊授权令牌
         * 
         * 用于 Chatbot 闲聊功能的授权验证。
         *
         * @param t 授权令牌字符串
         * @return Builder 引用，支持链式调用
         */
        Builder& token(const std::string& t) {
            config->token = t;
            return *this;
        }

        /**
         * @brief 设置智能对话 TTS 配置
         * 
         * TTS 配置会在智能对话的 start signal 中发送给服务器，
         * 用于控制语音合成的音色、语速、音调和音量。
         *
         * @param tts TTS 配置对象
         * @return Builder 引用，支持链式调用
         * 
         * @see TtsConfig
         */
        Builder& dialogueTtsConfig(const TtsConfig& tts) {
            config->dialogueTtsConfig = tts;
            return *this;
        }

        /**
         * @brief 设置客户端类型标识
         * 
         * 用于服务器识别不同类型的客户端，可能影响服务器返回的差异化响应。
         *
         * @param id 客户端类型标识字符串
         * @return Builder 引用，支持链式调用
         */
        Builder& clientID(const std::string& id) {
            config->clientID = id;
            return *this;
        }

        /**
         * @brief 设置是否启用语音自动增益控制（AGC）
         * 
         * AGC 可以自动调节音频输入的增益，使音量保持稳定。
         *
         * @param enable 是否启用 AGC，默认为 false
         * @return Builder 引用，支持链式调用
         */
        Builder& enableVoiceGain(bool enable) {
            config->enableVoiceGain = enable;
            return *this;
        }

        /**
         * 构建 AIAssistConfig 对象
         *
         * 完成所有配置后，调用此方法生成最终的配置对象
         *
         * @return 构建好的 AIAssistConfig 唯一指针
         */
        std::unique_ptr<AIAssistConfig> build() {
            return std::move(config);
        }

    private:
        std::unique_ptr<AIAssistConfig> config;  ///< 正在构建的配置对象
    };

    // ===== 配置字段 =====

    // ---- 设备信息 ----
    std::string deviceId;          ///< 设备唯一标识（平台注册后获得）
    std::string deviceSecret;      ///< 设备密钥（平台注册后获得）
    std::string productId;         ///< 产品ID（平台创建产品时生成）
    std::string productKey;        ///< 产品密钥（平台创建产品时生成）
    std::string deviceNoType;      ///< 设备号类型，如 "SN"、"MAC" 等，默认为 "SN"
    std::string deviceNo;          ///< 设备序列号（产品内唯一）

    // ---- 网关配置 ----
    /**
     * @brief 是否启用网关代理
     * 
     * 如果启用，SDK 会自动从云端获取网关配置并使用代理服务器。
     * 
     * 默认值：true
     */
    bool enableGateway;

    // ---- TTS 配置 ----
    /**
     * @brief 语音助手 TTS 配置
     * 
     * 用于语音助手时的语音播报参数配置。
     * 在智能对话的 start signal 中会发送给服务器。
     * 
     * 使用位置: AsrIntelligentDialogue::sendStartSignal() 中的 SpeechState
     * 
     * @see TtsConfig
     */
    TtsConfig dialogueTtsConfig;

    // ---- 授权配置 ----
    /**
     * @brief Chatbot 闲聊授权令牌
     * 
     * 用于 Chatbot 闲聊功能的授权验证。
     */
    std::string token;

    // ---- 客户端信息 ----
    /**
     * @brief 客户端类型标识
     * 
     * 用于服务器识别不同类型的客户端，可能影响服务器返回的差异化响应。
     */
    std::string clientID;

    // ---- 音频配置 ----
    /**
     * @brief 是否启用语音自动增益控制（AGC）
     * 
     * AGC 可以自动调节音频输入的增益，使音量保持稳定。
     * 
     * 默认值：false
     */
    bool enableVoiceGain;

    // ---- 中控配置 ----
    /**
     * @brief 中控配置版本
     *
     * 用于智能对话（inside_rc）请求中的 rc_version 字段
     * 服务器根据此版本号返回对应的对话配置
     *
     * 如果版本号不存在，服务器会返回错误：
     * {"status":"error","type":"error","result":"inside_rc req err: ...version not exist..."}
     */
    std::string centralConfigVersion;

    // ---- 其他配置 ----
    /**
     * @brief 包名
     * 
     * 默认值为 "esp32.ai.assistant"。
     */
    std::string packageName;

    /**
     * @brief 默认构造函数
     * 
     * 初始化所有字段为默认值：
     * - enableGateway: true
     * - dialogueTtsConfig: TtsConfig() (默认 TTS 配置)
     * - enableVoiceGain: false
     * - packageName: "esp32.ai.assistant"
     * 
     * 注意：初始化列表顺序必须与成员声明顺序一致，否则会触发 -Wreorder 警告
     */
    AIAssistConfig()
        : enableGateway(true)           // 行 352
        , dialogueTtsConfig()           // 行 366
        , enableVoiceGain(false)        // 行 397
        , packageName("esp32.ai.assistant")  // 行 421
        {}
};

/**
 * AI助手管理器
 *
 * AI SDK 的中央管理类，采用单例模式设计，提供统一的入口点来管理所有 AI 相关的服务和功能。
 *
 * 核心功能：
 * - 管理设备注册和认证流程
 * - 管理网关配置和代理设置
 * - 管理数据上报和心跳
 * - 管理各种 AI 服务
 * - 提供统一的状态管理和错误处理
 *
 * 生命周期：
 * 1. 创建配置 AIAssistConfig
 * 2. 调用 initialize() 初始化管理器
 * 3. 调用 getInstance() 获取实例
 * 4. 调用各功能方法
 * 5. 程序结束时自动销毁
 *
 * 使用示例：
 * // 1. 创建配置
 * auto builder = std::make_unique<AIAssistConfig::Builder>();
 * auto config = builder->deviceNo("ESP32-001")
 *                      ->productId("PROD001")
 *                      ->productKey("KEY001")
 *                      ->build();
 *
 * // 2. 初始化管理器
 * AIAssistantManager::initialize(std::move(config));
 *
 * // 3. 获取实例并调用方法
 * auto& manager = AIAssistantManager::getInstance();
 * manager.obtainDeviceInformation();
 */
class AIAssistantManager {
public:
    /**
     * @brief 初始化 AI 助手管理器
     *
     * 必须在调用 getInstance() 之前先调用此方法。
     *
     * 初始化过程：
     * 1. 创建单例实例
     * 2. 保存配置信息
     * 3. 初始化内部组件
     * 4. 记录初始化日志
     *
     * @param config AI 助手配置对象，包含所有必要的配置信息
     *
     * 安全说明：
     * - 线程安全，可以被多次调用（第二次及之后调用会被忽略）
     * - 幂等性，重复调用不会产生副作用
     *
     * 使用示例：
     * @code
     * auto builder = std::make_unique<AIAssistConfig::Builder>();
     * auto config = builder->deviceNo("ESP32-001")
     *                      ->productId("PROD001")
     *                      ->productKey("KEY001")
     *                      ->build();
     * AIAssistantManager::initialize(std::move(config));
     * @endcode
     */
    static void initialize(std::unique_ptr<AIAssistConfig> config);

    /**
     * 获取 AI 助手管理器单例实例
     *
     * 返回唯一的全局实例，如果未初始化会抛出异常。
     *
     * 注意事项：
     * - 必须在 initialize() 成功后调用
     * - 非线程安全，建议在程序启动时单线程调用
     * - 返回实例的引用，确保实例不会被销毁
     *
     * @return AIAssistantManager 实例的引用
     * @throws std::runtime_error 如果管理器未初始化
     *
     * 使用示例：
     * // 推荐用法：先检查是否初始化
     * if (AIAssistantManager::isInitialized()) {
     *     auto& manager = AIAssistantManager::getInstance();
     *     manager.gateWayHelp().obtainDeviceInformation(...);
     * } else {
     *     ESP_LOGE(TAG, "AI SDK not initialized");
     * }
     */
    static AIAssistantManager& getInstance();

    /**
     * 检查 AI 助手管理器是否已初始化
     *
     * 提供给业务层检查 SDK 是否已正确初始化的方法。
     * 建议在使用 SDK 功能前调用此方法检查状态。
     *
     * @return true 已初始化，false 未初始化
     *
     * 使用示例：
     * if (AIAssistantManager::isInitialized()) {
     *     auto& manager = AIAssistantManager::getInstance();
     *     // 使用 manager...
     * }
     */
    static bool isInitialized();

    /**
     * 销毁 AI 助手管理器实例
     *
     * 清理所有资源，释放内存。
     *
     * 调用时机：
     * - 程序退出时
     * - 需要重置 SDK 状态时
     * - 单元测试结束时
     *
     * 注意事项：
     * 销毁后，必须重新调用 initialize() 才能再次使用
     */
    static void destroyInstance();

    // ===== 核心管理方法 =====

    /**
     * 提供 Gateway 网关和设备接入实例
     *
     * 每次调用返回同一个 GateWay 实例，提供网关相关的操作。
     *
     * 网关功能包括：
     * - 获取网关配置
     * - 获取设备信息
     * - 数据上报
     *
     * 设计模式：单例模式（GateWay 在管理器中是单例）
     *
     * @return GateWay 实例的引用
     *
     * 使用示例：
     * auto& gateway = manager.gateWayHelp();
     * gateway.getGateWay(onSuccess, onError);
     * gateway.obtainDeviceInformation(onSuccess, onError);
     * gateway.dataReport(request, onSuccess, onError);
     */
    GateWay& gateWayHelp();

    /**
     * 获取 ASR 智能对话实例
     *
     * 提供实时语音识别和智能对话的单例实例。
     *
     * 使用示例：
     * @code
     * auto& asr = AIAssistantManager::getInstance().asrIntelligentDialogueHelp();
     * asr.setCallbacks(...);
     * asr.start();
     * asr.sendAudio(data, len);
     * asr.stop();
     * @endcode
     *
     * @return AsrIntelligentDialogue& ASR对话实例的引用
     */
    AsrIntelligentDialogue& asrIntelligentDialogueHelp();

    /**
     * 提供 ASR 智能对话（会话版本）
     *
     * 支持多轮对话的 ASR 功能。
     *
     * @return 会话实例指针，ESP32 上返回 nullptr
     */
    void* asrIntelligentConversationHelp();

    /**
     * 提供 ASR 翻译帮助
     *
     * 提供语音识别翻译功能。
     *
     * @return 翻译实例指针，ESP32 上返回 nullptr（功能未实现）
     */
    void* asrTranslationHelp();

    /**
     * @brief 获取 AI 功能基础工具包实例
     *
     * 提供大模型闲聊（Chatbot）和文本翻译功能。
     *
     * 使用示例：
     * @code
     * auto& kit = AIAssistantManager::getInstance().aiFoundationKit();
     *
     * // Chatbot 闲聊
     * ChatbotCompletionRequest req;
     * req.messages = {{"user", "你好"}};
     * req.stream = true;
     * kit.largeModelChatbot(req, onSuccess, onError);
     *
     * // 文本翻译
     * TranslationRequest transReq;
     * transReq.targetLanguage = LanguageCode::ZH;
     * transReq.originText = "Hello";
     * kit.textTranslate(transReq, onSuccess, onError);
     * @endcode
     *
     * @return AIFoundationKit 实例的引用
     *
     * @see AIFoundationKit
     */
    AIFoundationKit& aiFoundationKit();

    /**
     * @brief 修改 TTS 配置信息
     *
     * 用于动态更新文本转语音的配置，如音色、语速、音调和音量。
     * 
     * 新配置会在下一次智能对话的 start signal 中生效。
     *
     * @param tts TTS 配置对象
     *
     * 使用示例：
     * @code
     * TtsConfig newTts(4100, 10, 5, 15);  // 音色4100, 语速10, 音调5, 音量15
     * manager.changeTtsConfig(newTts);
     * @endcode
     * 
     * @see TtsConfig
     */
    void changeTtsConfig(const TtsConfig& tts);

    /**
     * 获取配置对象的常量引用
     *
     * 为其他类（如 GateWay）提供对配置的只读访问
     * 保持封装性的同时允许必要的内部访问
     * 当对象是const时会调用此方法
     *
     * @return AIAssistConfig 的常量引用
     */
    const AIAssistConfig& config() const {
        return *config_;
    }

    /**
     * 获取配置对象的可变引用
     *
     * 提供对配置的读写访问，用于在运行时更新配置参数
     * 典型场景：设备注册成功后更新deviceId和deviceSecret
     * 当对象是非const时会调用此方法（函数重载）
     *
     * 使用示例：
     * auto& config = AIAssistantManager::getInstance().config();
     * config.deviceId = "new_device_id";
     * config.deviceSecret = "new_device_secret";
     *
     * @return AIAssistConfig 的可变引用
     */
    AIAssistConfig& config() {
        return *config_;
    }

private:
    /**
     * @brief 私有构造函数
     *
     * 单例模式，只能在 initialize() 中创建实例。
     *
     * @param config AI助手配置（所有权转移）
     */
    explicit AIAssistantManager(std::unique_ptr<AIAssistConfig> config);

    /**
     * 析构函数
     *
     * 清理所有资源
     */
    ~AIAssistantManager();

    // 禁止拷贝和赋值（单例模式）
    AIAssistantManager(const AIAssistantManager&) = delete;
    AIAssistantManager& operator=(const AIAssistantManager&) = delete;

    // ===== 内部方法 =====

    /**
     * 使用配置初始化各个组件
     *
     * 初始化流程：
     * 1. 验证配置有效性
     * 2. 保存 packageName
     * 3. 初始化设备信息
     * 4. 初始化设备管理器
     *
     * 注意事项：
     * - 此方法在构造函数中调用
     * - 失败不会抛出异常，但会记录错误日志
     */
    void initWithConfig();

    // ===== 静态成员 =====

    static AIAssistantManager* instance_;           ///< 单例唯一实例

    // ===== 实例成员 =====

    std::unique_ptr<AIAssistConfig> config_;        ///< AI 助手配置
    GateWay gateWay_;                               ///< 网关对象
    AIFoundationKit aiFoundationKit_;               ///< AI 功能基础工具包
    bool initialized_;                              ///< 初始化标志位
    std::string lastError_;                         ///< 最后错误信息
};

} // namespace ai_sdk
