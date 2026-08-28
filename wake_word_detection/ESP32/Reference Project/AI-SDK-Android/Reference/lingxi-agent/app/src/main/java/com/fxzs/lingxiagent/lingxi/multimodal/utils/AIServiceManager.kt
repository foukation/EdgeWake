package com.ai.multimodal.utils

import android.content.Context
import android.text.TextUtils
import com.cmdc.ai.assist.AIAssistantManager
import com.cmdc.ai.assist.constraint.AIAssistConfig
import com.cmdc.ai.assist.constraint.DeviceInfoResponse
import com.fxzs.lingxiagent.util.DeviceUUIDGenerator
import timber.log.Timber

/**
 *  AI-SDK 设备管理器
 * 负责管理 AI-SDK 的初始化
 */
class AIServiceManager private constructor() {

    companion object {
        @Volatile
        private var INSTANCE: AIServiceManager? = null
        private val TAG = AIServiceManager::class.simpleName.toString()

        /**
         * 获取单例实例
         */
        private fun getInstance(): AIServiceManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AIServiceManager().also { INSTANCE = it }
            }
        }

        /**
         * 初始化 AI-SDK 设备管理器
         * @param context 应用上下文
         */
        fun initialize(context: Context) {
            getInstance().init(context)
        }
    }

    // AI-SDK 助手配置
    private lateinit var aiAssistConfig: AIAssistConfig
        private set

    // 应用上下文
    private lateinit var applicationContext: Context

    // 初始化状态
    private var isInitialized = false

    private lateinit var deviceInfoResponse: DeviceInfoResponse

    /**
     * 内部初始化方法
     */
    private fun init(context: Context) {
        if (isInitialized) {
            return
        }

        /*val config = AIAssistConfig.Builder()
            .setProductId("1924380531735904259")
            .setProductKey("ZVGHluHqQt")
            .setDeviceNo("1924380000001")
            .setDeviceNoType("SN")
            .build()*/

        val config = AIAssistConfig.Builder()
            .setProductId("1934805235342032898")
            .setProductKey("ris8ilYirF")
            .setDeviceNo(DeviceUUIDGenerator.getDeviceUUID(context))
            .setDeviceNoType("SN")
            .build()
        config.deviceSecret = "5qktyla8jkb4n5tf"

        this.applicationContext = context.applicationContext
        this.aiAssistConfig = config
        this.isInitialized = true

        initializeServices()
    }

    /**
     * 初始化各种 AI-SDK
     */
    private fun initializeServices() {

        val deviceInfoResponse = Prefs.getInstance(applicationContext)
            .getObject<DeviceInfoResponse>("deviceInfoResponse")

        if (deviceInfoResponse != null &&
            deviceInfoResponse.data?.deviceNo == aiAssistConfig.deviceNo &&
            deviceInfoResponse.data?.productId == aiAssistConfig.productId
        ) {
            aiAssistConfig.deviceId = deviceInfoResponse.data?.deviceId ?: ""
            aiAssistConfig.deviceSecret = deviceInfoResponse.data?.deviceSecret ?: ""
        }

        // 检查配置是否有效
        if (aiAssistConfig.isValid()) {
            // 使用配置初始化
            AIAssistantManager.initialize(applicationContext, aiAssistConfig)
        }

        // 获取 ai 网关服务
        val gateWay = AIAssistantManager.getInstance().gateWayHelp()

        if (TextUtils.isEmpty(aiAssistConfig.deviceId) || TextUtils.isEmpty(aiAssistConfig.deviceSecret))
            gateWay.obtainDeviceInformation({ response ->
                Timber.tag(TAG).d("%s%s", "response: ", response)
                this.deviceInfoResponse = response
                Prefs.getInstance(applicationContext).putObject("deviceInfoResponse", response)
            }, { error ->
                Timber.tag(TAG).e("%s%s", "error: ", error)
            })

    }

    fun getDeviceInfoResponse(): DeviceInfoResponse {
        checkInitialized()
        return this.deviceInfoResponse

    }

    /**
     * 检查是否已初始化
     */
    private fun checkInitialized() {
        if (!isInitialized) {
            throw IllegalStateException("AIServiceManager must be initialized before use. Call initialize() first.")
        }
    }

}