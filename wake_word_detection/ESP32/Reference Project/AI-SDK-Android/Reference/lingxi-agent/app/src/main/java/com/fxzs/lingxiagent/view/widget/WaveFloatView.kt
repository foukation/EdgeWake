package com.fxzs.lingxiagent.view.widget

import android.R.attr.height
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import com.fxzs.lingxiagent.R
import kotlin.math.sin

/**
 *创建者：ZyOng
 *描述：
 *创建时间：2026/3/5 16:15
 */
class WaveFloatView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val barCount = 6

    private var barWidth = dp(6f)
    private var barSpace = dp(10f)

    private var minHeight = dp(8f)
    private var maxHeight = dp(48f)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
    }

    // 中间高两边低权重
    private val weight = floatArrayOf(
        0.5f,
        0.75f,
        1f,
        1f,
        0.75f,
        0.5f
    )

    private val heights = FloatArray(barCount)

    private var phase = 0f

    private val animator = ValueAnimator.ofFloat(0f, (Math.PI * 2).toFloat()).apply {

        duration = 1400
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()

        addUpdateListener {

            phase = it.animatedValue as Float

            updateHeights()

            invalidate()
        }
    }

    init {
        minHeight = resources.getDimension(R.dimen.dp_2)
        maxHeight = resources.getDimension(R.dimen.dp_24)
        barWidth = resources.getDimension(R.dimen.dp_3)
        barSpace = resources.getDimension(R.dimen.dp_3)
    }


    private fun updateHeights() {

        for (i in 0 until barCount) {

            val wave = sin((phase + i * 0.6).toDouble()).toFloat()

            val normalized = (wave + 1) / 2f

            val target =
                minHeight + normalized * (maxHeight - minHeight) * weight[i]

            heights[i] += (target - heights[i]) * 0.25f
        }
    }

    override fun onDraw(canvas: Canvas) {

        val centerY = height / 2f

        var x = barWidth

        for (i in 0 until barCount) {

            val barHeight = heights[i]

            val top = centerY - barHeight / 2
            val bottom = centerY + barHeight / 2

            // 每条线渐变色
            val gradient = LinearGradient(
                x, top,
                x, bottom,
                intArrayOf(
                    Color.parseColor("#7B3FF2"),
                    Color.parseColor("#4F6CF7"),
                    Color.parseColor("#2FA4FF")
                ),
                null,
                Shader.TileMode.CLAMP
            )

            paint.shader = gradient
            paint.strokeWidth = barWidth

            canvas.drawLine(x, top, x, bottom, paint)

            x += barWidth + barSpace
        }
    }

    fun start() {
        if (!animator.isStarted) animator.start()
    }

    fun stop() {
        animator.cancel()
    }

    private fun dp(v: Float): Float {
        return v * resources.displayMetrics.density
    }
}