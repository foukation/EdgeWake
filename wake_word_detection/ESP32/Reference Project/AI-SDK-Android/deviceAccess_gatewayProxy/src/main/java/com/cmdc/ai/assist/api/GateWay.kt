package com.cmdc.ai.assist.api

import ModelsAvailRes
import com.cmdc.ai.assist.constraint.AgentServeRes
import com.cmdc.ai.assist.constraint.ChatModelReq
import com.cmdc.ai.assist.constraint.ChatModelRes
import com.cmdc.ai.assist.constraint.DeviceInfoResponse
import com.cmdc.ai.assist.constraint.DeviceReportRequest
import com.cmdc.ai.assist.constraint.DeviceReportResponse
import com.cmdc.ai.assist.http.RequestApi

/**
 * 用于管理和处理与网关相关的操作和数据。
 * 网关作为不同设备和云端之间的桥梁，实现通信和数据传输。
 *
 * */
class GateWay() {

    /**
     * 获取网关信息
     *
     * 使用回调函数处理成功和错误情况，使得该函数可以在异步操作中方便地处理结果
     *
     * @param onSuccess 成功回调，接收一个AgentServeRes类型的参数，表示网关信息
     * @param onError 错误回调，接收一个String类型的参数，表示错误信息
     */
    fun getGateWay(
        onSuccess: (AgentServeRes) -> Unit,
        onError: (String) -> Unit
    ) {
        return RequestApi.getGateWay(onSuccess, onError)
    }

    /**
     * 获取设备信息
     * 设备通过产品信息及设备号从云端获取设备信息，前提是设备号已录⼊到云端平台；
     *
     * 使用回调函数来处理成功和错误的情况
     *
     * @param onSuccess 成功回调，当设备信息成功获取时调用，传递DeviceInfoResponse对象
     * @param onError 错误回调，当获取设备信息遇到错误时调用，传递错误信息字符串
     */
    fun obtainDeviceInformation(
        onSuccess: (DeviceInfoResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        return RequestApi.obtainDeviceInformation(onSuccess, onError)
    }

    /**
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
     * 向设备发送报告请求它采用回调方式处理请求结果，
     * 分别定义了成功和错误处理的回调函数通过这种方式，调用者可以根据需要自定义如何处理成功响应和错误情况
     *
     * @param deviceReportRequest 包含设备报告请求的数据对象
     * @param onSuccess 当设备报告请求成功时调用的回调函数，传递一个DeviceReportResponse对象给它
     * @param onError 当设备报告请求失败时调用的回调函数，传递一个表示错误原因的字符串给它
     */
    fun dataReport(
        deviceReportRequest: DeviceReportRequest,
        onSuccess: (DeviceReportResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        return RequestApi.dataReport(deviceReportRequest, onSuccess, onError)
    }

    /**
     * 获取可用模型列表信息
     * @param onSuccess ModelsAvailRes 成功回调
     * @param onError String 失败回调
     * @return ModelsAvailRes?
     */
    private fun getModelsAvail(onSuccess: (ModelsAvailRes) -> Unit, onError: (String) -> Unit) {
        return RequestApi.getModelsAvail(onSuccess, onError)
    }

    /**
     * 调用聊天模型
     * @param params ChatModelReq
     * @param onSuccess ChatModelRes 成功回调
     * @param onError String 失败回调
     * @return ChatModelRes?
     */
    private fun chatModel(
        vararg params: ChatModelReq,
        onSuccess: (ChatModelRes) -> Unit,
        onError: (String) -> Unit
    ) {
        return RequestApi.getChatModel(params, onSuccess, onError)
    }
}