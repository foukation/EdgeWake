package com.fxzs.lingxiagent.lingxi.lingxi_conversation

import android.content.Context
import android.os.Build
import android.util.Log
import com.cmdc.ai.assist.AIAssistantManager
import com.cmdc.ai.assist.constraint.AIAssistConfig
import com.cmdc.ai.assist.constraint.DeviceInfoResponse
import com.cmdc.ai.assist.constraint.TtsConfig
import com.fxzs.lingxiagent.BuildConfig
import com.fxzs.lingxiagent.IYAApplication
import com.fxzs.lingxiagent.lingxi.multimodal.utils.DeviceInfoUtil
import com.fxzs.lingxiagent.model.common.Constants
import com.fxzs.lingxiagent.model.user.UserUtil
import com.fxzs.lingxiagent.util.DeviceUUIDGenerator
import com.fxzs.lingxiagent.util.SignatureUtil
import com.fxzs.lingxiagent.util.audio.TTSManager
import okhttp3.Request
import timber.log.Timber
import java.util.UUID

 // 注册在计费平台设备
object TempBenefitConfig2 {

    // 产品信息
    const val PRODUCT_ID = "1931900040799399938"
    const val PRODUCT_KEY = "lnydIXAgqZFOVMFK"
    // 设备编号
    const val DEVICE_NO = "869700070000752"
    const val DEVICE_ID = "1951159148103008257"
    const val DEVICE_SECRET = "fVLJFuhjYBkYMEhD"
}

// 未注册
object TempBenefitConfig3 {

