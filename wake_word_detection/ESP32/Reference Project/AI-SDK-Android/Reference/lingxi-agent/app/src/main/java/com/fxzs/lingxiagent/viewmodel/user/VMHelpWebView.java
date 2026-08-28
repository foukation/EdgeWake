package com.fxzs.lingxiagent.viewmodel.user;

import android.app.Application;

import androidx.lifecycle.MutableLiveData;

import com.fxzs.lingxiagent.model.common.BaseViewModel;

public class VMHelpWebView extends BaseViewModel {
    
    private final MutableLiveData<Boolean> loadingState = new MutableLiveData<>(false);
    
    // 帮助页面URL，可以根据需要配置
    private static final String HELP_URL = "https://www.example.com/help"; // 替换为实际的帮助页面URL
    
    public VMHelpWebView(Application application) {
        super(application);
    }
    
    public String getHelpUrl() {
        return HELP_URL;
    }
    
    public MutableLiveData<Boolean> getLoadingState() {
        return loadingState;
    }
    
    public void setLoadingState(boolean isLoading) {
        loadingState.postValue(isLoading);
    }
}