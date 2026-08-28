package com.ai.multimodal.business.reminder

import android.content.Context
import com.ai.multimodal.model.response.MedicineResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

/**
 * 药物提醒服务，处理药物用法用量的解析和提醒设置
 */
class MedicationReminderService(private val context: Context) {

    private val reminderScheduler = ReminderScheduler(context)

    /**
     * 根据药物名称和用法用量设置提醒
     *
     * @param medicationName 药物名称
     * @param dosageInstructions 用法用量文本
     * @param reminderType 提醒类型，默认为通知栏提醒
     * @return 是否成功设置提醒
     */
    fun setMedicationReminder(
        medicationName: String,
        dosageInstructions: String,
        reminderType: ReminderType = ReminderType.NOTIFICATION
    ): Boolean {
        try {
            // 1. 解析每日服药次数
            val frequency = parseFrequency(dosageInstructions)

            // 2. 解析疗程天数
            val courseInDays = parseCourseInDays(dosageInstructions)

            // 3. 创建提醒对象
            val reminder = MedicationReminder(
                medicationName = medicationName,
                frequencyPerDay = frequency,
                courseInDays = courseInDays
            )

            // 4. 根据指定类型调度提醒
            val scheduledCount = reminderScheduler.scheduleReminder(reminder, reminderType)

            return scheduledCount > 0
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * 根据API返回的药物提醒数据设置闹钟
     *
     * @param response API返回的药物提醒数据
     * @param reminderType 提醒类型，默认为通知栏提醒
     * @return 是否成功设置提醒
     */
    fun setMedicationReminderFromApi(
        response: MedicineResponse,
        reminderType: ReminderType = ReminderType.NOTIFICATION
    ): Boolean {
        try {
            // 检查响应状态
            if (response.state != "success" || response.content.alarms.isEmpty()) {
                return false
            }

            var totalScheduledCount = 0

            // 在调用处
            CoroutineScope(Dispatchers.Main).launch {

                // 处理每个闹钟
                response.content.alarms.forEach { alarm ->
                    // 只处理启用的闹钟
                    if (!alarm.enabled) return@forEach

                    // 提取药物名称，移除前缀"吃"
                    val medicationName = if (alarm.label.startsWith("吃")) {
                        alarm.label.substring(1).trim()
                    } else {
                        alarm.label
                    }

                    // 解析时间
                    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                    val time = try {
                        LocalTime.parse(alarm.time, timeFormatter)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        return@forEach // 如果时间解析失败，跳过此闹钟
                    }

                    // 设置闹钟，每周重复
                    val scheduledCount =
                        scheduleWeeklyAlarm(medicationName, time, alarm.repeat, reminderType)
                    totalScheduledCount += scheduledCount

                    delay(1000)
                }

            }

            return totalScheduledCount > 0
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * 设置每周重复的闹钟
     */
    private fun scheduleWeeklyAlarm(
        medicationName: String,
        time: LocalTime,
        repeatDays: List<String>,
        reminderType: ReminderType
    ): Int {
        if (repeatDays.isEmpty()) return 0

        // 如果是系统闹钟提醒，使用支持多天重复的方法
        if (reminderType == ReminderType.SYSTEM_ALARM) {
            return if (reminderScheduler.scheduleSystemAlarmWithMultipleDays(
                    medicationName,
                    time,
                    repeatDays
                )
            ) 1 else 0
        }

        // 如果是通知栏提醒，保持原有逻辑，为每天创建单独的提醒
        var scheduledCount = 0
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)

        // 为每个重复日创建一个提醒
        repeatDays.forEach { dayName ->
            // 计算下一个对应的日期
            val targetDayOfWeek = when (dayName) {
                "Monday" -> 1
                "Tuesday" -> 2
                "Wednesday" -> 3
                "Thursday" -> 4
                "Friday" -> 5
                "Saturday" -> 6
                "Sunday" -> 7
                else -> return@forEach // 无效的星期几
            }

            // 计算从明天开始的下一个目标日期
            var nextDate = tomorrow
            while (nextDate.dayOfWeek.value != targetDayOfWeek) {
                nextDate = nextDate.plusDays(1)
            }

            // 为这一天创建提醒
            val reminder = MedicationReminder(
                medicationName = medicationName,
                frequencyPerDay = 1,  // 因为API已经提供了具体时间
                courseInDays = 1,     // 只设置一天的提醒，通过重复日期实现多天
                startDate = nextDate,
                times = listOf(time)
            )

            // 使用现有的调度器设置提醒
            val count = reminderScheduler.scheduleReminder(reminder, reminderType)
            scheduledCount += count
        }

        return scheduledCount
    }

    /**
     * 解析用法用量文本中的每日服药次数
     *
     * @param dosageInstructions 用法用量文本
     * @return 每日服药次数 (1-3)
     */
    private fun parseFrequency(dosageInstructions: String): Int {
        // 查找"一日/天X次"或"每日/天X次"等模式
        val patternDaily = Pattern.compile("(一|每)(日|天)(一|二|三|1|2|3|两)次")
        val matcherDaily = patternDaily.matcher(dosageInstructions)

        if (matcherDaily.find()) {
            val frequencyText = matcherDaily.group(3)
            return when (frequencyText) {
                "一", "1" -> 1
                "二", "2", "两" -> 2
                "三", "3" -> 3
                else -> 1 // 默认一天一次
            }
        }

        // 如果找不到明确的频率，根据"顿"解析
        if (dosageInstructions.contains("一天一顿") ||
            !dosageInstructions.contains("顿")
        ) {
            return 1
        } else if (dosageInstructions.contains("一天两顿")) {
            return 2
        } else if (dosageInstructions.contains("一天三顿")) {
            return 3
        }

        // 默认一天一次
        return 1
    }

    /**
     * 解析用法用量文本中的疗程天数
     *
     * @param dosageInstructions 用法用量文本
     * @return 疗程天数
     */
    private fun parseCourseInDays(dosageInstructions: String): Int {
        // 查找"X天为一疗程"或"疗程X天"等模式
        val patternCourse = Pattern.compile("(\\d+)天(为一|一个)?疗程|疗程(\\d+)天")
        val matcherCourse = patternCourse.matcher(dosageInstructions)

        if (matcherCourse.find()) {
            val days = matcherCourse.group(1)?.toIntOrNull()
                ?: matcherCourse.group(3)?.toIntOrNull()
                ?: DEFAULT_COURSE_DAYS

            return days
        }

        // 查找"连续服用X天"或"服用X天"等模式
        val patternContinuous = Pattern.compile("(连续)?(服用|用药|吃)(\\d+)天")
        val matcherContinuous = patternContinuous.matcher(dosageInstructions)

        if (matcherContinuous.find()) {
            return matcherContinuous.group(3)?.toIntOrNull() ?: DEFAULT_COURSE_DAYS
        }

        // 默认疗程
        return DEFAULT_COURSE_DAYS
    }

    /**
     * 取消指定药物的所有提醒
     *
     * @param medicationName 药物名称
     */
    fun cancelMedicationReminder(medicationName: String) {
        reminderScheduler.cancelReminders(medicationName)
    }

    companion object {
        const val DEFAULT_COURSE_DAYS = 7 // 默认疗程天数
    }
}