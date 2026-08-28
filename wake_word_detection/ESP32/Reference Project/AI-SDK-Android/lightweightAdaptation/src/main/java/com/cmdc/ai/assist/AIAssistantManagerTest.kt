package com.cmdc.ai.assist

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.cmdc.ai.assist.api.AIFoundationKit
import com.cmdc.ai.assist.api.GateWay
import com.cmdc.ai.assist.api.SpeechRecognition
import com.cmdc.ai.assist.api.SpeechRecognitionPersistent
import com.cmdc.ai.assist.constraint.*
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
    private val hanlder = Looper.getMainLooper()?.let { Handler(it) }

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
            .setToken("sk-crwLQ3MEel44LsGW1273601f7e6b472584634f4b27C35414")
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
//            largeModelCasualChat()
//            largeModelChatbot()
            contentSummary()
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
                //Timber.tag(TAG).d("%s%s", "speechRecognitionPersistent  ", "$message")
            }

            override fun onMessageReceived(bytes: ByteBuffer?) {
                // 返回 pcm 格式的 tts 音频播报数据
                //Timber.tag(TAG).d("%s%s", "speechRecognitionPersistent  ", "$bytes")
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                // 当连接关闭时调用
                //Timber.tag(TAG).d("%s%s", "speechRecognitionPersistent  ", "onClose")
            }

            override fun onError(ex: Exception?) {
                // 当发生错误时调用
                //Timber.tag(TAG).d("%s%s", "speechRecognitionPersistent  ", "onError")
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
            aiFoundationKit.cancelStreamRequest(requestId) // 停止当前正在进行的操作，适用于流式请求。
        }, 1000)
    }

    /**
     * 大模型闲聊 chatbot 闲聊
     * */
    private fun largeModelChatbot() {
        val messages = listOf(
            ChatbotMessage(role = "system", content = "You are a helpful assistant."),
            ChatbotMessage(role = "user", content = "写一篇100字的作为")
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
            aiFoundationKit.cancelStreamRequest(requestId) // 停止当前正在进行的操作，适用于流式请求。
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
                aue = 3
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
                ),
            { response ->
                Timber.tag(TAG).d("%s%s", "response: ", response)
            }, { error ->
                Timber.tag(TAG).e("%s%s", "error: ", error)
            })
        hanlder?.postDelayed({
            aiFoundationKit.cancelStreamRequest(requestId) // 停止当前正在进行的操作，适用于流式请求。
        }, 2000)
    }

    /**
     * 文本翻译
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


}
