# Module AI-Assist SDK

## 1. 概述

### 版本信息
- SDK 版本：0.9.6
- 发布日期：2025-02
- 文档更新：2025-06-10

### 系统要求
- Android API Level：21+
- Java Version：11+
- Kotlin Version：1.8+

### 主要功能
- 设备管理和连接

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
    implementation(name: 'deviceAccess-release_0.9.6_', ext: 'aar') // 设备管理
}
```

### 权限配置
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" />
```
### 混淆配置
```kotlin
-keep class com.cmdc.ai.assist.** {*;}

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

// 3. 获取设备管理实例
val deviceAccess = getInstance().gateWayHelp()

```

## 3. 接口定义

### 配置管理
提供以下配置选项：
- 产品 ID 配置
- 产品密钥配置
- 设备编号配置
- 设备编号类型配置

### 设备服务
服务功能：
- 设备信息获取
- 设备采集信息上报

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
- 设备管理

### GateWay
服务功能：
- 获取设备信息
- 设备信息数据上报

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
val deviceAccess = AIAssistantManager.getInstance().gateWayHelp()
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

        // 获取设备管理服务
        val deviceAccess = manager.gateWayHelp()

    }

    /**
     * 获取设备信息
     *
     * 本函数旨在收集并获取当前设备的相关信息，包括：设备ID，平台上唯一设备标识；设备号，产品内唯一标识设备的序列号；产品ID，平台创建产品时生成；设备密钥，平台创建产品时生成
     * 这些信息将用于 AIAssistantManager 类中的其他功能，以确保 SDK 在设备中的正确使用
     */
    fun obtainDeviceInformation() {
      deviceAccess.obtainDeviceInformation({ response ->
            Timber.tag(TAG).d("%s%s", "response: ", response)
            getGateWay()
            dataReport()
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
     * 策略1：设备每天首次使用时上报设备数据信息。
     * 策略2：设备每隔12小时向平台上报设备数据信息。
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
      deviceAccess.dataReport(
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
- A: INTERNET（网络通信）权限、READ_EXTERNAL_STORAGE、WRITE_EXTERNAL_STORAGE、MANAGE_EXTERNAL_STORAGE（文件读写权限）。

### 连接问题
- Q: 设备连接失败如何处理？
- A: 检查网络状态，确认设备标识是否正确，查看日志中的具体错误信息。

### 设备信息上报问题
- Q: 设备信息上报，sdk verson 这个信息如何获取 ？
- A: AI-SDK 内部自动维护这个字段的信息上报，厂商无需关注。

- Q: CMEI码如何获取，编码规则及要求？
- A: CMEI码为15位，规则如下，其中前8位是TUI码(Terminal unique
  identification)，厂商在营销系统提交产品报备后系统自动分配TUI码。按照要求，CMEI码要在硬件及外包装盒体现，软件同步写入，心跳上报时CMEI码必须上传，并在入库测试时检查确认。

|        |    |  |         |   |
|----------|------|--------|---------------|---|
| 编码       | 11   | 123456 | 123456        | 1 |
| 位数       | 2位   | 6位     | 6位            | 1位 |
| 含义       | 非蜂窝智能 硬件标识 位，固定为 11 | 随机码，用来区分产品品牌与型号，由中国移动统一分配      | 序列号，厂家自行填写，标识产品唯一性。 |  随机码，此位无实际意义，可以固定为0-9的 任何一个值，或按照 IMEI 生成规 则做校验位 |


## 7. 设备信息上报说明

### 请求参数

|               |  |  |  |  |
|-----------------|------|-------|---------|-------|
| 字段              | 说明 | 值类型 | 是否必填 | 说明 |
| deviceId        | 设备ID，平台上唯⼀设备标识 | string | 是 |  |
| deviceSecret    | 设备密钥，与设备ID⼀⼀对应 | string | 是 |  |
| productId       | 产品ID，平台创建产品时⽣成 | string | 是 |  |
| productKey      | 产品密钥，平台创建产品时⽣成 | string | 是 |  |
| params          | 上报携带的参数 | map | 是 |  |
| +innerIp        | 内网IP | List<string> | 是 |  |
| +netSpeed        | 网络分级 （300Mbps） | string | 是 | 如无法获取，可以传固定（0Mbps） |
| +netType         | 网络类型（wifi，5G） | string |是 |  |
| +platform        | 操作系统 （Android11,RTOS） | string | 是 |  |
| +sdkVersion      | SDK版本（固定ai_前缀） | string | 是 | 使用api，传ai_http_1.0，若使用aisdk, aisdk内部根据实际版本号填充 |
| +firmwareVersion | 固件版本 | string | 是 |  |
| +imei            | imei(蜂窝类产品必传) | string | 是 | 获取不了，传空字符串("") |
| +cmei            | cmei（有则必传） | string | 是 | 获取不了，传空字符串("") |
| +mac             | mac地址 | string | 是 | 设备mac地址 （xx-xx-xx-xx-xx-xx） |

### 请求示例

```kotlin
{
    "deviceId": "123456789",
    "deviceSecret": "xxxxx",
    "productId": "1234567890123",
    "productKey": "xxxxxxx",
    "params": {
    "innerIp": [
    "10.229.111.172",
    "192.168.1.7"
    ],
    "netSpeed": "0Mbps",
    "netType": "WiFi",
    "platform": "rtos",
    "sdkVersion": "ai_http_1.0",
    "firmwareVersion": "0.0.5.2",
    "imei": "",
    "cmei": "",
    "mac": "XXXX"
}
}
```