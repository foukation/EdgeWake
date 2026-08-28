package com.fxzs.lingxiagent.ui.guide

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.animation.doOnEnd
import androidx.core.view.isVisible

/**
 * 手指引导动画工具类
 * 纯手指移动动画，无波纹/按压效果
 *
 * 动画效果：手指从上方淡入 -> 下移到目标位置 -> 停留 -> 淡出返回上方 -> 循环
 *
 * 使用方式:
 * ```kotlin
 * // 方式1: 直接添加到布局
 * val guideView = FingerGuideAnimationView(context).apply {
 *     setFingerDrawable(R.drawable.finger)
 *     setTargetView(button)
 * }
 * container.addView(guideView)
 * guideView.startAnimation()
 *
 * // 方式2: 使用 Helper 快捷显示
 * val helper = FingerGuideAnimationHelper(activity)
 * helper.showGuideOnView(button, R.drawable.finger)
 * ```
 */
class FingerGuideAnimationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        const val DEFAULT_DURATION = 1500L           // 单次动画总时长(ms)
        const val DEFAULT_FADE_DURATION = 200L       // 淡入淡出时长
        const val DEFAULT_MOVE_DURATION = 400L       // 移动时长
        const val DEFAULT_STAY_DURATION = 300L       // 停留时长
        const val DEFAULT_OFFSET_Y = 150f            // 手指起始位置偏移量(dp)
        const val DEFAULT_REPEAT_COUNT = ValueAnimator.INFINITE
    }

    // 配置参数
    var animationDuration: Long = DEFAULT_DURATION
    var fadeDuration: Long = DEFAULT_FADE_DURATION
    var moveDuration: Long = DEFAULT_MOVE_DURATION
    var stayDuration: Long = DEFAULT_STAY_DURATION
    var offsetY: Float = DEFAULT_OFFSET_Y         // 手指起始Y轴偏移（目标上方）
    var repeatCount: Int = DEFAULT_REPEAT_COUNT
    var autoStart: Boolean = true

    // 手指图片资源
    private var fingerDrawableRes: Int = -1

    // 视图组件
    private lateinit var fingerImageView: ImageView

    // 动画
    private var animatorSet: AnimatorSet? = null
    private var isAnimating = false

    // 目标位置（屏幕坐标）
    private var targetLocation = IntArray(2)
    private var targetView: View? = null

    init {
        setupView()
    }

    private fun setupView() {
        setBackgroundColor(Color.TRANSPARENT)
        isClickable = false
        isFocusable = false

        fingerImageView = ImageView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
            isVisible = false
        }
        addView(fingerImageView)
    }

    /**
     * 设置手指图片资源
     */
    fun setFingerDrawable(resId: Int) {
        fingerDrawableRes = resId
        if (resId != -1) {
            fingerImageView.setImageResource(resId)
        }
    }

    /**
     * 设置目标视图，动画将围绕该视图中心播放
     */
    fun setTargetView(view: View) {
        targetView = view
        view.getLocationOnScreen(targetLocation)
    }

    /**
     * 设置目标位置（屏幕坐标）
     */
    fun setTargetPosition(x: Int, y: Int) {
        targetLocation[0] = x
        targetLocation[1] = y
    }

    /**
     * 开始动画
     */
    fun startAnimation() {
        if (isAnimating) return
        if (fingerDrawableRes == -1) {
            throw IllegalStateException("必须先调用 setFingerDrawable() 设置手指图片")
        }

        isAnimating = true
        fingerImageView.isVisible = true

        // 计算手指位置
        val fingerWidth = fingerImageView.drawable?.intrinsicWidth ?: 120
        val fingerHeight = fingerImageView.drawable?.intrinsicHeight ?: 120

        val targetX = targetLocation[0] + (targetView?.width ?: 0) / 2f - fingerWidth / 2f
        val targetY = targetLocation[1] + (targetView?.height ?: 0) / 2f - fingerHeight * 0.3f
        val startY = targetY - offsetY

        // 设置初始状态
        fingerImageView.x = targetX
        fingerImageView.y = startY
        fingerImageView.alpha = 0f

        // 创建动画序列
        val animatorSet = AnimatorSet()

        // 1. 淡入 + 下落
        val fadeIn = ObjectAnimator.ofFloat(fingerImageView, "alpha", 0f, 1f).apply {
            duration = fadeDuration
        }
        val moveDown = ObjectAnimator.ofFloat(fingerImageView, "y", startY, targetY).apply {
            duration = moveDuration
            interpolator = AccelerateDecelerateInterpolator()
        }

        // 2. 停留（通过延迟实现）
        val stay = ObjectAnimator.ofFloat(fingerImageView, "alpha", 1f, 1f).apply {
            duration = stayDuration
        }

        // 3. 淡出 + 上移返回
        val fadeOut = ObjectAnimator.ofFloat(fingerImageView, "alpha", 1f, 0f).apply {
            duration = fadeDuration
        }
        val moveUp = ObjectAnimator.ofFloat(fingerImageView, "y", targetY, startY).apply {
            duration = moveDuration
            interpolator = AccelerateDecelerateInterpolator()
        }

        // 按顺序组合
        animatorSet.play(fadeIn).with(moveDown)
        animatorSet.play(stay).after(moveDown)
        animatorSet.play(fadeOut).with(moveUp).after(stay)

        // 设置循环
        this.animatorSet = animatorSet.apply {
            doOnEnd {
                if (isAnimating && repeatCount == ValueAnimator.INFINITE) {
                    // 无限循环：重新开始
                    startAnimation()
                } else {
                    isAnimating = false
                }
            }
            start()
        }
    }

    /**
     * 停止动画
     */
    fun stopAnimation() {
        animatorSet?.cancel()
        animatorSet = null
        fingerImageView.isVisible = false
        isAnimating = false
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }
}

