package com.fxzs.lingxiagent.lingxi.main.utils

import android.content.pm.PackageManager
import com.fxzs.lingxiagent.IYAApplication

object ApplicationStatusUtils {

    fun appIsInsert(pageName: String):Boolean {
        val packageManager: PackageManager = IYAApplication.getInstance().packageManager
        return try {
            packageManager.getPackageInfo(pageName, 0)
            true
        } catch (e : PackageManager.NameNotFoundException) {
            false
        }
    }
}