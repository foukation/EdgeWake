package com.fxzs.lingxiagent.lingxi.multimodal.utils.eventTracker

import android.content.Context
import com.cmdc.ai.assist.eventTracking.AIEventTracker
import org.json.JSONObject

/**
 * 跟踪AI翻译模块行为类，负责处理AI翻译相关功能的事件跟踪
 * @author 于海生
 * @since 2025-09-26
 * @param applicationContext 应用程序上下文，用于获取应用包名等信息
 * @param tracker AI事件跟踪器，用于处理和上传事件数据
 */
class TrackingAITranslation(
    applicationContext: Context,
    tracker: AIEventTracker
) : TrackingBase(applicationContext, tracker) {

    // AI翻译模块相关事件跟踪方法将在此处定义
    // 例如：翻译请求、语言选择、翻译结果、翻译历史等事件
    override val eventMap: Map<String, String> = hashMapOf(

        /*========================= P0 优先级事件 ====================================*/

        // AI翻译
        "600001" to "ai_translate_enter",
        "600002" to "ai_translate_interface_show",
        "600006" to "ai_translate_text_request",
        "600007" to "ai_translate_text_success",
        "600008" to "ai_translate_text_failed",
        "600009" to "ai_translate_voice_start",
        "600012" to "ai_translate_voice_result",
    )

    /**
     * 跟踪AI翻译相关事件
     * @param eventId 事件唯一标识符
     * @param extraJson 事件额外参数，JSON格式
     */
    override fun trackEvent(eventId: String, extraJson: JSONObject) {
        // 验证事件是否存在
        if (getEventName(eventId) == unknownEvent) return
        
        // 创建事件数据并上传
        val eventData = createCommonEventData(eventId, getEventName(eventId), extraJson)
        tracker.processAndUploadEvents(eventData)
    }
}
