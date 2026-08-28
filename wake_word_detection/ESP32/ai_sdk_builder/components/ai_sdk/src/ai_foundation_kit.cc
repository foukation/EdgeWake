/**
 * @file ai_foundation_kit.cc
 * @brief AIFoundationKit 实现
 *
 * AIFoundationKit 是 AI 功能基础工具包的实现，
 * 提供大模型闲聊和文本翻译功能。
 */

#include "ai_sdk/ai_foundation_kit.h"
#include "chatbot_client.h"
#include "translate_client.h"
#include "content_summary_client.h"
#include "inside_rc_chat_client.h"
#include "esp_log.h"

static const char* TAG = "AIFoundationKit";

namespace ai_sdk {

// ============================================================================
// AIFoundationKitImpl - PIMPL 实现类
// ============================================================================

/**
 * @class AIFoundationKitImpl
 * @brief AIFoundationKit 的内部实现类
 *
 * 使用 PIMPL 模式隐藏实现细节。
 */
class AIFoundationKitImpl {
public:
    AIFoundationKitImpl()
        : chatbot_client_(std::make_unique<ChatbotClient>())
        , translate_client_(std::make_unique<TranslateClient>())
        , content_summary_client_(std::make_unique<ContentSummaryClient>())
        , inside_rc_chat_client_(std::make_unique<InsideRcChatClient>()) {
        ESP_LOGI(TAG, "AIFoundationKitImpl created");
    }

    ~AIFoundationKitImpl() {
        ESP_LOGI(TAG, "AIFoundationKitImpl destroyed");
    }

    // Chatbot
    std::string largeModelChatbot(
        const ChatbotCompletionRequest& request,
        AIFoundationKit::ChatbotSuccessCallback onSuccess,
        AIFoundationKit::ChatbotErrorCallback onError) {
        
        return chatbot_client_->sendRequest(request, onSuccess, onError);
    }

    // Translate
    void textTranslate(
        const TranslationRequest& request,
        AIFoundationKit::TranslateSuccessCallback onSuccess,
        AIFoundationKit::TranslateErrorCallback onError) {
        
        translate_client_->sendRequest(request, TranslateClient::TranslateMode::MACHINE,
            onSuccess, onError);
    }

    void textTranslateWithModel(
        const TranslationRequest& request,
        AIFoundationKit::TranslateSuccessCallback onSuccess,
        AIFoundationKit::TranslateErrorCallback onError) {
        
        translate_client_->sendRequest(request, TranslateClient::TranslateMode::MODEL,
            onSuccess, onError);
    }

    // Content Summary
    std::string contentSummary(
        const ContentSummaryRequest& request,
        AIFoundationKit::ContentSummarySuccessCallback onSuccess,
        AIFoundationKit::ContentSummaryErrorCallback onError) {

        return content_summary_client_->sendRequest(request, onSuccess, onError);
    }

    // InsideRcChat（文本链路智能问答）
    std::string insideRcChat(
        const InsideRcChatRequest& request,
        AIFoundationKit::InsideRcChatSuccessCallback onSuccess,
        AIFoundationKit::InsideRcChatErrorCallback onError) {

        return inside_rc_chat_client_->sendRequest(request, onSuccess, onError);
    }

    bool cancelStreamRequest(const std::string& requestId) {
        // ====================================================================
        // 统一取消流式请求接口
        // ====================================================================
        // 
        // 设计说明：
        // - requestId 由 SSEClient::generateRequestId() 生成，格式为 sse_时间戳_随机数
        // - requestId 是全局唯一的，不可能有两个客户端持有相同的 ID
        // - 因此依次尝试各客户端取消，只会有一个成功
        // 
        // 支持的流式请求类型：
        // - largeModelChatbot() - 大模型闲聊
        // - contentSummary() - 内容摘要
        // - insideRcChat() - 文本链路智能问答
        // 
        // 返回值：
        // - true: 找到并成功取消请求
        // - false: 请求不存在或已完成
        // ====================================================================
        
        // 尝试在 chatbot 客户端中取消
        if (chatbot_client_->cancelRequest(requestId)) {
            return true;
        }
        
        // 尝试在 content_summary 客户端中取消
        if (content_summary_client_->cancelRequest(requestId)) {
            return true;
        }

        // 尝试在 inside_rc_chat 客户端中取消
        if (inside_rc_chat_client_->cancelRequest(requestId)) {
            return true;
        }

        return false;
    }

private:
    std::unique_ptr<ChatbotClient> chatbot_client_;
    std::unique_ptr<TranslateClient> translate_client_;
    std::unique_ptr<ContentSummaryClient> content_summary_client_;
    std::unique_ptr<InsideRcChatClient> inside_rc_chat_client_;
};

// ============================================================================
// AIFoundationKit 实现
// ============================================================================

AIFoundationKit::AIFoundationKit()
    : impl_(std::make_unique<AIFoundationKitImpl>()) {
}

AIFoundationKit::~AIFoundationKit() = default;

AIFoundationKit::AIFoundationKit(AIFoundationKit&&) noexcept = default;

AIFoundationKit& AIFoundationKit::operator=(AIFoundationKit&&) noexcept = default;

std::string AIFoundationKit::largeModelChatbot(
    const ChatbotCompletionRequest& request,
    ChatbotSuccessCallback onSuccess,
    ChatbotErrorCallback onError) {
    
    if (!impl_) {
        ESP_LOGE(TAG, "AIFoundationKit not initialized");
        if (onError) {
            onError("AIFoundationKit not initialized");
        }
        return "";
    }
    
    return impl_->largeModelChatbot(request, onSuccess, onError);
}

bool AIFoundationKit::cancelStreamRequest(const std::string& requestId) {
    if (!impl_) {
        return false;
    }
    return impl_->cancelStreamRequest(requestId);
}

void AIFoundationKit::textTranslate(
    const TranslationRequest& request,
    TranslateSuccessCallback onSuccess,
    TranslateErrorCallback onError) {
    
    if (!impl_) {
        ESP_LOGE(TAG, "AIFoundationKit not initialized");
        if (onError) {
            onError("AIFoundationKit not initialized");
        }
        return;
    }
    
    impl_->textTranslate(request, onSuccess, onError);
}

void AIFoundationKit::textTranslateWithModel(
    const TranslationRequest& request,
    TranslateSuccessCallback onSuccess,
    TranslateErrorCallback onError) {
    
    if (!impl_) {
        ESP_LOGE(TAG, "AIFoundationKit not initialized");
        if (onError) {
            onError("AIFoundationKit not initialized");
        }
        return;
    }
    
    impl_->textTranslateWithModel(request, onSuccess, onError);
}

std::string AIFoundationKit::contentSummary(
    const ContentSummaryRequest& request,
    ContentSummarySuccessCallback onSuccess,
    ContentSummaryErrorCallback onError) {
    
    if (!impl_) {
        ESP_LOGE(TAG, "AIFoundationKit not initialized");
        if (onError) {
            onError("AIFoundationKit not initialized");
        }
        return "";
    }
    
    return impl_->contentSummary(request, onSuccess, onError);
}

std::string AIFoundationKit::insideRcChat(
    const InsideRcChatRequest& request,
    InsideRcChatSuccessCallback onSuccess,
    InsideRcChatErrorCallback onError) {

    if (!impl_) {
        ESP_LOGE(TAG, "AIFoundationKit not initialized");
        if (onError) {
            onError("AIFoundationKit not initialized");
        }
        return "";
    }

    return impl_->insideRcChat(request, onSuccess, onError);
}

}  // namespace ai_sdk
