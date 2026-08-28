package com.cmdc.ai.assist.test

import android.content.Context
import com.cmdc.ai.assist.AIAssistantManager
import com.cmdc.ai.assist.api.GateWay
import com.cmdc.ai.assist.constraint.AIAssistConfig
import timber.log.Timber

/**
 * AI助手管理器类
 * 该类用于管理AI助手的相关操作和状态
 * 注意：该类不允许外部直接实例化，以确保其作为单例模式实现
 */
class AIAssistantManagerTest private constructor() {

    /**
     * AIAssistantManager 初始化和获取实例入口
     * */
    companion object {

        @Volatile
        private var instance: AIAssistantManagerTest? = null

        /**
         * 初始化AI助手的功能
         *
         * @param context 应用程序的上下文，用于访问应用程序资源和数据库等
         * @param deviceNo 设备号
         */
        fun initialize(context: Context, deviceNo: String) {
            instance ?: synchronized(this) {
                instance ?: AIAssistantManagerTest().also {
                    instance = it
                    it.initWithConfig(context, deviceNo)
                    it.obtainDeviceInformation()
                }
            }
        }

        /**
         * 获取当前对象的实例（单例）。实例未初始化时抛出 [IllegalStateException]。
         */
        fun getInstance() = instance ?: throw IllegalStateException("Not initialized")
    }

    private val TAG = AIAssistantManagerTest::class.simpleName.toString()

    private val gateWay by lazy {
        GateWay()
    }

    private fun initWithConfig(context: Context, deviceNo: String) {
        // 玛斯特智学屏-问答翻译识图
        val config = AIAssistConfig.Builder()
            .setProductId("***")
            .setProductKey("***")
            .setDeviceNo("***")
            .setDeviceNoType("***") // 传 SN 或 IMEI
            .setToken("***") // jiutian-image
            .setCentralConfigVersion("***")
            .build()

        // 检查配置是否有效
        if (config.isValid()) {
            // 使用配置初始化
            AIAssistantManager.initialize(context, config)
        }
    }

    /**
     * 获取设备信息
     *
     * 收集并获取当前设备的相关信息（设备ID、设备号、产品ID、设备密钥等），
     * 供 SDK 在设备中正确使用。
     */
    internal fun obtainDeviceInformation() {
        gateWay.obtainDeviceInformation({ response ->
            Timber.tag(TAG).d("%s%s", "response: ", response)
        }, { error ->
            Timber.tag(TAG).e("%s%s", "error: ", error)
        })
    }

}
