package com.fxzs.lingxiagent.lingxi.multimodal.utils.eventTracker

import android.content.Context
import com.cmdc.ai.assist.eventTracking.AIEventTracker
import org.json.JSONObject

/**
 * 跟踪核心对话模块行为类，负责处理核心对话相关功能的事件跟踪
 * @author 于海生
 * @since 2025-09-26
 * @param applicationContext 应用程序上下文，用于获取应用包名等信息
 * @param tracker AI事件跟踪器，用于处理和上传事件数据
 */
class TrackingCoreDialog(
    applicationContext: Context,
    tracker: AIEventTracker
) : TrackingBase(applicationContext, tracker) {

    // 核心对话模块相关事件跟踪方法将在此处定义
    // 例如：对话开始、消息发送、语音交互、多模态交互等事件
    override val eventMap: Map<String, String> = hashMapOf(

        /*========================= P0 优先级事件 ====================================*/

        // 核心对话模块
        "400001" to "语音输入开始",
        "400002" to "语音输入结束",
        "400008" to "文本消息发送",
        "400011" to "TTS播放开始",
        "400012" to "TTS播放完成",
        "400016" to "模型切换",
        "400018" to "模型性能监控",
        "400030" to "会话开始",
        "400031" to "会话结束",
        "400033" to "进入主页面",
        "400034" to "胶囊位点击",
        "400035" to "底部导航点击",
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
