package com.fxzs.lingxiagent.lingxi.common.dialog

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import com.fxzs.lingxiagent.R

class CustomPopupWindow(context: Context) {
    private val popupView: View = LayoutInflater.from(context).inflate(R.layout.custom_popup_window, null)
    private val popupWindow: PopupWindow = PopupWindow(
        popupView,
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT,
        true
    )
    private var callback: ((String) -> Unit)? = null

    init {
        // 设置全屏显示，覆盖状态栏
        popupWindow.width = WindowManager.LayoutParams.MATCH_PARENT
        popupWindow.height = WindowManager.LayoutParams.MATCH_PARENT
        // 初始化PopupWindow配置
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popupWindow.isOutsideTouchable = true
        popupWindow.isFocusable = true
        popupWindow.animationStyle = R.style.PopupAnimation

        // 设置系统级弹窗
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            popupWindow.windowLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            popupWindow.windowLayoutType = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

    }

    fun setCallback(callback: (String) -> Unit): CustomPopupWindow {
        this.callback = callback
        return this
    }

    fun setText(text: String): CustomPopupWindow {
        popupView.findViewById<TextView>(R.id.tvTitle).text = text
        return this
    }

    fun showEditText(hint: String): CustomPopupWindow {
        popupView.findViewById<EditText>(R.id.etInput).apply {
            visibility = View.VISIBLE
            setHint(hint)
            setHintTextColor(Color.GRAY)
            setTextColor(Color.BLACK)
        }
        return this
    }

    fun hideEditText(): CustomPopupWindow {
        popupView.findViewById<EditText>(R.id.etInput).visibility = View.GONE
        return this
    }

    fun setButtonTexts(positiveText: String, negativeText: String? = null): CustomPopupWindow {
        popupView.findViewById<Button>(R.id.btnPositive).apply {
            text = positiveText
            visibility = View.VISIBLE
        }
        negativeText?.let {
            popupView.findViewById<Button>(R.id.btnNegative).apply {
                text = it
                visibility = View.VISIBLE
            }
        }
        return this
    }

    fun hideButtons(): CustomPopupWindow {
        popupView.findViewById<ViewGroup>(R.id.llButton).visibility = View.GONE
        return this
    }

    fun setCancelable(enable: Boolean): CustomPopupWindow {
        popupWindow.isOutsideTouchable = enable
        popupWindow.isFocusable = enable
        popupView.setBackgroundDrawable(ColorDrawable(0x80000000.toInt()))
        popupView.findViewById<LinearLayout>(R.id.llBackground).setOnClickListener {
            if (enable) {
                dismiss()
            }
        }

        return this
    }

    fun setAutoDismiss(delayMillis: Long): CustomPopupWindow {
        popupView.postDelayed({ dismiss() }, delayMillis)
        return this
    }

    fun show(anchor: View? = null) {
        // 设置按钮点击事件
        popupView.findViewById<Button>(R.id.btnPositive).setOnClickListener {
            val input = popupView.findViewById<EditText>(R.id.etInput).text.toString()
            callback?.invoke(input)
            dismiss()
        }

        popupView.findViewById<Button>(R.id.btnNegative).setOnClickListener {
            dismiss()
        }

        if (anchor != null) {
            popupWindow.showAsDropDown(anchor)
        } else {
            // 不依赖外部View的显示方式
            // 全屏显示，覆盖状态栏
            popupWindow.showAtLocation(
                popupView,
                android.view.Gravity.NO_GRAVITY,
                0,
                0
            )
        }
    }

    fun dismiss() {
        popupWindow.dismiss()
    }
}