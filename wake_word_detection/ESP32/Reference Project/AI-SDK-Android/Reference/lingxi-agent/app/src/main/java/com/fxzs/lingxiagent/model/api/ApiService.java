package com.fxzs.lingxiagent.model.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ApiService {
    @GET("api/data")
    Call<ApiResponse> getData(@Query("id") String id);
}