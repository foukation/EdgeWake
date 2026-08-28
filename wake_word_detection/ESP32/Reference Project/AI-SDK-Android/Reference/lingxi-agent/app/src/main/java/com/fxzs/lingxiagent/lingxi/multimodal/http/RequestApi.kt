package com.ai.multimodal.http

import com.ai.multimodal.model.request.ImageAssistantRequest
import com.ai.multimodal.model.request.MedicineRequest
import com.ai.multimodal.model.request.PromptRequest
import com.ai.multimodal.model.response.ImageAssistantResponse
import com.ai.multimodal.model.response.MedicineResponse
import com.ai.multimodal.model.response.PromptResponse
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
                add("X-PROXY-PASS", httpUrl)
            }
            .build()
    }

    /*
     * 图片信息提取
     */
    internal fun imageInformationExtraction(
        imageAssistantRequest: ImageAssistantRequest,
        onSuccess: (ImageAssistantResponse) -> Unit,
        onError: (String) -> Unit
    ) {

        val jsonResult = gson.toJson(imageAssistantRequest)

        mOkHttpManager.post(
            ApiConfig.MULTIMODAL_API,
            ApiConfig.IMAGE_RECOGNITION_BASE_URL,
            params = jsonResult,
            callback = object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    try {

                        val result = response.body?.string()
                        Timber.tag(TAG).d("response : $result")

                        val resp =
                            gson.fromJson(result, ImageAssistantResponse::class.java)
                        Timber.tag(TAG).d(resp.toString())
                        onSuccess(resp)
                    } catch (e: Exception) {
                        onError("请求失败:${e}")
                        Timber.tag(TAG).e(e.message.toString())
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    onError("接口网络请求异常:${e}")
                    e.message?.let { Timber.tag(TAG).e(it) }
                }
            })

    }

    /*
     * 问答
     */
    internal fun questionAnswer(
        promptRequest: PromptRequest,
        onSuccess: (PromptResponse) -> Unit,
        onError: (String) -> Unit
    ) {

        val jsonResult = gson.toJson(promptRequest)

        mOkHttpManager.post(
            ApiConfig.QUESTION_ANSWER_API,
            ApiConfig.QUESTION_ANSWER_BASE_URL,
            params = jsonResult,
            callback = object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    try {

                        val result = response.body?.string()
                        Timber.tag(TAG).d("response : $result")

                        val resp =
                            gson.fromJson(result, PromptResponse::class.java)
                        Timber.tag(TAG).d(resp.toString())
                        onSuccess(resp)
                    } catch (e: Exception) {
                        onError("请求失败:${e}")
                        Timber.tag(TAG).e(e.message.toString())
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    onError("接口网络请求异常:${e}")
                    e.message?.let { Timber.tag(TAG).e(it) }
                }
            })
    }

    /*
     * 问答
     */
    internal fun medicationReminder(
        medicineRequest: MedicineRequest,
        onSuccess: (MedicineResponse) -> Unit,
        onError: (String) -> Unit
    ) {

        val jsonResult = gson.toJson(medicineRequest)

        mOkHttpManager.post(
            ApiConfig.MED_REMINDERS_API,
            ApiConfig.QUESTION_ANSWER_BASE_URL,
            params = jsonResult,
            callback = object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    try {

                        val result = response.body?.string()
                        Timber.tag(TAG).d("response : $result")

                        val resp =
                            gson.fromJson(result, MedicineResponse::class.java)
                        Timber.tag(TAG).d(resp.toString())
                        onSuccess(resp)
                    } catch (e: Exception) {
                        onError("请求失败:${e}")
                        Timber.tag(TAG).e(e.message.toString())
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    onError("接口网络请求异常:${e}")
                    e.message?.let { Timber.tag(TAG).e(it) }
                }
            })
    }

}