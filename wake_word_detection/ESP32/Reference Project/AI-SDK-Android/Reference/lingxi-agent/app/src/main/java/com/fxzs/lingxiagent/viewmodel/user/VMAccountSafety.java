package com.fxzs.lingxiagent.viewmodel.user;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.fxzs.lingxiagent.model.common.BaseViewModel;
import com.fxzs.lingxiagent.model.common.ObservableField;
import com.fxzs.lingxiagent.model.user.UserUtil;
import com.fxzs.lingxiagent.model.user.repository.UserRepository;
import com.fxzs.lingxiagent.model.user.repository.UserRepositoryImpl;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;
import com.fxzs.lingxiagent.view.auth.RegisterActivity;

public class VMAccountSafety extends BaseViewModel {
    
    // 双向绑定字段
    private final ObservableField<String> mobile = new ObservableField<>("");
    
    // 业务状态
    private final MutableLiveData<Boolean> logoutResult = new MutableLiveData<>();
    
    // Repository
    private final UserRepository userRepository;
    
    public VMAccountSafety(@NonNull Application application) {
        super(application);
        userRepository = new UserRepositoryImpl();
    }
    
    // Getters
    public ObservableField<String> getMobile() {
        return mobile;
    }
    
    public MutableLiveData<Boolean> getLogoutResult() {
        return logoutResult;
    }
    
    // 业务方法
    public void loadUserInfo() {
        setLoading(true);
        String phone = SharedPreferencesUtil.getUserPhone();
        mobile.postValue(UserUtil.formatPhone(phone));
        setLoading(false);
    }
    
    public void performLogout() {
        setLoading(true);
        
//        userRepository.deleteAccount(new UserRepository.Callback<Boolean>() {
//            @Override
//            public void onSuccess(Boolean result) {
//                setLoading(false);
//                logoutResult.postValue(true);
//            }
//
//            @Override
//            public void onError(String error) {
//                setError("注销失败: " + error);
//                logoutResult.postValue(false);
//            }
//        });
    }
    
    public void clearUserDataAndLogout() {
        // 清除用户登录信息
        SharedPreferencesUtil.clearLoginInfo();
        
        // 同时清除测试账号信息
        Context context = getApplication();
        if (context != null) {
            // 跳转到登录页面
            Intent intent = new Intent(context, RegisterActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);
        }
    }
    
    private String formatPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
    }
}