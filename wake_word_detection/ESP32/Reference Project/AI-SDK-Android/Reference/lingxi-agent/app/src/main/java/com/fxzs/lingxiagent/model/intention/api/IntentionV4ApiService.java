package com.fxzs.lingxiagent.model.intention.api;

import com.fxzs.lingxiagent.model.intention.dto.ClientActionsRequest;
import com.fxzs.lingxiagent.model.intention.dto.ClientApiActionsRes;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * 意图识别相关API接口
 * baseURL:http://36.213.71.163:11508
 */
public interface IntentionV4ApiService {

    @POST("/api/v1/web/generateAction")
    Call<ClientApiActionsRes> getClientActions(@Body ClientActionsRequest request);
}