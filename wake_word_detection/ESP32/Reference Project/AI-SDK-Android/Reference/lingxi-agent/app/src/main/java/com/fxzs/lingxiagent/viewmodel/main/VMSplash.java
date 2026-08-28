package com.fxzs.lingxiagent.viewmodel.main;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fxzs.lingxiagent.model.auth.dto.LoginResponse;
import com.fxzs.lingxiagent.model.auth.repository.AuthRepository;
import com.fxzs.lingxiagent.model.auth.repository.AuthRepositoryImpl;
import com.fxzs.lingxiagent.model.common.BaseViewModel;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;

public class VMSplash extends BaseViewModel {
    private final MutableLiveData<Boolean> authToken = new MutableLiveData<>(false);
    private final AuthRepository authRepository;
    public VMSplash(@NonNull Application application) {
        super(application);
        authRepository = new AuthRepositoryImpl();
    }

    public LiveData<LoginResponse> refreshToken() {
        String refreshToken = SharedPreferencesUtil.getRefreshToken();
        if (!refreshToken.isEmpty()) {
          return  authRepository.refreshToken(refreshToken);
        }else {
            MutableLiveData<LoginResponse> emptyLiveData = new MutableLiveData<>();
            emptyLiveData.setValue(null);
            return emptyLiveData;
        }
    }

}