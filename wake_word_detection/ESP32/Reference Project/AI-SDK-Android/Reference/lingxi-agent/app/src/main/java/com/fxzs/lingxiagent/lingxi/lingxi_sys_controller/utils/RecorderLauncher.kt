package com.example.device_control.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.example.device_control.AgentResult

/**
 *创建者：ZyOng
 *描述：
 *创建时间：2025/7/25 下午4:04
 */
object RecorderLauncher {

    private val recorderApps = listOf(
        RecorderApp("小米", "com.android.soundrecorder", "com.android.soundrecorder.SoundRecorder"),
        RecorderApp("华为", "com.huawei.soundrecorder", "com.huawei.soundrecorder.MainActivity"),
        RecorderApp("三星", "com.sec.android.app.voicenote", "com.sec.android.app.voicenote.main.VoiceNoteActivity"),
        RecorderApp("OPPO", "com.coloros.soundrecorder", "com.coloros.soundrecorder.SoundRecorderActivity"),
        RecorderApp("Vivo", "com.android.bbksoundrecorder", "com.android.bbksoundrecorder.SoundRecorder")
    )

    fun launch(context: Context) : AgentResult {
        for (app in recorderApps) {
            if (isAppInstalled(context, app.packageName)) {
                try {
                    val intent = Intent(Intent.ACTION_MAIN)
                    intent.setClassName(app.packageName, app.activityName)
                    intent.addCategory(Intent.CATEGORY_LAUNCHER)
                    context.startActivity(intent)
                    return AgentResult(true,sucMsg ="已为您开启录音App")
                } catch (e: Exception) {
                    // continue trying next one
                }
            }
        }
        return AgentResult(true,sucMsg ="无法开启录音App")
    }

    private fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private data class RecorderApp(
        val brand: String,
        val packageName: String,
        val activityName: String
    )
}
