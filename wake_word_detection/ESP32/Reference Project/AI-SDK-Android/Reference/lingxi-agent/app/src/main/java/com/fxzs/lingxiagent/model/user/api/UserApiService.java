package com.fxzs.lingxiagent.model.user.api;

import com.fxzs.lingxiagent.model.common.BaseResponse;
import com.fxzs.lingxiagent.model.user.dto.FeedbackDto;
import com.fxzs.lingxiagent.model.user.dto.UserDto;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface UserApiService {
    // 获取用户信息
    @GET("app-api/member/user/get")
    Call<BaseResponse<UserDto>> getUserProfile();
    
    // 更新用户信息
    @POST("app-api/member/user/update-base-info")
    Call<BaseResponse<Boolean>> updateUserProfile(@Body Map<String, Object> params);

    // 更新用户头像
    @Multipart
    @PUT("app-api/member/user/update-avatar")
    Call<BaseResponse<String>> updateAvatar(@Part MultipartBody.Part file);

    // 修改手机号
    @POST("app-api/member/user/update-mobile")
    Call<BaseResponse<Boolean>> updateMobile(@Body Map<String, Object> params);

    // 上传文件
    @Multipart
    @POST("app-api/member/user/file/upload")
    Call<BaseResponse<String>> uploadFile(@Part MultipartBody.Part file);
    
    // 提交反馈
    @POST("app-api/ai/feedback/create")
    Call<BaseResponse<Long>> submitFeedback(@Body Map<String, Object> params);
    
    // 获取反馈列表
    @GET("app-api/ai/feedback/list")
    Call<BaseResponse<List<FeedbackDto>>> getFeedbackList(@Query("pageNo") Integer pageNo,
                                                          @Query("pageSize") Integer pageSize);

    // 获取反馈详情
    @GET("app-api/ai/feedback/get")
    Call<BaseResponse<FeedbackDto>> getFeedbackDetail(@Query("id") Long id);

    // 注销账号
    @POST("app-api/member/user/clean-up")
    Call<BaseResponse<Boolean>> deleteAccount(@Body Map<String, Object> params);
}