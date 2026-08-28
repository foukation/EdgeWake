package com.example.device_control.alarm

import android.text.TextUtils
import com.example.device_control.SlotsData
import com.fxzs.lingxiagent.lingxi.lingxi_sys_controller.alarm.TimeParser
import timber.log.Timber
import java.util.Calendar

/**
 *创建者：ZyOng
 *描述：
 *创建时间：2025/6/24 3:44 PM
 */
object AlarmParser {

    fun analysisAlarmTime(slot: SlotsData): AlarmConfig { // {"Action":"Create","Repeat":"Daily","Time":"八点","TimeAPM":"AM"}
        //{"Action":"Create","Date":"Today","Time":"八点","TimeAPM":"PM"}
        //{"Action":"Create","Date":"Tomorrow","Time":"八点","TimeAPM":"AM"}
        //{"Action":"Create","Date":"Wednesday","Text":"喝水","Time":"三点"}
        //{"Action":"Create","AlarmTimeMinute":5}
        //{"Action":"Create","AlarmTimeHour":2}
        //{"Action":"Create","TimeHour":2,"TimeMinute":45}
        //{"Action":"Create","Date":"三天之后"}
        //{"Action":"Create","Date":"TwoDaysLater"}
        var type = AlarmType.ONCE
        var repeatDays: ArrayList<Int>? = null
        try {
            if (!TextUtils.isEmpty(slot.Repeat)) { //重复闹钟
                repeatDays = TimeParser.createWeekday(slot.Repeat,slot.Date?:"")
                type = AlarmType.DAILY
            } //只支持设置24小时之内的
            if (slot.Time == null && slot.AlarmTimeMinute == 0 && slot.AlarmTimeHour == 0) {
                return AlarmConfig(type, repeatDays, -1, -1)
            }
            if (slot.Time != null) { //设置具体时间
                val timeParser = TimeParser.parseTime(slot.Time, slot.TimeAPM)
                Timber.tag("timer").d("解析结果小时: 小时 = %s  分钟=: %s",   timeParser.hour  , timeParser.minute)
                return AlarmConfig(type, repeatDays, timeParser.hour, timeParser.minute)
            }

            if (slot.AlarmTimeMinute > 0 || slot.AlarmTimeHour > 0 || slot.TimeHour > 0 || slot.TimeMinute > 0) { //单独设置几分钟 、几个小时
                val totalMinutes =
                    (slot.AlarmTimeMinute + slot.AlarmTimeHour * 60 + slot.TimeHour * 60 + slot.TimeMinute)
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.MINUTE, totalMinutes)
                val targetHour = calendar[Calendar.HOUR_OF_DAY]
                val targetMinute = calendar[Calendar.MINUTE]
                Timber.tag("timer").d("解析结果 总分钟数: totalMinutes = $totalMinutes  targetHour = $targetHour  targetMinute= $targetMinute")
                return AlarmConfig(type, repeatDays, targetHour, targetMinute)
            }

            return AlarmConfig(type, repeatDays, 0, 0)

        } catch (e: Exception) {
            Timber.tag("timer").d("解析失败message = %s" , e.message)
            return AlarmConfig(type, repeatDays, 0, 0)
        }


    }


}