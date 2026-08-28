package com.fxzs.lingxiagent.view.common;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.ai.multimodal.utils.Prefs;
import com.baidu.mobstat.StatService;
import com.cmdc.ai.assist.AIAssistantManager;
import com.cmdc.ai.assist.api.GateWay;
import com.cmdc.ai.assist.constraint.AIAssistConfig;
import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.lingxi.lingxi_conversation.AIServiceManager;
import com.fxzs.lingxiagent.lingxi.main.utils.ScreenUtils;
import com.fxzs.lingxiagent.model.common.BaseViewModel;
import com.fxzs.lingxiagent.util.AppManager;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;
import com.fxzs.lingxiagent.util.ZUtil.Constant;
import com.fxzs.lingxiagent.util.ZUtil.DisplayUtil;
import com.fxzs.lingxiagent.util.ZUtils;

import timber.log.Timber;

/**
 * 基础Activity，提供双向绑定支持
 * @param <T> ViewModel类型
 */
public abstract class BaseActivity<T extends BaseViewModel> extends AppCompatActivity {
    private final String TAG = BaseActivity.class.getName();

    protected T viewModel;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppManager.addActivity(this);
        // 设置透明状态栏
        setTransparentStatusBar();
        
        setContentView(getLayoutResource());
        
        // 自动处理状态栏适配
        handleStatusBarAdaptation();
        
        try {
            // 初始化ViewModel
            viewModel = new ViewModelProvider(this).get(getViewModelClass());

            // 观察公共状态
            observeCommonStates();

            // 子类初始化
            initializeViews();
            // 设置双向绑定（必须在initializeViews之后）
            setupDataBinding();
            setupObservers();
        } catch (Exception e) {
            Timber.tag("BaseActivity").e( "Activity初始化失败"+ e);
            handleInitializationError(e);
        }
        ZUtils.setSystem(this);
    }
    
    /**
     * 获取布局资源ID
     * @return 布局资源ID
     */
    protected abstract int getLayoutResource();
    
    /**
     * 获取ViewModel类
     * @return ViewModel类
     */
    protected abstract Class<T> getViewModelClass();
    
    /**
     * 设置数据绑定
     */
    protected abstract void setupDataBinding();
    
    /**
     * 初始化视图
     */
    protected abstract void initializeViews();
    
    /**
     * 设置观察者
     */
    protected abstract void setupObservers();

    public boolean isScreen(){
        if (isTablet()){
            return false;
        }
        return true;
    }
    
    /**
     * 观察公共状态
     */
    private void observeCommonStates() {
        try {
            if (viewModel != null) {
                // 观察加载状态
                viewModel.getLoading().observe(this, loading -> {
                    try {
                        if (loading != null) {
                            handleLoadingState(loading);
                        }
                    } catch (Exception e) {
                        Timber.tag("BaseActivity").e( "处理加载状态失败"+ e);
                    }
                });

                // 观察错误信息
                viewModel.getError().observe(this, error -> {
                    try {
                        if (error != null && !error.isEmpty()) {
                            handleError(error);
                        }
                    } catch (Exception e) {
                        Timber.tag("BaseActivity").e( "处理错误信息失败"+ e);
                    }
                });

                // 观察成功消息
                viewModel.getSuccess().observe(this, success -> {
                    try {
                        if (success != null && !success.isEmpty()) {
                            if(Constant.lastSuccess != null && Constant.lastSuccess.equals(success)){
                                return;
                            }
                            Constant.lastSuccess = success;
                            handleSuccess(success);
                        }
                    } catch (Exception e) {
                        Timber.tag("BaseActivity").e( "处理成功消息失败"+ e);
                    }
                });
            }
        } catch (Exception e) {
            Timber.tag("BaseActivity").e( "观察公共状态失败"+ e);
        }
    }
    
    /**
     * 处理加载状态
     * @param loading 是否加载中
     */
    protected void handleLoadingState(boolean loading) {
        // 子类可以重写此方法来自定义加载状态显示
        // 例如显示/隐藏进度条
    }
    
    /**
     * 处理错误信息
     * @param error 错误信息
     */
    protected void handleError(String error) {
        GlobalToast.show(this, error, GlobalToast.Type.ERROR);
    }
    
    /**
     * 处理成功消息
     * @param success 成功消息
     */
    protected void handleSuccess(String success) {
//        GlobalToast.show(this, success, GlobalToast.Type.SUCCESS);
    }
    
    /**
     * 显示Toast消息
     * @param message 消息内容
     */
    protected void showToast(String message) {
        GlobalToast.show(this, message, GlobalToast.Type.NORMAL);
    }

    /**
     * 处理初始化错误
     * @param error 错误信息
     */
    protected void handleInitializationError(Exception error) {
        Timber.tag("BaseActivity").e( "Activity初始化失败: " + error.getMessage(), error);

        // 显示错误提示
        try {
            GlobalToast.show(this, "页面初始化失败，请重试", GlobalToast.Type.ERROR);
        } catch (Exception e) {
            // 如果连Toast都无法显示，则直接关闭Activity
            Timber.tag("BaseActivity").e( "无法显示错误提示"+ e);
        }

        // 延迟关闭Activity，给用户看到错误提示的时间
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            try {
                finish();
            } catch (Exception e) {
                Timber.tag("BaseActivity").e( "关闭Activity失败"+ e);
            }
        }, 2000);
    }
    
    /**
     * 设置透明状态栏
     */
    private void setTransparentStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(android.graphics.Color.TRANSPARENT);
            
            // 设置状态栏图标为深色（适合浅色背景）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | 
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | 
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                );
            } else {
                window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | 
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                );
            }
        }
    }

    /**
     * 设置状态栏颜色为白色，与背景一致，并保证内容不被遮挡
     */
    protected void setWhiteStatusBar() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(Color.parseColor("#FFFFFF"));
        getWindow().getDecorView().postDelayed(() -> {
            getWindow().setStatusBarColor(Color.parseColor("#FFFFFF"));
        }, 100);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    }
    
    /**
     * 处理状态栏适配
     * 自动为根布局设置fitsSystemWindows属性
     */
    private void handleStatusBarAdaptation() {
        // 获取根视图
        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            View contentView = ((android.view.ViewGroup) rootView).getChildAt(0);
            if (contentView != null) {
                // 对于Android 12及以上版本，自动设置fitsSystemWindows
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    contentView.setFitsSystemWindows(true);
                }
            }
        }
    }
    static float fontScale = 1f;

    // TODO: 2026/4/8 临时注释掉BaseActivity getResources。这个导致创维平板竖屏切换横屏靠左背景为黑色问题 
