package com.fxzs.lingxiagent.network.ZNet;

import com.fxzs.lingxiagent.lingxi.translate.util.LanguageListDto;
import com.fxzs.lingxiagent.model.chat.api.getStsConfig;
import com.fxzs.lingxiagent.model.chat.dto.OptionModel;
import com.fxzs.lingxiagent.model.history.AllHistoryDto;
import com.fxzs.lingxiagent.network.ZNet.bean.ChatContentRequest;
import com.fxzs.lingxiagent.network.ZNet.bean.GUIWidgetBean;
import com.fxzs.lingxiagent.network.ZNet.bean.GetImageCat;
import com.fxzs.lingxiagent.network.ZNet.bean.GetMenuBean;
import com.fxzs.lingxiagent.network.ZNet.bean.SampleList;
import com.fxzs.lingxiagent.network.ZNet.bean.SmsLoginBean;
import com.fxzs.lingxiagent.network.ZNet.bean.StyleListBean;
import com.fxzs.lingxiagent.network.ZNet.bean.getCatDetailListBean;

import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

public interface HttpApi {
    @POST("app-api/member/auth/send-sms-code")
    Observable<ApiResponse<Boolean>> sendSmsCode(@Body Map<String,Object> map);
    @POST("app-api/member/auth/sms-login")
    Observable<ApiResponse<SmsLoginBean>> smsLogin(@Body Map<String,Object> map);
    @POST("app-api/lt/ai/image/getImageCatList")
    Observable<ApiResponse<List<GetImageCat>>> getImageCatList(@Body Map<String,Object> map);
    @POST("app-api/lt/ai/menu/getList")
    Observable<ApiResponse<List<GetMenuBean>>> getMenuList(@Body Map<String,Object> map);
    @POST("app-api/lt/ai/skill/getCategoryList")
    Observable<ApiResponse<List<GUIWidgetBean>>> getCategoryList(@Body Map<String,Object> map);
    @POST("app-api/lt/ai/image/volc/styleList")
    Observable<ApiResponse<List<StyleListBean>>> styleList(@Body Map<String,Object> map);
    @POST("app-api/lt/ai/image/volc/sampleList")
    Observable<ApiResponse<SampleList>> sampleList(@Body Map<String,Object> map);
    @POST("app-api/lt/ai/menu/getCatDetailList")
    Observable<ApiResponse<List<getCatDetailListBean>>> getCatDetailList(@Body Map<String,Object> map);
    @GET("app-api/lt/ai/chat/model/getModelTypeList")
    Observable<ApiResponse<List<OptionModel>>> getModelTypeList(@QueryMap Map<String,Object> map);
    @POST("app-api/lt/ai/chat/conversation/create-my")
    Observable<ApiResponse<Integer>> createMy(@Body Map<String,Object> map);

    @POST("app-api/lt/ai/chat/conversation/update-my")
    Observable<ApiResponse<Boolean>> updateMy(@Body Map<String,Object> map);
//    @POST("app-api/lt/ai/chat/message/send-stream")
//    Observable<String> sendStream(@Body Map<String,Object> map);

    @PUT("app-api/lt/ai/chat/conversation/update-my")
    Observable<ApiResponse<Boolean>> updateMyPUT(@Body Map<String,Object> map);
    @POST("app-api/lt/ai/file/getStsConfig")
    Observable<ApiResponse<getStsConfig>> getStsConfig(@Body Map<String,Object> map);
    @POST("app-api/lt/ai/menu/getDetailByModel")
    Observable<ApiResponse<getCatDetailListBean>> getDetailByModel(@Body Map<String,Object> map);
    @POST("h5/generate-html/chat_history")
    Observable<ResponseBody> sendChatLink(@Body ChatContentRequest request);

    // 历史记录 - 全部
    @POST("app-api/member/history/page")
    Observable<ApiResponse<AllHistoryDto>> getAllHistory(@Body java.util.Map<String,Object> map);

    @GET("app-api/lt/ai/translation/languages")
    Observable<ApiResponse<LanguageListDto>> getLanguages( );


}
