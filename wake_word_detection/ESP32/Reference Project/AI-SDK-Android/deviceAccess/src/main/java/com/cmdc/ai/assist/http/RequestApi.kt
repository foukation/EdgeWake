package com.cmdc.ai.assist.http

import ModelsAvailRes
import com.cmdc.ai.assist.AIAssistantManager
import com.cmdc.ai.assist.constraint.AgentServeRes
import com.cmdc.ai.assist.constraint.ChatModelRes
import com.cmdc.ai.assist.constraint.DeviceInfoRequest
import com.cmdc.ai.assist.constraint.DeviceInfoResponse
import com.cmdc.ai.assist.constraint.DeviceReportRequest
import com.cmdc.ai.assist.constraint.DeviceReportResponse
import com.cmdc.ai.assist.deviceAccess.BuildConfig
import com.cmdc.ai.assist.utils.AssistUtils
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Headers
import okhttp3.Response
import timber.log.Timber
import java.io.IOException

object RequestApi {

    private val TAG = RequestApi::class.simpleName.toString()
    private val gson: Gson = GsonBuilder().create()
    private val mOkHttpManager = OkHttpManager()

    private fun createHeaders(httpUrl: String): Headers {
        return Headers.Builder()
            .add("Content-Type", "application/json; charset=utf-8")
            .apply {
                if (ApiConfig.useAgent) {
                    add("X-PROXY-PASS", httpUrl)
                    add("Token", ApiConfig.apiToken)
                }
            }
            .build()
    }

    private fun createGateWayHeaders(): Headers {
        return Headers.Builder()
            .add("Content-Type", "application/json; charset=utf-8")
            .apply {
                add("X-AI-VID", AIAssistantManager.getInstance().aiAssistConfig.productId)
                add("X-AI-UID", AIAssistantManager.getInstance().aiAssistConfig.deviceId)
            }
            .build()
    }

    private fun createAIServerHeaders(): Headers {
        val timestamp = AssistUtils.timestamp()
        return Headers.Builder()
            .add("Content-Type", "application/json; charset=utf-8")
            .apply {
                add("sign", AssistUtils.signMd5(timestamp))
                add("deviceNo", AIAssistantManager.getInstance().aiAssistConfig.deviceNo)
                add("productId", AIAssistantManager.getInstance().aiAssistConfig.productId)
                add("productKey", AIAssistantManager.getInstance().aiAssistConfig.productKey)
                add("ts", timestamp.toString())
                add("deviceId", AIAssistantManager.getInstance().aiAssistConfig.deviceId)
            }
            .build()
    }

