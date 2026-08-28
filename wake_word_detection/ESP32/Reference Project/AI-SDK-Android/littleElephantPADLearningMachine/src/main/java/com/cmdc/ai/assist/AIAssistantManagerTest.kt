package com.cmdc.ai.assist

import android.content.Context
import com.cmdc.ai.assist.api.*
import com.cmdc.ai.assist.constraint.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import timber.log.Timber
import java.util.*

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
         * 此函数用于设置AI助手的初始配置和上下文环境，使其能够根据提供的配置信息
         * 进行正确的操作和响应
         *
         * @param context 应用程序的上下文，用于访问应用程序资源和数据库等
         * @param config AI助手的配置信息，包括但不限于语言设置、识别模型等
         */
        fun initialize(context: Context, deviceNo: String) {
            instance ?: synchronized(this) {
                instance ?: AIAssistantManagerTest().also {
                    instance = it
                    /*plant(DebugTree())*/
                    /*it.sdkLogInit(context)
                    it.setEdgeCacheFilePath(context)*/
                    it.initWithConfig(context, deviceNo)
                    it.obtainDeviceInformation()
                    /*it.textToAudio()*/
                    /*it.insideRcChat()*/
                    /*it.asrTranslation()*/
                    /*it.speechRecognitionPersistent()*/
                    /*it.textTranslate()*/
                }
            }
        }

        /**
         * 获取当前对象的实例
         *
         * 此函数用于实现单例模式的延迟初始化它检查实例是否已经存在，
         * 如果不存在，则抛出IllegalStateException异常，表明实例尚未初始化
         * 这种方法确保了在实例未被初始化时，不会返回null，避免了空指针异常的发生
         *
         * @return 当前对象的实例如果实例未初始化，则抛出IllegalStateException
         */
        fun getInstance() = instance ?: throw IllegalStateException("Not initialized")
    }

    private val TAG = AIAssistantManagerTest::class.simpleName.toString()
    private val gson: Gson = GsonBuilder().create()

    private val gateWay by lazy {
        GateWay()
    }

    private val aiFoundationKit by lazy {
        AIFoundationKit()
    }

    private fun initWithConfig(context: Context, deviceNo: String) {
        // 创建配置 小象平板学习机
        val config = AIAssistConfig.Builder()
            .setProductId("1967506372755107841")
            .setProductKey("hcSGlfoEMfULqLnL")
            .setDeviceNo("2025030686000155")
            .setDeviceNoType("C86") // 传 SN 或 IMEI
            .build()

        // 检查配置是否有效
        if (config.isValid()) {
            // 使用配置初始化
            AIAssistantManager.initialize(context, config)
        }
    }

    /**
     * 提供对GateWay对象的辅助功能或信息
     *
     * 此函数可能涉及与GateWay相关的复杂操作或查询，但由于函数体不包含具体实现，
     * 具体功能细节和实现逻辑无法直接从函数签名中得知
     *
     * @return 可能返回一个GateWay对象，也可能返回null，具体取决于实现逻辑和运行时情况
     */
    fun gateWayHelp(): GateWay? {
        return gateWay
    }

    /**
     * 获取AI基础套件实例
     *
     * 该函数用于初始化或获取一个AI基础套件的实例这个套件提供了一系列的AI相关工具和功能，
     * 可以帮助开发者更容易地在应用中集成AI能力如果没有之前的实例，该函数可能返回null，或者
     * 根据具体实现，可能返回一个新的实例
     *
     * @return AIFoundationKit? 可能返回AI基础套件的实例，也可能返回null如果当前上下文没有实例，
     *         或者尚未实现该功能
     */
    fun aiFoundationKit(): AIFoundationKit? {
        return AIFoundationKit()
    }

    /**
     * 获取设备信息
     *
     * 本函数旨在收集并获取当前设备的相关信息，包括：设备ID，平台上唯一设备标识；设备号，产品内唯一标识设备的序列号；产品ID，平台创建产品时生成；设备密钥，平台创建产品时生成
     * 这些信息将用于 AIAssistantManager 类中的其他功能，以确保 SDK 在设备中的正确使用
     */
    internal fun obtainDeviceInformation() {
        gateWay.obtainDeviceInformation({ response ->
            Timber.tag(TAG).d("%s%s", "response: ", response)
//            getGateWay()
//            dataReport()
            largeModelCasualChat()
//            textToAudio()
//            insideRcChat()
//            textToImage()
//            imageClassify()
//            speechFileTransfer()
//            speechFileTransferQuery()
//            getUploadAudioUrl()
//            imageTranslate()
//            asrTranslation()
//            speechRecognitionPersistent()
//          contentSummary()
        }, { error ->
            Timber.tag(TAG).e("%s%s", "error: ", error)
        })
    }

    /**
     * 获取网关信息
     *
     * 此方法用于获取当前系统的网关信息包含：
     * token，网关验证令牌，用于验证代理服务的合法性
     * expires，代理有效期（单位：秒），表示该代理服务的有效时间长度
     * status，状态码（1：使用代理，0：不使用代理），指示是否需要使用代理服务
     */
    private fun getGateWay() {

        gateWay.getGateWay({ response ->
            Timber.tag(TAG).d("%s%s", "response: ", response)
        }, { error ->
            Timber.tag(TAG).e("%s%s", "error: ", error)
        })

    }

    /**
     * 执行设备参数采集上报
     *
     * ⼼跳接⼝/上报接⼝(定时向云端发送消息)
     * 设备（24小时至少上报一次）向云端上报信息，更新最后活动时间。
     *
     * 请求策略(参考)
     * 设备每隔12小时向平台上报设备数据信息。
     * 实施步骤：
     * 1. 设备初始化：设备首次启动时，向平台上报数据并记录上报时间T_current。
     * 2. 计算下次上报时间：设备每次上报数据后，记录当前时间 T_current ，并计算下次上报时间 T_next ：
     * T_next =T_current +12 小时+随机偏移量
     * 其中，随机偏移量可以是在-15分钟到+15分钟之间的一个随机值。
     * 3. 调度上报任务：设备根据计算出的 T_next 设置定时任务，确保在该时间点上报数据。
     * 注意：避免固定时间集中上报，造成服务器压力过大。
     *
     */
    private fun dataReport() {
        gateWay.dataReport(
            DeviceReportRequest(
                deviceId = AIAssistantManager.getInstance().aiAssistConfig.deviceId,
                deviceSecret = AIAssistantManager.getInstance().aiAssistConfig.deviceSecret,
                productId = AIAssistantManager.getInstance().aiAssistConfig.productId,
                productKey = AIAssistantManager.getInstance().aiAssistConfig.productKey,
                params = mutableMapOf()
            ),
            { response ->
                Timber.tag(TAG).d("%s%s", "response: ", response)
            }, { error ->
                Timber.tag(TAG).e("%s%s", "error: ", error)
            })
    }

    /**
     * 大模型闲聊、信息问答
     * */
    private fun largeModelCasualChat() {
        val messages = listOf(
            Message(role = "system", content = "You are a helpful assistant."),
            Message(role = "user", content = "你好")
        )
        val model = "jiutian_75b" // 默认模型名称，实际应用中应根据需求动态生成或从其他地方获取
        aiFoundationKit.largeModelCasualChat(
            ChatCompletionRequest(
                messages = messages,
                model = model,
                temperature = 0.1,
                top_p = 0.1,
                stream = true
            ),
            { response ->
                Timber.tag(TAG).d("%s%s", "response: ", response)
            }, { error ->
                Timber.tag(TAG).e("%s%s", "error: ", error)
            })
    }

}
