package com.fxzs.lingxiagent.viewmodel.main;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.fxzs.lingxiagent.model.common.BaseResponse;
import com.fxzs.lingxiagent.model.common.BaseViewModel;
import com.fxzs.lingxiagent.model.common.ObservableField;
import com.fxzs.lingxiagent.model.upgrade.UpgradeHelper;
import com.fxzs.lingxiagent.model.user.dto.AppVersionResponse;
import com.fxzs.lingxiagent.model.user.repository.UserRepository;
import com.fxzs.lingxiagent.model.user.repository.UserRepositoryImpl;

import java.util.Map;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class VMMain extends BaseViewModel {
    private static final String TAG = "VMMain";
    // Repository
    private final UserRepository userRepository;
    // 双向绑定字段
    private final ObservableField<String> welcomeMessage = new ObservableField<>("欢迎使用通通助手");
    private final ObservableField<String> statusText = new ObservableField<>("准备就绪");
    private final ObservableField<Boolean> refreshEnabled = new ObservableField<>(true);
    
    // 业务状态
    private final MutableLiveData<Boolean> dataLoaded = new MutableLiveData<>(false);
    private final MutableLiveData<String> navigationTarget = new MutableLiveData<>();
    private final MutableLiveData<AppVersionResponse> versionInfo = new MutableLiveData<>();
    
    public VMMain(@NonNull Application application) {
        super(application);
        // 初始化状态
        loadInitialData();
        this.userRepository = new UserRepositoryImpl();
    }
    
    /**
     * 获取欢迎消息
     * @return 欢迎消息观察字段
     */
    public ObservableField<String> getWelcomeMessage() {
        return welcomeMessage;
    }
    
    /**
     * 获取状态文本
     * @return 状态文本观察字段
     */
    public ObservableField<String> getStatusText() {
        return statusText;
    }
    
    /**
     * 获取刷新按钮启用状态
     * @return 刷新按钮启用状态观察字段
     */
    public ObservableField<Boolean> getRefreshEnabled() {
        return refreshEnabled;
    }
    
    /**
     * 获取数据加载结果
     * @return 数据加载结果LiveData
     */
    public MutableLiveData<Boolean> getDataLoaded() {
        return dataLoaded;
    }
    
    /**
     * 获取导航目标
     * @return 导航目标LiveData
     */
    public MutableLiveData<String> getNavigationTarget() {
        return navigationTarget;
    }

    /**
     * 获取版本信息
     * @return 版本信息
     */
    public MutableLiveData<AppVersionResponse> getVersionInfo() {
        return versionInfo;
    }
    
    /**
     * 加载数据
     */
    public void loadData() {
        if (getLoading().getValue() == Boolean.TRUE) {
            return; // 正在加载中，忽略重复请求
        }
        
        setLoading(true);
        refreshEnabled.set(false);
        statusText.set("正在加载数据...");
        clearError();
        
        // 模拟异步加载数据
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // 模拟网络延迟
                Thread.sleep(2000);
                
                // 模拟成功加载
                statusText.postValue("数据加载成功");
                dataLoaded.postValue(true);
                setSuccess("数据刷新完成");
                
            } catch (Exception e) {
                statusText.postValue("加载失败");
                dataLoaded.postValue(false);
                setError("数据加载失败: " + e.getMessage());
            } finally {
                refreshEnabled.postValue(true);
            }
        });
    }
    
    /**
     * 刷新数据
     */
    public void refreshData() {
        loadData();
    }
    
    /**
     * 加载初始数据
     */
    private void loadInitialData() {
        statusText.set("初始化中...");
        
//        // 模拟初始化
//        Executors.newSingleThreadExecutor().execute(() -> {
//            try {
//                Thread.sleep(500);
//                statusText.postValue("准备就绪");
//                dataLoaded.postValue(true);
//            } catch (Exception e) {
//                statusText.postValue("初始化失败");
//                setError("初始化失败");
//            }
//        });
    }
    
    /**
     * 导航到模块
     * @param moduleName 模块名称
     */
    public void navigateToModule(String moduleName) {
        navigationTarget.setValue(moduleName);
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