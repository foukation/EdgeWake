package com.fxzs.lingxiagent.helper

import android.app.Application
import com.fxzs.lingxiagent.layout.AgentFloatView
import com.fxzs.lingxiagent.layout.FloatModelView
import com.fxzs.lingxiagent.layout.ModelType
import com.fxzs.lingxiagent.lingxi.config.MenuHandlerCallBack
import com.fxzs.lingxiagent.lingxi.main.utils.MenuActionFloat
import com.fxzs.lingxiagent.lingxi.main.utils.ScreenUtils
import com.lzf.easyfloat.EasyFloat
import com.lzf.easyfloat.enums.ShowPattern
import com.lzf.easyfloat.enums.SidePattern
import timber.log.Timber
import kotlin.div

enum class FloatType{
    ACTION_FLOAT,
    MODAL_FLOAT,
    MODAL_TOAST_FLOAT,
    MENU_ACTION_FLOAT,
}
/**
 * 悬浮窗
 */
object FloatComponent {
    fun create(app: Application, floatType: FloatType, description:String?, close:()->Unit = {}, stop:()->Unit = {}, stopText: String = "停止", closeText: String = "关闭") {
        if (floatType == FloatType.MODAL_FLOAT) {
            EasyFloat
                .with(app)
                .setLayout(
                    FloatModelView(app, ModelType.DEFAULT, description, ok = close, cancel = stop, stopText = stopText, closeText = closeText)
                )
                .setShowPattern(ShowPattern.ALL_TIME)
                .setTag(FloatType.MODAL_FLOAT.name)
                .setLocation(0, 0)
                .setDragEnable(false)
                .setMatchParent(widthMatch = true, heightMatch = true)
                .show()
        } else if(floatType == FloatType.MODAL_TOAST_FLOAT) {
            EasyFloat
                .with(app)
                .setLayout(FloatModelView(app, ModelType.TASK_TOAST, description))
                .setShowPattern(ShowPattern.ALL_TIME)
                .setTag(FloatType.MODAL_FLOAT.name)
                .setLocation(0, 0)
                .setDragEnable(false)
                .setMatchParent(widthMatch = true, heightMatch = true)
                .show()
        } else if (floatType == FloatType.ACTION_FLOAT) {
            EasyFloat
                .with(app)
                .setLayout(AgentFloatView(app))
                .setShowPattern(ShowPattern.ALL_TIME)
                .setTag(FloatType.ACTION_FLOAT.name)
                .setAnimator(null)
                .setDragEnable(true)
                .setLocation(0, ScreenUtils.getScreenHeight(app) / 2 + 150)
                .setSidePattern(SidePattern.RESULT_HORIZONTAL)
                .show()
        } else if(floatType == FloatType.MENU_ACTION_FLOAT) {
            EasyFloat
                .with(app)
                .setLayout(MenuActionFloat(app, object: MenuHandlerCallBack {
                    override fun onClose() {
                        close()
                    }
                    override fun onLingxiClick() {
                        stop()

                    }
                }))
                .setShowPattern(ShowPattern.ALL_TIME)
                .setTag(FloatType.MENU_ACTION_FLOAT.name)
                .setDragEnable(true)
                .setAnimator(null)
                .setLocation((0 - ScreenUtils.dpToPx(60, app))/2, ScreenUtils.getScreenHeight(app) - 500)
                .show()
        }
    }

    fun hide(floatType: FloatType) {
        EasyFloat.hide(floatType.name)
    }

    fun show(floatType: FloatType) {
        EasyFloat.show(floatType.name)
    }

    fun dismiss(floatType: FloatType) {
        Timber.tag("FloatComponent").d("dismiss")
        EasyFloat.dismiss(floatType.name, true)
    }
}