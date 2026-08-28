package com.cmdc.ai.assist.constraint

// 请求体数据类
/**
 * DeviceReportRequest 数据类用于定义设备上报请求的参数结构
 * 它包含了设备上报所需的基本信息和额外的参数
 *
 * @param deviceId 设备ID，用于唯一标识一个设备
 * @param deviceSecret 设备密钥，用于设备身份验证
 * @param productId 产品ID，标识设备所属的产品
 * @param productKey 产品密钥，用于产品身份验证
 * @param params 包含设备上报的额外参数的映射，如内网IP、网络速度、操作系统等
 */
data class DeviceReportRequest(
    val deviceId: String,
    val deviceSecret: String,
    val productId: String,
    val productKey: String,
    /**
     *  innerIp: List<String>,  内网IP
     *  netSpeed: String,   网络分级（300Mbps）
     *  netType: String,    网络类型（wifi，5G）
     *  platform: String,   操作系统 （Android11）
     *  sdkVersion: String, SDK版本（固定ai_前缀）
     *  firmwareVersion: String,    固件版本
     *  imei: String,   imei(蜂窝类产品必传)
     *  cmei: String,   cmei（有则必传）
     *  mac: String     mac地址
     *  以上参数，添加到 params 中，进行上报
     */
    val params: MutableMap<String, Any>
)

// 响应体数据类
/**
 * 设备报告响应类
 *
 * 该类用于封装设备报告的响应信息，包括响应状态码、消息内容和具体的数据对象
 * 它是一个数据类，自动生成了一些有用的函数，如equals、hashCode和toString等
 *
 * @param status 响应状态码，用于表示响应的类型或结果
 * @param message 响应消息，提供了关于响应的更多信息
 * @param data 设备报告的具体数据对象，包含实际的报告内容
 */
data class DeviceReportResponse(
    val status: Int,
    val message: String,
    val data: DeviceReportData
)

/**
 * 设备报告数据类
 * @param deviceId 设备ID，用于唯一标识一个设备
 * @param protocolTypeTime 协议类型时间，表示设备使用的协议类型及其相关时间信息
 */
data class DeviceReportData(
    val deviceId: String,
    val protocolTypeTime: String
)