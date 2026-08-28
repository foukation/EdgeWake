package com.fxzs.lingxiagent.lingxi.multimodal.utils.eventTracker

import android.content.Context
import com.fxzs.lingxiagent.util.SharedPreferencesUtil

/**
 * 埋点工具类 - 统一管理所有埋点逻辑
 *
 * 业务代码直接调用此类方法，无需关心具体埋点实现细节
 * * 使用示例:
 *      * ```kotlin
 *      * // 在AiWorkFragment的onResume()中调用
 *      * TrackerUtils.trackEnterAIOfficePageEvent()
 *      * ```
 * @author 于海生
 * @since 2025-09-26
 */
object TrackerUtils {

    /**
     * 初始化事件追踪管理器并启动 app 埋点
     *
     * @param context 应用上下文
     */
    @JvmStatic
    fun initEventTrackerManager(context: Context) {
        EventTracker.initialize(context)
        EventTracker.create().startApp()
    }

    /**
     * 跟踪进入AI办公页面事件
     *
     * 事件ID: 200079
     * 说明: 进入AI办公模块
     * 触发时机: AiWorkFragment onResume()
     * 事件参数: user_id: 用户ID；current_page: AI办公页面
     * 业务价值: AI办公模块价值评估
     * 埋点类型: 页面埋点
     *
     */
    @JvmStatic
    fun trackEnterAIOfficePageEvent() {
        val userId = SharedPreferencesUtil.getUserIdStr()
        EventTracker.create()
            .buildParameters("user_id", userId)
            .buildParameters("current_page", "AI办公页面")
            .trackEvent(EventConstants.AIOffice.ENTER_AI_OFFICE_PAGE)
    }

    // 可以继续添加其他页面的埋点方法...

    /**
     * 跟踪进入智能体页面事件 (预留)
     * 事件ID: 300036
     */
    @JvmStatic
    fun trackEnterAgentPageEvent() {
        val userId = SharedPreferencesUtil.getUserIdStr()
        EventTracker.create()
            .buildParameters("user_id", userId)
            .buildParameters("current_page", "智能体页面")
            .trackEvent(EventConstants.AIAgent.ENTER_AGENT_PAGE)
    }

    /**
     * 跟踪进入主页面事件 (预留)
     * 事件ID: 400033
     */
    @JvmStatic
    fun trackEnterMainPageEvent() {
        val userId = SharedPreferencesUtil.getUserIdStr()
        EventTracker.create()
            .buildParameters("user_id", userId)
            .buildParameters("current_page", "主页面")
            .trackEvent(EventConstants.CoreDialog.ENTER_MAIN_PAGE)
    }

    /**
     * 跟踪胶囊位点击事件
     *
     * 事件ID: 400034
     * 说明: 功能胶囊点击
     * 触发时机: ChatFunctionAdapter点击事件
     * 事件参数: user_id: 用户ID；function_category: 胶囊位类型
     * 业务价值: 功能使用偏好分析
     * 埋点类型: 行为埋点
     */
    @JvmStatic
    fun trackCapsuleClickEvent(functionCategory: String) {
        val userId = SharedPreferencesUtil.getUserIdStr()
        EventTracker.create()
            .buildParameters("user_id", userId)
            .buildParameters("function_category", functionCategory)
            .trackEvent(EventConstants.CoreDialog.CAPSULE_POSITION_CLICK)
    }

    /**
     * 跟踪底部导航点击事件
     *
     * 事件ID: 400035
     * 说明: 底部Tab切换
     * 触发时机: MainActivity selectTab()调用
     * 事件参数: user_id: 用户ID；clicked_tab: 点击的Tab
     * 业务价值: 用户导航行为分析
     * 埋点类型: 行为埋点
     */
    @JvmStatic
    fun trackBottomNavigationClickEvent(clickedTab: String) {
        val userId = SharedPreferencesUtil.getUserIdStr()
        EventTracker.create()
            .buildParameters("user_id", userId)
            .buildParameters("clicked_tab", clickedTab)
            .trackEvent(EventConstants.CoreDialog.BOTTOM_NAVIGATION_CLICK)
    }

