package com.fxzs.lingxiagent.lingxi.multimodal.utils.eventTracker

import android.content.Context
import com.cmdc.ai.assist.eventTracking.AIEventTracker
import org.json.JSONObject

/**
 * 跟踪核心对话模块行为类，负责处理核心对话相关功能的事件跟踪
 * @param applicationContext 应用程序上下文，用于获取应用包名等信息
 * @param tracker 会话管理模块
 */
class TrackingSession(
    applicationContext: Context,
    tracker: AIEventTracker
) : TrackingBase(applicationContext, tracker) {

    // 核心对话模块相关事件跟踪方法将在此处定义
    // 例如：对话开始、消息发送、语音交互、多模态交互等事件
    override val eventMap: Map<String, String> = hashMapOf(

        /*========================= P0 优先级事件 ====================================*/

        // 核心对话模块
        "900001" to "会话创建",
        "900002" to "会话异常中断",
        "900003" to "用户停止会话",
        "900004" to "任务完成"
    )

    /**
     * 跟踪核心对话相关事件
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