//    @Override
//    public Resources getResources() {
//        Resources resources = super.getResources();
//        fontScale = getFontScale();
//        return DisplayUtil.getResources(this,resources,fontScale);
//    }
//
//    @Override
//    protected void attachBaseContext(Context newBase) {
//        fontScale = getFontScale();
//        super.attachBaseContext(DisplayUtil.attachBaseContext(newBase,fontScale));
//    }

    public void setFontScale(float fontScale) {
        this.fontScale = fontScale;
        DisplayUtil.recreate(this);
    }

    public float getFontScale(){
        if(isOnePlusDevice()){
            return 0.9f;
        }else {
            return 1f;
        }
    }
    public boolean isOnePlusDevice() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        String brand = Build.BRAND;
        return (manufacturer != null && manufacturer.equalsIgnoreCase("oneplus")) ||
                (model != null && model.toLowerCase().startsWith("oneplus")) ||
                (brand != null && brand.equalsIgnoreCase("oneplus"));
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (SharedPreferencesUtil.isAgreePrivacy()) {
            StatService.onPageStart(this, this.getClass().getName());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        adjustRootLayout();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (SharedPreferencesUtil.isAgreePrivacy()) {
            StatService.onPageEnd(this, this.getClass().getName());
        }
    }

    protected void adjustRootLayout() {
        ViewGroup content = findViewById(android.R.id.content);
        Timber.tag("BaseActivity").d(" 手机=%s", isTablet());
        if (content == null || content.getChildCount() == 0) return;

        ViewGroup rootLayout = (ViewGroup) content.getChildAt(0);
        if (rootLayout == null) return;

        if (!isScreen()) {
            int orientation = getResources().getConfiguration().orientation;
            int maxWidth = (orientation == Configuration.ORIENTATION_LANDSCAPE) ? (int) (ScreenUtils.INSTANCE.getScreenHeight(this)) : ScreenUtils.INSTANCE.getScreenWidth(this);

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    maxWidth,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            params.gravity = Gravity.CENTER;
            rootLayout.setLayoutParams(params);
        } else {
            // 手机全屏
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            rootLayout.setLayoutParams(params);
        }
    }

    // 判断是否平板
    protected boolean isTablet() {
        return getResources().getBoolean(R.bool.isTablet);
    }


    public void commonDeviceAuth(DeviceAuthCallback authCallback){
        AIAssistConfig aiAssistConfig = AIServiceManager.Companion.getInstance().getAiAssistConfig(); // 获取 ai 网关服务
        if (!aiAssistConfig.isValid()){
            authCallback.onFail("设备鉴权失败");
            SharedPreferencesUtil.saveAuthStatus(false);
            return;
        }
        if (TextUtils.isEmpty(aiAssistConfig.getDeviceId()) || TextUtils.isEmpty(aiAssistConfig.getDeviceSecret())) {
            GateWay gateWay = AIAssistantManager.Companion.getInstance().gateWayHelp();
            gateWay.obtainDeviceInformation(response -> {
                Timber.tag(TAG).d("获取deviceId =  %s", response);
                Log.d(TAG,"获取鉴权成功设备信息 = "+response);
                if (response.getData() == null) {
                    runOnUiThread(() -> {
                        GlobalToast.show(this, response.getMessage() != null ? response.getMessage() : "设备鉴权失败", GlobalToast.Type.ERROR);
                        authCallback.onFail("设备鉴权失败");
                    });
                    SharedPreferencesUtil.saveAuthStatus(false);
                    return null;
                }

                aiAssistConfig.setDeviceSecret(response.getData().getDeviceSecret() != null ? response.getData().getDeviceSecret() : "");
                aiAssistConfig.setDeviceId(response.getData().getDeviceId() != null ? response.getData().getDeviceId() : "");
                Prefs.Companion.getInstance(getApplicationContext()).putObject("deviceInfoResponse", response);
                runOnUiThread(()->authCallback.onSuccess(response.getData().toString()));
                SharedPreferencesUtil.saveAuthStatus(true);
                return null;
            }, error -> {
                Timber.tag(TAG).e("error: %s", error);
                runOnUiThread(() -> authCallback.onFail(error));
                return null;
            });

        } else {
            authCallback.onSuccess("成功");
            SharedPreferencesUtil.saveAuthStatus(true);
        }

    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AppManager.removeActivity(this);
    }

    public interface DeviceAuthCallback {
        void onSuccess(String msg);
        void onFail(String msg);
    }

}