    /**
     * 跟踪会话创建事件
     *
     * 事件ID: 900001
     * 说明: 创建新的会话
     * 触发时机: 用户开始新的对话或任务时调用
     * 事件参数: user_id: 用户ID；session_id: 会话ID；
     * 业务价值: 会话生命周期管理和用户交互行为分析
     * 埋点类型: 会话埋点
     */
    @JvmStatic
    fun trackSessionCreateEvent(sessionId: String) {
        val userId = SharedPreferencesUtil.getUserIdStr()
        EventTracker.create()
            .buildParameters("user_id", userId)
            .buildParameters("session_id", sessionId)
            .trackEvent(EventConstants.SessionManagement.SESSION_CREATE)
    }

    /**
     * 跟踪异常中断事件
     *
     * 事件ID: 900002
     * 说明: 会话或任务异常中断
     * 触发时机: 发生异常导致会话中断时调用
     * 事件参数: user_id: 用户ID；session_id: 会话ID；error_code: 错误代码；error_message: 错误信息
     * 业务价值: 异常监控和系统稳定性分析
     * 埋点类型: 异常埋点
     */
    @JvmStatic
    fun trackSessionAbortEvent(sessionId: String, errorCode: String, errorMessage: String) {
        val userId = SharedPreferencesUtil.getUserIdStr()
        EventTracker.create()
            .buildParameters("user_id", userId)
            .buildParameters("session_id", sessionId)
            .buildParameters("error_code", errorCode)
            .buildParameters("error_message", errorMessage)
            .trackEvent(EventConstants.SessionManagement.SESSION_ABORT)
    }

    /**
     * 跟踪用户停止事件
     *
     * 事件ID: 900003
     * 说明: 用户主动点击停止按钮
     * 触发时机: 用户点击停止按钮时调用
     * 事件参数: user_id: 用户ID；session_id: 会话ID；stop_reason: 停止原因
     * 业务价值: 用户行为分析和产品优化
     * 埋点类型: 行为埋点
     */
    @JvmStatic
    fun trackUserStopEvent(sessionId: String) {
        val userId = SharedPreferencesUtil.getUserIdStr()
        EventTracker.create()
            .buildParameters("user_id", userId)
            .buildParameters("session_id", sessionId)
            .trackEvent(EventConstants.SessionManagement.USER_STOP_SESSION)
    }

    /**
     * 跟踪任务完成事件
     *
     * 事件ID: 900004
     * 说明: 任务正常完成
     * 触发时机: 任务成功完成时调用
     * 事件参数: user_id: 用户ID；session_id: 会话ID；
     * 业务价值: 任务成功率统计和性能分析
     * 埋点类型: 完成埋点
     */
    @JvmStatic
    fun trackTaskCompleteEvent(sessionId: String) {
        val userId = SharedPreferencesUtil.getUserIdStr()
        EventTracker.create()
            .buildParameters("user_id", userId)
            .buildParameters("session_id", sessionId)
            .trackEvent(EventConstants.SessionManagement.TASK_COMPLETE)
    }

    /**
     * 跟踪唤醒事件
     */
    @JvmStatic
    fun trackWakeAbortEvent(wakeUpStatus: Boolean,eventId: String) {
        val userId = SharedPreferencesUtil.getUserIdStr()
        EventTracker.create()
            .buildParameters("user_id", userId)
            .buildParameters("wake_status",wakeUpStatus )
            .trackEvent(eventId)
    }

    /**
     * 唤醒我咋事件
     */
    @JvmStatic
    fun trackWakeUpEvent() {
        val userId = SharedPreferencesUtil.getUserIdStr()
        EventTracker.create()
            .buildParameters("user_id", userId)
            .trackEvent(EventConstants.WakeUpManagement.WAKE_UP_CREATE)
    }
    @JvmStatic
    fun trackCommonEvent(eventId: String) {
        val userId = SharedPreferencesUtil.getUserIdStr()
        EventTracker.create()
            .buildParameters("user_id", userId)
            .trackEvent(eventId)
    }
}