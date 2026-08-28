package com.fxzs.lingxiagent.lingxi.multimodal.utils.eventTracker

import android.content.Context
import com.cmdc.ai.assist.eventTracking.AIEventTracker
import org.json.JSONObject

/**
 * 跟踪智能体模块行为类，负责处理智能体相关功能的事件跟踪
 * @author 于海生
 * @since 2025-09-26
 * @param applicationContext 应用程序上下文，用于获取应用包名等信息
 * @param tracker AI事件跟踪器，用于处理和上传事件数据
 */
class TrackingAIAgent(
    applicationContext: Context,
    tracker: AIEventTracker
) : TrackingBase(applicationContext, tracker) {

    // 智能体模块相关事件跟踪方法将在此处定义
    // 例如：智能体创建、配置、交互、管理等事件
    override val eventMap: Map<String, String> = hashMapOf(

        /*========================= P0 优先级事件 ====================================*/

        // 智能体模块
        "300001" to "火车票查询启动",
        "300002" to "机票查询启动",
        "300003" to "酒店预订启动",
        "300004" to "旅游计划生成",
        "300006" to "交通卡片点击",
        "300021" to "视频摘要生成",
        "300022" to "视频推荐展示",
        "300031" to "卡片报告生成",
        "300032" to "金融数据查询",
        "300041" to "话费充值启动",
        "300042" to "话费余额查询",
        "300043" to "流量使用查询",
        "300051" to "研究任务创建",
        "300053" to "网络搜索启动",
        "300055" to "研究报告完成",
        "300036" to "进入智能体页面",
    )

    /**
     * 跟踪智能体相关事件
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
