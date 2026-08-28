package com.fxzs.lingxiagent.view.common;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * ViewModel基类
 * 提供通用的状态管理功能
 */
public class BaseViewModel extends ViewModel {
    
    // 加载状态
    protected final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    
    // 错误信息
    protected final MutableLiveData<String> error = new MutableLiveData<>();
    
    // 成功消息
    protected final MutableLiveData<String> message = new MutableLiveData<>();
    
    // Getters
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<String> getError() {
        return error;
    }
    
    public LiveData<String> getMessage() {
        return message;
    }
    
    // Protected setters for subclasses
    public void setLoading(boolean loading) {
        isLoading.setValue(loading);
    }
    
    protected void setError(String errorMessage) {
        error.setValue(errorMessage);
    }
    
    protected void setMessage(String successMessage) {
        message.setValue(successMessage);
    }
    
    protected void clearError() {
        error.setValue(null);
    }
    
    protected void clearMessage() {
        message.setValue(null);
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        // 清理资源
    }
}
