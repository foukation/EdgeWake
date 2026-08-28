package com.fxzs.lingxiagent.lingxi.main.utils

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import com.fxzs.lingxiagent.R
import com.fxzs.lingxiagent.lingxi.config.MenuHandlerCallBack
import com.fxzs.lingxiagent.lingxi.main.helper.IconStateManager
import timber.log.Timber

@SuppressLint("ViewConstructor")
class MenuActionFloat @JvmOverloads constructor(
    context: Context,
    menuHandlerCallBack: MenuHandlerCallBack,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attributeSet, defStyleAttr) {

    private var menuIcon: ImageView? = null
    private var callback: MenuHandlerCallBack? = null

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.menu_float_action, this)
        menuIcon = view.findViewById(R.id.menu_close_task)
        this.callback = menuHandlerCallBack

        // 设置静态引用
        instance = this

        // 设置初始图标
        updateIcon()

        // 点击事件
        menuIcon?.setOnClickListener {
            if (IconStateManager.isLingxiIcon) {
                callback?.onLingxiClick()
                Timber.tag("MenuActionFloat").d("onLingxiClick")

            } else {
                Timber.tag("MenuActionFloat").d("onClose")
                callback?.onClose()
            }
        }

//        // 长按切换图标
//        menuIcon?.setOnLongClickListener {
//            IconStateManager.switchIcon()
//            updateIcon()
//            Toast.makeText(context, IconStateManager.getCurrentFunction(), Toast.LENGTH_SHORT).show()
//            true
//        }
    }

    private fun updateIcon() {
        menuIcon?.setImageResource(IconStateManager.getCurrentIconResource())
    }

    companion object {
        @Volatile
        private var instance: MenuActionFloat? = null

        /**
         * 刷新图标显示
         */
        fun refreshIcon() {
            instance?.updateIcon()
        }

        /**
         * 清除静态引用
         */
        fun clearInstance() {
            instance = null
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        clearInstance()
    }
}