package com.fxzs.lingxiagent.model.preview;

import com.google.gson.JsonObject;

import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface PreviewApiService {

    @POST("/console/getFileId")
    Observable<JsonObject> getFileId(@Body JsonObject body);
}
