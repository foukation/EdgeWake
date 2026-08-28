package com.ai.multimodal.http

import com.ai.multimodal.model.request.ImageAssistantRequest
import com.ai.multimodal.model.request.MedicineRequest
import com.ai.multimodal.model.request.PromptRequest
import com.ai.multimodal.model.response.ImageAssistantResponse
import com.ai.multimodal.model.response.MedicineResponse
import com.ai.multimodal.model.response.PromptResponse


class MultimodalKit() {

    /**
     * 获取图片信息
     *
     * 使用回调函数处理成功和错误情况，使得该函数可以在异步操作中方便地处理结果
     *
     */
    fun imageInformationExtraction(
        imageAssistantRequest: ImageAssistantRequest,
        onSuccess: (ImageAssistantResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        return RequestApi.imageInformationExtraction(imageAssistantRequest, onSuccess, onError)
    }

    /**
     * 问答
     *
     * 使用回调函数处理成功和错误情况，使得该函数可以在异步操作中方便地处理结果
     *
     */
    fun questionAnswer(
        promptRequest: PromptRequest,
        onSuccess: (PromptResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        return RequestApi.questionAnswer(promptRequest, onSuccess, onError)
    }

    /**
     * 问答
     *
     * 使用回调函数处理成功和错误情况，使得该函数可以在异步操作中方便地处理结果
     *
     */
    fun medicationReminder(
        medicineRequest: MedicineRequest,
        onSuccess: (MedicineResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        return RequestApi.medicationReminder(medicineRequest, onSuccess, onError)
    }

}