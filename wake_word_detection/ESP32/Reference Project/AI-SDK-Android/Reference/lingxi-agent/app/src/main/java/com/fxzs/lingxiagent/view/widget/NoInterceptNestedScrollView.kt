package com.fxzs.lingxiagent.view.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.widget.NestedScrollView

/**
 *创建者：ZyOng
 *描述：禁止拦截
 *创建时间：2025/12/2 下午5:34
 */
class NoInterceptNestedScrollView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : NestedScrollView(context, attrs) {
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return false // 不拦截任何事件
    }
}
