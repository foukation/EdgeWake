package com.fxzs.lingxiagent;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.amap.api.location.AMapLocationClient;
import com.baidu.mobstat.StatService;
import com.bumptech.glide.Glide;
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;
import com.bumptech.glide.load.model.GlideUrl;
import com.fxzs.lingxiagent.crash.CrashLogger;
import com.fxzs.lingxiagent.crash.CrashReportConfig;
import com.fxzs.lingxiagent.crash.FreezeAnrWatcher;
import com.fxzs.lingxiagent.crash.UiAnomalyMonitor;
import com.fxzs.lingxiagent.lingxi.accessibility_api.AccessibilityApi;
import com.fxzs.lingxiagent.lingxi.common.log.FileLoggingTree;
import com.fxzs.lingxiagent.lingxi.float_manager.FloatWindowHelper;
import com.fxzs.lingxiagent.lingxi.multimodal.utils.DeviceInfoUtil;
import com.fxzs.lingxiagent.lingxi.multimodal.utils.eventTracker.TrackerUtils;
import com.fxzs.lingxiagent.model.auth.AuthHelper;
import com.fxzs.lingxiagent.service.BaseAccessibilityService;
import com.fxzs.lingxiagent.util.AMapKeyProvider;
import com.fxzs.lingxiagent.util.DeviceUUIDGenerator;
import com.fxzs.lingxiagent.util.GlobalSettings;
import com.fxzs.lingxiagent.util.LocaleManager;
import com.fxzs.lingxiagent.util.SharedPreferencesUtil;
import com.fxzs.lingxiagent.util.ZUtil.MarkdownUtils;
import com.lingxi.nexuspilot.NexusPilotManager;
import com.tencent.bugly.crashreport.CrashReport;

import java.io.InputStream;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import timber.log.Timber;

/**
 * 应用Application类
 */
public class IYAApplication extends Application {

    private static IYAApplication instance;
    public  Activity currentActivity;
    private  boolean isAppForeground = false;


