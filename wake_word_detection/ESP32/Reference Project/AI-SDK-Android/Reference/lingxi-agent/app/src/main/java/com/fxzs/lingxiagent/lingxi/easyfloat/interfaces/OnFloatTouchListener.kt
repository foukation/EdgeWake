package com.lzf.easyfloat.interfaces

import android.view.MotionEvent

/**
 * @function: 系统浮窗的触摸事件
 */
internal interface OnFloatTouchListener {

    fun onTouch(event: MotionEvent)
}