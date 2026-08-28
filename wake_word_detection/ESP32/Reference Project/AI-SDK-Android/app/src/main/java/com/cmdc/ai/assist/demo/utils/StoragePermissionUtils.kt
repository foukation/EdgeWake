package com.cmdc.ai.assist.demo.utils

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import timber.log.Timber

object StoragePermissionUtils {
    private const val TAG = "StoragePermissionUtils"

    /**
     * 检查是否有文件管理权限
     */
    @JvmStatic
    fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    /**
     * 请求文件管理权限
     * @param activity 当前Activity
     */
    @JvmStatic
    fun requestStoragePermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${activity.packageName}")
                }
                activity.startActivity(intent)
                Timber.tag(TAG).d("Requesting MANAGE_EXTERNAL_STORAGE permission")
            } catch (e: Exception) {
                // 如果上面的意图失败，尝试打开通用的文件访问设置页面
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                activity.startActivity(intent)
                Timber.tag(TAG).d("Opening general storage settings")
            }
        }
    }
} 