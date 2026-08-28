package com.fxzs.lingxiagent.service

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import com.fxzs.lingxiagent.IYAApplication
import com.fxzs.lingxiagent.lingxi.accessibility_api.AccessibilityApi
import com.fxzs.lingxiagent.lingxi.core.AppScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import timber.log.Timber

class BaseAccessibilityService : AccessibilityApi() {

    override val enableListenPageUpdate: Boolean
        get() = true

    override fun onCreate() {
        super.onCreate()
        gestureService = this
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: BaseAccessibilityService? = null
        fun acquire(): BaseAccessibilityService? = instance
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Timber.tag("测试").e("onAccessibility_onServiceConnected_app是否在前台 = %s", IYAApplication.getInstance().isAppInForeground())
        instance = this
//
//        if (!IYAApplication.getInstance().isAppInForeground) {
//            launchApp()
//        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun takeScreenshotSec(result: (bitmap: Bitmap?) -> Unit) {
        instance?.takeScreenshot(Display.DEFAULT_DISPLAY,
            Dispatchers.Main.asExecutor(),
            object : TakeScreenshotCallback {
                override fun onSuccess(p0: ScreenshotResult) {
                    try {
                        p0.hardwareBuffer.use { buffer ->
                            val bitmap = Bitmap.wrapHardwareBuffer(buffer, p0.colorSpace)
                            result(bitmap)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onFailure(p0: Int) {
                    Timber.tag("onFailure").e("onFailure: $p0")
                }
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        gestureService = null
    }

    override fun onPageUpdate(currentScope: AppScope) {
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    }


    private fun launchApp() {
        runCatching {
            val intent = packageManager.getLaunchIntentForPackage("com.fxzs.lingxiagent")
            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }.onFailure {
            Timber.e(it, "启动 App 失败")
        }
    }
}