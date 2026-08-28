package com.fxzs.lingxiagent.lingxi.float_manager

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.toColorInt
import com.fxzs.lingxiagent.R
import timber.log.Timber
import kotlin.math.sin

/**
 * 创建者：ZyOng
 * 描述：录音波动动画
 * 创建时间：2026/1/28 10:03
 */
class AudioWaveGlowView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(
    context,
    attrs) {

    private val barCount = 24
    private var barWidth = dp(4f)
    private var barSpace = dp(4f)
    private var fixedBarHeight = dp(8f)
    private val heightBreathRange = dp(2f)//±2dp 微呼吸

    private val amplitudes = FloatArray(barCount) { 0.3f }
    private val targetAmplitudes = FloatArray(barCount) { 0.3f }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var gradient: LinearGradient? = null
    private val gradientMatrix = Matrix()
    private var gradientOffset = 0f
    private var gradientSpeed = dp(4f)

    // 呼吸光效果
    private var breathPhase = 0.0
    private var breathAlpha = 0f
    private val breathSpeed = 0.05
//    private val breathSpeed = 0.12
    // 布局缓存
    private var totalWidth = 0f
    private var startX = 0f
    private var centerY = 0f

    //边框
    private val borderWidth = dp(2f)
    private val borderRadius = dp(40f)
    private val innerRadius = dp(38f)

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1000L
        repeatCount = ValueAnimator.INFINITE
        addUpdateListener {
            updateBreath()
            updateAmplitudesSmooth()
            updateGradientOffset()
            invalidate()
        }
    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        barWidth = context.resources.getDimension(R.dimen.dp_4)
        barSpace = context.resources.getDimension(R.dimen.dp_4)
        fixedBarHeight =  context.resources.getDimension(R.dimen.dp_10)
        // 创建渐变，只创建一次
        val gradientWidth = w * 1.5f
        gradient = LinearGradient(-gradientWidth,
            0f,
            0f,
            0f,
            intArrayOf("#7B4DFF".toColorInt(), "#00C2FF".toColorInt(), "#7B4DFF".toColorInt()),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP)
        paint.shader = gradient

        // 预计算布局
        totalWidth = barCount * barWidth + (barCount - 1) * barSpace
        startX = (w - totalWidth) / 2f
        centerY = h / 2f


        // 边框渐变
        val borderGradient = LinearGradient(
            0f, 0f,
            w.toFloat(), 0f,
            intArrayOf(
                "#00F0FF".toColorInt(),
                "#7B4DFF".toColorInt(),
                "#00C2FF".toColorInt()
            ),
            null,
            Shader.TileMode.CLAMP
        )

        borderPaint.shader = borderGradient
    }

    override fun onDraw(canvas: Canvas) { // 设置呼吸效果透明度
        paint.alpha = (128 + 127 * breathAlpha).toInt()
        val barHeight = fixedBarHeight / 2f
        for (i in 0 until barCount) {
            val x = startX + i * (barWidth + barSpace)
            canvas.drawRoundRect(x,
                centerY - barHeight,
                x + barWidth,
                centerY + barHeight,
                barWidth,
                barWidth,
                paint)
        }
    }

    /** 渐变动画偏移 */
    private fun updateGradientOffset() {
        gradient?.also {
            gradientOffset += gradientSpeed
            if (gradientOffset > width * 1.5f) gradientOffset = 0f
            gradientMatrix.setTranslate(gradientOffset, 0f)
            it.setLocalMatrix(gradientMatrix)
        }

    }


    /** 呼吸动画平滑 */
    private fun updateBreath() {
        breathPhase += breathSpeed
        if (breathPhase > 2 * Math.PI) breathPhase -= 2 * Math.PI
//        breathAlpha = (sin(breathPhase) * 0.5 + 0.5).toFloat()
        val raw = (sin(breathPhase) * 0.5 + 0.5)
        breathAlpha = (raw * raw).toFloat()
    }

//    private fun updateBreath() {//呼吸动画闪烁
//        breathPhase = (sin(breathPhase * Math.PI * 2) * 0.5f + 0.5f).toFloat()
//        breathPhase += breathSpeed
//        if (breathPhase > 1f) breathPhase -= 1f
//    }


    /** 波形平滑插值 */
    private fun updateAmplitudesSmooth() {
        for (i in amplitudes.indices) {
            amplitudes[i] += (targetAmplitudes[i] - amplitudes[i]) * 0.2f
        }
    }

    /** 外部更新波形值 */
    fun updateAmplitudes(values: FloatArray) {
        post {
            for (i in values.indices) {
                targetAmplitudes[i] = values[i].coerceIn(0f, 1f)
            }
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator.cancel()
    }
}
