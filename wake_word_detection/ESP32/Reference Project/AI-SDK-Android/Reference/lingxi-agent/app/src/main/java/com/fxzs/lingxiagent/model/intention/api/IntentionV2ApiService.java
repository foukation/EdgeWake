package com.fxzs.lingxiagent.model.intention.api;

import com.fxzs.lingxiagent.model.intention.dto.LLmQueryParams;
import com.fxzs.lingxiagent.model.intention.dto.LlmQueryResult;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * 意图识别相关API接口
 * baseURL:http://36.213.71.163:11470
 */
public interface IntentionV2ApiService {

    @POST("/predict")
    Call<LlmQueryResult> handleLlmAction(@Body LLmQueryParams params);
}