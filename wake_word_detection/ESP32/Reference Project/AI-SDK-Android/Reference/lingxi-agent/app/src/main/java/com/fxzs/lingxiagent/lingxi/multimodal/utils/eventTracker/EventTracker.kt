package com.fxzs.lingxiagent.lingxi.multimodal.utils.eventTracker

import android.content.Context
import org.json.JSONObject

/**
 * @author 于海生
 * @since 2025-09-26
 * 事件追踪器类，用于构建和发送事件追踪数据
 */
class EventTracker {

    companion object {
        /**
         * 创建 EventTracker 实例
         * @return EventTracker 实例对象
         */
        fun create(): EventTracker {
            return EventTracker()
        }

        /**
         * 初始化事件追踪管理器
         * @param context 应用上下文对象
         */
        fun initialize(context: Context) {
            EventTrackerManager.initialize(context)
        }
    }

    /** 存储事件额外参数的JSON对象 */
    private val extraJson = JSONObject()

    /**
     * 构建事件参数，将键值对添加到参数JSON对象中
     * @param key 参数键名
     * @param value 参数值
     * @return 当前EventTracker实例，支持链式调用
     */
    fun buildParameters(key: String, value: String): EventTracker {
        extraJson.put(key, value)
        return this
    }
    fun buildParameters(key: String, value: Boolean): EventTracker {
        extraJson.put(key, value)
        return this
    }

    /**
     * 启动应用事件追踪
     */
    fun startApp() {
        EventTrackerManager.INSTANCE?.startApp()
    }

    /**
     * 追踪指定事件ID的事件
     * @param eventId 事件唯一标识符
     */
    fun trackEvent(eventId: String) {
        EventTrackerManager.INSTANCE?.trackEvent(eventId, extraJson)
    }
}