    /*
     * 获取网关代理地址
     */
    internal fun getGateWay(
        onSuccess: (AgentServeRes) -> Unit,
        onError: (String) -> Unit
    ) {
        // 将 AgentServeReq 转换为 Map
        /*val paramsMap = gson.fromJson(gson.toJson(params), Map::class.java)*/

        mOkHttpManager.get(
            ApiConfig.GATEWAY_API,
            header = createGateWayHeaders(),
            params = null,
            callback = object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    try {

                        var result = response.body?.string()
                        Timber.tag(TAG).d("response : $result")

                        val resp = gson.fromJson(result, AgentServeRes::class.java)
                        if (resp.status == AgentUseCode.USE.alias) {
                            ApiConfig.useAgent = true
                            ApiConfig.agentBaseUrl = resp.data.http
                            ApiConfig.apiToken = resp.token ?: ""
                        }
                        onSuccess(resp)
                    } catch (e: Exception) {
                        onError("请求失败:${e.toString()}")
                        Timber.tag(TAG).e(e.message.toString())
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    onError("接口网络请求异常:${e.toString()}")
                    e.message?.let { Timber.tag(TAG).e(it) }
                }
            })
    }

    /*
     * 获取设备信息
     */
    internal fun obtainDeviceInformation(
        onSuccess: (DeviceInfoResponse) -> Unit,
        onError: (String) -> Unit
    ) {

        var deviceInfoRequest = DeviceInfoRequest(
            deviceNoType = AIAssistantManager.getInstance().aiAssistConfig.deviceNoType,
            deviceNo = AIAssistantManager.getInstance().aiAssistConfig.deviceNo,
            productId = AIAssistantManager.getInstance().aiAssistConfig.productId,
            productKey = AIAssistantManager.getInstance().aiAssistConfig.productKey
        )
        // 将 deviceInfoRequest 转成 json
        var jsonResult = gson.toJson(deviceInfoRequest)

        mOkHttpManager.post(
            ApiConfig.OBTAIN_DEVICE_INFORMATION_API,
            params = jsonResult,
            callback = object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    try {

                        var result = response.body?.string()
                        Timber.tag(TAG).d("response : $result")

                        val resp =
                            gson.fromJson(result, DeviceInfoResponse::class.java)
                        if (resp.status == ResCode.SUC.alias) {
                            resp.data?.let { data ->
                                AIAssistantManager.getInstance().aiAssistConfig.apply {
                                    deviceId = data.deviceId
                                    deviceSecret = data.deviceSecret
                                }
                            }
                        }
                        onSuccess(resp)
                    } catch (e: Exception) {
                        onError("请求失败:${e.toString()}")
                        Timber.tag(TAG).e(e.message.toString())
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    onError("接口网络请求异常:${e.toString()}")
                    e.message?.let { Timber.tag(TAG).e(it) }
                }
            })
    }

    /*
     * 数据上报
     */
    internal fun dataReport(
        deviceReportRequest: DeviceReportRequest,
        onSuccess: (DeviceReportResponse) -> Unit,
        onError: (String) -> Unit
    ) {

        //给 deviceReportRequest.params 添加新的参数 sdkVersion
        deviceReportRequest.params["sdkVersion"] = BuildConfig.SDK_VERSION

        var jsonResult = gson.toJson(deviceReportRequest)

        mOkHttpManager.post(
            ApiConfig.DEVICE_DATA_REPORT_API,
            params = jsonResult,
            callback = object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    try {

                        var result = response.body?.string()
                        Timber.tag(TAG).d("response : $result")

                        val resp =
                            gson.fromJson(result, DeviceReportResponse::class.java)
                        /*if (resp.status == ResCode.SUC.alias) {
                            Timber.tag(TAG).d(resp.toString())
                        }*/
                        onSuccess(resp)
                    } catch (e: Exception) {
                        onError("请求失败:${e.toString()}")
                        Timber.tag(TAG).e(e.message.toString())
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    onError("接口网络请求异常:${e.toString()}")
                    e.message?.let { Timber.tag(TAG).e(it) }
                }
            })
    }

    /*
     * 获取可用模型列表
     */
    internal fun getModelsAvail(
        onSuccess: (ModelsAvailRes) -> Unit,
        onError: (String) -> Unit
    ) {
        mOkHttpManager.get(
            ApiConfig.AVAILABLE_MODELS_API,
            baseUrl = ApiConfig.agentBaseUrl,
            params = null,
            callback = object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    try {
                        val resp =
                            gson.fromJson(response.body?.string(), ModelsAvailRes::class.java)
                        if (resp.code == ResCode.SUC.alias) {
                            onSuccess(resp)
                        } else {
                            val msg = resp.error?.message ?: "请求失败"
                            onError(msg)
                        }
                    } catch (e: Exception) {
                        onError("请求失败")
                        Timber.tag(TAG).e(e.message.toString())
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    onError("接口网络请求异常")
                    e.message?.let { Timber.tag(TAG).e(it) }
                }
            })
    }

    /*
     * 调用模型 chat
     */
    internal fun <ChatModelReq> getChatModel(
        params: ChatModelReq,
        onSuccess: (ChatModelRes) -> Unit,
        onError: (String) -> Unit
    ) {
        mOkHttpManager.post(
            ApiConfig.MODEL_CHAT_API,
            baseUrl = if (ApiConfig.useAgent) ApiConfig.agentBaseUrl else ApiConfig.BASE_URL,
            params = params,
            callback = object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    try {
                        val resp = gson.fromJson(response.body?.string(), ChatModelRes::class.java)
                        if (resp.code == ResCode.SUC.alias) {
                            onSuccess(resp)
                        } else {
                            val msg = resp.error?.message ?: "请求失败"
                            onError(msg)
                        }
                    } catch (e: Exception) {
                        onError("请求失败")
                        Timber.tag(TAG).e(e.message.toString())
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    onError("接口网络请求异常")
                    e.message?.let { Timber.tag(TAG).e(it) }
                }
            })
    }

}