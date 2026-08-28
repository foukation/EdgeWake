# Module AI-Assist SDK

## 1. 概述

### 版本信息
- SDK 版本：0.9.5
- 发布日期：2025-02
- 文档更新：2025-03-24

### 系统要求
- Android API Level：21+
- Java Version：11+
- Kotlin Version：1.8+

### 主要功能
- 网关服务集成
- 设备管理和连接
- AI 语音识别和对话
- 实时语音翻译
- NLU 中控
- AI 功能基础工具包

## 2. 快速开始

### 依赖配置
```gradle
dependencies {

    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.squareup.okhttp3:okhttp-sse:4.9.3")
    implementation("com.google.code.gson:gson:2.10.1")

    implementation platform('org.jetbrains.kotlin:kotlin-bom:1.8.0')
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1'

    implementation("com.jakewharton.timber:timber:4.7.1")
    implementation 'org.java-websocket:Java-WebSocket:1.5.1'
    implementation "androidx.annotation:annotation:1.5.0"
    
    // SDK
    implementation(name: 'deviceAccess_gatewayProxy-release_0.9.5_', ext: 'aar') // 设备管理、网关
    implementation(name: 'asrIntelligentDialogue-release_0.9.5_', ext: 'aar') // ASR 智能对话
    implementation(name: 'asrTranslation-release_0.9.5_', ext: 'aar') // 实时语音翻译
    implementation(name: 'aiFoundationKit-release_0.9.5_', ext: 'aar') // AI 功能基础工具包
}
```

### 权限配置
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

### 初始化步骤
```kotlin
// 1. 创建配置
val config = AIAssistConfig.Builder()
    .setProductId("***")
    .setProductKey("***")
    .setDeviceNo("***")
    .setDeviceNoType("***")
    .build()

// 2. 检查配置是否有效,初始化 SDK 
if (config.isValid()) {
    // 使用配置初始化
    initialize(this, config)
}

// 3. 获取网关实例
val gateWay = getInstance().gateWayHelp()

// 4. 获取 asr 智能对话实例
val asr = getInstance().asrIntelligentDialogueHelp()

// 5. 获取实时语音翻译实例
val asrTranslation = getInstance().asrTranslationHelp() as ASRTranslation

// 6. 获取 AI 功能基础工具包
val aiFoundationKit = getInstance().aiFoundationKit()
```

## 3. 接口定义

### 配置管理
提供以下配置选项：
- 产品 ID 配置
- 产品密钥配置
- 设备编号配置
- 设备编号类型配置

### 网关服务
网关服务功能：
- 路由代理地址获取
- 设备信息获取
- 设备采集信息上报

### 智能语音识别
语音识别和对话功能：
- 实时语音识别
- 智能对话交互
- 语音命令处理

### 实时语音翻译
实时语音翻译功能：
- 中译英
- 英译中

### AI 功能基础工具包
提供基础性 AI 服务：
- 设备端中控 NLU 及问答、闲聊、文案创作等。
- 获取音频文件的上传 URL。
- 通用物体及场景识别。
- 图像翻译。
- 语音文件识别。
- 语音文件识别结果查询。
- AI 画图。

## 4. API 参考

### AIAssistConfig
配置管理类，用于设置 SDK 运行参数：
```kotlin
class AIAssistConfig private constructor(
    val productId: String,     // 产品 id
    val productKey: String,    // 产品 key
    val deviceNo: String,      // 设备编号
    val deviceNoType: String,  // 设备类型
)
```

### AIAssistantManager
SDK 核心管理类，提供以下功能：
- SDK 初始化
- 网关服务访问
- 设备管理
- ASR 智能对话
- ASR 实时翻译
- AI 功能基础工具包

### GateWay
网关服务功能：
- 获取网关信息
- 获取设备信息
- 设备信息数据上报

### ASRIntelligentDialogue
ASR 智能对话功能：
- 语音识别
- TTS
- 闲聊
- 查询及问答
- 文本创作
- 文生图
- 播放音乐
- 意图识别

### ASRTranslation
ASR 实时翻译功能：
- 中译英
- 英译中

### AIFoundationKit
AI 功能基础工具包：
- NLU 中控（TTS、闲聊、查询及问答、文本创作、文生图、播放音乐、意图识别）
- 获取音频文件的上传 URL。
- 通用物体及场景识别。
- 图像翻译。
- 语音文件识别。
- 语音文件识别结果查询。
- AI 画图。

