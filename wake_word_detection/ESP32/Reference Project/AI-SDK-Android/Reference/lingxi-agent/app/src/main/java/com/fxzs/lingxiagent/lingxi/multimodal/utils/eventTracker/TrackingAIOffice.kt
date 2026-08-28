package com.fxzs.lingxiagent.lingxi.multimodal.utils.eventTracker

import android.content.Context
import com.cmdc.ai.assist.eventTracking.AIEventTracker
import org.json.JSONObject

/**
 * 跟踪AI办公模块行为类，负责处理AI办公相关功能的事件跟踪
 * @author 于海生
 * @since 2025-09-26
 * @param applicationContext 应用程序上下文，用于获取应用包名等信息
 * @param tracker AI事件跟踪器，用于处理和上传事件数据
 */
class TrackingAIOffice(
    applicationContext: Context,
    tracker: AIEventTracker
) : TrackingBase(applicationContext, tracker) {

    // AI办公模块相关事件跟踪方法将在此处定义
    // 例如：文档处理、表格操作、演示文稿制作等事件
    override val eventMap: Map<String, String> = hashMapOf(

        /*========================= P0 优先级事件 ====================================*/

        // AI办公模块
        "200001" to "ai_meeting_enter",
        "200002" to "ai_meeting_start",
        "200003" to "ai_meeting_transcript",
        "200006" to "ai_meeting_qa_send",
        "200007" to "ppt_topic_input",
        "200008" to "ppt_topic_submit",
        "200012" to "ai_drawing_enter",
        "200013" to "ai_drawing_style_select",
        "200014" to "ai_drawing_prompt_input",
        "200017" to "ai_drawing_generate_submit",
        "200018" to "ai_drawing_result_view",
        "200019" to "translate_mode_select",
        "200020" to "translate_language_select",
        "200021" to "translate_start",
        "200037" to "ppt_generation_complete",
        "200042" to "ai_drawing_new_creation",
        "200053" to "translate_listen_mode",
        "200054" to "translate_dialogue_mode",
        "200056" to "translate_text_result",
        "200079" to "进入AI办公页面",
    )

    /**
     * 跟踪AI办公相关事件
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
