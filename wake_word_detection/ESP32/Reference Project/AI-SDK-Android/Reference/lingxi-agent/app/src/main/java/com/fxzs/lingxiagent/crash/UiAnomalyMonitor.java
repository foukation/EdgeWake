package com.fxzs.lingxiagent.crash;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.SystemClock;

import com.fxzs.lingxiagent.IYAApplication;

import java.util.WeakHashMap;

/**
 * 监控 UI 异常：短时间内同一 Activity 频繁 onCreate/重建，或冷启动后秒开秒关的“闪一下屏”
 */
public final class UiAnomalyMonitor implements Application.ActivityLifecycleCallbacks {

    // 由 CrashReportConfig 提供阈值

    private final IYAApplication application;
    private final WeakHashMap<Class<?>, Integer> createCount = new WeakHashMap<>();
    private long appStartUptime = SystemClock.uptimeMillis();

    public UiAnomalyMonitor(IYAApplication application) {
        this.application = application;
    }

    @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        long now = SystemClock.uptimeMillis();
        // 统计重建次数
        int c = (createCount.containsKey(activity.getClass()) ? createCount.get(activity.getClass()) : 0) + 1;
        createCount.put(activity.getClass(), c);
        if (now - appStartUptime <= CrashReportConfig.getUiRecreateWindowMs() && c >= CrashReportConfig.getUiRecreateThreshold()) {
            String msg = activity.getClass().getSimpleName() + " recreated " + c + " times in " + (now - appStartUptime) + "ms";
            CrashLogger.logEvent(application, "UI_ANOMALY_RECREATE", msg, "");
            // 重置窗口，避免持续放大
            appStartUptime = now;
            createCount.clear();
        }
    }

    @Override public void onActivityStarted(Activity activity) {}
    @Override public void onActivityResumed(Activity activity) {
        application.setCurrentActivity(activity);

    }
    @Override public void onActivityPaused(Activity activity) {
        if (application.getCurrentActivity()== activity) {
            application.setCurrentActivity(null);
        }
    }
    @Override public void onActivityStopped(Activity activity) {}
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
    @Override public void onActivityDestroyed(Activity activity) {}
}