## 5. 示例代码

### 基础示例
```kotlin
// 初始化示例
val config = AIAssistConfig.Builder()
    .setProductId("product_id")
    .setProductKey("product_key")
    .setDeviceNo("device_no")
    .setDeviceNoType("device_type")
    .build()

AIAssistantManager.initialize(context, config)

// 获取服务示例
val gateway = AIAssistantManager.getInstance().gateWayHelp()
val asrIntelligentDialogue = AIAssistantManager.getInstance().asrIntelligentDialogueHelp()
val asrTranslation = AIAssistantManager.getInstance().asrTranslationHelp()
val aiFoundationKit = getInstance().aiFoundationKit()
```

### 业务流程
```kotlin
// 完整业务流程示例
class AIAssistExample {
    
    fun initializeSDK(context: Context) {
        // 1. 创建配置
        val config = AIAssistConfig.Builder()
            .setProductId("product_id")
            .setProductKey("product_key")
            .setDeviceNo("device_no")
            .setDeviceNoType("device_type")
            .build()

        // 2. 初始化 SDK
        if (config.isValid()) {
            // 使用配置初始化
            AIAssistantManager.initialize(context, config)
        }

        // 3. 获取服务实例
        val manager = AIAssistantManager.getInstance()
        
        // 4. 使用各项服务
        
        // 获取网关服务
        val gateway = manager.gateWayHelp()
        // 获取 ASR 智能对话服务
        val asrIntelligentDialogue = manager.asrIntelligentDialogueHelp()
        // 获取 ASR 实时翻译
        val asrTranslation = manager.asrTranslationHelp()
        // 获取 AI 基础功能包
        val aiFoundationKit = getInstance().aiFoundationKit()
        
    }

    /**
     * 获取设备信息
     *
     * 本函数旨在收集并获取当前设备的相关信息，包括：设备ID，平台上唯一设备标识；设备号，产品内唯一标识设备的序列号；产品ID，平台创建产品时生成；设备密钥，平台创建产品时生成
     * 这些信息将用于 AIAssistantManager 类中的其他功能，以确保 SDK 在设备中的正确使用
     */
    fun obtainDeviceInformation() {
        gateWay.obtainDeviceInformation({ response ->
            Timber.tag(TAG).d("%s%s", "response: ", response)
            getGateWay()
            dataReport()
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
     * "data": {
     *  "http": "https://域名:端口/p/http",
     *  "ws": "wss://域名:端口/p/ws"
     *  }
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
                deviceId = "deviceId",
                deviceSecret = "deviceSecret",
                productId = "productId",
                productKey = "productKey",
                params = mutableMapOf() // 示例：params = mutableMapOf("key" to "value") 将需要采集的参数添加到 params 中
            ),
            { response ->
                Timber.tag(TAG).d("%s%s", "response: ", response)
            }, { error ->
                Timber.tag(TAG).e("%s%s", "error: ", error)
            })
    }

    /**
     * asr 智能对话
     *
     */
    private fun intelligentDialogue() {

        asrIntelligentDialogue.setListener(object : ASRIntelligentDialogue.RealtimeAsrListener {
            override fun onAsrMidResult(text: String) {
                // ASR 识别结果回调,返回中间状态的识别结果
            }

            override fun onAsrFinalResult(text: String) {
                // ASR 识别结果回调,返回最终状态的识别结果
            }

            override fun onDialogueResult(result: DialogueResult) {
                // 智能对话结果回调    
                
                    // 返回数据包的具体 id 
                    val qid = result.getQid()
                    // 本次智能对话结束标志
                    val isEnd = result.is_end()
                    // 智能对话完整回复内容
                    val assistantAnswerContent = result.getAssistant_answer_content()
                    // 返回通用渲染指令
                    val header = result.getHeader()
                    // 返回具体指令对应的字段信息
                    val payload = result.getPayload()

                    if (isEnd == 1) {
                        // 一次智能对话过程，返回的最后一个包
                        // assistantAnswerContent ：这时 ai 助手返回完整内容
                    }

                    try {
                        // 取出具体指令
                        val name = header.optString("name")
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
                                Timber.tag(TAG).d("%s%s", "NluTag  = domain: $domain  ", "intent: $intent")
                            }
                            // 文生图结果指令
                            "RenderMultiImageCard" -> {
                                // 图片
                                val images = payload?.optJSONArray("images")
                                val pic = images.optJSONObject(0).optString("url")
                                Timber.tag(TAG).d("percent = 100 图片 ：%s", pic)
                            }
                            // 音乐播放指令
                            "Play" -> {
                                // 音乐
                                val mediaUrl = payload?.optJSONObject("audioItem")?.optJSONObject("stream")
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
                        Log.e(TAG, "analyzePayload err : $e")
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
        asrIntelligentDialogue.startRecognition(this.baseContext)
        // stop
        //asrIntelligentDialogue.release()
        
    }

    /**
     * 实时翻译
     * */
    private fun asrTranslation() {

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
    }

    /**
     * 中控 NLU
     * 此范例可代替 asr 智能对话的语音输入
     *
     */
    private fun insideRcChat() {
        val messages = listOf(
            InsideRcChatRequest.Message(role = "system", content = "You are a helpful assistant."),
            InsideRcChatRequest.Message(
                role = "user",
                content = "写一篇关于日出的 800 字的小作文。"
            )
        )
        aiFoundationKit.insideRcChat(
            InsideRcChatRequest(
                qid = "***",
                third_user_id = "***",
                cuid = "deviceId",
                messages = messages,
                stream = true,
                dialog_request_id = "***"
            ),
            { response ->
                Timber.tag(TAG).d("%s%s", "response: ", response)
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
                /*url = "https://static001.infoq.cn/resource/image/34/1e/3494fff9202a21yy99465e887b149e1e.jpg",*/
                url = "",
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
    
}
```

