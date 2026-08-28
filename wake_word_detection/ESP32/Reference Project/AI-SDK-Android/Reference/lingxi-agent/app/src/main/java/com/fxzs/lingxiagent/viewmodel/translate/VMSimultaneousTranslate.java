package com.fxzs.lingxiagent.viewmodel.translate;

import android.app.Application;

import androidx.annotation.NonNull;

import com.fxzs.lingxiagent.model.common.BaseViewModel;

/**
 * 同声传译主页面ViewModel
 */
public class VMSimultaneousTranslate extends BaseViewModel {
    
    private static final String TAG = "VMSimultaneousTranslate";

    public VMSimultaneousTranslate(@NonNull Application application) {
        super(application);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }
}