/**
 * 手指引导动画辅助类
 */
class FingerGuideAnimationHelper(private val context: Context) {

    private var guideView: FingerGuideAnimationView? = null
    private var windowManager: WindowManager? = null
    private var isShowing = false

    /**
     * 在指定视图上显示引导动画
     */
    fun showGuideOnView(
        targetView: View,
        fingerDrawableRes: Int,
        config: (FingerGuideAnimationView.() -> Unit)? = null
    ) {
        dismissGuide()

        val parent = targetView.rootView as? ViewGroup ?: return
        val location = IntArray(2)
        targetView.getLocationOnScreen(location)

        guideView = FingerGuideAnimationView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setFingerDrawable(fingerDrawableRes)
            setTargetPosition(
                location[0],
                location[1]
            )
            config?.invoke(this)
        }

        parent.addView(guideView)
        guideView?.startAnimation()
        isShowing = true
    }

    /**
     * 在指定坐标位置显示引导动画
     */
    fun showGuideAtPosition(
        x: Int,
        y: Int,
        fingerDrawableRes: Int,
        config: (FingerGuideAnimationView.() -> Unit)? = null
    ) {
        dismissGuide()

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        this.windowManager = windowManager

        guideView = FingerGuideAnimationView(context).apply {
            setFingerDrawable(fingerDrawableRes)
            setTargetPosition(x, y)
            config?.invoke(this)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            android.graphics.PixelFormat.TRANSLUCENT
        )

        windowManager.addView(guideView, params)
        guideView?.startAnimation()
        isShowing = true
    }

    /**
     * 关闭引导动画
     */
    fun dismissGuide() {
        guideView?.let { view ->
            view.stopAnimation()
            try {
                if (view.parent is ViewGroup) {
                    (view.parent as ViewGroup).removeView(view)
                } else {
                    windowManager?.removeView(view)
                }
            } catch (e: Exception) {
                // 忽略
            }
        }
        guideView = null
        windowManager = null
        isShowing = false
    }

    fun isShowing(): Boolean = isShowing
}

/**
 * 使用示例:
 *
 * ```kotlin
 * // 基础用法 - 在 Activity 中
 * class GuideActivity : AppCompatActivity() {
 *     private lateinit var guideHelper: FingerGuideAnimationHelper
 *
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         setContentView(R.layout.activity_guide)
 *
 *         guideHelper = FingerGuideAnimationHelper(this)
 *
 *         // 对某个按钮显示引导
 *         val button = findViewById<Button>(R.id.button)
 *         button.post {
 *             guideHelper.showGuideOnView(button, R.drawable.finger)
 *         }
 *     }
 *
 *     override fun onDestroy() {
 *         super.onDestroy()
 *         guideHelper.dismissGuide()
 *     }
 * }
 *
 * // 自定义参数
 * guideHelper.showGuideOnView(
 *     targetView = button,
 *     fingerDrawableRes = R.drawable.finger,
 *     config = {
 *         animationDuration = 2000
 *         offsetY = 200f
 *         repeatCount = 3
 *     }
 * )
 *
 * // 在 Fragment 中使用
 * override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
 *     val helper = FingerGuideAnimationHelper(requireContext())
 *     view.post {
 *         helper.showGuideOnView(targetButton, R.drawable.finger)
 *     }
 * }
 *
 * // 在 Compose 中使用
 * @Composable
 * fun FingerGuide(targetView: View, fingerRes: Int) {
 *     val context = LocalContext.current
 *     val helper = remember { FingerGuideAnimationHelper(context) }
 *
 *     DisposableEffect(targetView) {
 *         helper.showGuideOnView(targetView, fingerRes)
 *         onDispose { helper.dismissGuide() }
 *     }
 * }
 * ```
 */