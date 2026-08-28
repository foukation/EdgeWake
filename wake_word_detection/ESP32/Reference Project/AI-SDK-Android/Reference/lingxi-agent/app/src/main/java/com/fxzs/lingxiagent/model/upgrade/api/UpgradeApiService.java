package com.fxzs.lingxiagent.model.upgrade.api;

import com.fxzs.lingxiagent.model.common.BaseResponse;
import com.fxzs.lingxiagent.model.user.dto.AppVersionResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface UpgradeApiService {
    // 检查版本更新
    @POST("open-api/ota/v2/app-check-upgrade")
    Call<BaseResponse<AppVersionResponse>> checkAppUpgrade(@Body Map<String, String> params);
}
