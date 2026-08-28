package com.fxzs.lingxiagent.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.fxzs.lingxiagent.IYAApplication
import com.fxzs.lingxiagent.helper.FloatComponent
import com.fxzs.lingxiagent.helper.FloatType
import com.fxzs.lingxiagent.lingxi.config.MenuHandlerCallBack
import timber.log.Timber

class MenuActionFloatService : Service() {
    private var callback: MenuHandlerCallBack? = null

    fun setTaskHandlerCallback(taskHandlerCallback: MenuHandlerCallBack) {
        this.callback = taskHandlerCallback
        FloatComponent.create(
            IYAApplication.getInstance(),
            FloatType.MENU_ACTION_FLOAT,
            null,
            close = { callback?.onClose() },
            stop = { callback?.onLingxiClick() }  // 使用stop参数传递灵犀点击回调
        )
    }

    override fun onBind(intent: Intent?): IBinder? {
        return ViewModelBinder(this)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Timber.tag("MenuActionFloatService").d("onUnbind")
        FloatComponent.dismiss(FloatType.MENU_ACTION_FLOAT)
        return super.onUnbind(intent)
    }

    class ViewModelBinder(private val service: MenuActionFloatService) : Binder() {
        fun getService(): MenuActionFloatService {
            return service
        }
    }
}