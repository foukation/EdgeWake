package com.fxzs.lingxiagent.model.intention.api;

import com.fxzs.lingxiagent.model.intention.dto.ClientActionsMulRequest;
import com.fxzs.lingxiagent.model.intention.dto.ClientApiActionsResMul;
import com.fxzs.lingxiagent.model.intention.dto.ClientApiAppListRes;
import com.fxzs.lingxiagent.model.intention.dto.ClientApiGetTokenRes;
import com.fxzs.lingxiagent.model.intention.dto.ClientTimeRes;
import com.fxzs.lingxiagent.model.intention.dto.ImageAssistantRequest;
import com.fxzs.lingxiagent.model.intention.dto.ImageAssistantResponse;
import com.fxzs.lingxiagent.model.intention.dto.TripContentRes;
import com.fxzs.lingxiagent.model.intention.dto.TripCreateRequest;
import com.fxzs.lingxiagent.model.intention.dto.TripCreateRes;
import com.fxzs.lingxiagent.model.intention.dto.TripDelRes;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.HTTP;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * 意图识别相关API接口
 * baseURL:http://36.213.71.163:11453
 */
public interface IntentionApiService {
    @GET("/api/v1/web/token")
    Call<ClientApiGetTokenRes> getClientToken(@Query("clientId") String clientId);

    @GET("/api/v1/web/appList")
    Call<ClientApiAppListRes> getAppList(@Query("pageIndex") int pageIndex,
                                         @Query("pageSize") int pageSize,
                                         @Query("status") int status);

    @POST("/api/v1/web/multGenerateAction")
    Call<ClientApiActionsResMul> getClientActionsMul(@Body ClientActionsMulRequest request);

    @GET("/api/v1/web/normalize-time")
    Call<ClientTimeRes> normalizeTime(@Query("timeDescription") String timeDescription);

    @POST("/api/v1/task")
    Call<TripCreateRes> createTrip(@Body TripCreateRequest request);

    @POST("/api/v1/task")
    Call<TripContentRes> getTripList(@Query("pageNum") int pageNum,
                                     @Query("pageSize") int pageSize);

    @HTTP(method = "DELETE", path = "/api/v1/task", hasBody = true)
    Call<TripDelRes> deleteTrip(@Body String delId);

    @POST("/api/v1/proxy/multimodal")
    Call<ImageAssistantResponse> imageInformationExtraction(@Body ImageAssistantRequest request);
}