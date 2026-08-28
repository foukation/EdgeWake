package com.fxzs.lingxiagent.model.intention.api;

import com.fxzs.lingxiagent.model.intention.dto.IsOcrResult;
import com.fxzs.lingxiagent.model.intention.dto.OcrResult;
import com.fxzs.lingxiagent.model.intention.dto.QueryParams;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * OCR相关API接口
 * baseURL:http://36.213.71.200:5692
 */
public interface IntentionV5ApiService {

    @POST("/multimodal")
    Call<OcrResult> handleOcr(@Body QueryParams params);

    @POST("/multimodal")
    Call<IsOcrResult> isAdditionOcr(@Body QueryParams params);
}