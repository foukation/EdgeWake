package com.fxzs.lingxiagent.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import com.fxzs.lingxiagent.JumpParameterManager.openScheme

class WakeupProxyFloatWindow private constructor(context: Context) {

    private val mContext = context.applicationContext
    private val windowManager: WindowManager by lazy {
        mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    private var floatView: View? = null
    private val params: WindowManager.LayoutParams by lazy {
        WindowManager.LayoutParams().apply {
            width = 1
            height = 1
            format = PixelFormat.TRANSPARENT
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            }
            flags = (
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                            or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    )
        }
    }

    // 标记：是否正在显示浮窗，防止重复添加
    private var isShowing = false

    init {
        floatView = View(mContext).apply {
            layoutParams = FrameLayout.LayoutParams(1, 1)
            setBackgroundColor(Color.TRANSPARENT)
        }
    }

    fun showAndJump() {
        if (isShowing) return

        try {
            floatView?.let { view ->
                if (view.parent == null) {
                    windowManager.addView(view, params)
                    isShowing = true
                }
                Handler(Looper.getMainLooper()).post {
                    jumpAndDismiss()
                }
            }
        } catch (e: Exception) {
            dismiss()
        }
    }

    private fun jumpAndDismiss() {
        // 跳转
        openScheme("lingxiagent://com.fxzs.lingxiagent?target=wakeupwindow", mContext)
        // 关闭浮窗
        dismiss()
    }

    fun dismiss() {
        try {
            floatView?.let { view ->
                if (view.parent != null) {
                    windowManager.removeView(view)
                }
            }
        } catch (e: Exception) {
            // 忽略
        } finally {
            isShowing = false
        }
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: WakeupProxyFloatWindow? = null

        fun getInstance(context: Context): WakeupProxyFloatWindow {
            return INSTANCE ?: synchronized(this) {
                WakeupProxyFloatWindow(context).also { INSTANCE = it }
            }
        }
    }
}