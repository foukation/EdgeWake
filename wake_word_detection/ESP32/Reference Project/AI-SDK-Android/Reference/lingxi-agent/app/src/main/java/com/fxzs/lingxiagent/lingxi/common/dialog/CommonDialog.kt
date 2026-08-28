package com.fxzs.lingxiagent.lingxi.common.dialog

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class CommonDialog(private val context: Context) {
    private var dialog: AlertDialog? = null
    private var message: String = ""
    private var editTextHint: String = ""
    private var showEditText: Boolean = true
    private var autoDismissMillis: Long = 0
    private var backgroundColor: Int = Color.WHITE
    private var textColor: Int = Color.BLACK
    private var positiveButtonText: String = "确认"
    private var negativeButtonText: String = "取消"
    private var showSingleButton: Boolean = false
    private var showButtons: Boolean = true
    private var callback: ((String) -> Unit)? = null
    private var cancelable: Boolean = false

    fun setText(text: String): CommonDialog {
        this.message = text
        return this
    }

    fun showEditText(hint: String): CommonDialog {
        this.editTextHint = hint
        this.showEditText = true
        return this
    }

    fun hideEditText(): CommonDialog {
        this.showEditText = false
        return this
    }

    @Deprecated("Use showEditText() or hideEditText() instead")
    fun setEditText(hint: String, show: Boolean = true): CommonDialog {
        this.editTextHint = hint
        this.showEditText = show
        return this
    }

    fun setCallback(callback: (String) -> Unit): CommonDialog {
        this.callback = callback
        return this
    }

    fun openCommonDialog() {
        val builder = AlertDialog.Builder(context)
        // 创建圆角背景Drawable
        val backgroundDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 30f
            setColor(Color.WHITE)
        }

        val margin = (20 * context.resources.displayMetrics.density).toInt()
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = backgroundDrawable
            setPadding(margin, margin, margin, margin)
        }

        val textView = TextView(context).apply {
            text = message
            setTextColor(textColor)
            setPadding(0, 0, 0, margin / 2)
        }

        val editText = EditText(context).apply {
            hint = editTextHint
            setTextColor(textColor)
            setHintTextColor(textColor and 0x80FFFFFF.toInt())
            background = null // 移除背景
            setPadding(0, margin, 0, 0) // 增加顶部间距
        }

        container.addView(textView)
        if (showEditText) {
            container.addView(editText)
        }

        builder.setView(container)
        if (showButtons) {
            builder.setPositiveButton(positiveButtonText) { _, _ ->
                callback?.invoke(if (showEditText) editText.text.toString() else "")
            }

            if (!showSingleButton) {
                builder.setNegativeButton(negativeButtonText) { dialog, _ ->
                    dialog.dismiss()
                }
            }

            dialog = builder.create()
            dialog?.setOnShowListener {
                val buttonBg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 30f
                    setColor(Color.WHITE)
                }

                dialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                    background = buttonBg
                    setTextColor(Color.BLACK)
                    setAllCaps(false)
                }

                if (!showSingleButton) {
                    dialog?.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
                        background = buttonBg
                        setTextColor(Color.BLACK)
                        setAllCaps(false)
                    }
                }
            }
        }

        dialog = builder.create()
        dialog?.apply {
            // 配置点击外部消失行为
            setCancelable(cancelable)
            setCanceledOnTouchOutside(cancelable)
            // 设置窗口背景为透明，让圆角可见
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            } else {
                window?.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
            }

            // 单按钮模式下设置按钮居中
            if (showSingleButton) {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE)?.let { button ->
                        val params = button.layoutParams as LinearLayout.LayoutParams
                        params.width = LinearLayout.LayoutParams.WRAP_CONTENT
                        params.gravity = Gravity.CENTER_HORIZONTAL
                        button.layoutParams = params
                        // 强制重新布局
                        (button.parent as? View)?.requestLayout()
                    }
                }
            }

            show()
        }

        if (autoDismissMillis > 0) {
            Handler().postDelayed({
                dismiss()
            }, autoDismissMillis)
        }
    }

    fun setAutoDismiss(delayMillis: Long): CommonDialog {
        this.autoDismissMillis = delayMillis
        return this
    }

    fun setButtonTexts(positiveText: String, negativeText: String? = null): CommonDialog {
        this.positiveButtonText = positiveText
        this.showSingleButton = negativeText == null
        this.showButtons = true
        negativeText?.let { this.negativeButtonText = it }
        return this
    }

    fun hideButtons(): CommonDialog {
        this.showButtons = false
        return this
    }

    fun setColors(backgroundColor: Int, textColor: Int): CommonDialog {
        this.backgroundColor = backgroundColor
        this.textColor = textColor
        return this
    }

    fun setCancelable(enable: Boolean): CommonDialog {
        this.cancelable = enable
        return this
    }

    fun dismiss() {
        dialog?.dismiss()
    }
}