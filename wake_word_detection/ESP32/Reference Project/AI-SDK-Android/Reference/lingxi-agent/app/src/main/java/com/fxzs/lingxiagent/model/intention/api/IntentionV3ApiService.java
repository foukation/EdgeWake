package com.fxzs.lingxiagent.model.intention.api;

import com.fxzs.lingxiagent.model.intention.dto.MedicineRequest;
import com.fxzs.lingxiagent.model.intention.dto.PromptRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * 问答相关API接口
 * baseURL:http://36.213.71.163:11507
 */
public interface IntentionV3ApiService {

    @POST("/completions")
    Call<PromptRequest.PromptResponse> questionAnswer(@Body PromptRequest request);

    @POST("/med-reminders")
    Call<MedicineRequest.MedicineResponse> medicationReminder(@Body MedicineRequest request);

}