    @Override
    public void onCreate() {
        super.onCreate();
        initProcess();
        initMemoryLeakDetection();
        instance = this;
        // 初始化SharedPreferences
        SharedPreferencesUtil.init(this);
        // 初始化全局设置
        GlobalSettings.getInstance().init(this);

        // 初始化语言管理器
        LocaleManager.init(this);

        // 应用保存的语言设置
        LocaleManager.applyLanguage(this);

        // 预加载字体，提升Markdown渲染性能
        MarkdownUtils.preloadFonts(this);

        // 解决glide加载https证书问题
        try {
            Glide.get(this).getRegistry().replace(
                    GlideUrl.class, InputStream.class,
                    new OkHttpUrlLoader.Factory(getSSLOkHttpClient()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(SharedPreferencesUtil.isAgreePrivacy()){
            initAllSensitiveServicesAfterAgreePrivacy();
        }
    }

    // ========================================================================
// 【隐私合规核心方法】
// 所有敏感初始化、获取ID、SDK、统计、崩溃、服务 全部放在这里
// 必须在用户同意隐私政策之后 才能调用！！！
// ========================================================================
    public void initAllSensitiveServicesAfterAgreePrivacy() {
        // 崩溃日志：安装全局异常捕获
        CrashReportConfig.init(this);
        CrashLogger.install(this);

        // 监控疑似 ANR/卡顿
        if (CrashReportConfig.isEnableAnrWatch()) {
            new FreezeAnrWatcher(this).start();
        }
        // 监控 UI 异常
        if (CrashReportConfig.isEnableUiAnomalyMonitor()) {
            registerActivityLifecycleCallbacks(new UiAnomalyMonitor(this));
        }
        //初始化地图
        initAMap();
        // 初始化登录接口
        AuthHelper.getInstance().init(this);

        // 初始化服务
        initializeServices();

        // 日志
        initTimber();

        // Bugly 崩溃上报（会采集设备信息）
        CrashReport.UserStrategy strategy = new CrashReport.UserStrategy(this);
        strategy.setAppChannel(BuildConfig.BRAND);
        strategy.setAppVersion(BuildConfig.VERSION_NAME);
        CrashReport.initCrashReport(getApplicationContext(), "39632c95e2", true, strategy);

        // 事件统计
        initEventTrackerManager();

        // 百度统计（审核重点）
        initBaidu();

        // GUI 服务（会获取 DeviceId）
        initGUiServices();
    }

    public static IYAApplication getInstance() {
        return instance;
    }

    public static Context getAppContext() {
        return instance.getApplicationContext();
    }

    /**
     * 设置https 访问的时候对所有证书都进行信任
     *
     * @throws Exception
     */
    private OkHttpClient getSSLOkHttpClient() throws Exception {
        final X509TrustManager trustManager = new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };

        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, new TrustManager[]{trustManager}, new SecureRandom());
        SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        return new OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, trustManager)
                .hostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                })
                .build();
    }


    private static void initializeServices() {

        AccessibilityApi.BASE_SERVICE_CLS = BaseAccessibilityService.class;
        AccessibilityApi.GESTURE_SERVICE_CLS = BaseAccessibilityService.class;
        AccessibilityApi.Companion.init(instance, BaseAccessibilityService.class, BaseAccessibilityService.class);
    }

    private void initTimber() {
        // 初始化 Timber - 使用内部存储
        if (BuildConfig.DEBUG || SharedPreferencesUtil.getLogOpen()) {

            Timber.plant(new FileLoggingTree(this));
            SharedPreferencesUtil.saveLogOpen(true);
        }
    }

    private void initEventTrackerManager() {
        TrackerUtils.initEventTrackerManager(this);
    }
    private void initBaidu() {
        StatService.setDebugOn(BuildConfig.DEBUG);

        // 通过该接口可以控制敏感数据采集，true表示可以采集，false表示不可以采集，
        // 该方法一定要最优先调用，请在StatService.start(this)之前调用，采集这些数据可以帮助App运营人员更好的监控App的使用情况，
        // 建议有用户隐私策略弹窗的App，用户未同意前设置false,同意之后设置true
        StatService.setAuthorizedState(this,SharedPreferencesUtil.isAgreePrivacy());
        // SDK初始化，该函数不会采集用户个人信息，也不会向百度移动统计后台上报数据
        StatService.setAppKey("f596fe294b");
        StatService.setAppVersionName(this, BuildConfig.VERSION_NAME);
        StatService.setAppChannel(this, Build.MODEL + "_" + DeviceInfoUtil.INSTANCE.getProductId(this),true);
        StatService.start(this);
    }

    public Activity getCurrentActivity(){
        return currentActivity;
    }

    public void setCurrentActivity(Activity currentActivity){
        this.currentActivity = currentActivity;
    }

    private void initGUiServices(){
        String devicesID = DeviceUUIDGenerator.getDeviceUUID(this);
        int result = NexusPilotManager.INSTANCE.init(this,devicesID);
        Timber.tag("IYAApplication").d("初始化GUI服务结果："+result);
        SharedPreferencesUtil.saveString("GUI_ENV", "GUI正式服务器");
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onForeground() {
        isAppForeground = true;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onBackground() {
        isAppForeground = false;
    }


    /**
     * Beta Debug 包通过源码集注入 LeakCanary，其它变体无此类，反射调用避免编译依赖。
     */
    private void initMemoryLeakDetection() {
        if (!BuildConfig.DEBUG || !BuildConfig.FLAVOR.contains("Beta")) {
            return;
        }
        try {
            Class.forName("com.fxzs.lingxiagent.debug.MemoryLeakDetection")
                    .getMethod("install", Application.class)
                    .invoke(null, this);
        } catch (ReflectiveOperationException ignored) {
            // 非 betaDebug 变体或未集成 LeakCanary
        }
    }

    private void initProcess(){
        ProcessLifecycleOwner.get().getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onStart(@NonNull LifecycleOwner owner) {
                DefaultLifecycleObserver.super.onStart(owner);
                isAppForeground = true;
            }

            @Override
            public void onStop(@NonNull LifecycleOwner owner) {
                DefaultLifecycleObserver.super.onStop(owner);
                isAppForeground = false;
            }
        });
    }
    public boolean isAppInForeground() {
        return isAppForeground;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        FloatWindowHelper.release();
    }

    private void initAMap() {
        // 从 .so 库中获取 Key（运行时解密，不出现在 APK 可读文件中）
        String apiKey = AMapKeyProvider.INSTANCE.getApiKey();
//        Log.e("测试key","key = "+apiKey);
        AMapLocationClient.setApiKey(apiKey);
    }
}