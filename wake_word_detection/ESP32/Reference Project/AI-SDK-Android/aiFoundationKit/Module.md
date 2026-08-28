# Module AI-Assist SDK

## 1. 概述

### 版本信息
- SDK 版本：1.0.0
- 发布日期：2025-02
- 文档更新：2025-03-18

### 系统要求
- Android API Level：21+
- Java Version：11+
- Kotlin Version：1.8+

### 主要功能
- 网关服务集成
- 设备管理和连接
- AI 基础服务功能包

## 2. 快速开始

### 依赖配置
```gradle
dependencies {

    implementation platform('org.jetbrains.kotlin:kotlin-bom:1.8.0')

    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.squareup.okhttp3:okhttp-sse:4.9.3")
    implementation("com.google.code.gson:gson:2.10.1")

    // Kotlin Coroutines
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1'

    implementation("com.jakewharton.timber:timber:4.7.1")
    implementation 'org.java-websocket:Java-WebSocket:1.5.1'
    
    // SDK
    implementation(name: 'glasses-SDK0.9.4_', ext: 'aar')
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

// 4. 获取 AI 功能基础工具包
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

### AI 功能基础工具包
提供基础性 AI 服务：
- 设备端中控 NLU 及问答、闲聊、文案创作等


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
- AI 功能基础工具包

### GateWay
网关服务功能：
- 获取网关信息
- 获取设备信息
- 设备信息数据上报

### AIFoundationKit
AI 功能基础工具包：
- NLU 中控（TTS、闲聊、查询及问答、文本创作、文生图、播放音乐、意图识别）

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