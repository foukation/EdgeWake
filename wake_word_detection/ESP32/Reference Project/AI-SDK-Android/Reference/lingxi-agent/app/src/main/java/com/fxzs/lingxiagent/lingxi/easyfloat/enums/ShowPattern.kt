package com.lzf.easyfloat.enums

/**
 * @function: 浮窗显示类别
 */
enum class ShowPattern {

    // 只在当前Activity显示、仅应用前台时显示、仅应用后台时显示，一直显示（不分前后台）
    CURRENT_ACTIVITY, FOREGROUND, BACKGROUND, ALL_TIME
}