### 错误处理
```kotlin
try {
    val config = AIAssistConfig.Builder()
        .setProductId("product_id")
        .setProductKey("product_key")
        .build()

    // 检查配置是否有效
    if (config.isValid()) {
        AIAssistantManager.initialize(context, config)
    } else {
        Log.e("AIAssist", "Invalid configuration")
    }
} catch (e: Exception) {
    Log.e("AIAssist", "Initialization failed", e)
}
```

## 6. 常见问题

### 初始化问题
- Q: SDK 初始化失败怎么办？
- A: 检查配置参数是否完整，确保必需的权限已经授予。

### 权限问题
- Q: 需要哪些权限？
- A: 主要需要 RECORD_AUDIO（语音识别）和 INTERNET（网络通信）权限。

### 连接问题
- Q: 设备连接失败如何处理？
- A: 检查网络状态，确认设备标识是否正确，查看日志中的具体错误信息。

## 7. 代理网关使用说明

![img.png](img.png)

### 不使用代理的情况
- 响应状态码 status = 0
- HTTP 状态码不等于 200
- 接口网络请求异常

### 代理请求规则
- URL 转换示例
```
原始请求：
https://aqua-digital.aipaas.com/smart-channel-aggregation-hubtest/smartChannel/asr

代理请求：
https://域名:端口/p/http/smart-channel-aggregation-hubtest/smartChannel/asr

新增Header：
X-AI-PROXY-PASS: https://aqua-digital.aipaas.com
```
- 必需的请求头

| Header名 | 值 | 是否必须 | 说明 |
|----|------|---------|------|
| X-AI-PROXY-PASS | 原始请求URL | 是 | 需要填写完整的原始请求地址 |
| X-AI-UID | xxxx | 是 | 设备ID(设备注册下发的) |
| X-AI-VID | xxxx | 是 | 产品标识（产品注册获取） |

### 可信域名管理

#### 基本规则
- 仅限可信域名列表中的厂商使用繁星智算网关代理
- 域名必须包含端口号
- 域名列表需由厂商提供并审核

#### 可信域名列表示例

| 厂商名 | 域名和端口 | 业务描述 |
|----|------|---------|
| 示例a | https://aqua-digital.aipaas.com | 语音服务 |
| 示例b | https://aqua-digital2.aipaas.com | 翻译服务 |

### 可信域名管理
- 当发生网络异常时，系统自动切换到原始请求方式
- 代理服务有效期超过expires时间后，需重新获取代理地址
- 建议在应用层面实现自动重试机制