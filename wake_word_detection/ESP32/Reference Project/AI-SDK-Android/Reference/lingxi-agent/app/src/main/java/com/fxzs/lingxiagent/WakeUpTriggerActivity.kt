package com.fxzs.lingxiagent

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
// 确保导入正确

class WakeUpTriggerActivity : BaseSplashActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val window = window ?: return

        // 1. Android 5.0+ 基础设置
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // 清除旧的半透明标志
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)

            // 【关键】必须添加此标志，否则 color 设置无效
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

            // 设置完全透明
            window.navigationBarColor = Color.TRANSPARENT
            window.statusBarColor = Color.TRANSPARENT
        }

        // 2. Android 10+ 核心修复：关闭系统强制对比度
        // 如果这行不执行，底部一定会出现白色/灰色条
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
//                window.enforceStatusBarContrast = false
//                window.enforceNavigationBarContrast = false // <--- 这就是去掉底部白条的关键
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 3. Android 11+ 可选：让内容真正延伸到屏幕最底端（包括手势条区域）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 如果你的布局需要覆盖到手势横条下面，取消下面注释
            // window.setDecorFitsSystemWindows(false)
        }
    }
}