package com.fxzs.lingxiagent.lingxi.gui_agent.entity


enum class TaskStatus (val alias: Int) {

    SUCCESS(10000),//成功
    SCREEN_SHOT_ERROR(10001), //截图失败
    ACCESSIBILITY_NO_OPEN(10002),//无障碍服务未开启
    OPEN_APP(10003),//打开app
    NETWORK_ERROR(10004),//网络错误
    MULTIPLE_CONVERSATION(10005),//多轮会话
    MULTIPLE_OPERATIONS(10006),//多次操作
    OPEN_MENU(10007),//打开按钮
    CLOSE_MENU(10008),//关闭按钮
    EXCEPTION(10009),//异常
    TASK_INTERRUPTION(10010),//任务中断
    TASK_EXECUTE_FAIL(10011),//任务执行失败
    MANUAL_TAKEOVER(10012),//手动接管
}

/**
 * 简单的agent状态管理器
 */
object AgentStatus {
    // 状态值常量
    const val STATUS_IDLE = "空闲"      // 空闲状态
    const val STATUS_RUNNING = "执行中"  // 执行中状态

    // 当前状态（默认空闲）
    @Volatile
    private var currentStatus: String = STATUS_IDLE

    /**
     * 获取当前状态值
     */
    fun getStatus(): String = currentStatus

    /**
     * 设置状态值
     * @param status 状态（空闲/执行中）
     */
    fun setStatus(status: String) {
        currentStatus = when(status) {
            STATUS_IDLE -> STATUS_IDLE
            STATUS_RUNNING -> STATUS_RUNNING
            else -> STATUS_IDLE // 未知状态默认为空闲
        }
    }

    /**
     * 检查是否为空闲状态
     */
    fun isIdle(): Boolean = currentStatus == STATUS_IDLE

    /**
     * 检查是否为执行中状态
     */
    fun isRunning(): Boolean = currentStatus == STATUS_RUNNING
}

enum class ExecuteStatus (val alias: Int) {
    INPUT(10100),//输入
    OPEN_APP_FAIL(10101)//打开应用失败
}

