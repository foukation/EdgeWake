package com.fxzs.lingxiagent.lingxi.marquee


import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.FrameLayout
import androidx.core.app.NotificationCompat
import com.fxzs.lingxiagent.R
import com.fxzs.lingxiagent.lingxi.gui_agent.actions.HandlerLineTaskLlm
import timber.log.Timber

import kotlin.math.sin

class MarqueeService : Service() {
    private var configChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_CONFIGURATION_CHANGED) {
                Timber.d("检测到屏幕方向变化，重新布局跑马灯")
                recreateMarqueeView()
            }
        }
    }

    private var windowManager: WindowManager? = null
    private var marqueeContainer: FrameLayout? = null

    // 配置
    private lateinit var config: MarqueeConfig

    // 通知相关
    private val notificationID: Int = 1001
    private val channelID = "MarqueeServiceChannel"
    private val channelName = "跑马灯服务"

    // 内侧圆角半径（动态计算）
    private var innerCornerRadius = 0f
    // 底部安全边距（用于避开 Home Indicator）
    private var bottomSafeInset = 0

    // 禁止Home Indicator的广播接收器
    private var homeIndicatorReceiver: BroadcastReceiver? = null
    // 新增：状态栏高度
    private var statusBarInset = 0

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        config = MarqueeConfig() // 默认配置

        // 注册配置变更监听器
        registerReceiver(configChangeReceiver, IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED))
        Timber.d("注册屏幕方向监听器")

        // 初始化动态圆角半径
        calculateCornerRadius()
        calculateBottomSafeInset()  // 计算底部home indicator 安全边距
        calculateStatusBarInset()  // 计算状态栏高度

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        // 设置禁止Home Indicator上划功能
        setupHomeIndicatorBlocking()
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 【关键修复】立即启动前台服务，避免超时
        // 必须在5秒内调用 startForeground()
        startForeground(notificationID, createNotification())

        // 使用MarqueeManager中的最新配置
        config = MarqueeManager.getConfig()

        // 重新计算圆角半径以适应可能的配置变化
        calculateCornerRadius()
        calculateBottomSafeInset()  // 计算底部home indicator 安全边距
        calculateStatusBarInset()  // 计算状态栏高度

        if (marqueeContainer == null) {
            showMarqueeOverlay()
        }

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelID,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "跑马灯服务运行中"
                setSound(null, null)
                enableVibration(false)
            }

            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, channelID)
            .setContentTitle("跑马灯服务运行中")
            .setContentText("点击返回应用")
            .setSmallIcon(R.drawable.app_logo)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
    }

    private fun showMarqueeOverlay() {
        val layoutParams = createWindowLayoutParams()

        // 获取屏幕真实尺寸（包含状态栏）
        val screenWidth = getRealScreenWidth()
        val screenHeight = getRealScreenHeight()

        // 创建容器
        marqueeContainer = FrameLayout(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            this.layoutParams = FrameLayout.LayoutParams(
                screenWidth,
                screenHeight
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
            }

            // 屏蔽导航栏触摸事件（沉浸式模式）
            systemUiVisibility = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
        }

        // 创建彩虹色内侧圆角边框（带增强多层次发光效果）
        createRainbowRoundedBorder(screenWidth, screenHeight)

        windowManager?.addView(marqueeContainer, layoutParams)
    }

    private fun getRealScreenWidth(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager?.currentWindowMetrics
            val bounds = windowMetrics?.bounds
            bounds?.width() ?: resources.displayMetrics.widthPixels
        } else {
            val display = windowManager?.defaultDisplay
            val realSize = Point()
            display?.getRealSize(realSize)
            realSize.x
        }
    }

    private fun getRealScreenHeight(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager?.currentWindowMetrics
            val bounds = windowMetrics?.bounds
            (bounds?.height() ?: resources.displayMetrics.heightPixels)
        } else {
            val display = windowManager?.defaultDisplay
            val realSize = Point()
            display?.getRealSize(realSize)
            realSize.y
        }
    }

    /**
     * 计算自适应的圆角半径
     * 结合dp单位和屏幕比例，确保在不同设备上显示一致
     */
    private fun calculateCornerRadius() {
        val baseRadius = resources.getDimension(R.dimen.marquee_corner_radius_base)
        val maxRadius = resources.getDimension(R.dimen.marquee_corner_radius_max)
        Timber.tag("MarqueeService").d("Calculated corner baseRadius: $baseRadius, maxRadius: $maxRadius")

        // 根据屏幕高度动态调整圆角半径，但不超过最大值
        val screenHeight = getRealScreenHeight()
        var proportionalFactor = 0.03f  // 默认比例为屏幕高度的3%
        if (HandlerLineTaskLlm.isTablet(this)) {
// 动态比例计算：根据具体屏幕高度进行精细调整
            proportionalFactor = 0.01f
            Timber.tag("MarqueeService").d("Calculated corner proportionalFactor: $proportionalFactor")
//            proportionalFactor = when {
//                screenHeight <= 1600 -> 0.010f   // 1600及以下：3.0%
//                screenHeight <= 1800 -> 0.014f  // 1600-1800：3.4%
//                screenHeight <= 2000 -> 0.018f  // 1800-2000：3.8%
//                screenHeight <= 2200 -> 0.022f  // 2000-2200：4.2%
//                screenHeight <= 2400 -> 0.026f  // 2200-2400：4.6%
//                screenHeight <= 2600 -> 0.028f  // 2400-2600：4.8%
//                screenHeight <= 2800 -> 0.030f  // 2600-2800：5.0%
//                screenHeight <= 3000 -> 0.032f  // 2800-3000：5.2%
//                else -> 0.034f                   // 3000以上：5.4%
//            }
        } else {
// 动态比例计算：根据具体屏幕高度进行精细调整
            proportionalFactor = when {
                screenHeight <= 1600 -> 0.030f   // 1600及以下：3.0%
                screenHeight <= 1800 -> 0.034f  // 1600-1800：3.4%
                screenHeight <= 2000 -> 0.038f  // 1800-2000：3.8%
                screenHeight <= 2200 -> 0.042f  // 2000-2200：4.2%
                screenHeight <= 2400 -> 0.046f  // 2200-2400：4.6%
                screenHeight <= 2600 -> 0.048f  // 2400-2600：4.8%
                screenHeight <= 2800 -> 0.050f  // 2600-2800：5.0%
                screenHeight <= 3000 -> 0.052f  // 2800-3000：5.2%
                else -> 0.054f                   // 3000以上：5.4%
            }
        }

        val proportionalRadius = screenHeight * proportionalFactor

        innerCornerRadius = baseRadius.coerceAtMost(proportionalRadius).coerceAtMost(maxRadius)

        Timber.tag("MarqueeService").d("Calculated corner radius: $innerCornerRadius, screenHeight: $screenHeight, factor: $proportionalFactor")
    }
    /**
     * 计算底部安全边距（避开 Home Indicator）
     */
    private fun calculateBottomSafeInset() {
        bottomSafeInset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 使用 WindowInsets 获取安全区域
            try {
                val windowMetrics = windowManager?.currentWindowMetrics
                val insets = windowMetrics?.windowInsets
                insets?.getInsets(WindowInsets.Type.navigationBars())?.bottom ?: 0
            } catch (e: Exception) {
                // fallback 到估算值
                estimateBottomSafeInset()
            }
        } else {
            // Android 10 及以下版本使用估算值
            estimateBottomSafeInset()
        }

        Timber.tag("MarqueeService").d("Calculated bottom safe inset: $bottomSafeInset")
    }
    /**
     * 估算底部安全边距
     */
    private fun estimateBottomSafeInset(): Int {
        val screenHeight = getRealScreenHeight()
        val density = resources.displayMetrics.density

        // 根据屏幕高度和密度估算 Home Indicator 高度
        return when {
            screenHeight >= 2400 -> (44 * density).toInt()  // 大屏设备
            screenHeight >= 2000 -> (38 * density).toInt()  // 中大屏设备
            screenHeight >= 1600 -> (34 * density).toInt()  // 标准平板
            else -> (24 * density).toInt()                  // 小屏设备
        }
    }
    /**
     * 计算状态栏高度
     */
    private fun calculateStatusBarInset() {
        statusBarInset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 使用 WindowInsets 获取状态栏高度
            try {
                val windowMetrics = windowManager?.currentWindowMetrics
                val insets = windowMetrics?.windowInsets
                insets?.getInsets(WindowInsets.Type.statusBars())?.top ?: 0
            } catch (e: Exception) {
                // fallback 到资源维度
                getStatusBarHeightByResource()
            }
        } else {
            // Android 10 及以下版本使用资源维度
            getStatusBarHeightByResource()
        }

        Timber.tag("MarqueeService").d("Calculated status bar inset: $statusBarInset")
    }
    /**
     * 通过资源维度获取状态栏高度
     */
    private fun getStatusBarHeightByResource(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else {
            // fallback 到估算值
            (24 * resources.displayMetrics.density).toInt()
        }
    }
    private fun createWindowLayoutParams(): WindowManager.LayoutParams {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                        or WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR // 关键：允许覆盖状态栏
                        or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM // 阻止系统导航交互
                        or WindowManager.LayoutParams.FLAG_FULLSCREEN,       // 全屏标志，禁止Home Indicator
                PixelFormat.TRANSLUCENT
            )
        } else {
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        or WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR // 关键：允许覆盖状态栏
                        or WindowManager.LayoutParams.FLAG_FULLSCREEN,         // 全屏标志，禁止Home Indicator
                PixelFormat.TRANSLUCENT
            )
        }.apply {
            // 设置位置从屏幕最左上角开始（包含状态栏）
            x = 0
            y = 0
            gravity = Gravity.TOP or Gravity.START

            // 设置 layoutInDisplayCutoutMode 以支持刘海屏和状态栏区域
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    /**
     * 创建彩虹色内侧圆角边框（带增强多层次发光效果）
     */
    private fun createRainbowRoundedBorder(screenWidth: Int, screenHeight: Int) {
        val borderThickness = config.borderWidth * 2

        // 创建彩虹色边框视图
        val rainbowView = createEnhancedRainbowBorderView(
            screenWidth,
            screenHeight,
            borderThickness
        )
        marqueeContainer?.addView(rainbowView)
    }

    /**
     * 创建增强的彩虹色边框视图（多层次发光 + 特殊效果）
     */
    private fun createEnhancedRainbowBorderView(
        screenWidth: Int,
        screenHeight: Int,
        borderThickness: Int
    ): View {
        return object : View(this) {
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            private val innerGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            private val middleGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            private val outerGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            private val farGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            private val bloomPaint = Paint(Paint.ANTI_ALIAS_FLAG) // 光晕扩散效果

            private val rect = RectF()
            private var animationTime = 0L
            private var pulsePhase = 0f

            // 增强的色彩
            private val enhancedRainbowColors = intArrayOf(
                Color.argb(255, 255, 50, 50),     // 鲜艳红色
                Color.argb(255, 255, 255, 50),    // 鲜艳黄色
                Color.argb(255, 50, 255, 50),     // 鲜艳绿色
                Color.argb(255, 50, 255, 255),    // 鲜艳青色
                Color.argb(255, 50, 50, 255),     // 鲜艳蓝色
                Color.argb(255, 255, 50, 255),    // 鲜艳洋红
                Color.argb(255, 255, 50, 50)      // 回到鲜艳红色
//                Color.argb(121, 34, 231, 1),
//                Color.argb(121, 34, 231, 1),
//                Color.argb(121, 34, 231, 1),
//                Color.argb(20, 164, 247, 1),
//                Color.argb(20, 164, 247, 1),
//                Color.argb(20, 164, 247, 1),
//                Color.argb(20, 164, 247, 1)
            )

            init {
                layoutParams = FrameLayout.LayoutParams(screenWidth, screenHeight)

                // 1. 主边框画笔 - 使用鲜艳色彩
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = borderThickness.toFloat() * config.glowIntensity
                paint.strokeCap = Paint.Cap.ROUND
                paint.alpha = 255
                paint.maskFilter = BlurMaskFilter(5f, BlurMaskFilter.Blur.NORMAL) // 轻微模糊使边缘柔和

                // 2. 内层发光画笔（紧贴主边框）
                innerGlowPaint.style = Paint.Style.STROKE
                innerGlowPaint.strokeWidth = borderThickness.toFloat() * config.glowIntensity * 2f
                innerGlowPaint.strokeCap = Paint.Cap.ROUND
                innerGlowPaint.color = config.glowColor
                innerGlowPaint.alpha = (config.glowAlpha * 1.5f).toInt().coerceAtMost(255)
                innerGlowPaint.maskFilter = BlurMaskFilter(config.glowRadius * 1.5f, BlurMaskFilter.Blur.NORMAL)

                // 3. 中层发光画笔
                middleGlowPaint.style = Paint.Style.STROKE
                middleGlowPaint.strokeWidth = borderThickness.toFloat() * config.glowIntensity * 3f
                middleGlowPaint.strokeCap = Paint.Cap.ROUND
                middleGlowPaint.color = config.glowColor
                middleGlowPaint.alpha = (config.outerGlowAlpha * 1.2f).toInt().coerceAtMost(255)
                middleGlowPaint.maskFilter = BlurMaskFilter(config.outerGlowRadius * 1.2f, BlurMaskFilter.Blur.NORMAL)

                // 4. 外层发光画笔
                outerGlowPaint.style = Paint.Style.STROKE
                outerGlowPaint.strokeWidth = borderThickness.toFloat() * config.glowIntensity * 4f
                outerGlowPaint.strokeCap = Paint.Cap.ROUND
                outerGlowPaint.color = config.glowColor
                outerGlowPaint.alpha = (config.farGlowAlpha * 1.5f).toInt().coerceAtMost(255)
                outerGlowPaint.maskFilter = BlurMaskFilter(config.farGlowRadius, BlurMaskFilter.Blur.NORMAL)

                // 5. 最外层光晕画笔
                farGlowPaint.style = Paint.Style.STROKE
                farGlowPaint.strokeWidth = borderThickness.toFloat() * config.glowIntensity * 6f
                farGlowPaint.strokeCap = Paint.Cap.ROUND
                farGlowPaint.color = config.glowColor
                farGlowPaint.alpha = (config.farGlowAlpha * 0.8f).toInt().coerceAtMost(255)
                farGlowPaint.maskFilter = BlurMaskFilter(config.farGlowRadius * 1.5f, BlurMaskFilter.Blur.NORMAL)

                // 6. 光晕扩散效果画笔（绘制填充区域）
                bloomPaint.style = Paint.Style.FILL
                bloomPaint.color = Color.TRANSPARENT

                // 启用硬件加速
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    setLayerType(LAYER_TYPE_HARDWARE, null)
                }

                // 开始动画
                post(object : Runnable {
                    override fun run() {
                        if (parent == null) return

                        animationTime += 16
                        pulsePhase = (animationTime * 0.002f * config.glowPulseSpeed) % (Math.PI * 2).toFloat()

                        // 动态更新发光强度
                        updateGlowIntensity()

                        invalidate()
                        postDelayed(this, 16)
                    }
                })
            }

            private fun updateGlowIntensity() {
                // 主脉冲（基础脉冲）
                val basePulse = sin(pulsePhase).toFloat()

                // 内层：快速脉冲
                val innerPulse = basePulse * 2
                innerGlowPaint.alpha = ((config.glowAlpha * 1.5f) + (sin(innerPulse) * config.glowAlpha * 0.5f)).toInt()
                    .coerceIn(80, 255)

                // 中层：中等速度脉冲，相位偏移
                val middlePulse = pulsePhase + 1.0f
                middleGlowPaint.alpha = ((config.outerGlowAlpha * 1.2f) + (sin(middlePulse) * config.outerGlowAlpha * 0.4f)).toInt()
                    .coerceIn(60, 200)

                // 外层：慢速脉冲，更大相位偏移
                val outerPulse = pulsePhase + 2.0f
                outerGlowPaint.alpha = ((config.farGlowAlpha * 1.5f) + (sin(outerPulse) * config.farGlowAlpha * 0.3f)).toInt()
                    .coerceIn(40, 150)

                // 最外层：最慢脉冲
                val farPulse = pulsePhase + 3.0f
                farGlowPaint.alpha = ((config.farGlowAlpha * 0.8f) + (sin(farPulse) * config.farGlowAlpha * 0.2f)).toInt()
                    .coerceIn(20, 100)

                // 主边框：轻微脉冲
                paint.alpha = (200 + (sin(pulsePhase * 0.5f) * 55)).toInt()
            }

            override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
                super.onSizeChanged(w, h, oldw, oldh)

                // 计算主矩形区域
                val halfStroke = paint.strokeWidth / 2
                rect.set(
                    halfStroke,
                    halfStroke,
                    w - halfStroke,
                    h - halfStroke - bottomSafeInset // 减去底部安全边距
                )
            }

            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)

                val time = animationTime * 0.001f

                // 根据色彩鲜艳度调整颜色
