package com.fxzs.lingxiagent.lingxi.multimodal.utils.eventTracker

import android.content.Context
import com.cmdc.ai.assist.eventTracking.AIEventTracker
import org.json.JSONObject

/**
 * 跟踪用户资料模块行为类，负责处理"我的"页面相关功能的事件跟踪
 * @author 于海生
 * @since 2025-09-26
 * @param applicationContext 应用程序上下文，用于获取应用包名等信息
 * @param tracker AI事件跟踪器，用于处理和上传事件数据
 */
class TrackingUserProfile(
    applicationContext: Context,
    tracker: AIEventTracker
) : TrackingBase(applicationContext, tracker) {

    // 用户资料模块相关事件跟踪方法将在此处定义
    // 例如：个人信息查看、设置修改、账户管理、偏好设置等事件
    override val eventMap: Map<String, String> = hashMapOf(

        /*========================= P0 优先级事件 ====================================*/

        // 我的
        "800001" to "我的页面访问",
        "800005" to "历史记录访问",
        "800007" to "设置页面访问",
        "800008" to "大模型切换",
        "800015" to "注销账号点击",
        "800017" to "检查更新操作",
        "800018" to "更新确认",
        "800021" to "反馈页面访问",
        "800022" to "反馈提交成功"
    )

    /**
     * 跟踪用户资料相关事件
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
