package com.fxzs.lingxiagent.model.auth.repository;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fxzs.lingxiagent.lingxi.main.utils.GsonUtils;
import com.fxzs.lingxiagent.model.auth.api.AuthApiService;
import com.fxzs.lingxiagent.model.auth.dto.LoginRequest;
import com.fxzs.lingxiagent.model.auth.dto.LoginResponse;
import com.fxzs.lingxiagent.model.auth.dto.OneClickLoginRequest;
import com.fxzs.lingxiagent.model.auth.dto.RegisterRequest;
import com.fxzs.lingxiagent.model.auth.dto.SendSmsRequest;
import com.fxzs.lingxiagent.model.auth.dto.SmsLoginRequest;
import com.fxzs.lingxiagent.model.common.BaseResponse;
import com.fxzs.lingxiagent.model.network.RetrofitClient;
import com.fxzs.lingxiagent.model.network.SignVerifier;
import com.fxzs.lingxiagent.model.user.dto.ResetPasswordReqDto;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.HttpException;
import retrofit2.Response;
import timber.log.Timber;

/**
 * 认证仓库实现
 */
public class AuthRepositoryImpl implements AuthRepository {
    
    private static final String TAG = "AuthRepository";
    private final AuthApiService authApiService;
    
    public AuthRepositoryImpl() {
        this.authApiService = RetrofitClient.getInstance().getAuthApiService();
    }
    
