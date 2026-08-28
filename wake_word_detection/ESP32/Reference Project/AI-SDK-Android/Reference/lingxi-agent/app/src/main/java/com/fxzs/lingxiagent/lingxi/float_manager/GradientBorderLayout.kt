package com.fxzs.lingxiagent.lingxi.float_manager

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.core.graphics.toColorInt
import com.fxzs.lingxiagent.R
import kotlin.math.max

/**
 * 可复用霓虹渐变边框布局
 */
open class GradientBorderLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(
    context,
    attrs) {

    // ===== 可配置参数 =====

    private var borderWidth = dp(2f)
    private var cornerRadius = dp(40f)

    @ColorInt
    private var startColor = "#7922E7".toColorInt()


    @ColorInt
    private var endColor = "#14A4F7".toColorInt()

    /** 渐变方向角度（0=水平，90=垂直） */
    private var gradientAngle = 0f

    /** 发光强度 */
    private var glowRadius = dp(12f)

    // ===== Paint =====

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val contentPath = Path()
    private val rect = RectF()
    private val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE   // 默认白色
    }


    private val outerRect = RectF()
    private val innerRect = RectF()

    init {
        cornerRadius = context.resources.getDimension(R.dimen.dp_16)
        borderWidth = context.resources.getDimension(R.dimen.dp_1)
        setWillNotDraw(false) //        setLayerType(LAYER_TYPE_HARDWARE, null)
        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeWidth = borderWidth
        borderPaint.strokeCap = Paint.Cap.ROUND
        borderPaint.strokeJoin = Paint.Join.ROUND

        updateGradient()
    }


    // =============================
    // 公共可调用方法
    // =============================

    fun setBorderWidth(dpValue: Float) {
        borderWidth = dp(dpValue)
        invalidate()
    }

    fun setCornerRadius(dpValue: Float) {
        cornerRadius = dp(dpValue)
        invalidate()
    }

    fun setGradientColors(@ColorInt start: Int, @ColorInt center: Int, @ColorInt end: Int) {
        startColor = start
        endColor = end
        updateGradient()
        invalidate()
    }

    fun setGradientAngle(angle: Float) {
        gradientAngle = angle
        updateGradient()
        invalidate()
    }

    fun setGlowRadius(dpValue: Float) {
        glowRadius = dp(dpValue)
        invalidate()
    }

    // =============================
    // 核心绘制
    // =============================

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateGeometry()
        updateGradient()
    }

    private fun updateGeometry() {

        val half = borderWidth / 2f

        // 外描边中心线
        outerRect.set(half, half, width - half, height - half)

        // 内部真实区域（只缩 half）
        innerRect.set(borderWidth, borderWidth, width - borderWidth, height - borderWidth)
    }


    override fun dispatchDraw(canvas: Canvas) {
        if (width == 0 || height == 0) {
            super.dispatchDraw(canvas)
            return
        }

        val half = borderWidth / 2f
        val outerRadius = (cornerRadius - half).coerceAtLeast(0f)
        val innerRadius = (cornerRadius - borderWidth).coerceAtLeast(0f)

        // ===== 1️⃣ 画边框 =====
        canvas.drawRoundRect(outerRect, outerRadius, outerRadius, borderPaint)

        // ===== 2️⃣ 画内部背景 =====
        canvas.drawRoundRect(innerRect, innerRadius, innerRadius, innerPaint)

        // ===== 3️⃣ 裁剪子View（保证不溢出）=====
        val save = canvas.save()
        canvas.clipRect(innerRect)
        super.dispatchDraw(canvas)
        canvas.restoreToCount(save)
    }

    // =============================
    // 渐变
    // =============================
    private fun updateGradient() {

        if (width == 0 || height == 0) return

        val radians = Math.toRadians(gradientAngle.toDouble())
        val dx = (max(width, height) * kotlin.math.cos(radians)).toFloat()
        val dy = (max(width, height) * kotlin.math.sin(radians)).toFloat()

        val shader = LinearGradient(0f, 0f, dx, dy,
            intArrayOf(startColor, endColor),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP)

        borderPaint.shader = shader
    }

    // =============================
    // 工具
    // =============================

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}