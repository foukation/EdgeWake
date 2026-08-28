package com.cmdc.ai.assist

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.cmdc.ai.assist.api.AISessionManager
import com.cmdc.ai.assist.api.AISessionManager.buildMessagesInsideRcChat
import com.cmdc.ai.assist.api.AIFoundationKit
import com.cmdc.ai.assist.api.ASRIntelligentDialogue
import com.cmdc.ai.assist.api.ASRTranslation
import com.cmdc.ai.assist.api.GateWay
import com.cmdc.ai.assist.api.SpeechRecognition
import com.cmdc.ai.assist.api.SpeechRecognitionPersistent
import com.cmdc.ai.assist.constraint.*
import com.cmdc.ai.assist.http.AIFoundationKitRequestApi
import timber.log.Timber
import java.lang.Exception
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
                    /*it.sdkLogInit(context)
                    it.setEdgeCacheFilePath(context)*/
                    it.initWithConfig(context, deviceNo)
                    it.obtainDeviceInformation()
                    /*it.insideRcChat()*/
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

    private val hanlder = Looper.getMainLooper()?.let { Handler(it) }

    private val gateWay by lazy {
        GateWay()
    }

    private val aiFoundationKit by lazy {
        AIFoundationKit()
    }

    /*private val asrTranslation by lazy {
        ASRTranslation()
    }*/

    private val speechRecognition by lazy {
        aiFoundationKit.speechRecognitionHelp()
    }

    private val speechRecognitionPersistent by lazy {
        aiFoundationKit.speechRecognitionPersistentHelp()
    }

    private fun initWithConfig(context: Context, deviceNo: String) {

        // 创建配置 影目
        /*val config = AIAssistConfig.Builder()
            .setProductId("1889495584410234882")
            .setProductKey("riAtcQzVmPLQprAL")
            .setDeviceNo("YM00GCDCK01896")
            .setDeviceNoType("SN")
            .build()*/

        // 创建配置 小水
        /*val config = AIAssistConfig.Builder()
            .setProductId("1894635152963973121")
            .setProductKey("JBqShMKINSawhTXP")
            .setDeviceNo("863996213426591")
            .setDeviceNoType("IMEI")
            .build()*/

        // 创建配置 灵犀 2.0 demo app
        /*val config = AIAssistConfig.Builder()
            .setProductId("1924380531735904259")
            .setProductKey("ZVGHluHqQt")
            .setDeviceNo("1924380000001")
            .setDeviceNoType("SN")
            .build()*/

        // 创建配置 来酷
        /*val config = AIAssistConfig.Builder()
            .setProductId("1853455163759656961")
            .setProductKey("gTcvObosZDAqDkbt")
            .setDeviceNo("867089070001891")
            .setDeviceNoType("IMEI") // 传 SN 或 IMEI
            .build()*/

        // 创建配置 小象平板学习机
        val config = AIAssistConfig.Builder()
            .setProductId("1967506372755107841")
            .setProductKey("hcSGlfoEMfULqLnL")
            .setDeviceNo("2025030686000155")
            .setDeviceNoType("C86") // 传 SN 或 IMEI
            .build()

        // 莲藕 AR 眼镜
        /*val config = AIAssistConfig.Builder()
            .setProductId("1963851166210125825")
            .setProductKey("ckccftyMKyGlAdVR")
            .setDeviceNo("XR052507Z20224")
            .setDeviceNoType("SN")
            .setToken("sk-crwLQ3MEel44LsGW1273601f7e6b472584634f4b27C35414")
            .build()*/

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
    fun gateWayHelp(): GateWay {
        return gateWay
    }

    /**
     * 获取 ASR 智能对话助手
     *
     * 此方法用于初始化或获取一个ASR（自动语音识别）智能对话助手的实例该助手能够帮助处理与语音识别相关的智能对话任务，如语音命令识别、对话管理等
     *
     * @return ASRIntelligentDialogue? 可能返回一个ASR智能对话助手的实例，也可能返回null如果助手不可用或尚未实现
     */
    fun asrIntelligentDialogueHelp(): ASRIntelligentDialogue? {

        val asrIntelligentDialogue = AIAssistantManager.getInstance().asrIntelligentDialogueHelp()

        return asrIntelligentDialogue as ASRIntelligentDialogue
    }

    /**
     * 提供ASR（自动语音识别）翻译帮助的功能
     * 此函数无需参数，返回一个ASRTranslation对象，用于进行翻译相关的操作
     * 如果无法提供翻译帮助，则返回null
     */
    /*internal fun asrTranslationHelp(): ASRTranslation {
        return ASRTranslation()
    }*/

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
    fun aiFoundationKit(): AIFoundationKit {
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
//            textToAudio()
            insideRcChat()
//            largeModelChatbot()
//            textToImage()
//            imageClassify()
//            speechFileTransfer()
//            speechFileTransferQuery()
//            getUploadAudioUrl()
//            imageTranslate()
//            ocr()
//            wordQuery()
//            speechRecognition()
//            asrTranslation()
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
     * 语音识别
     * */
    private fun speechRecognition() {

        speechRecognition.setListener(object : SpeechRecognition.ASRListener {
            override fun onMessageReceived(message: SpeechRecognitionData?) {
                // 接收翻译结果
            }

            override fun onMessageReceived(bytes: ByteBuffer?) {
                // 返回 pcm 格式的 tts 音频播报数据
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                // 当连接关闭时调用
            }

            override fun onError(ex: Exception?) {
                // 当发生错误时调用
            }
        })

        // 中译英
        speechRecognition.startRecognition()
        // stop
        //speechRecognition.release()
    }

    /**
     * 语音识别
     * */
    private fun speechRecognitionPersistent() {

        speechRecognitionPersistent.setListener(object : SpeechRecognitionPersistent.ASRListener {
            override fun onMessageReceived(message: SpeechRecognitionPersistentData?) {
                // 接收翻译结果
            }

            override fun onMessageReceived(bytes: ByteBuffer?) {
                // 返回 pcm 格式的 tts 音频播报数据
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                // 当连接关闭时调用
            }

            override fun onError(ex: Exception?) {
                // 当发生错误时调用
            }
        })

        // 中译英
        speechRecognitionPersistent.startRecognition()
        // stop
        //speechRecognition.release()
    }

    /**
     * 实时翻译
     * */
    /*private fun asrTranslation() {

        asrTranslation.setListener(object : ASRTranslation.ASRTranslationListener {
            override fun onMessageReceived(message: TranslationData?) {
                // 接收翻译结果
            }

            override fun onMessageReceived(bytes: ByteBuffer?) {
                // 返回 pcm 格式的 tts 音频播报数据
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                // 当连接关闭时调用
            }

            override fun onError(ex: Exception?) {
                // 当发生错误时调用
            }
        })

        // 中译英
        asrTranslation.startRecognition(TranslationTypeCode.ZH_TO_EN)
        // 英译中
        //asrTranslation.startRecognition(TranslationTypeCode.EN_TO_ZH)
        // stop
        //asrTranslation.release()
    }*/

    /**
     * 大模型闲聊
     * */
    private fun largeModelCasualChat() {
        /*val messages = listOf(
            Message(role = "system", content = "You are a helpful assistant."),
            Message(role = "user", content = "Who won the world series in 2020?")
        )
        val model = "jiutian_75b" // 默认模型名称，实际应用中应根据需求动态生成或从其他地方获取
        aiFoundationKit.largeModelCasualChat(
            ChatCompletionRequest(
                messages = messages,
                model = model,
                temperature = 0.1,
                top_p = 0.1,
                stream = false
            ),
            { response ->
                Timber.tag(TAG).d("%s%s", "response: ", response)
            }, { error ->
                Timber.tag(TAG).e("%s%s", "error: ", error)
            })*/
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
                aue = 3
            ),
            { response ->
                Timber.tag(TAG).d("%s%s", "response: ", response)
            }, { error ->
                Timber.tag(TAG).e("%s%s", "error: ", error)
            })
    }

    /**
     * 此接口用于设备端 NLU 及聊天等
     * */
    private fun insideRcChat() {

        /*val messages = listOf(
            InsideRcChatRequest.Message(role = "system", content = "You are a helpful assistant."),
            InsideRcChatRequest.Message(
                role = "user",
                content = "您好。"
            )
        )*/

//        val messages = listOf(

        /**
         * 出行订票
         * */

        //InsideRcChatRequest.Message(role = "user", content = "帮我订一张今天北京去上海的机票"),
        //InsideRcChatRequest.Message(role = "user", content = "为我在北京预定一个酒店,预算 1000元，明天入住，朝阳区。"),
        //InsideRcChatRequest.Message(role = "user", content = "我要去北京玩，明天出发，总共玩10天，来回飞机票，帮我制定一个旅行规划"),
        //InsideRcChatRequest.Message(role = "user", content = "我在北京，我有五天假期，我想去个人少的海边，帮我推荐"),
        //InsideRcChatRequest.Message(role = "user", content = "明天我和朋友准备去吃东北菜，我在石景山首钢园，朋友A在望京，朋友B在知春路，我们都在北京，你帮我们推荐几个价格在150元左右的餐厅吧"),
//            InsideRcChatRequest.Message(role = "user", content = "今天天气怎么样"),
        /*InsideRcChatRequest.Message(
            role = "assistant",
            content = "当然可以！不过得告诉我你想订什么票哦，是电影票、车票还是机票呢？再给我点详细信息，我就能帮你搞定啦！"
        ),
        InsideRcChatRequest.Message(role = "user", content = "飞机票"),
        InsideRcChatRequest.Message(
            role = "assistant",
            content = "想订飞机票啊，没问题！你得告诉我你的出发地、目的地还有出发日期哦，这样我才能帮你查找最合适的航班信息。或者，你也可以直接去航空公司官网、第三方平台如携程、同程、飞猪上查询和购买。"
        ),
        InsideRcChatRequest.Message(
            role = "user",
            content = "出发地是，北京"
        ),
        InsideRcChatRequest.Message(
            role = "assistant",
            content = "嗯"
        ),
        InsideRcChatRequest.Message(
            role = "user",
            content = "目的地是，上海"
        ),
        InsideRcChatRequest.Message(
            role = "assistant",
            content = "嗯"
        ),
        InsideRcChatRequest.Message(
            role = "user",
            content = "出发日期是，明天，上午的"
        ),*/
//            InsideRcChatRequest.Message(
//                role = "assistant",
//                content = "没问题，我帮你查查看。明天从北京飞上海的机票价格大概在几百到上千不等哦，具体价格还得看航班和时间呢。你是想订早上的航班还是下午的呀？"
//            ),
//            InsideRcChatRequest.Message(
//                role = "user",
//                content = "早上的"
//            )

        /**
         * 出行订酒店
         * */
        /*InsideRcChatRequest.Message(role = "user", content = "帮我订一张机票"),
        InsideRcChatRequest.Message(
            role = "assistant",
            content = "没问题，不过你得告诉我你想从哪里飞到哪里，还有出发的日期哦，这样我才能帮你订机票呀！"
        ),*/
//        )

        val messages = AISessionManager.getChatDataList().buildMessagesInsideRcChat()
        messages.add(
            InsideRcChatRequest.Message(
                role = "user",
                content = "你是谁"
            )
        )

        aiFoundationKit.insideRcChat(
            InsideRcChatRequest(
                qid = UUID.randomUUID().toString(),
                third_user_id = UUID.randomUUID().toString(),
                cuid = AIAssistantManager.getInstance().aiAssistConfig.deviceId,
                messages = messages,
                stream = true,
                dialog_request_id = UUID.randomUUID().toString()
            ),
            { response ->
                Timber.tag(TAG).d("%s%s", "insideRcChat: ", response.toString())
            }, { error ->
                Timber.tag(TAG).e("%s%s", "error: ", error)
            })
    }

    /**
     * 此接口用于设备端请求进行 AI 画图
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
     * 此接口用于通用物体及场景识别，即对于输入的一张图片（可正常解码，且长宽比适宜），输出图片中的多个物体及场景标签。
     * */
    private fun imageClassify() {
        aiFoundationKit.imageClassify(
            ImageRecognitionRequest(
                imageBase64 = "",
                url = "https://static001.infoq.cn/resource/image/34/1e/3494fff9202a21yy99465e887b149e1e.jpg",
                baikeNum = 1
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
     * 语音文件识别结果查询。 根据 task_id 的数组批量查询音频转写任务结果。
     * */
    private fun speechFileTransferQuery() {
        val taskIds = listOf(
            "67cd99f5f4df4b0001190119",
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
     * 大模型闲聊、信息问答
     * */
    private fun largeModelChatbot() {
        val messages = listOf(
            ChatbotMessage(role = "system", content = "You are a helpful assistant."),
            ChatbotMessage(role = "user", content = "写一篇500字的作为")
        )
        val model = "jiutian-lan" // 默认模型名称，实际应用中应根据需求动态生成或从其他地方获取
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
            aiFoundationKit.cancelStreamRequest(requestId)
        }, 3000)
    }


}
