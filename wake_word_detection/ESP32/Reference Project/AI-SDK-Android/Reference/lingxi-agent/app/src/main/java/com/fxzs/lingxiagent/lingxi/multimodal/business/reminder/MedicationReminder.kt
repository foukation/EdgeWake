package com.ai.multimodal.business.reminder

import java.time.LocalDate
import java.time.LocalTime

/**
 * 表示一个药物提醒
 *
 * @property medicationName 药物名称
 * @property frequencyPerDay 一天服药次数 (1-3)
 * @property courseInDays 疗程天数
 * @property startDate 开始日期
 * @property times 每天的提醒时间列表
 */
data class MedicationReminder(
    val medicationName: String,
    val frequencyPerDay: Int,
    val courseInDays: Int,
    val startDate: LocalDate = LocalDate.now().plusDays(1),
    val times: List<LocalTime> = generateTimesBasedOnFrequency(frequencyPerDay)
) {
    /**
     * 获取实际设置的天数（最多7天）
     */
    val actualDays: Int = minOf(courseInDays, MAX_REMINDER_DAYS)

    /**
     * 获取提醒标题
     */
//    fun getTitle(): String = "吃$medicationName"
    fun getTitle(): String = "用药提醒"

    companion object {
        const val MAX_REMINDER_DAYS = 7

        /**
         * 根据每日服药频率生成提醒时间
         */
        fun generateTimesBasedOnFrequency(frequency: Int): List<LocalTime> {
            return when (frequency) {
                1 -> listOf(LocalTime.of(8, 0)) // 上午8点
                2 -> listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)) // 上午8点, 晚上8点
                3 -> listOf(LocalTime.of(8, 0), LocalTime.of(14, 0), LocalTime.of(20, 0)) // 上午8点, 下午2点, 晚上8点
                else -> throw IllegalArgumentException("不支持的服药频率: $frequency, 应为1-3")
            }
        }
    }
}