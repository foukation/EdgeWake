package com.example.device_control.alarm

/**
 *创建者：ZyOng
 *描述：
 *创建时间：2025/6/24 4:11 PM
 */


data class AlarmConfig(var type: AlarmType, var repeatDays: ArrayList<Int>?,var hour: Int,var minute: Int,var text :String = "闹钟提醒")

enum class AlarmType {
    ONCE, DAILY
}

enum class Date(var date:String) {
    Today("Today"), Tomorrow("Tomorrow")
}
