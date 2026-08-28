package com.fxzs.lingxiagent.model.common;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

/**
 * 基础ViewModel，提供双向绑定支持
 */
public abstract class BaseViewModel extends AndroidViewModel {
    
    public BaseViewModel(@NonNull Application application) {
        super(application);
    }
    
    /**
     * 加载状态
     */
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    
    /**
     * 错误信息
     */
    private final MutableLiveData<String> error = new MutableLiveData<>();
    
    /**
     * 成功消息
     */
    private final MutableLiveData<String> success = new MutableLiveData<>();
    
    /**
     * 获取加载状态
     * @return 加载状态LiveData
     */
    public MutableLiveData<Boolean> getLoading() {
        return loading;
    }
    
    /**
     * 获取错误信息
     * @return 错误信息LiveData
     */
    public MutableLiveData<String> getError() {
        return error;
    }
    
    /**
     * 获取成功消息
     * @return 成功消息LiveData
     */
    public MutableLiveData<String> getSuccess() {
        return success;
    }
    
    /**
     * 设置加载状态
     * @param isLoading 是否加载中
     */
    public void setLoading(boolean isLoading) {
        loading.postValue(isLoading);
    }
    
    /**
     * 设置错误信息
     * @param errorMessage 错误信息
     */
    public void setError(String errorMessage) {
        error.postValue(errorMessage);
        setLoading(false);
    }

    /**
     * 设置权益服务错误信息
     * @param errorMessage 错误信息
     */
    public void setBillError(String errorMessage) {
        error.postValue(errorMessage);
        setLoading(false);
    }
    
    /**
     * 设置成功消息
     * @param successMessage 成功消息
     */
    public void setSuccess(String successMessage) {
        success.postValue(successMessage);
        setLoading(false);
    }
    
    /**
     * 清除错误信息
     */
    protected void clearError() {
        error.postValue(null);
    }
    
    /**
     * 清除成功消息
     */
    protected void clearSuccess() {
        success.postValue(null);
    }
}