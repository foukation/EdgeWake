package com.fxzs.lingxiagent.network.ZNet;


import java.util.Map;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Streaming;

public interface SseApi {


    @POST("app-api/lt/ai/chat/message/send-stream")
    @Streaming
    Observable<ResponseBody> sendStream(@Body Map<String,Object> map);
//    Observable<ApiResponse<SSEBean>> sendStream(@Body Map<String,Object> map);

    @POST("app-api/lt/ai/file/analyse")
    @Streaming
    Observable<ResponseBody> analyse(@Body Map<String,Object> map);

    @POST("app-api/lt/ai/ppt/generatePptOutline/v2")
    @Streaming
    Observable<ResponseBody> generatePptOutlineStream(@Body Map<String,Object> map);
}
