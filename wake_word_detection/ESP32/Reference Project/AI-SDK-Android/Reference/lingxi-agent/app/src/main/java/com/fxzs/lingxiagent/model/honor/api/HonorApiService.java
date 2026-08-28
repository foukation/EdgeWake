package com.fxzs.lingxiagent.model.honor.api;

import java.util.Map;

import io.reactivex.Observable;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.http.Body;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;
import retrofit2.http.Streaming;


public interface HonorApiService {

    @POST("honor-agent/v1/medical-advice")
    @Streaming
    Observable<ResponseBody> sendStreamMeetRequest(
            @HeaderMap Map<String, String> headers,
            @Body RequestBody body
    );

    @POST("honor-agent/v2/travel-planning")
    @Streaming
    Observable<ResponseBody> sendStreamTripRequest(
            @HeaderMap Map<String, String> headers,
            @Body RequestBody body
    );
}