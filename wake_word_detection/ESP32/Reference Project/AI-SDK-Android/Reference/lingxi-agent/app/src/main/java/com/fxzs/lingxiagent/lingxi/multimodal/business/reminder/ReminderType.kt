package com.ai.multimodal.business.reminder

/**
 * 定义药物提醒的方式
 */
enum class ReminderType {
    /**
     * 通知栏提醒
     */
    NOTIFICATION,
    
    /**
     * 系统闹钟提醒
     */
    SYSTEM_ALARM
}