package com.fxzs.lingxiagent.lingxi.multimodal.utils.eventTracker

import android.content.Context
import com.cmdc.ai.assist.eventTracking.AIEventTracker
import org.json.JSONObject

/**
 * 跟踪系统行为类，负责处理应用程序的启动和关闭事件跟踪
 * @author 于海生
 * @since 2025-09-26
 * @param applicationContext 应用程序上下文，用于获取应用包名等信息
 * @param tracker AI事件跟踪器，用于处理和上传事件数据
 */
class TrackingSystemBehavior(
    applicationContext: Context,
    tracker: AIEventTracker
) : TrackingBase(applicationContext, tracker) {

    override val eventMap: Map<String, String> = hashMapOf(

        /*========================= P0 优先级事件 ====================================*/

        // 系统行为
        "100001" to "启动app",
    )

    /**
     * 跟踪系统行为相关事件
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

    /**
     * 启动应用程序事件跟踪
     * 获取应用版本号和包名，构建启动事件数据并上传
     * @author 于海生
     * @since 2025-09-26
     */
    fun startApp() {
        val versionName = getAppVersion()
        val json = JSONObject()
        json.put("pkg", applicationContext.packageName)
        json.put("ver", versionName)

        if (!containsEventId(EventConstants.SystemBehavior.APP_START)) return
        if (getEventName(EventConstants.SystemBehavior.APP_START) == unknownEvent) return

        val eventData =
            createCommonEventData(
                EventConstants.SystemBehavior.APP_START,
                getEventName(EventConstants.SystemBehavior.APP_START),
                json
            )
        tracker.processAndUploadEvents(eventData)
    }

    /**
     * 关闭应用程序事件跟踪
     * 构建关闭事件数据并上传
     * 
     * 注意：该方法目前使用硬编码事件ID "100002"，该ID尚未在EventConstants中定义
     * TODO: 需要在EventConstants.SystemBehavior中添加APP_CLOSE常量
     * 
     * @author 于海生
     * @since 2025-09-26
     */
    private fun closeApp() {
        val json = JSONObject()
        // 可以在这里添加关闭应用时的额外信息，如：
        // - 应用使用时长
        // - 最后访问的页面
        // - 是否有未完成的任务等

        val eventData = createCommonEventData("100002", "close app", json)
        tracker.processAndUploadEvents(eventData)
    }

}