    @Override
    public LiveData<BaseResponse<LoginResponse>> loginByPassword(String mobile, String password) {
        MutableLiveData<BaseResponse<LoginResponse>> result = new MutableLiveData<>();

        LoginRequest request = new LoginRequest(mobile, password);
        authApiService.loginByPassword(request).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                String raw = response.body().toString();
	            boolean isVerifySuccess = SignVerifier.verify(raw);
	            if (!isVerifySuccess) {
		            Timber.tag(TAG).e( "签名验证失败");
		            result.postValue(SignVerifier.buildErrorResponse());
		            return;
	            }
                BaseResponse<LoginResponse> baseResponse = GsonUtils.fromJson(raw, new TypeToken<BaseResponse<LoginResponse>>() {}.getType());
                // 保存登录信息
                saveLoginInfo(response, baseResponse);
                result.postValue(baseResponse);
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                BaseResponse<LoginResponse> baseResponse = new BaseResponse<>();
                baseResponse.setMessage(getErrorMsg(t));
                result.postValue(baseResponse);
                Timber.tag(TAG).e( "loginByPassword error"+ t);
            }
        });
        
        return result;
    }
    
    @Override
    public LiveData<LoginResponse> loginBySms(String mobile, String code) {
        return loginBySms(mobile, code, null);
    }
    
    @Override
    public LiveData<LoginResponse> loginBySms(String mobile, String code, String password) {
        MutableLiveData<LoginResponse> result = new MutableLiveData<>();
        
        Timber.tag(TAG).d( "=== 开始验证码登录 ===");
        Timber.tag(TAG).d( "手机号: " + mobile);
        Timber.tag(TAG).d( "验证码: " + code);
        Timber.tag(TAG).d( "密码: " + (password != null ? "已提供" : "未提供"));
        
        SmsLoginRequest request = new SmsLoginRequest(mobile, code, password);
        authApiService.loginBySms(request).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call,
                                 Response<JsonObject> response) {
                String raw = response.body().toString();
	            boolean isVerifySuccess = SignVerifier.verify(raw);
	            if (!isVerifySuccess) {
		            Timber.tag(TAG).e( "签名验证失败");
		            result.postValue(null);
		            return;
	            }
                
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<LoginResponse> baseResponse = GsonUtils.fromJson(raw, new TypeToken<BaseResponse<LoginResponse>>() {}.getType());
                    Timber.tag(TAG).d( "响应成功状态: " + baseResponse.isSuccess());
                    Timber.tag(TAG).d( "响应消息: " + baseResponse.getMessage());
                    Timber.tag(TAG).d( "响应码: " + baseResponse.getCode());

                    LoginResponse loginResponse = baseResponse.getData();
                    if (baseResponse.isSuccess() && null != loginResponse) {
                        // 打印获取到的Token信息
                        Timber.tag(TAG).d( "=== 登录成功，Token信息 ===");
                        Timber.tag(TAG).d( "AccessToken: " + loginResponse.getAccessToken());
                        Timber.tag(TAG).d( "RefreshToken: " + loginResponse.getRefreshToken());
                        Timber.tag(TAG).d( "ExpiresTime: " + loginResponse.getExpiresTime());
                        Timber.tag(TAG).d( "UserId: " + loginResponse.getUserId());
                        if (loginResponse.getUser() != null) {
                            Timber.tag(TAG).d( "用户昵称: " + loginResponse.getUser().getNickname());
                            Timber.tag(TAG).d( "用户手机: " + loginResponse.getUser().getMobile());
                        }
                        
                        // 保存登录信息
                        SharedPreferencesUtil.saveLoginInfo(loginResponse);
                        Timber.tag(TAG).d( "Token已保存到SharedPreferences");
                        
                        result.postValue(loginResponse);
                    } else {
                        if (loginResponse == null){
                            loginResponse = new LoginResponse();
                        }
                        if (TextUtils.isEmpty(baseResponse.getMessage())){
                            loginResponse.setMessage("登录失败，请检查账号或验证码是否正确");
                        }else {
                            loginResponse.setMessage(baseResponse.getMessage());
                        }
                        result.postValue(loginResponse);
                        Timber.tag(TAG).e( "SMS login failed: " + baseResponse.getMessage());
                    }
                } else {
                    result.postValue(null);
                    Timber.tag(TAG).e( "SMS login request failed: " + response.code());
                    if (response.errorBody() != null) {
                        try {
                            Timber.tag(TAG).e( "错误响应: " + response.errorBody().string());
                        } catch (Exception e) {
                            Timber.tag(TAG).e( "读取错误响应失败"+ e);
                        }
                    }
                }
            }
            
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                result.postValue(null);
                Timber.tag(TAG).e( "SMS login error"+ t);
                Timber.tag(TAG).e( "错误详情: " + t.getMessage());
            }
        });
        
        return result;
    }
    
    @Override
    public LiveData<BaseResponse<Boolean>> sendSmsCode(String mobile, int scene) {
        MutableLiveData<BaseResponse<Boolean>> result = new MutableLiveData<>();
        
        Timber.tag(TAG).d( "=== 发送验证码请求 ===");
        Timber.tag(TAG).d( "手机号: %s", mobile);
        Timber.tag(TAG).d( "场景: " + scene + " (1=验证码登录, 2=修改手机号, 4=重置密码, 5=注销账号)");
        
        SendSmsRequest request = new SendSmsRequest(mobile, scene);
        authApiService.sendSmsCode(request).enqueue(new Callback<BaseResponse<Boolean>>() {
            @Override
            public void onResponse(Call<BaseResponse<Boolean>> call, 
                                 Response<BaseResponse<Boolean>> response) {
                Timber.tag(TAG).d( "=== 发送验证码响应 ===");
                Timber.tag(TAG).d( "响应码: %s", response.code());
                BaseResponse<Boolean> baseResponse = response.body();
                Timber.tag(TAG).d( "响应状态码: %s", baseResponse.getCode());
                    Timber.tag(TAG).d( "响应成功状态: %s", baseResponse.isSuccess());
                    Timber.tag(TAG).d( "响应消息: %s", baseResponse.getMessage());
                    Timber.tag(TAG).d( "响应数据: %s", baseResponse.getData());
                if (response.errorBody() != null) {
                    try {
                        Timber.tag(TAG).e( "错误响应: %s", response.errorBody().string());
                    } catch (Exception e) {
                        Timber.tag(TAG).e( "读取错误响应失败%s", e);
                    }
                }
                result.postValue(baseResponse);
            }
            
            @Override
            public void onFailure(Call<BaseResponse<Boolean>> call, Throwable t) {
                BaseResponse<Boolean> baseResponse = new BaseResponse<>();
                baseResponse.setMessage(getErrorMsg(t));
                result.postValue(baseResponse);
                Timber.tag(TAG).e( "Send SMS error:%s", t.getMessage());
            }
        });
        
        return result;
    }
    
    @Override
    public LiveData<BaseResponse<LoginResponse>> register(String mobile, String code, String password) {
        MutableLiveData<BaseResponse<LoginResponse>> result = new MutableLiveData<>();

        RegisterRequest request = new RegisterRequest(mobile, code, password);
        authApiService.register(request).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call,
                                 Response<JsonObject> response) {

                String raw = response.body().toString();
	            boolean isVerifySuccess = SignVerifier.verify(raw);
	            if (!isVerifySuccess) {
		            Timber.tag(TAG).e( "签名验证失败");
		            BaseResponse<LoginResponse> baseResponse = new BaseResponse<>();
		            baseResponse.setMessage("签名校验失败");
		            result.postValue(baseResponse);
		            return;
	            }

                BaseResponse<LoginResponse> baseResponse = GsonUtils.fromJson(raw, new TypeToken<BaseResponse<LoginResponse>>() {}.getType());
                if (response.isSuccessful() && baseResponse != null) {
                    LoginResponse loginResponse = baseResponse.getData();
                    // 保存登录信息
                    SharedPreferencesUtil.saveLoginInfo(loginResponse);
                }
                result.postValue(baseResponse);
            }
            
            @Override
            public void onFailure(Call<JsonObject> call, @NonNull Throwable t) {
                BaseResponse<LoginResponse> baseResponse = new BaseResponse<>();
                baseResponse.setMessage(getErrorMsg(t));
                result.postValue(baseResponse);
                Timber.tag(TAG).e( "Register error: "+ t);
            }
        });
        
        return result;
    }
    
    @Override
    public LiveData<LoginResponse> refreshToken(String refreshToken) {
        MutableLiveData<LoginResponse> result = new MutableLiveData<>();

        authApiService.refreshToken(refreshToken).enqueue(new Callback<BaseResponse<LoginResponse>>() {
            @Override
            public void onResponse(Call<BaseResponse<LoginResponse>> call, 
                                 Response<BaseResponse<LoginResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<LoginResponse> baseResponse = response.body();
                    if (baseResponse.isSuccess()) {
                        LoginResponse loginResponse = baseResponse.getData();
                        SharedPreferencesUtil.saveLoginInfo(loginResponse);
                        result.postValue(loginResponse);
                    } else {
                        // 业务异常才清本地缓存数据 (400：无效的刷新令牌)
                        SharedPreferencesUtil.clearLoginInfo();
                        result.postValue(null);
                        Timber.tag(TAG).e( "Refresh token failed: %s", baseResponse.getMsg());
                    }
                } else {
                    result.postValue(null);
                    Timber.tag(TAG).e( "Refresh token request failed: %s", response.code());
                }
            }
            
            @Override
            public void onFailure(Call<BaseResponse<LoginResponse>> call, Throwable t) {
                Timber.tag(TAG).e( "Refresh token error%s", t);
                result.postValue(null);
            }
        });
        
        return result;
    }
    
    @Override
    public LiveData<Boolean> logout() {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        
        authApiService.logout().enqueue(new Callback<BaseResponse<Boolean>>() {
            @Override
            public void onResponse(Call<BaseResponse<Boolean>> call,
                                 Response<BaseResponse<Boolean>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    boolean success = response.body().getData();
                    if (success) {
                        // 清除本地登录信息
                        SharedPreferencesUtil.clearLoginInfo();
                    }
                    result.postValue(success);
                } else {
                    result.postValue(false);
                }
            }
            
            @Override
            public void onFailure(Call<BaseResponse<Boolean>> call, Throwable t) {
                // 即使请求失败也清除本地信息
                SharedPreferencesUtil.clearLoginInfo();
                result.postValue(false);
                Timber.tag(TAG).e( "Logout error%s", t);
            }
        });
        
        return result;
    }

    @Override
    public LiveData<BaseResponse<LoginResponse>> resetPassword(String mobile, String code, String password) {
        MutableLiveData<BaseResponse<LoginResponse>> result = new MutableLiveData<>();
        ResetPasswordReqDto req = new ResetPasswordReqDto(mobile, code, password);
        authApiService.resetPassword(req).enqueue(new Callback<BaseResponse<LoginResponse>>() {
            @Override
            public void onResponse(Call<BaseResponse<LoginResponse>> call,
                                   Response<BaseResponse<LoginResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.postValue(response.body());
                } else {
                    handleFailure(response.message());
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<LoginResponse>> call, Throwable t) {
                Timber.tag(TAG).e( "重置密码失败:" + t.getMessage());
                handleFailure(null);
            }

            private void handleFailure(String errorMsg) {
                BaseResponse<LoginResponse> error = new BaseResponse<>();
                error.setCode(-1);
                error.setMessage(TextUtils.isEmpty(errorMsg) ? "重置密码失败，请稍后重试": errorMsg);
                result.postValue(error);
            }
        });
        return result;
    }
    
    @Override
    public LiveData<LoginResponse> oneClickLogin(String loginToken) {
        MutableLiveData<LoginResponse> result = new MutableLiveData<>();
        
        Timber.tag(TAG).d( "=== 开始一键登录 ===");
        Timber.tag(TAG).d( "LoginToken: " + loginToken);
        
        OneClickLoginRequest request = new OneClickLoginRequest(loginToken);
        authApiService.oneClickLogin(request).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call,
                                 Response<JsonObject> response) {
                Timber.tag(TAG).d( "=== 一键登录响应 ===");
                Timber.tag(TAG).d( "响应码: " + response.code());
                String raw = response.body().toString();
	            boolean isVerifySuccess = SignVerifier.verify(raw);
	            if (!isVerifySuccess) {
		            Timber.tag(TAG).e( "签名验证失败");
		            LoginResponse errorResponse = new LoginResponse();
		            errorResponse.setMessage("签名校验失败");
		            result.postValue(errorResponse);
		            return;
	            }
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<LoginResponse> baseResponse = GsonUtils.fromJson(raw, new TypeToken<BaseResponse<LoginResponse>>() {}.getType());
                    Timber.tag(TAG).d( "响应成功状态: " + baseResponse.isSuccess());
                    Timber.tag(TAG).d( "响应消息: " + baseResponse.getMessage());
                    
                    if (baseResponse.isSuccess() && baseResponse.getData() != null) {
                        LoginResponse loginResponse = baseResponse.getData();

                        // 打印获取到的Token信息
                        Timber.tag(TAG).d( "=== 一键登录成功 ===");
                        Timber.tag(TAG).d( "AccessToken: " + loginResponse.getAccessToken());
                        Timber.tag(TAG).d( "RefreshToken: " + loginResponse.getRefreshToken());
                        Timber.tag(TAG).d( "ExpiresTime: " + loginResponse.getExpiresTime());
                        Timber.tag(TAG).d( "UserId: " + loginResponse.getUserId());
                        
                        // 保存登录信息
                        SharedPreferencesUtil.saveLoginInfo(loginResponse);
                        result.postValue(loginResponse);
                    } else {
                        LoginResponse errorResponse = new LoginResponse();
                        errorResponse.setMessage(baseResponse.getMessage());
                        errorResponse.setCode(baseResponse.getCode());
                        result.postValue(errorResponse);
                        Timber.tag(TAG).e( "One click login failed: " + baseResponse.getMessage());
                    }
                } else {
                    LoginResponse errorResponse = new LoginResponse();
                    errorResponse.setMessage("一键登录失败");
                    result.postValue(errorResponse);
                    Timber.tag(TAG).e( "One click login request failed: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                LoginResponse errorResponse = new LoginResponse();
                errorResponse.setMessage("网络错误");
                result.postValue(errorResponse);
                Timber.tag(TAG).e( "One click login error"+ t);
            }
        });
        
        return result;
    }

    @NonNull
    private String getErrorMsg(Throwable throwable) {
        String errorMsg;
        if (throwable instanceof IOException) {
            errorMsg = "网络连接失败，请检查网络设置";
        } else if (throwable instanceof HttpException) {
            errorMsg = "HTTP错误:" + throwable.getMessage();
        } else {
            errorMsg = "请求失败，请稍后重试";
        }
        return errorMsg;
    }

    // 保存登录信息
    private void saveLoginInfo(Response<JsonObject> response,
                               BaseResponse<LoginResponse> baseResponse) {
        if (response.isSuccessful() && baseResponse != null) {
            LoginResponse loginResponse = baseResponse.getData();
            SharedPreferencesUtil.saveLoginInfo(loginResponse);
        }
    }
}