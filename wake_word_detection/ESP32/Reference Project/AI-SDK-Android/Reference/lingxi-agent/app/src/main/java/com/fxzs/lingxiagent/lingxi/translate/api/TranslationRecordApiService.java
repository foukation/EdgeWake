package com.fxzs.lingxiagent.lingxi.translate.api;

import com.fxzs.lingxiagent.lingxi.translate.model.TranslateDetailBean;
import com.fxzs.lingxiagent.network.ZNet.ApiResponse;
import com.fxzs.lingxiagent.network.ZNet.bean.TranslationRecord;

import java.util.Map;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Translation record API for dialog/listen mode records
 */
public interface TranslationRecordApiService {

    @POST("app-api/lt/ai/translation/add")
    Call<ResponseBody> addTranslationRecord(@Body Map<String, Object> body);

    @POST("app-api/lt/ai/translation/batchSaveTranslationMessage")
    Call<ResponseBody> batchSaveTranslationMessage(@Body java.util.List<java.util.Map<String, Object>> list);


    @POST("app-api/lt/ai/translation/list")
    Observable<ApiResponse<TranslationRecord>> getTranslationRecordList(@Body Map<String, Object> body);
    @POST("app-api/lt/ai/translation/deleteById")
    Observable<ApiResponse<String>> deleteById(@Body Map<String, Object> body);
    @POST("app-api/lt/ai/translation/updateById")
    Observable<ApiResponse<String>> updateById(@Body Map<String, Object> body);
    @POST("app-api/lt/ai/translation/getDetailById")
    Observable<ApiResponse<TranslateDetailBean>> getDetailById(@Body Map<String, Object> body);

    /**
     * 导出同传记录为Word（后端生成并返回docx直链）
     * @param id 翻译记录ID
     * 返回结构示例：{"code":0, "data":"https://.../xxxx.docx", "msg":""}
     */
    @GET("app-api/lt/ai/translation/export/word/url/{id}")
    Call<ResponseBody> exportWordById(@Path("id") Long id);
}
