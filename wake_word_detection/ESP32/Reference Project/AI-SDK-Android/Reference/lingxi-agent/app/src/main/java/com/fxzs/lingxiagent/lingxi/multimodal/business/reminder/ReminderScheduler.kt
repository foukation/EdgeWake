package com.ai.multimodal.business.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.AlarmClock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * 提醒调度器，负责设置和管理药物提醒闹钟
 */
class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * 为指定的药物提醒设置闹钟
     *
     * @param reminder 药物提醒对象
     * @param reminderType 提醒类型（通知栏或系统闹钟）
     * @return 成功设置的闹钟数量
     */
    fun scheduleReminder(
        reminder: MedicationReminder,
        reminderType: ReminderType = ReminderType.NOTIFICATION
    ): Int {
        return when (reminderType) {
            ReminderType.NOTIFICATION -> scheduleNotifications(reminder)
            ReminderType.SYSTEM_ALARM -> scheduleSystemAlarms(reminder)
        }
    }

    /**
     * 设置通知栏提醒
     */
    private fun scheduleNotifications(reminder: MedicationReminder): Int {
        var scheduledCount = 0
        
        // 从明天开始，设置指定天数的闹钟
        for (dayOffset in 0 until reminder.actualDays) {
            val date = reminder.startDate.plusDays(dayOffset.toLong())
            
            // 为每一天的每个时间点设置闹钟
            for (time in reminder.times) {
                if (scheduleNotification(reminder, date, time)) {
                    scheduledCount++
                }
            }
        }
        
        return scheduledCount
    }

    /**
     * 为指定的日期和时间设置单个通知提醒
     */
    private fun scheduleNotification(
        reminder: MedicationReminder,
        date: LocalDate,
        time: LocalTime
    ): Boolean {
        try {
            val dateTime = LocalDateTime.of(date, time)
            val triggerTimeMillis = dateTime
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            // 创建闹钟的Intent
            val intent = createReminderIntent(reminder)
            
            // 使用唯一的requestCode，确保不会覆盖其他闹钟
            val requestCode = generateRequestCode(reminder, date, time)
            val pendingIntent = PendingIntent.getBroadcast(
                context, 
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 根据Android版本选择合适的API设置闹钟
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            }
            
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * 设置系统闹钟提醒
     */
    private fun scheduleSystemAlarms(reminder: MedicationReminder): Int {
        var scheduledCount = 0
        
        // 从明天开始，设置指定天数的闹钟
        for (dayOffset in 0 until reminder.actualDays) {
            val date = reminder.startDate.plusDays(dayOffset.toLong())
            
            // 为每一天的每个时间点设置系统闹钟
            for (time in reminder.times) {
                if (scheduleSystemAlarm(reminder, date, time)) {
                    scheduledCount++
                }
            }
        }
        
        return scheduledCount
    }
    
    /**
     * 设置单个系统闹钟
     */
    private fun scheduleSystemAlarm(
        reminder: MedicationReminder,
        date: LocalDate,
        time: LocalTime
    ): Boolean {
        try {
            // 创建系统闹钟的Intent
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_MESSAGE, reminder.getTitle())
                putExtra(AlarmClock.EXTRA_HOUR, time.hour)
                putExtra(AlarmClock.EXTRA_MINUTES, time.minute)
                
                // 设置闹钟的日期
                val days = arrayListOf<Int>()
                when (date.dayOfWeek.value) {
                    1 -> days.add(Calendar.MONDAY)
                    2 -> days.add(Calendar.TUESDAY)
                    3 -> days.add(Calendar.WEDNESDAY)
                    4 -> days.add(Calendar.THURSDAY)
                    5 -> days.add(Calendar.FRIDAY)
                    6 -> days.add(Calendar.SATURDAY)
                    7 -> days.add(Calendar.SUNDAY)
                }
                if (days.isNotEmpty()) {
                    putExtra(AlarmClock.EXTRA_DAYS, days)
                }
                
                // 不显示UI，直接设置闹钟
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                
                // 添加药物标识信息
                putExtra("medication_name", reminder.medicationName)
                putExtra("reminder_date", date.toString())
            }
            
            // 设置启动标志
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * 设置包含多个重复日期的系统闹钟
     *
     * @param medicationName 药物名称
     * @param time 提醒时间
     * @param repeatDays 重复的星期几列表，如 ["Monday", "Tuesday", ...]
     * @return 是否成功设置闹钟
     */
    fun scheduleSystemAlarmWithMultipleDays(
        medicationName: String,
        time: LocalTime,
        repeatDays: List<String>
    ): Boolean {
        try {
            // 将API返回的星期几名称转换为系统闹钟API需要的整数值
            val days = ArrayList<Int>()
            
            // 添加所有重复日期
            for (dayName in repeatDays) {
                when (dayName) {
                    "Monday" -> days.add(Calendar.MONDAY)
                    "Tuesday" -> days.add(Calendar.TUESDAY)
                    "Wednesday" -> days.add(Calendar.WEDNESDAY)
                    "Thursday" -> days.add(Calendar.THURSDAY)
                    "Friday" -> days.add(Calendar.FRIDAY)
                    "Saturday" -> days.add(Calendar.SATURDAY)
                    "Sunday" -> days.add(Calendar.SUNDAY)
                }
            }
            
            if (days.isEmpty()) return false
            
            // 创建唯一的闹钟标识，使用时间作为部分标识
            val alarmId = "${medicationName}_${time.hour}_${time.minute}".hashCode() and 0x7FFFFFFF
            
            // 创建系统闹钟的Intent
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                // 使用时间信息定制闹钟标题，以区分不同时间的闹钟
                val timeLabel = if (time.hour < 12) "早上" else "晚上"
                putExtra(AlarmClock.EXTRA_MESSAGE, "${timeLabel}吃$medicationName")
                putExtra(AlarmClock.EXTRA_HOUR, time.hour)
                putExtra(AlarmClock.EXTRA_MINUTES, time.minute)
                
                // 设置所有重复日期
                putExtra(AlarmClock.EXTRA_DAYS, days)
                
                // 不显示UI，直接设置闹钟
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                
                // 添加药物标识信息和自定义的闹钟ID
                putExtra("medication_name", medicationName)
                putExtra("custom_alarm_id", alarmId.toString())
                putExtra("alarm_time", "${time.hour}:${time.minute}")
            }
            
            // 设置启动标志
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * 创建药物提醒的Intent
     */
    private fun createReminderIntent(reminder: MedicationReminder): Intent {
        return Intent(context, MedicationReminderReceiver::class.java).apply {
            putExtra(MedicationReminderReceiver.EXTRA_MEDICATION_NAME, reminder.medicationName)
            putExtra(MedicationReminderReceiver.EXTRA_REMINDER_TITLE, reminder.getTitle())
        }
    }

    /**
     * 生成唯一的请求代码，用于区分不同的闹钟
     */
    private fun generateRequestCode(reminder: MedicationReminder, date: LocalDate, time: LocalTime): Int {
        // 使用药物名称、日期和时间生成唯一的请求代码
        val uniqueString = "${reminder.medicationName}_${date}_${time}"
        return uniqueString.hashCode()
    }
    
    /**
     * 取消指定药物的所有提醒
     */
    fun cancelReminders(medicationName: String) {
        // 这里只是一个简单的实现，在实际应用中可能需要存储和检索已设置的闹钟
        // 在更复杂的实现中，应该有一个数据库或存储机制来跟踪所有已设置的闹钟
    }
    
    /**
     * 日历常量，用于系统闹钟的星期几设置
     */
    private object Calendar {
        const val SUNDAY = 1
        const val MONDAY = 2
        const val TUESDAY = 3
        const val WEDNESDAY = 4
        const val THURSDAY = 5
        const val FRIDAY = 6
        const val SATURDAY = 7
    }
}