package com.fxzs.lingxiagent.lingxi.main.helper

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.fxzs.lingxiagent.IYAApplication
import com.fxzs.lingxiagent.helper.FloatComponent
import com.fxzs.lingxiagent.helper.FloatType
import com.fxzs.lingxiagent.layout.ModelType
import com.fxzs.lingxiagent.lingxi.config.MenuHandlerCallBack
import com.fxzs.lingxiagent.lingxi.main.callback.TaskHandlerCallback
import com.fxzs.lingxiagent.lingxi.main.service.FloatViewModelService
import com.fxzs.lingxiagent.service.FloatWindowService
import com.fxzs.lingxiagent.service.MenuActionFloatService
import com.fxzs.lingxiagent.lingxi.main.utils.MenuActionFloat
import timber.log.Timber

object FloatHelper {
    private class Connection(val description: String = "", val modelType: ModelType = ModelType.DEFAULT, val close: () -> Unit = {}, val stop: () -> Unit = {}, val stopText: String = "停止", val closeText: String = "关闭", var tag:String="")  : ServiceConnection{
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val floatViewModelService = (service as FloatViewModelService.ViewModelBinder).getService()
            floatViewModelService.setTaskHandlerCallback(object : TaskHandlerCallback {
                override fun onClose() {
                    close()
                }
                override fun onStop() {
                    stop()
                }
            }, modelType, description, stopText, closeText, tag)
        }
        override fun onServiceDisconnected(name: ComponentName?) {}
    }

    private var connection: Connection? = null
    private var menuConnection: MenuConnection? = null

    fun modelToast(description: String){
        val intent = Intent(IYAApplication.getInstance(), FloatViewModelService::class.java)
        connection = Connection(description = description, modelType = ModelType.TASK_TOAST)
        IYAApplication.getInstance().applicationContext.bindService(intent, connection!!, Context.BIND_AUTO_CREATE)
    }

    fun actionToast(description: String, close:()->Unit, stop:()->Unit, stopText: String = "停止", closeText: String = "关闭",) {
        val intent = Intent(IYAApplication.getInstance(), FloatViewModelService::class.java)
        connection = Connection( description, ModelType.DEFAULT, close, stop, stopText, closeText)
        IYAApplication.getInstance().applicationContext.bindService(intent, connection!!, Context.BIND_AUTO_CREATE)
    }

    fun closeModel() {
        if (connection != null) {
            IYAApplication.getInstance().applicationContext.unbindService(connection!!)
            connection = null
        }
    }

    private class MenuConnection(
        val close: () -> Unit = {},
        val lingxiClick: () -> Unit = {}
    ) : ServiceConnection{
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val menuService = (service as MenuActionFloatService.ViewModelBinder).getService()
            menuService.setTaskHandlerCallback(object : MenuHandlerCallBack {
                override fun onClose() {
                    close()
                }
                override fun onLingxiClick() {
                    lingxiClick()
                }
            })
        }
        override fun onServiceDisconnected(name: ComponentName?) {}
    }

    fun openFloatMenu(
        close: () -> Unit = {},
        lingxiClick: () -> Unit = {}
    ) {
        val intent = Intent(IYAApplication.getInstance(), MenuActionFloatService::class.java)
        menuConnection = MenuConnection(close, lingxiClick)
        IYAApplication.getInstance().stopService(Intent(IYAApplication.getInstance(), FloatWindowService::class.java))
        IYAApplication.getInstance().applicationContext.bindService(intent, menuConnection!!, Context.BIND_AUTO_CREATE)
    }
    /**
     * 切换菜单图标状态
     * 切换到灵犀图标（如果当前是默认图标）
     * 或切换回默认图标（如果当前是灵犀图标）
     * @return 切换后的状态 (true=灵犀图标, false=默认图标)
     */
    fun switchMenuIcon(): Boolean {
        // 当前是默认图标 → 切换到灵犀图标
        // 当前是灵犀图标 → 切换回默认图标
        val newState = !IconStateManager.isLingxiIcon
        IconStateManager.setIconState(newState)
        MenuActionFloat.refreshIcon()
        return newState
    }

    /**
     * 强制切换到灵犀图标
     */
    fun switchToLingxiIcon() {
        IconStateManager.setIconState(true)
        MenuActionFloat.refreshIcon()
    }

    /**
     * 强制切换到默认图标
     */
    fun switchToDefaultIcon() {
        IconStateManager.setIconState(false)
        MenuActionFloat.refreshIcon()
    }

    /**
     * 获取当前图标对应的功能
     */
    fun getCurrentFunction(): String {
        return if (IconStateManager.isLingxiIcon) {
            "灵犀功能"
        } else {
            "关闭任务"
        }
    }

    /**
     * 检查当前是否是灵犀图标
     */
    fun isLingxiIcon(): Boolean = IconStateManager.isLingxiIcon

    /**
     * 检查当前是否是默认图标
     */
    fun isDefaultIcon(): Boolean = !IconStateManager.isLingxiIcon

    fun closeFloatMenu() {
        if (menuConnection != null) {
            Timber.tag("FloatHelper").d("关闭悬浮菜单")
            IYAApplication.getInstance().startService(Intent(IYAApplication.getInstance(), FloatWindowService::class.java))
            IYAApplication.getInstance().applicationContext.unbindService(menuConnection!!)
            menuConnection = null
        }
    }

    fun closeFloatMenuNew(){
        FloatComponent.dismiss(FloatType.MENU_ACTION_FLOAT)
        if (menuConnection != null) {
            try {
                IYAApplication.getInstance().applicationContext.unbindService(menuConnection!!)
            } catch (e: IllegalArgumentException) {
                // 服务未绑定时的异常处理
                Timber.tag("FloatHelper").w("服务未绑定: ${e.message}")
            }
            menuConnection = null
        }
    }
}