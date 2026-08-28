package com.lzf.easyfloat.interfaces

import com.lzf.easyfloat.widget.BaseSwitchView

/**
 * @Package: com.lzf.easyfloat.interfaces
 * @Description: 区域触摸事件回调
 */
interface OnTouchRangeListener {

    /**
     * 手指触摸到指定区域
     */
    fun touchInRange(inRange: Boolean, view: BaseSwitchView)

    /**
     * 在指定区域抬起手指
     */
    fun touchUpInRange()

}