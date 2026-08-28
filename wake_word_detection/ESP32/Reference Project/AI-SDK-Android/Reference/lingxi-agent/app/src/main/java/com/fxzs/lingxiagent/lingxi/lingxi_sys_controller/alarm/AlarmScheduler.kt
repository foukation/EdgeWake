package com.example.device_control.alarm

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import timber.log.Timber


/**
 *创建者：ZyOng
 *描述：
 *创建时间：2025/6/24 4:10 PM
 */
class AlarmScheduler {
    fun schedule(context: Context, config: AlarmConfig) {
        Timber.tag("AlarmScheduler").d("闹钟数据 = %s",config.toString())
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).putExtra(AlarmClock.EXTRA_HOUR, config.hour)
                .putExtra(AlarmClock.EXTRA_MINUTES, config.minute)
                .putExtra(AlarmClock.EXTRA_MESSAGE, config.text)
                .putExtra(AlarmClock.EXTRA_SKIP_UI, true) // 显示系统闹钟界面
        if (config.repeatDays != null) {
            intent.putExtra(AlarmClock.EXTRA_DAYS, config.repeatDays)
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        }
    }
}