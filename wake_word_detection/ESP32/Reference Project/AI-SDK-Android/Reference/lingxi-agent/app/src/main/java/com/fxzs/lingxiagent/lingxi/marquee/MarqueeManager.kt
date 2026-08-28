package com.fxzs.lingxiagent.lingxi.marquee// MarqueeManager.kt
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import com.fxzs.lingxiagent.lingxi.gui_agent.entity.AgentStatus
import com.fxzs.lingxiagent.lingxi.main.utils.BroadcastUtils

import timber.log.Timber

object MarqueeManager {

    private var config = MarqueeConfig()
    private var isRunning = false

    /**
     * 初始化配置
     */
    fun init(config: MarqueeConfig = MarqueeConfig()) {
        this.config = config
    }

    /**
     * 启动跑马灯服务
     */
    fun startMarquee(context: Context) {
        if (isRunning) {
            Timber.tag("MarqueeManager").d("跑马灯已在运行")
            return
        }

        if (checkOverlayPermission(context)) {
            startService(context)
        } else {
            requestOverlayPermission(context)
        }
    }

    /**
     * 停止跑马灯服务
     */
    fun stopMarquee(context: Context) {
        BroadcastUtils.taskCompletedBroadcast()
        if (!isRunning) {
            Timber.tag("MarqueeManager").d("跑马灯未在运行")
            return
        }
        AgentStatus.setStatus(AgentStatus.STATUS_IDLE)
        val serviceIntent = Intent(context, MarqueeService::class.java)
        context.stopService(serviceIntent)
        isRunning = false
//        Timber.tag("MarqueeManager").d("跑马灯已停止")
    }

    /**
     * 检查是否正在运行
     */
    fun isMarqueeRunning(): Boolean = isRunning

    /**
     * 获取当前配置
     */
    fun getConfig(): MarqueeConfig = config

    /**
     * 更新配置
     */
    fun updateConfig(newConfig: MarqueeConfig) {
        this.config = newConfig
    }

    /**
     * 设置跑马灯颜色
     * @param color 颜色值，例如Color.WHITE, Color.parseColor("#FF0000")
     */
    fun setMarqueeColor(color: Int) {
        config.glowColor = color
        config.outerGlowColor = color
        config.farGlowColor = color
    }

    /**
     * 设置跑马灯发光颜色
     * @param color 颜色值，例如Color.WHITE, Color.parseColor("#FF0000")
     */
    fun setGlowColor(color: Int) {
        config.glowColor = color
    }

    /**
     * 设置跑马灯外层发光颜色
     * @param color 颜色值，例如Color.WHITE, Color.parseColor("#FF0000")
     */
    fun setOuterGlowColor(color: Int) {
        config.outerGlowColor = color
    }

    /**
     * 设置跑马灯最外层发光颜色
     * @param color 颜色值，例如Color.WHITE, Color.parseColor("#FF0000")
     */
    fun setFarGlowColor(color: Int) {
        config.farGlowColor = color
    }

    /**
     * 设置所有发光层颜色
     * @param innerColor 內层发光颜色
     * @param outerColor 外层发光颜色
     * @param farColor 最外层发光颜色
     */
    fun setGlowColors(innerColor: Int, outerColor: Int, farColor: Int) {
        config.glowColor = innerColor
        config.outerGlowColor = outerColor
        config.farGlowColor = farColor
    }

    /**
     * 设置所有发光层颜色
     * @param isFinish
     */
    fun setFinish(isFinish: Int) {
        config.isFinish = isFinish
    }

    /**
     * 检查悬浮窗权限
     */
    private fun checkOverlayPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    /**
     * 请求悬浮窗权限
     */
    private fun requestOverlayPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context is Activity) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivityForResult(intent, 100)
            } else {
                Toast.makeText(
                    context,
                    "需要在Activity中请求权限",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * 启动服务
     */
    private fun startService(context: Context) {
        val serviceIntent = Intent(context, MarqueeService::class.java).apply {
            // 传递配置到服务
            putExtra("borderWidth", config.borderWidth)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        isRunning = true
//        Toast.makeText(context, "跑马灯已启动", Toast.LENGTH_SHORT).show()
    }

    /**
     * 处理权限请求结果
     */
    fun handlePermissionResult(context: Context, requestCode: Int) {
        if (requestCode == 100) {
            if (checkOverlayPermission(context)) {
                startService(context)
            } else {
                Toast.makeText(
                    context,
                    "需要悬浮窗权限才能显示跑马灯",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // 根据应用主题动态切换颜色
    fun updateMarqueeTheme(context: Activity, isDarkMode: Boolean) {
        if (isDarkMode) {
            // 暗色主题：白 -> 灰 -> 透明黑
            MarqueeManager.setGlowColors(
                Color.WHITE,
                Color.parseColor("#80808080"),
                Color.parseColor("#20000000")
            )
        } else {
            // 亮色主题：黑 -> 浅灰 -> 透明白
            MarqueeManager.setGlowColors(
                Color.parseColor("#FF0BBF6E"),
                Color.parseColor("#FF0BBF6E"),
                Color.parseColor("#FF0BBF6E")
            )
        }

        // 如果跑马灯正在运行，需要重启服务使颜色生效
        if (MarqueeManager.isMarqueeRunning()) {
            MarqueeManager.stopMarquee(context)
            MarqueeManager.startMarquee(context)
        }
    }

    /**
     * 强制刷新跑马灯绘制
     */
    fun refreshMarqueeDraw(context: Context) {
        if (isRunning) {
            val refreshIntent = Intent(context, MarqueeService::class.java).apply {
                action = "REFRESH_DRAW"
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(refreshIntent)
            } else {
                context.startService(refreshIntent)
            }
        }
    }
}