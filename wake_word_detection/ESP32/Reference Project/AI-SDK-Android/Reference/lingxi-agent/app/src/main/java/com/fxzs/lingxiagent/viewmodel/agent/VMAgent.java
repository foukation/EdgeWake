package com.fxzs.lingxiagent.viewmodel.agent;

import android.app.Application;

import androidx.annotation.NonNull;

import com.fxzs.lingxiagent.model.common.BaseViewModel;

public class VMAgent extends BaseViewModel {
    
    public VMAgent(@NonNull Application application) {
        super(application);
        // 初始化
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        // 清理资源
    }
}