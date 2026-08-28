package com.fxzs.lingxiagent.model.auth.api;

import com.fxzs.lingxiagent.model.auth.dto.LoginRequest;
import com.fxzs.lingxiagent.model.auth.dto.LoginResponse;
import com.fxzs.lingxiagent.model.auth.dto.OneClickLoginRequest;
import com.fxzs.lingxiagent.model.auth.dto.RegisterRequest;
import com.fxzs.lingxiagent.model.auth.dto.SendSmsRequest;
import com.fxzs.lingxiagent.model.auth.dto.SmsLoginRequest;
import com.fxzs.lingxiagent.model.common.BaseResponse;
import com.fxzs.lingxiagent.model.user.dto.ResetPasswordReqDto;
import com.google.gson.JsonObject;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * 认证相关API接口
 */
public interface AuthApiService {
    
    /**
     * 使用手机号 + 密码登录
     */
    @POST("app-api/member/auth/login")
    Call<JsonObject> loginByPassword(@Body LoginRequest request);
    
    /**
     * 使用手机号 + 验证码登录
     */
    @POST("app-api/member/auth/sms-login")
    Call<JsonObject> loginBySms(@Body SmsLoginRequest request);
    
    /**
     * 发送手机验证码
     */
    @POST("app-api/member/auth/send-sms-code")
    Call<BaseResponse<Boolean>> sendSmsCode(@Body SendSmsRequest request);
    
    /**
     * 校验手机验证码
     */
    @POST("app-api/member/auth/validate-sms-code")
    Call<BaseResponse<Boolean>> validateSmsCode(
            @Query("mobile") String mobile,
            @Query("code") String code,
            @Query("scene") Integer scene
    );
    
    /**
     * 注册新用户
     * 注意：某些后端实现可能没有独立的注册接口，而是通过短信验证码登录自动注册
     */
    @POST("app-api/member/auth/register")
    Call<JsonObject> register(@Body RegisterRequest request);
    
    /**
     * 刷新令牌
     */
    @POST("app-api/member/auth/refresh-token")
    Call<BaseResponse<LoginResponse>> refreshToken(@Query("refreshToken") String refreshToken);
    
    /**
     * 退出登录
     */
    @POST("app-api/member/auth/logout")
    Call<BaseResponse<Boolean>> logout();

    /**
     * 重置密码
     */
    @POST("app-api/member/auth/reset-password")
    Call<BaseResponse<LoginResponse>> resetPassword(@Body ResetPasswordReqDto request);
    
    /**
     * 一键登录
     */
    @POST("app-api/member/auth/login-by-cmi")
    Call<JsonObject> oneClickLogin(@Body OneClickLoginRequest request);

    /**
     * 修改密码
     */
    @POST("app-api/member/auth/update-password")
    Call<BaseResponse<LoginResponse>> updatePassword(@Body Map<String, Object> params);
}