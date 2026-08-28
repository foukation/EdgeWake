package com.fxzs.lingxiagent.viewmodel.user;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.fxzs.lingxiagent.BuildConfig;
import com.fxzs.lingxiagent.model.common.BaseResponse;
import com.fxzs.lingxiagent.model.common.BaseViewModel;
import com.fxzs.lingxiagent.model.common.ObservableField;
import com.fxzs.lingxiagent.model.upgrade.UpgradeHelper;
import com.fxzs.lingxiagent.model.user.dto.AppVersionResponse;
import com.fxzs.lingxiagent.model.user.repository.UserRepository;
import com.fxzs.lingxiagent.model.user.repository.UserRepositoryImpl;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class VMAboutApp extends BaseViewModel {
    private static final String TAG = "VMAboutApp";
    // Repository
    private final UserRepository userRepository;

    // 双向绑定字段
    private final ObservableField<String> versionText = new ObservableField<>("");
    
    // 业务状态
    private final MutableLiveData<AppVersionResponse> versionInfo = new MutableLiveData<>();
    
    public VMAboutApp(@NonNull Application application) {
        super(application);
        this.userRepository = new UserRepositoryImpl();
    }
    
    // Getters
    public ObservableField<String> getVersionText() {
        return versionText;
    }
    
    public MutableLiveData<AppVersionResponse> getVersionInfo() {
        return versionInfo;
    }
    
    // 业务方法
    public void setVersionDisplay(String version) {
        String brand = BuildConfig.BRAND;
        versionText.set(version+"_Pad" );
    }

    // 获取版本信息
    public void fetchAppUpgradeInfo(Context context) {
        setLoading(true);
        // 请求body参数
        Map<String, String> params = UpgradeHelper.getRequestBody(context);

        userRepository.checkAppUpgrade(params).enqueue(new Callback<BaseResponse<AppVersionResponse>>() {
            @Override
            public void onResponse(Call<BaseResponse<AppVersionResponse>> call, Response<BaseResponse<AppVersionResponse>> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<AppVersionResponse> baseResponse = response.body();
                    if (baseResponse.getCode() == 0 && baseResponse.getData() != null) {
                        Timber.tag(TAG).i( "获取版本信息成功");
                        versionInfo.postValue(baseResponse.getData());
                    } else {
                        Timber.tag(TAG).e( baseResponse.getMsg() != null ? baseResponse.getMsg() : "获取版本信息失败");
                    }
                } else {
                    Timber.tag(TAG).e( "网络请求失败");
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<AppVersionResponse>> call, Throwable t) {
                setLoading(false);
                Timber.tag(TAG).e( "网络异常: " + t.getMessage());
            }
        });
    }
}