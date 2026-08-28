package com.fxzs.lingxiagent.model.scene.api;

import java.util.Map;

import io.reactivex.Observable;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.http.Body;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;
import retrofit2.http.Streaming;


public interface SceneApiService {

    @POST("lingxi-proxy/proxy/lingxi-chat")
    @Streaming
    Observable<ResponseBody> sendStreamRequest(
            @HeaderMap Map<String, String> headers,
            @Body RequestBody body
    );
}