    // 产品信息
    const val PRODUCT_ID = "1924380531735904259"
    const val PRODUCT_KEY = "ZVGHluHqQt"
    // 设备编号
    const val DEVICE_NO = "1924380000002"
    const val DEVICE_ID = "172134920644933"
    const val DEVICE_SECRET = "xotzn39fvwdxr8u6"
}

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
        fun getInstance(): AIServiceManager {
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
        this.applicationContext = context.applicationContext

        /* val config = AIAssistConfig.Builder()
            .setDeviceIdentifier(DeviceUUIDGenerator.getDeviceUUID(IYAApplication.getInstance()))
            .setProductId(DeviceInfoUtil.getProductId(applicationContext))
            .setProductKey(DeviceInfoUtil.getProductKey(applicationContext))
            .setDeviceNo(DeviceInfoUtil.getDeviceNo(applicationContext))
            .setDeviceNoType(DeviceInfoUtil.getNoType(applicationContext))
            .setClientID("lingxi_android")
            .setDialogueTtsConfig(
                TtsConfig(
                    TTSManager.getInstance().getCurrentVoiceOption()?.per ?: 4100,
                    TTSManager.getInstance().getCurrentVoiceOption()?.spd ?: 5,
                    volume = TTSManager.getInstance().getCurrentVoiceOption()?.vol ?: 5
                )
            )
            .setCentralConfigVersion("6")
            .build()
        config.packageName = context.packageName
        this.aiAssistConfig = config
        Timber.tag("AIServiceManager").i("deviceNo = " + config.deviceNo + "  productId= " + config.productId + "  productKey" + config.productKey + " deviceNoType=" + config.deviceNoType)
        Log.e("获取设备信息", "deviceNo = " + config.deviceNo + "  productId= " + config.productId + "  productKey" + config.productKey + " deviceNoType=" + config.deviceNoType)

        // 临时权益包 设备信息
        val config = AIAssistConfig.Builder()
            .setDeviceIdentifier(DeviceUUIDGenerator.getDeviceUUID(IYAApplication.getInstance()))
            .setProductId(TempBenefitConfig2.PRODUCT_ID)
            .setProductKey(TempBenefitConfig2.PRODUCT_KEY)
            .setDeviceNo(TempBenefitConfig2.DEVICE_NO)
            .setDeviceNoType(DeviceInfoUtil.getNoTypeTest())
            .setClientID("lingxi_android")
            .setDialogueTtsConfig(
                TtsConfig(
                    TTSManager.getInstance().getCurrentVoiceOption()?.per ?: 4100,
                    TTSManager.getInstance().getCurrentVoiceOption()?.spd ?: 5,
                    volume = TTSManager.getInstance().getCurrentVoiceOption()?.vol ?: 5
                )
            )
            .setCentralConfigVersion("6")
            .build()

        config.deviceId = TempBenefitConfig2.DEVICE_ID
        config.deviceSecret = TempBenefitConfig2.DEVICE_SECRET
        config.packageName = context.packageName

        this.aiAssistConfig = config */

        if (DeviceInfoUtil.getDeviceFlavorType()) {
            val config = AIAssistConfig.Builder()
                .setDeviceIdentifier(DeviceUUIDGenerator.getDeviceUUID(IYAApplication.getInstance()))
                .setProductId(DeviceInfoUtil.getProductId(applicationContext))
                .setProductKey(DeviceInfoUtil.getProductKey(applicationContext))
                .setDeviceNo(DeviceInfoUtil.getDeviceNo(applicationContext))
                .setDeviceNoType(DeviceInfoUtil.getNoType(applicationContext))
                .setClientID("lingxi_android")
                .setDialogueTtsConfig(
                    TtsConfig(
                        TTSManager.getInstance().getCurrentVoiceOption()?.per ?: 4100,
                        TTSManager.getInstance().getCurrentVoiceOption()?.spd ?: 5,
                        volume = TTSManager.getInstance().getCurrentVoiceOption()?.vol ?: 5
                    )
                )
                .setCentralConfigVersion("6")
                .setAudioFileSaveEnabled(BuildConfig.FLAVOR.contains("Beta"))
                .build()
            config.packageName = context.packageName
            this.aiAssistConfig = config
            Timber.tag("AIServiceManager").i("deviceNo = " + config.deviceNo + "  productId= " + config.productId + "  productKey" + config.productKey + " deviceNoType=" + config.deviceNoType)
            Log.e("获取设备信息", "deviceNo = " + config.deviceNo + "  productId= " + config.productId + "  productKey" + config.productKey + " deviceNoType=" + config.deviceNoType)
        } else {
            val config = AIAssistConfig.Builder()
                .setDeviceIdentifier(DeviceUUIDGenerator.getDeviceUUID(IYAApplication.getInstance()))
                .setProductId("1924380531735904259")
                .setProductKey("ZVGHluHqQt")
                .setDeviceNo("1924380000002")
                .setDeviceNoType(DeviceInfoUtil.getNoTypeTest())
                .setClientID("lingxi_android")
                .setDialogueTtsConfig(
                    TtsConfig(
                        TTSManager.getInstance().getCurrentVoiceOption()?.per ?: 4100,
                        TTSManager.getInstance().getCurrentVoiceOption()?.spd ?: 5,
                        volume = TTSManager.getInstance().getCurrentVoiceOption()?.vol ?: 5
                    )
                )
                .setCentralConfigVersion("6")
                .setAudioFileSaveEnabled(BuildConfig.FLAVOR.contains("Beta"))
                .build()

            config.deviceId = "172134920644933"
            config.deviceSecret = "xotzn39fvwdxr8u6"
            config.packageName = context.packageName
            this.aiAssistConfig = config
        }

        this.isInitialized = true
        initializeServices()
    }

    /**l
     * 初始化各种 AI-SDK
     */
    private fun initializeServices() {
        Timber.tag(TAG).d("aiAssistConfig = %s", aiAssistConfig)
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
//        // 获取 ai 网关服务
//        val gateWay = AIAssistantManager.getInstance().gateWayHelp()
//        if (TextUtils.isEmpty(aiAssistConfig.deviceId) || TextUtils.isEmpty(aiAssistConfig.deviceSecret)){
//
//            gateWay.obtainDeviceInformation({ response ->
//
//                Timber.tag(TAG).d("%s%s", "response:11 ", response)
//                this.deviceInfoResponse = response
//                com.ai.multimodal.utils.Prefs.getInstance(applicationContext).putObject("deviceInfoResponse", response)
//                AuthHelper.getInstance().refreshToken()
//            }, { error ->
//                Timber.tag(TAG).e("%s%s", "error: ", error)
//            })}
//        else {
//            AuthHelper.getInstance().refreshToken()
//        }


    }

    fun getAiAssistConfig(): AIAssistConfig {
        if (::aiAssistConfig.isInitialized){
            return aiAssistConfig
        } else{
            Log.e(TAG, "getAiAssistConfig未初始化" )
            initialize(IYAApplication.getInstance().applicationContext)
            return aiAssistConfig
        }

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

    fun getHeaderInfo(headers: Request.Builder): Request.Builder {
        Timber.tag(TAG)
            .d("正式获取方式 :%s%s", DeviceInfoUtil.getDeviceFlavorType(), BuildConfig.BRAND)
        val requestId = UUID.randomUUID().toString()
        headers.header("Content-Type", "application/json; charset=utf-8")
            .header("X-Request-Id", requestId)
            .header("appSource", "private") //public表示公开市场，   private表示非公开市场
            .header("modelName", Build.MODEL)
            .header(
                Constants.HEADER_CLIENT_ID, if (BuildConfig.FLAVOR.contains("tablet")) {
                    Constants.CLIENT_PAD_ID
                } else {
                    Constants.CLIENT_ID
                }
            )
        if (!this::aiAssistConfig.isInitialized) {
            isInitialized = false
            init(IYAApplication.getInstance())
        }
        val flavorName = if (BuildConfig.FLAVOR.contains("tablet")) {
            "com.fxzs.lingxiagentpad"
        } else {
            aiAssistConfig.packageName
        }
        val timestamp = (System.currentTimeMillis()).toString()
        val signature = SignatureUtil.setMd5Signature(aiAssistConfig.deviceSecret + timestamp)
        headers.apply {
            header("sign", signature)
            header("deviceNo", aiAssistConfig.deviceNo)
            header("deviceId", aiAssistConfig.deviceId)
            header("productKey", aiAssistConfig.productKey)
            header("productId", aiAssistConfig.productId)
            header("ts", timestamp)
            header("packageName", flavorName)
            header("versionCode", UserUtil.getAppVersionCode(applicationContext).toString())
            header("versionName", UserUtil.getAppVersionName(applicationContext))
            header("deviceIdentifier", DeviceUUIDGenerator.getDeviceUUID(IYAApplication.getInstance()))
        }
        return headers
    }

}