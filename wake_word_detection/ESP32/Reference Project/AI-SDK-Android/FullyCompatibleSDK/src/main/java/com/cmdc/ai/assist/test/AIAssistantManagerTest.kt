package com.cmdc.ai.assist.test

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.cmdc.ai.assist.AIAssistantManager
import com.cmdc.ai.assist.api.AIFoundationKit
import com.cmdc.ai.assist.api.AISessionManager
import com.cmdc.ai.assist.api.AISessionManager.buildMessagesInsideRcChat
import com.cmdc.ai.assist.api.ASRIntelligentDialogue
import com.cmdc.ai.assist.api.ASRTranslation
import com.cmdc.ai.assist.api.GateWay
import com.cmdc.ai.assist.api.SpeechRecognition
import com.cmdc.ai.assist.api.SpeechRecognitionPersistent
import com.cmdc.ai.assist.constraint.AIAssistConfig
import com.cmdc.ai.assist.constraint.AsrResult
import com.cmdc.ai.assist.constraint.ChatCompletionRequest
import com.cmdc.ai.assist.constraint.ChatbotCompletionRequest
import com.cmdc.ai.assist.constraint.ChatbotContentImageUrl
import com.cmdc.ai.assist.constraint.ChatbotContentText
import com.cmdc.ai.assist.constraint.ChatbotImageUrl
import com.cmdc.ai.assist.constraint.ChatbotMessage
import com.cmdc.ai.assist.constraint.ChineseCharacterRequest
import com.cmdc.ai.assist.constraint.ContentSummaryRequest
import com.cmdc.ai.assist.constraint.DeviceReportRequest
import com.cmdc.ai.assist.constraint.DialogueResult
import com.cmdc.ai.assist.constraint.ImageGenerationRequest
import com.cmdc.ai.assist.constraint.ImageRecognitionRequest
import com.cmdc.ai.assist.constraint.ImageTranslationRequest
import com.cmdc.ai.assist.constraint.InsideRcChatRequest
import com.cmdc.ai.assist.constraint.LanguageConvert
import com.cmdc.ai.assist.constraint.LanguageConvertModel
import com.cmdc.ai.assist.constraint.Message
import com.cmdc.ai.assist.constraint.OcrRequest
import com.cmdc.ai.assist.constraint.SpeechRecognitionData
import com.cmdc.ai.assist.constraint.SpeechRecognitionPersistentData
import com.cmdc.ai.assist.constraint.SpeechRecognitionQueryRequest
import com.cmdc.ai.assist.constraint.SpeechRecognitionRequest
import com.cmdc.ai.assist.constraint.TextToAudioRequest
import com.cmdc.ai.assist.constraint.TranslationData
import com.cmdc.ai.assist.constraint.TranslationRequest
import com.cmdc.ai.assist.constraint.TranslationTypeCode
import com.cmdc.ai.assist.constraint.TtsConfig
import com.cmdc.ai.assist.constraint.UploadAudioUrlRequest
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import timber.log.Timber
import java.nio.ByteBuffer
import java.util.UUID

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
                    /*it.largeModelChatbot()*/
                    /*it.largeModelChatbot()*/
                    /*it.textToAudio()*/
                    /*it.textToImage()*/
                    /*it.imageClassify()*/
                    /*it.speechFileTransfer()*/
                    /*it.speechFileTransferQuery()*/
                    /*it.imageTranslate()*/
                    it.ocr()
                    /*it.wordQuery()*/
                    /*it.contentSummary()*/
                    /*it.textTranslate()*/
                    /*it.asrTranslation()*/
                    /*it.speechRecognition()*/
                    /*it.speechRecognitionPersistent()*/
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
    private val hanlder = Looper.getMainLooper()?.let { Handler(it) }

    private val gateWay by lazy {
        GateWay()
    }

    private val aiFoundationKit by lazy {
        AIFoundationKit()
    }

    private val asrTranslation by lazy {
        AIAssistantManager.Companion.getInstance().asrTranslationHelp() as ASRTranslation
    }

    private fun initWithConfig(context: Context, deviceNo: String) {

        // AI 服务管控
        /*productId:2042505798664425605
        deviceNo:012345678901234
        deviceId:206808845567301
        productKey:ybq09bwvlt*/
        /*val config = AIAssistConfig.Builder()
            .setProductId("2042505798664425605")
            .setProductKey("ybq09bwvlt")
            .setDeviceNo("012345678901234")
            .setDeviceNoType("CMEI") // 传 SN 或 IMEI
            .setToken("sk-mMjU1IYtS9Vadsas99Af217f692e4d329bD63bC2777eD1C9") // jiutian-image
            .setImageToken("sk-ogIiHX80FHLZdyIGFa9c7f1b5dA2421f902a918b7c5fA160") // jiutian-lan
            .setCentralConfigVersion("9")
            .setDialogueTtsConfig(TtsConfig.DEFAULT)
            .setVoiceGainEnabled(false)
            .setAudioFileSaveEnabled(true)
            .build()*/

        // 权益验证
        /*val config = AIAssistConfig.Builder()
            .setProductId("1931900040799399938")
            .setProductKey("lnydIXAgqZFOVMFK")
            .setDeviceNo("869700070000752")
            .setCentralConfigVersion("9")
            .setDialogueTtsConfig(TtsConfig.Companion.DEFAULT)
            .setDeviceNoType("SN")
            .setDeviceIdentifier("***")
            .build()*/

        // 创建配置 影目
        /*val config = AIAssistConfig.Builder()
            .setProductId("1889495584410234882")
            .setProductKey("riAtcQzVmPLQprAL")
            .setDeviceNo("YM00GCDCK01896")
            .setCentralConfigVersion("9")
            .setDialogueTtsConfig(TtsConfig.Companion.DEFAULT)
            .setDeviceNoType("SN")
            .setDeviceIdentifier("***")
            .build()*/

        // 创建小水手表配置
        /*val config = AIAssistConfig.Builder()
            .setProductId("1924380531735904259")
            .setProductKey("ZVGHluHqQt")
            .setDeviceNo("1924380000002")
            .setCentralConfigVersion("9")
            .setDialogueTtsConfig(TtsConfig.Companion.DEFAULT)
            .setDeviceNoType("IMEI") // 传 SN 或 IMEI
            .setDeviceIdentifier("***")
            .build()*/

        // 创建小水手表配置
        /*val config = AIAssistConfig.Builder()
            .setProductId("1894635152963973121")
            .setProductKey("JBqShMKINSawhTXP")
            .setDeviceNo("864845080002671")
            .setCentralConfigVersion("6")
            .setDialogueTtsConfig(TtsConfig.Companion.DEFAULT)
            .setDeviceNoType("IMEI") // 传 SN 或 IMEI
            .setDeviceIdentifier("***")
            .build()*/

        //创建来酷配置
        /*val config = AIAssistConfig.Builder()
            .setProductId("1853455163759656961")
            .setProductKey("gTcvObosZDAqDkbt")
            .setDeviceNo("867089070001891")
            .setCentralConfigVersion("3")
            .setDialogueTtsConfig(TtsConfig.Companion.DEFAULT)
            .setDeviceNoType("IMEI") // 传 SN 或 IMEI
            .build()*/

        // 创建配置 魅己玩具
        /*val config = AIAssistConfig.Builder()
            .setProductId("2031262928870518785")
            .setProductKey("SPVdcFklfsGPLmDp")
            .setDeviceNo("AIXL2026010001")
            .setDeviceNoType("SN") // 传 SN 或 IMEI
            .setToken("sk-crwLQ3MEel44LsGW1273601f7e6b472584634f4b27C35414")
            .setCentralConfigVersion("9")
            .setDialogueTtsConfig(TtsConfig.Companion.DEFAULT)
            .setVoiceGainEnabled(false)
            .setAudioFileSaveEnabled(true)
            .build()*/

        // 玛斯特智学屏-问答翻译识图
        val config = AIAssistConfig.Builder()
            .setProductId("2042505798664425569")
            .setProductKey("bla8tv4e6e")
            .setDeviceNo("111137590000010")
            .setDeviceNoType("CMEI") // 传 SN 或 IMEI
            .setImageToken("sk-mMjU1IYtS9Vadsas99Af217f692e4d329bD63bC2777eD1C9") // jiutian-image
            .setToken("sk-ogIiHX80FHLZdyIGFa9c7f1b5dA2421f902a918b7c5fA160") // jiutian-lan
            .setCentralConfigVersion("9")
            .setDialogueTtsConfig(TtsConfig.Companion.DEFAULT)
            .setVoiceGainEnabled(false)
            .setAudioFileSaveEnabled(true)
            .build()

        // 创建配置 沃特-AI耳机
        /* val config = AIAssistConfig.Builder()
             .setProductId("1988782995351662594")
             .setProductKey("mRgQQjUgfBqRPLWH")
             .setDeviceNo("NNNP03900162")
             .setDeviceNoType("C86") // 传 SN 或 IMEI
             *//*.setToken("sk-crwLQ3MEel44LsGW1273601f7e6b472584634f4b27C35414")*//*
            .setToken("sk-yBl3Er2kIfzyGAx79191Df282e3a4a459017A1E0F2A2EeB5")
            .setCentralConfigVersion("6")
            .setDialogueTtsConfig(TtsConfig.Companion.DEFAULT)
            .setVoiceGainEnabled(false)
            .setAudioFileSaveEnabled(true)
            .build()*/

        // 创建配置 小象平板学习机
        /*val config = AIAssistConfig.Builder()
            .setProductId("1967506372755107841")
            .setProductKey("hcSGlfoEMfULqLnL")
            .setDeviceNo("2025030686000155")
            .setDeviceNoType("C86") // 传 SN 或 IMEI
            .setToken("sk-crwLQ3MEel44LsGW1273601f7e6b472584634f4b27C35414")
            .build()*/

        // 创建配置 广东移动灵犀智能体 2.0 app
        /*val config = AIAssistConfig.Builder()
            .setProductId("1924380531735904259")
            .setProductKey("ZVGHluHqQt")
            .setDeviceNo("1924380000002")
            .setDeviceNoType("SN")
            .setDialogueTtsConfig(TtsConfig.DEFAULT)
            .build()
        config.deviceId = "172134920644933"
        config.deviceSecret = "xotzn39fvwdxr8u6"*/

        // AI-SDK 服务授权联调
        /*val config = AIAssistConfig.Builder()
            .setProductId("1990327053183799299")
            .setProductKey("qpZ1A1ExnK")
            .setDeviceNo("942223960485")
            .setDeviceNoType("SN")
            .setToken("sk-crwLQ3MEel44LsGW1273601f7e6b472584634f4b27C35414")
            .setDialogueTtsConfig(TtsConfig.DEFAULT)
            //.setClientID("lingxi_android")
            .build()
        config.deviceId = "185939099383109"
        config.deviceSecret = "hamf8bl8aeznh6tb"*/

        // 创建配置 灵犀语音助手 App（穿戴设备、儿童场景）
        /*{
            "ProductID": 1983440165929828355
            "ProductKey": "LVDYsRTLvr",
            "DeviceNo": "4344842548908868",
            "DeviceSecret": "6ti8xs9ao25yphk7",
            "DeviceId":184073526710597
        }*/
        /*val config = AIAssistConfig.Builder()
            .setProductId("1983440165929828355")
            .setProductKey("LVDYsRTLvr")
            .setDeviceNo(deviceNo)
            .setDeviceNoType("SN")
            .build()
        config.deviceId = "184073526710597"
        config.deviceSecret = "6ti8xs9ao25yphk7"*/

        // 检查配置是否有效
        if (config.isValid()) {
            // 使用配置初始化
            AIAssistantManager.Companion.initialize(context, config)
            AIAssistantManager.Companion.getInstance().changeTtsConfig(TtsConfig(speed = 10))
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
//            largeModelCasualChat()
//            largeModelChatbot()
//            textToAudio()
//            insideRcChat()
//            textToImage()
//              imageClassify()
//            speechFileTransfer()
//            speechFileTransferQuery()
//            getUploadAudioUrl()
//            imageTranslate()
//              ocr()
//              wordQuery()
//              contentSummary()
//              textTranslateModel()
//            asrTranslation()
//            speechRecognition()
//            speechRecognitionPersistent()
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
                deviceId = AIAssistantManager.Companion.getInstance().aiAssistConfig.deviceId,
                deviceSecret = AIAssistantManager.Companion.getInstance().aiAssistConfig.deviceSecret,
                productId = AIAssistantManager.Companion.getInstance().aiAssistConfig.productId,
                productKey = AIAssistantManager.Companion.getInstance().aiAssistConfig.productKey,
                params = mutableMapOf()
            ),
            { response ->
                Timber.tag(TAG).d("%s%s", "response: ", response)
            }, { error ->
                Timber.tag(TAG).e("%s%s", "error: ", error)
            })
    }

    private val speechRecognition by lazy {
        aiFoundationKit.speechRecognitionHelp()
    }

    /**
     * 获取当前语音识别实例。每次调用，返回不同实例。
     * 用于单次语音识别。
     * */
    private fun speechRecognition() {

        speechRecognition.setListener(object : SpeechRecognition.ASRListener {
            override fun onMessageReceived(message: SpeechRecognitionData?) {
                // 接收翻译结果
                Timber.tag(TAG).d("%s%s", "speechRecognition  ", "$message")
            }

            override fun onMessageReceived(bytes: ByteBuffer?) {
                // 返回 pcm 格式的 tts 音频播报数据
                Timber.tag(TAG).d("%s%s", "speechRecognition  ", "$bytes")
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                // 当连接关闭时调用
                Timber.tag(TAG).d("%s%s", "speechRecognition  ", "onClose")
            }

            override fun onError(ex: Exception?) {
                // 当发生错误时调用
                Timber.tag(TAG).d("%s%s", "speechRecognition  ", "onError")
            }
        })

        // 中译英
        speechRecognition.startRecognition()
        // stop
        //speechRecognition.release()
    }

    private val speechRecognitionPersistent by lazy {
        aiFoundationKit.speechRecognitionPersistentHelp()
    }

    /**
     * 语音识别（持续识别）
     * */
    private fun speechRecognitionPersistent() {

        speechRecognitionPersistent.setListener(object : SpeechRecognitionPersistent.ASRListener {
            override fun onMessageReceived(message: SpeechRecognitionPersistentData?) {
                // 接收翻译结果
                Timber.tag(TAG).d("%s%s", "speechRecognitionPersistent  ", "$message")
            }

            override fun onMessageReceived(bytes: ByteBuffer?) {
                // 返回 pcm 格式的 tts 音频播报数据
                Timber.tag(TAG).d("%s%s", "speechRecognitionPersistent  ", "$bytes")
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                // 当连接关闭时调用
                Timber.tag(TAG).d("%s%s", "speechRecognitionPersistent  ", "onClose")
            }

            override fun onError(ex: Exception?) {
                // 当发生错误时调用
                Timber.tag(TAG).d("%s%s", "speechRecognitionPersistent  ", "onError")
            }
        })

        speechRecognitionPersistent.startRecognition()
        // stop
        //speechRecognitionPersistent.cancel() 取消当前正在进行的操作
        //speechRecognitionPersistent.release() 语音识别完成之后，可调用此方法，停止语音识别
    }

    /**
     * 大模型闲聊、九天闲聊、信息问答
     * */
    private fun largeModelCasualChat() {
        val messages = listOf(
            Message(role = "system", content = "You are a helpful assistant."),
            Message(role = "user", content = "你好")
        )
        val model = "jiutian_75b" // 默认模型名称，实际应用中应根据需求动态生成或从其他地方获取
        val requestId = aiFoundationKit.largeModelCasualChat(
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
        hanlder?.postDelayed({
            //aiFoundationKit.cancelStreamRequest(requestId) // 停止当前正在进行的操作，适用于流式请求。
        }, 1000)
    }

    /**
     * 大模型闲聊 chatbot 闲聊
     * */
    private fun largeModelChatbot() {

        val messages = listOf(
            ChatbotMessage(
                role = "system",
                content = "您好，我是中国移动的智能助理灵犀。如果您询问我的身份，我会回答:您好，我是中国移动智能助理灵犀。回答用户非人设或身份问题时，不要重复你的人设设定。"
            ),
            ChatbotMessage(role = "user", content = "今天天气怎么样？")
        )
        val model = "jiutian-lan" // 默认模型名称，实际应用中应根据需求动态生成或从其他地方获取

        /*val imageBase64 = "" // 图片 base64，注意不要带换行
        var imageDataUrl = "data:image/jpeg;base64,$imageBase64"
        imageDataUrl = "https://dashscope.oss-cn-beijing.aliyuncs.com/images/dog_and_girl.jpeg"

        val messages = listOf(
            ChatbotMessage(
                role = "system",
                content = "您好，我是中国移动的智能助理灵犀。如果您询问我的身份，我会回答:您好，我是中国移动智能助理灵犀。回答用户非人设或身份问题时，不要重复你的人设设定。"
            ),
            ChatbotMessage(
                role = "user",
                content = listOf(
                    ChatbotContentText(text = "请描述这张图片"),
                    ChatbotContentImageUrl(
                        image_url = ChatbotImageUrl(url = imageDataUrl)
                    )
                )
            )
        )

        val model = "jiutian-image"*/

        val requestId = aiFoundationKit.largeModelChatbot(
            ChatbotCompletionRequest(
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
        hanlder?.postDelayed({
            //aiFoundationKit.cancelStreamRequest(requestId) // 停止当前正在进行的操作，适用于流式请求。
        }, 3000)
    }

    /**
     * 文本转语音
     * */
    private fun textToAudio() {
        aiFoundationKit.textToAudio(
            TextToAudioRequest(
                text = "春节期间，社交媒体上的一些外国人,对中国这一传统小吃赞不绝口。如今，肉夹馍也成为文化交流的使者。向世界展现一个富足、美味的中国",
                spd = 5,
                pit = 5,
                vol = 5,
                aue = 3,
            ),
            { response ->
                Timber.tag(TAG).d("%s%s", "response: ", response)
            }, { error ->
                Timber.tag(TAG).e("%s%s", "error: ", error)
            })
    }

    /**
     * 设备端请求进行 AI 画图
     * */
    private fun textToImage() {
        aiFoundationKit.textToImage(
            ImageGenerationRequest(
                userId = UUID.randomUUID().toString(),
                batchSize = 4,
                changeDegree = 0,
                query = "帮我画一只奔跑的小猪",
                style = "601",
                token = "14e7d29a2bd640a68493ba61ad1add15"
            ),
            { response ->
                Timber.tag(TAG).d("%s%s", "response: ", response)
            }, { error ->
                Timber.tag(TAG).e("%s%s", "error: ", error)
            })
    }

    /**
     * 通用物体及场景识别，即对于输入的一张图片（可正常解码，且长宽比适宜），输出图片中的多个物体及场景标签。
     * */
    private fun imageClassify() {
        aiFoundationKit.imageClassify(
            ImageRecognitionRequest(
                imageBase64 = "",
                url = "https://www.image2url.com/r2/default/images/1776310308703-0aa3cca1-b170-4a0d-8511-ebcfd4471dce.png",
                baikeNum = 5
            ),
            { response ->
                Timber.tag(TAG).d("%s%s", "response: ", response)
            }, { error ->
                Timber.tag(TAG).e("%s%s", "error: ", error)
            })
    }

    /**
     * 语音文件识别。 根据音频 url、音频格式、语言 id 以及采样率等参数创建音频转写任务。
     * */
    private fun speechFileTransfer() {
        aiFoundationKit.speechFileTransfer(
            SpeechRecognitionRequest(
                speech_url = "https://naiot.eos-wuxi-5.cmecloud.cn/device/ai/audio/d0aI2yidbUUK/869700070000570/9533e5408b5e4d24a431da1dac45f5e0_xxx.mp3?AWSAccessKeyId=5P4d7f1nmRYJfSGQW6W1WYz56XtZ&Expires=1743385080&Signature=REmKgFhvOSw%2FNb0TQprrm2VEE2s%3D",
                format = "pcm",
                pid = 80001,
                rate = 16000
            ),
            { response ->
                Timber.tag(TAG).d("%s%s", "response: ", response)
            }, { error ->
                Timber.tag(TAG).e("%s%s", "error: ", error)
            })
    }

    /**
     * 语音文件识别结果查询。
     * */
    private fun speechFileTransferQuery() {
        val taskIds = listOf(
            "67cd99f5f4df4b0001190119", // 识别任务 ID
        )
        aiFoundationKit.speechFileTransferQuery(
            SpeechRecognitionQueryRequest(
                task_ids = taskIds
            ),
            { response ->
                Timber.tag(TAG).d("%s%s", "response: ", response)
            }, { error ->
                Timber.tag(TAG).e("%s%s", "error: ", error)
            })
    }

    /**
     * 获取音频文件的上传 URL、访问 URL。此接口默认使用中国移动，移动云服务，如果厂商有自己的云文件存储，可不使用此接口。
     * */
    private fun getUploadAudioUrl() {
        aiFoundationKit.getUploadAudioUrl(
            UploadAudioUrlRequest(
                filename = "haha",
                filesize = "10"
            ),
            { response ->
                Timber.tag(TAG).d("%s%s", "response: ", response)
            }, { error ->
                Timber.tag(TAG).e("%s%s", "error: ", error)
            })
    }

    /**
     * 图像翻译。
     * */
    private fun imageTranslate() {
        aiFoundationKit.imageTranslate(
            ImageTranslationRequest(
                from = "en",
                to = "zh",
                imageBase64 = "",
                v = 3,
                paste = 1
            ),
            { response ->
                Timber.tag(TAG).d("%s%s", "response: ", response)
            }, { error ->
                Timber.tag(TAG).e("%s%s", "error: ", error)
            })
    }


    /**
     * ocr
     * */
    private fun ocr() {
        aiFoundationKit.ocr(
            OcrRequest(
                image = "iVBORw0ZmYvb1 ..... +ysEpS6Ha71HlzW/aR",
            ),
            { response ->
                Timber.tag(TAG).d("%s%s", "response: ", response)
            }, { error ->
                Timber.tag(TAG).e("%s%s", "error: ", error)
            })
    }

    /**
     * wordQuery
     * */
    private fun wordQuery() {
        aiFoundationKit.wordQuery(
            ChineseCharacterRequest(
                word = "你",
            ),
            { response ->
                Timber.tag(TAG).d("%s%s", "response: ", response)
            }, { error ->
                Timber.tag(TAG).e("%s%s", "error: ", error)
            })
    }

    /**
     * 内容摘要
     * */
    private fun contentSummary() {
        val requestId = aiFoundationKit.contentSummary(
            ContentSummaryRequest(
                content = "近日，中国科学院宣布在量子计算领域取得重大突破。研究团队成功研发出新型量子计算芯片，实现了72个量子比特的稳定控制，大幅提升了量子计算的处理能力。该成果发表在国际顶级期刊《Nature》上，引起国际科学界广泛关注。专家表示，这一突破将加速量子计算在密码学、新材料开发、药物研发等领域的应用进程。中国科学院量子信息与量子科技创新研究院院长潘建伟教授指出，团队计划在未来三年内，进一步提升量子比特数量至100个以上，并着手解决量子计算实用化过程中的关键技术难题。此外，研究院已与多家高科技企业达成合作，共同推动量子计算技术的产业化应用。国家自然科学基金委员会表示将继续加大对量子计算领域的支持力度，促进基础研究与应用研究的协同发展。",
                language = "English"
            ),
            { response ->
                Timber.tag(TAG).d("%s%s", "response: ", response)
            }, { error ->
                Timber.tag(TAG).e("%s%s", "error: ", error)
            })
        hanlder?.postDelayed({
            //aiFoundationKit.cancelStreamRequest(requestId) // 停止当前正在进行的操作，适用于流式请求。
        }, 2000)
    }

    /**
     * 文本翻译（机器翻译）
     * */
    private fun textTranslate() {
        aiFoundationKit.textTranslate(
            TranslationRequest(
                targetLanguage = LanguageConvert.LANG_EN.code,
                originText = "春节期间，社交媒体上的一些外国人,对中国这一传统小吃赞不绝口。如今，肉夹馍也成为文化交流的使者。向世界展现一个富足、美味的中国",
                sourceLanguage = LanguageConvert.LANG_ZH.code
            ),
            { response ->
                Timber.tag(TAG).d("%s%s", "response: ", response)
            }, { error ->
                Timber.tag(TAG).e("%s%s", "error: ", error)
            })
    }

    /**
     * 文本翻译（模型翻译）
     * */
    private fun textTranslateModel() {
        aiFoundationKit.textTranslateWithModel(
            TranslationRequest(
                targetLanguage = LanguageConvertModel.LANG_EN.code,
                originText = "春节期间，社交媒体上的一些外国人,对中国这一传统小吃赞不绝口。如今，肉夹馍也成为文化交流的使者。向世界展现一个富足、美味的中国",
                sourceLanguage = LanguageConvertModel.LANG_ZH.code
            ),
            { response ->
                Timber.tag(TAG).d("%s%s", "response: ", response)
            }, { error ->
                Timber.tag(TAG).e("%s%s", "error: ", error)
            })
    }

    /**
     * 实时翻译
     * */
    private fun asrTranslation() {

        asrTranslation.setListener(object : ASRTranslation.ASRTranslationListener {
            override fun onMessageReceived(message: TranslationData?) {
                // 接收翻译结果
                val jsonMessage = gson.toJson(message)
                Timber.tag(TAG)
                    .d("%s%s", "onMessageReceived: ", jsonMessage)
            }

            override fun onMessageReceived(bytes: ByteBuffer?) {
                // 返回 pcm 格式的 tts 音频播报数据
                Timber.tag(TAG)
                    .d("收到 pcm 格式的 tts 音频播报数据")
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                // 当连接关闭时调用
                Timber.tag(TAG).d("onClose")
            }

            override fun onError(ex: Exception?) {
                // 当发生错误时调用
                Timber.tag(TAG).e("%s%s", "onError: ", ex.toString())
            }
        })

        // 中译英
        asrTranslation.startRecognition(TranslationTypeCode.ZH_TO_EN)
        // 英译中
        //asrTranslation.startRecognition(TranslationTypeCode.EN_TO_ZH)
        //asrTranslation.startRecognition(Language.CHINESE, Language.ENGLISH)
        // stop
        //asrTranslation.release()
        hanlder?.postDelayed({
            asrTranslation.release()
            Timber.tag("asrTranslation").d("release")
        }, 20 * 1000)
    }

    private val asrIntelligentDialogue by lazy {
        val asrIntelligentDialogue =
            AIAssistantManager.Companion.getInstance().asrIntelligentDialogueHelp()
        asrIntelligentDialogue as ASRIntelligentDialogue
    }

    /**
     * 语音助手 智能对话
     *
     */
    private fun intelligentDialogue() {

        asrIntelligentDialogue.setListener(object : ASRIntelligentDialogue.RealtimeAsrListener {
            override fun onAsrMidResult(asrResult: AsrResult) {
                // ASR 识别结果回调,返回中间状态的识别结果
            }

            override fun onAsrFinalResult(asrResult: AsrResult) {
                // ASR 识别结果回调,返回最终状态的识别结果
            }

            override fun onDialogueResult(result: DialogueResult) {
                // 智能对话结果回调

                // 返回数据包的具体 id
                val qid = result.qid
                // 本次智能对话结束标志
                val isEnd = result.is_end
                // 智能对话完整回复内容
                val assistantAnswerContent = result.assistant_answer_content
                // 返回通用渲染指令
                val header = result.header
                // 返回具体指令对应的字段信息
                val payload = result.payload

                if (isEnd == 1) {
                    // 一次智能对话过程，返回的最后一个包
                    // assistantAnswerContent ：这时 ai 助手返回完整内容
                }

                try {
                    // 取出具体指令
                    val name = header?.optString("name")
                    when (name) {
                        // 图片渲染进度指令
                        "RenderProcessing" -> {
                            val percent = payload?.optInt("percent")
                            Timber.tag(TAG).d("isGenerating = true 进度 ：%s", percent)
                        }
                        // 意图指令
                        "Nlu" -> {
                            // nlu
                            val nlu = payload?.optJSONArray("nlu")
                            Timber.tag(TAG).d("意图 ：%s", nlu?.toString())
                        }
                        // 意图标签指令
                        "NluTag" -> {
                            // nlu
                            val domain = payload?.optString("domian")
                            val intent = payload?.optString("intent")
                            Timber.tag(TAG)
                                .d("%s%s", "NluTag  = domain: $domain  ", "intent: $intent")
                        }
                        // 文生图结果指令
                        "RenderMultiImageCard" -> {
                            // 图片
                            val images = payload?.optJSONArray("images")
                            val pic = images?.optJSONObject(0)?.optString("url")
                            Timber.tag(TAG).d("percent = 100 图片 ：%s", pic)
                        }
                        // 音乐播放指令
                        "Play" -> {
                            // 音乐
                            val mediaUrl =
                                payload?.optJSONObject("audioItem")?.optJSONObject("stream")
                                    ?.optString("url")
                            val albumName =
                                payload?.optJSONObject("audioItem")?.optString("extension")
                            Timber.tag(TAG)
                                .d("%s%s", "Play  = mediaUrl: $mediaUrl  ", "albumName: $albumName")
                        }
                        // 查询及问答、闲聊、文本创作流式输出指令
                        "RenderStreamCard" -> {
                            // 流式
                            val text = payload?.optString("answer")
                            Timber.tag(TAG).d("answer content ：%s", text)
                        }
                        // 语音播放指令
                        "Speak" -> {
                            // tts
                            val ttlUrl = payload?.optString("url")
                            Timber.tag(TAG).d("%s%s", "Speak  = qid: $qid  ", "ttlUrl: $ttlUrl")
                        }
                    }
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e)
                }

            }

            override fun onError(code: Int, message: String) {
                // 智能对话过程中，出现异常回调
            }

            override fun onComplete() {
                // 一次智能对话完整过程结束之后，调用
            }
        })

        // 开始智能对话
        // asrIntelligentDialogue.startRecognition(this.baseContext)
        asrIntelligentDialogue.release()

    }

    /**
     * 此接口用于设备端 NLU 及聊天等
     * */
    private fun insideRcChat() {

        /*val messages = listOf(
            InsideRcChatRequest.Message(role = "system", content = "You are a helpful assistant."),
            InsideRcChatRequest.Message(
                role = "user",
                content = "你是谁？"
            )
        )*/

        val messages = AISessionManager.getChatDataList().buildMessagesInsideRcChat()
        messages.add(
            InsideRcChatRequest.Message(
                role = "user",
                content = "调高音量"
            )
        )

        val requestId = aiFoundationKit.insideRcChat(
            InsideRcChatRequest(
                qid = UUID.randomUUID().toString(),
                third_user_id = UUID.randomUUID().toString(),
                cuid = AIAssistantManager.Companion.getInstance().aiAssistConfig.deviceId,
                messages = messages,
                stream = true,
                dialog_request_id = UUID.randomUUID().toString()
            ),
            { response ->
                Timber.tag(TAG).d("%s%s", "insideRcChat response: ", response)
            }, { error ->
                Timber.tag(TAG).e("%s%s", "insideRcChat error: ", error)
            })
        // aiFoundationKit.cancelStreamRequest(requestId) // 停止当前正在进行的操作，适用于流式请求。
    }

}
