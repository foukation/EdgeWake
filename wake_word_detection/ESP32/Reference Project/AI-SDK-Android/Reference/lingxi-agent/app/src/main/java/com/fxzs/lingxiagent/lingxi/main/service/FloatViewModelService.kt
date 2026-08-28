package com.fxzs.lingxiagent.lingxi.main.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.fxzs.lingxiagent.IYAApplication
import com.fxzs.lingxiagent.helper.FloatComponent
import com.fxzs.lingxiagent.helper.FloatType
import com.fxzs.lingxiagent.layout.ModelType
import com.fxzs.lingxiagent.lingxi.main.callback.TaskHandlerCallback

class FloatViewModelService : Service() {
    fun setTaskHandlerCallback(taskHandlerCallback: TaskHandlerCallback, modelType: ModelType, description: String, stopText: String = "停止", closeText: String = "关闭", tag: String = "") {
        if (modelType == ModelType.DEFAULT) {
            FloatComponent.create(
                IYAApplication.getInstance(), FloatType.MODAL_FLOAT, description,
                { taskHandlerCallback.onClose() }, { taskHandlerCallback.onStop() },stopText, closeText)
        } else if(modelType == ModelType.TASK_TOAST) {
            FloatComponent.create(IYAApplication.getInstance(), FloatType.MODAL_TOAST_FLOAT, description)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return ViewModelBinder(this)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        FloatComponent.dismiss(FloatType.MODAL_FLOAT)
        FloatComponent.dismiss(FloatType.MODAL_TOAST_FLOAT)
        return super.onUnbind(intent)
    }

    class ViewModelBinder(private val service: FloatViewModelService) : Binder() {
        fun getService(): FloatViewModelService {
            return service
        }
    }
}