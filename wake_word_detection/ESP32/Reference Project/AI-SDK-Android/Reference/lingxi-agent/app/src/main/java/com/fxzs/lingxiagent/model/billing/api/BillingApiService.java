package com.fxzs.lingxiagent.model.billing.api;

import com.fxzs.lingxiagent.model.billing.model.BaseResponse;
import com.fxzs.lingxiagent.model.billing.model.CheckDeviceResponse;
import com.fxzs.lingxiagent.model.billing.model.DeviceResponse;
import com.fxzs.lingxiagent.model.billing.model.TokenResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.POST;

public interface BillingApiService {
    // 验证设备是否注册于计费平台
    @POST("app-api/lt/ai/bill/api/checkServiceDevice")
    Call<BaseResponse<CheckDeviceResponse>> checkServiceDevice();

    // 获取计费平台token
    @POST("app-api/lt/ai/bill/api/getToken")
    Call<BaseResponse<TokenResponse>> getToken();

    // 获取账号关联设备
    @POST("app-api/member/user/getDevices")
    Call<BaseResponse<List<DeviceResponse>>> getDevices();
}