//                val rainbowColors = enhancedRainbowColors
                val rainbowColors = if (config.glowColorVibrance > 1.0f) {
                    enhancedRainbowColors // 使用增强色彩
                } else {
//                    Timber.tag("MarqueeService").d("isFinish ${config.isFinish}")
                    if(config.isFinish == 1){
                        intArrayOf(
                            resources.getColor(R.color.color_0BBF6E, null),
                            resources.getColor(R.color.color_0BBF6E, null),
                            resources.getColor(R.color.color_0BBF6E, null),
                            resources.getColor(R.color.color_0BBF6E, null),
                            resources.getColor(R.color.color_0BBF6E, null),
                            resources.getColor(R.color.color_0BBF6E, null),
                            resources.getColor(R.color.color_0BBF6E, null)
                        )
                    } else if(config.isFinish == 2){
                        intArrayOf(
                            resources.getColor(R.color.color_EE3636, null),
                            resources.getColor(R.color.color_EE3636, null),
                            resources.getColor(R.color.color_EE3636, null),
                            resources.getColor(R.color.color_EE3636, null),
                            resources.getColor(R.color.color_EE3636, null),
                            resources.getColor(R.color.color_EE3636, null),
                            resources.getColor(R.color.color_EE3636, null)
                        )
                    }else{
                        intArrayOf(

                            resources.getColor(R.color.color_2391F5, null),
                            resources.getColor(R.color.color_3B71F1, null),
                            resources.getColor(R.color.color_574DEC, null),
                            resources.getColor(R.color.color_7823E7, null),
                            resources.getColor(R.color.color_6837E9, null),
                            resources.getColor(R.color.color_4465EF, null),
                            resources.getColor(R.color.color_179FF6, null)

//                            resources.getColor(R.color.color_9C8EF6, null),
//                            resources.getColor(R.color.color_6EB4F5, null),
//                            resources.getColor(R.color.color_60E0EE, null),
//                            resources.getColor(R.color.color_7DE7D2, null),
//                            resources.getColor(R.color.color_7DE7D2, null),
//                            resources.getColor(R.color.color_60E0EE, null),
//                            resources.getColor(R.color.color_6EB4F5, null),
//                            resources.getColor(R.color.color_9C8EF6, null)
                        )
                    }

                }

                // 根据时间旋转渐变
                val angle = (time * 90) % 360
                val centerX = width / 2f
                val centerY = height / 2f

                // 创建扫描渐变
                val shader = SweepGradient(centerX, centerY, rainbowColors, null).apply {
                    val matrix = Matrix()
                    matrix.postRotate(angle, centerX, centerY)
                    setLocalMatrix(matrix)
                }

                // 保存画布状态
                val saveCount = canvas.save()

                // 方法1：使用混合模式增强发光效果
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
                    innerGlowPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
                    middleGlowPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
                    outerGlowPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
                    farGlowPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
                }

                // 绘制顺序：从最外层到最内层

                // 1. 最外层光晕（最大最模糊）
                if (config.tripleGlowEnabled) {
                    farGlowPaint.shader = shader
                    canvas.drawRoundRect(rect, innerCornerRadius * 1.2f, innerCornerRadius * 1.2f, farGlowPaint)
                }

                // 2. 外层发光
                if (config.doubleGlowEnabled) {
                    outerGlowPaint.shader = shader
                    canvas.drawRoundRect(rect, innerCornerRadius * 1.1f, innerCornerRadius * 1.1f, outerGlowPaint)
                }

                // 3. 中层发光
                middleGlowPaint.shader = shader
                canvas.drawRoundRect(rect, innerCornerRadius, innerCornerRadius, middleGlowPaint)

                // 4. 内层发光
                if (config.glowEnabled) {
                    innerGlowPaint.shader = shader
                    canvas.drawRoundRect(rect, innerCornerRadius, innerCornerRadius, innerGlowPaint)
                }

                // 5. 主边框（最清晰）
                paint.shader = shader
                canvas.drawRoundRect(rect, innerCornerRadius, innerCornerRadius, paint)

                // 方法2：添加径向光晕背景（增强整体发光效果）
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && config.glowIntensity > 1.2f) {
                    val radialGradient = RadialGradient(
                        centerX, centerY,
                        Math.min(width, height) / 1.5f,
                        intArrayOf(
                            Color.argb((40 * config.glowIntensity).toInt(), 255, 255, 255),
                            Color.argb((20 * config.glowIntensity).toInt(), 255, 255, 255),
                            Color.TRANSPARENT
                        ),
                        floatArrayOf(0f, 0.3f, 1f),
                        Shader.TileMode.CLAMP
                    )
                    bloomPaint.shader = radialGradient
                    bloomPaint.alpha = (60 * config.glowIntensity).toInt().coerceAtMost(255)
//                    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bloomPaint)
                    // 修改背景绘制区域，避开底部安全区域
                    canvas.drawRect(0f, statusBarInset.toFloat(), width.toFloat(), height.toFloat() - bottomSafeInset, bloomPaint)
                }

                // 恢复混合模式
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    paint.xfermode = null
                    innerGlowPaint.xfermode = null
                    middleGlowPaint.xfermode = null
                    outerGlowPaint.xfermode = null
                    farGlowPaint.xfermode = null
                }

                // 恢复画布
                canvas.restoreToCount(saveCount)

                // 已移除高光亮点绘制代码
            }
        }
    }

    private fun recreateMarqueeView() {
        removeMarqueeOverlay()
        showMarqueeOverlay()
        Timber.d("重建跑马灯视图")
    }

    private fun removeMarqueeOverlay() {
        if (marqueeContainer != null && windowManager != null) {
            try {
                windowManager?.removeView(marqueeContainer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            marqueeContainer = null
        }
    }

    override fun onDestroy() {
        // 取消注册Home Indicator广播接收器
        homeIndicatorReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                Timber.e("取消注册Home Indicator接收器失败: ${e.message}")
            }
        }
        unregisterReceiver(configChangeReceiver)
        removeMarqueeOverlay()
        super.onDestroy()
        Timber.d("销毁服务并取消监听器")
    }
    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * 设置禁止Home Indicator上划功能
     * 需系统权限才能完全生效
     */
    private fun setupHomeIndicatorBlocking() {
        // 拦截系统导航Intent（需系统权限）
        val intentFilter = IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        homeIndicatorReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val reason = intent?.getStringExtra("reason")
                Timber.d("收到系统对话框广播, reason: $reason")
                // 拦截home键/上划触发的导航行为
                if ("homekey" == reason || "recentapps" == reason) {
                    try {
                        abortBroadcast() // 系统应用可终止广播
                        Timber.d("已拦截Home Indicator操作")
                    } catch (e: Exception) {
                        Timber.e("拦截广播失败: ${e.message}")
                    }
                }
            }
        }
        try {
            registerReceiver(homeIndicatorReceiver, intentFilter)
            Timber.d("注册Home Indicator广播接收器成功")
        } catch (e: SecurityException) {
            Timber.e("注册Home Indicator广播接收器失败, 缺少系统权限: ${e.message}")
        } catch (e: Exception) {
            Timber.e("注册Home Indicator广播接收器失败: ${e.message}")
        }
    }
}