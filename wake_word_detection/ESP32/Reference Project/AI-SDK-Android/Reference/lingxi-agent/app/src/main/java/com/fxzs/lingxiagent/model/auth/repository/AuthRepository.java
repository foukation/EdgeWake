package com.fxzs.lingxiagent.model.auth.repository;

import androidx.lifecycle.LiveData;

import com.fxzs.lingxiagent.model.auth.dto.LoginResponse;
import com.fxzs.lingxiagent.model.common.BaseResponse;

/**
 * 认证仓库接口
 */
public interface AuthRepository {
    
    /**
     * 手机号+密码登录
     */
    LiveData<BaseResponse<LoginResponse>> loginByPassword(String mobile, String password);
    
    /**
     * 手机号验证码登录
     */
    LiveData<LoginResponse> loginBySms(String mobile, String code);
    
    /**
     * 手机号验证码登录（带密码）
     */
    LiveData<LoginResponse> loginBySms(String mobile, String code, String password);
    
    /**
     * 发送验证码
     */
    LiveData<BaseResponse<Boolean>> sendSmsCode(String mobile, int scene);
    
    /**
     * 注册
     */
    LiveData<BaseResponse<LoginResponse>> register(String mobile, String code, String password);
    
    /**
     * 刷新Token
     */
    LiveData<LoginResponse> refreshToken(String refreshToken);
    
    /**
     * 退出登录
     */
    LiveData<Boolean> logout();

    /**
     * 重置密码
     */
    LiveData<BaseResponse<LoginResponse>> resetPassword(String mobile, String code, String password);
    
    /**
     * 一键登录
     */
    LiveData<LoginResponse> oneClickLogin(String loginToken);
}