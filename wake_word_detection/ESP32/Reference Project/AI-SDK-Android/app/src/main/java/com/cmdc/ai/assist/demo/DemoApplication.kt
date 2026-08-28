package com.cmdc.ai.assist.demo

import android.app.Application
import com.cmdc.ai.assist.test.AIAssistantManagerTest
import com.cmdc.ai.assist.demo.utils.DeviceUUIDGenerator
import timber.log.Timber

class DemoApplication : Application() {

    private val TAG = DemoApplication::class.simpleName.toString()

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        AIAssistantManagerTest.initialize(this, DeviceUUIDGenerator.getDeviceUUID(this))
        //AIAssistantManagerTest.initialize(this, "4344842548908868")
    }

}
