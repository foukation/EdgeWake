package com.fxzs.lingxiagent.lingxi.multimodal.utils.eventTracker

import android.content.Context
import com.cmdc.ai.assist.eventTracking.AIEventTracker
import org.json.JSONObject

/**
 * 跟踪用户登录模块行为类，负责处理用户登录相关功能的事件跟踪
 * @author 于海生
 * @since 2025-09-26
 * @param applicationContext 应用程序上下文，用于获取应用包名等信息
 * @param tracker AI事件跟踪器，用于处理和上传事件数据
 */
class TrackingUserLogin(
    applicationContext: Context,
    tracker: AIEventTracker
) : TrackingBase(applicationContext, tracker) {

    // 用户登录模块相关事件跟踪方法将在此处定义
    // 例如：登录尝试、登录成功、登录失败、退出登录、第三方登录等事件
    override val eventMap: Map<String, String> = hashMapOf(

        /*========================= P0 优先级事件 ====================================*/

        // 用户登录
        "700001" to "应用启动登录检查",
        "700002" to "一键登录页面展示",
        "700004" to "一键登录点击",
        "700007" to "一键登录结果",
        "700008" to "短信验证码页面展示",
        "700009" to "发送验证码点击",
        "700012" to "短信登录提交",
        "700013" to "短信登录结果",
        "700016" to "密码登录提交",
        "700017" to "密码登录结果",
        "700025" to "登录成功跳转",
        "700032" to "登录错误处理",
    )

    /**
     * 跟踪用户登录相关事件
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
