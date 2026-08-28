package com.fxzs.lingxiagent.model.deepresearch.api;

import java.util.Map;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.Query;
import retrofit2.http.Streaming;

public interface DeepResearchApiService {


    @GET("deepresearch/query")
    @Streaming
    Observable<ResponseBody> sendStreamDeepResearchRequest(
            @HeaderMap Map<String, String> headers,
            @Query("req_id") String req_id,
            @Query("query") String query,
            @Query("device_id") String device_id,
            @Query("type_app") String type_app,
            @Query("phone_number") String phone_number
    );

}
