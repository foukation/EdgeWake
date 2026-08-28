package com.fxzs.lingxiagent.model.billing.api;

import com.fxzs.lingxiagent.model.billing.model.BaseResponse;
import com.fxzs.lingxiagent.model.billing.model.SyncDeviceRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ThirdPartyApiService {

    // 往计费平台同步设备
    @POST("/app-api/billing/v1/sync-devices")
    Call<BaseResponse<Object>> syncDevices(
            @Body List<SyncDeviceRequest> request
    );

    // 获取权益包信息
    @GET("/app-api/billing/v1/service-packages")
    Call<BaseResponse<Object>> getServicePackages();
}