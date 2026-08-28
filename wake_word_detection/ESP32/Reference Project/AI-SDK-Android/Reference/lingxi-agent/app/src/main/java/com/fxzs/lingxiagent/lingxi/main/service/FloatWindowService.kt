package com.fxzs.lingxiagent.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.fxzs.lingxiagent.IYAApplication
import com.fxzs.lingxiagent.helper.FloatComponent
import com.fxzs.lingxiagent.helper.FloatType

class FloatWindowService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        FloatComponent.create(IYAApplication.getInstance(), FloatType.ACTION_FLOAT, null)
    }

    override fun onDestroy() {
        super.onDestroy()
        FloatComponent.dismiss(FloatType.ACTION_FLOAT)
    }
}