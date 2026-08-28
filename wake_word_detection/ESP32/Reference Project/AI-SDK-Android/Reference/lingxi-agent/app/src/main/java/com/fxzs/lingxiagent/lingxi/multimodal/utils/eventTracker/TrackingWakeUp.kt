package com.fxzs.lingxiagent.lingxi.multimodal.utils.eventTracker

import android.content.Context
import com.cmdc.ai.assist.eventTracking.AIEventTracker
import org.json.JSONObject

/**
 * 跟踪核心对话模块行为类，负责处理核心对话相关功能的事件跟踪
 * @param applicationContext 应用程序上下文，用于获取应用包名等信息
 * @param tracker 唤醒
 */
class TrackingWakeUp(
    applicationContext: Context,
    tracker: AIEventTracker
) : TrackingBase(applicationContext, tracker) {

    // 核心对话模块相关事件跟踪方法将在此处定义
    // 例如：对话开始、消息发送、语音交互、多模态交互等事件
    override val eventMap: Map<String, String> = hashMapOf(

        /*========================= P0 优先级事件 ====================================*/

        // 唤醒开关 + 唤醒触发
        "500001" to "语音唤醒开关",
        "500002" to "按键唤醒开关",
        "500003" to "键盘唤醒开关",
        "500010" to "触发语音唤醒",
        "500012" to "触发键盘唤醒",
        "500013" to "触发电源键唤醒",

        // 小组件 - 快捷入口
        "500014" to "灵犀快捷入口",

        // 小组件 - 自动执行
        "1000002" to "自动执行组件-发布任务",
        "1000003" to "自动执行组件-看电视",
        "1000004" to "自动执行组件-电脑办公",
        "1000005" to "自动执行组件-餐饮外卖",
        "1000006" to "自动执行组件-购物比价",

        // 小组件 - AI办公
        "1000007" to "AI办公组件-问问灵犀",
        "1000008" to "AI办公组件-AI会议",
        "1000009" to "AI办公组件-AIPPT",
        "1000010" to "AI办公组件-深度研究",
        "1000011" to "AI办公组件-同声传译"

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
