package com.fxzs.lingxiagent.lingxi.main.helper

import com.fxzs.lingxiagent.R

/**
 * 图标状态管理器
 * 轻量级状态管理，不涉及服务重启
 */
object IconStateManager {

    var isLingxiIcon: Boolean = false
        private set

    /**
     * 获取当前图标资源
     */
    fun getCurrentIconResource(): Int {
        return if (isLingxiIcon) {
            R.mipmap.icon_gui_logo_top
        } else {
            R.mipmap.stop_ic
        }
    }

    /**
     * 获取当前功能描述
     */
    fun getCurrentFunction(): String {
        return if (isLingxiIcon) {
            "打开灵犀功能"
        } else {
            "关闭当前任务"
        }
    }

    /**
     * 切换图标状态
     */
    fun switchIcon() {
        isLingxiIcon = !isLingxiIcon
    }

    /**
     * 设置指定图标状态
     */
    fun setIconState(useLingxiIcon: Boolean) {
        isLingxiIcon = useLingxiIcon
    }

    /**
     * 重置为默认状态
     */
    fun resetToDefault() {
        isLingxiIcon = false
    }
}