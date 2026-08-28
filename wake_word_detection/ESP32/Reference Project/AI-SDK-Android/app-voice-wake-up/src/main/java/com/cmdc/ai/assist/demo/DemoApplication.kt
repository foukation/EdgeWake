package com.cmdc.ai.assist.demo

import android.app.Application
import com.cmdc.ai.assist.test.AIAssistantManagerTest
import timber.log.Timber

/**
 * Demo 应用入口。
 *
 * 在进程启动时完成 AI-SDK 的鉴权初始化：
 */
class DemoApplication : Application() {

    private val TAG = DemoApplication::class.simpleName.toString()

    override fun onCreate() {
        super.onCreate()
        // 日志树，便于联调查看 SDK 内部日志
        /*Timber.plant(Timber.DebugTree())*/
        AIAssistantManagerTest.initialize(this, "")
    }

}
