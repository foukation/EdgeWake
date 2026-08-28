package com.cmdc.ai.assist.constraint

// 请求体数据类
/**
 * 设备信息请求类
 *
 * 用于封装设备信息请求的相关数据，包括设备ID、设备类型、用户ID等
 * 主要解决设备信息在系统间传递和处理的问题
 *
 * @param deviceId 设备ID，用于唯一标识一个设备
 * @param deviceType 设备类型，描述设备的种类
 * @param userId 用户ID，标识设备所属的用户
 */
data class DeviceInfoRequest(
    val deviceNoType: String,  // 设备号类型：MAC、SN、IMEI
    val deviceNo: String,      // 设备号，产品唯一标识设备的序列号
    val productId: String,     // 产品ID，平台创建产品时生成
    val productKey: String     // 产品密钥，平台创建产品时生成
)

// 响应体数据类
/**
 * 设备信息响应类
 *
 * 该类用于封装设备信息请求的响应结果，包括响应状态码、提示信息和具体数据
 */
data class DeviceInfoResponse(
    val status: Int,           // 成功=200，其他值为异常
    val message: String,       // 响应结果，错误提示信息2分钟
    val data: DeviceData?      // 响应数据
)

// 响应数据详情
/**
 * 设备数据类，用于表示和操作设备的相关信息
 *
 * @param deviceId 设备ID，这是在平台上唯一标识一个设备的标识符
 * @param deviceNo 设备号，这是在产品内部唯一标识设备的序列号
 * @param productId 产品ID，这是在平台创建产品时生成的标识符
 * @param deviceSecret 设备密钥，这是在平台创建产品时生成的，用于设备身份验证
 */
data class DeviceData(
    val deviceId: String,      // 设备ID，平台上唯一设备标识
    val deviceNo: String,      // 设备号，产品内唯一标识设备的序列号
    val productId: String,     // 产品ID，平台创建产品时生成
    val deviceSecret: String   // 设备密钥，平台创建产品时生成
)

