package com.cmdc.ai.assist.test

import android.content.Context
import android.os.Handler
import android.os.Looper

class PerformanceTestingSDK private constructor() {

    /**
     * AIAssistantManager 初始化和获取实例入口
     * */
    companion object {

        @Volatile
        private var instance: PerformanceTestingSDK? = null
        private const val SDK_INIT_DELAY_MS = 60_000L
        private const val GC_DELAY_MS = 5_000L
        private const val GC_SETTLE_DELAY_MS = 1_000L

        /**
         * 初始化AI助手的功能
         *
         * 此函数用于设置AI助手的初始配置和上下文环境，使其能够根据提供的配置信息
         * 进行正确的操作和响应
         *
         * @param context 应用程序的上下文，用于访问应用程序资源和数据库等
         * @param config AI助手的配置信息，包括但不限于语言设置、识别模型等
         */
        fun initialize(context: Context, deviceNo: String) {
            instance ?: synchronized(this) {
                instance ?: PerformanceTestingSDK().also {
                    instance = it
                    /*Timber.plant(Timber.DebugTree())*/
                    it.handler.postDelayed({
                        it.requestGcForMemoryBaseline()
                    }, GC_DELAY_MS)
                    it.handler.postDelayed({
                        it.prepareMemoryBaselineThenInitializeSdk(context, deviceNo)
                    }, SDK_INIT_DELAY_MS)
                    //AIAssistantManagerTest.initialize(this, "4344842548908868")
                }
            }
        }

        fun getInstance() = instance ?: throw IllegalStateException("Not initialized")
    }

    private val TAG = PerformanceTestingSDK::class.simpleName.toString()
    private val handler = Handler(Looper.getMainLooper())

    /**
     * 用于 adb dumpsys meminfo 测试前尽量收敛 Java/Native 可回收对象。
     *
     * 注意：GC 只能降低测试噪声，不能保证系统立即回收所有内存；
     * 对外统计 RAM 时仍应多次采样后取平均值。
     */
    private fun prepareMemoryBaselineThenInitializeSdk(context: Context, deviceNo: String) {
        requestGcForMemoryBaseline()
        handler.postDelayed({
            requestGcForMemoryBaseline()
            handler.postDelayed({
                AIAssistantManagerTest.initialize(context, deviceNo)
            }, GC_SETTLE_DELAY_MS)
        }, GC_SETTLE_DELAY_MS)
    }

    /**
     * 主动请求一次 GC，仅用于本 demo 的内存测试场景。
     * 不建议把该方法作为线上业务流程的一部分。
     */
    private fun requestGcForMemoryBaseline() {
        Runtime.getRuntime().gc()
        System.gc()
        Runtime.getRuntime().runFinalization()
    }

}