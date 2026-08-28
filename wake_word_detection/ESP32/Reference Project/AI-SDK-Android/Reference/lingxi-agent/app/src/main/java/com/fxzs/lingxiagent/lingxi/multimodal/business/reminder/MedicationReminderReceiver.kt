package com.ai.multimodal.business.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * 广播接收器，用于接收药物提醒闹钟触发的广播并显示通知
 */
class MedicationReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val medicationName = intent.getStringExtra(EXTRA_MEDICATION_NAME) ?: "药物"
        val title = intent.getStringExtra(EXTRA_REMINDER_TITLE) ?: "吃$medicationName"

        // 显示通知
        showNotification(context, title, "现在是服用 medicationName 的时间了")
    }

    /**
     * 显示药物提醒通知
     */
    private fun showNotification(context: Context, title: String, content: String) {
        // 创建通知渠道（Android 8.0及以上需要）
        createNotificationChannel(context)

        // 获取默认的铃声
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // 创建通知
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // 使用系统图标，在实际应用中应替换为应用特定图标
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(defaultSoundUri)
            .setAutoCancel(true)

        // 显示通知
        with(NotificationManagerCompat.from(context)) {
            // 使用药物名称的hashCode作为通知ID，确保每种药物有自己的通知
            val notificationId = title.hashCode()

            try {
                notify(notificationId, notificationBuilder.build())
            } catch (e: SecurityException) {
                // 处理缺少通知权限的情况
                e.printStackTrace()
            }
        }
    }

    /**
     * 创建通知渠道（Android 8.0及以上需要）
     */
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "药物提醒"
            val descriptionText = "用于显示服药提醒的通知"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            // 注册通知渠道
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "medication_reminder_channel"
        const val EXTRA_MEDICATION_NAME = "extra_medication_name"
        const val EXTRA_REMINDER_TITLE = "extra_reminder_title"
    }
}