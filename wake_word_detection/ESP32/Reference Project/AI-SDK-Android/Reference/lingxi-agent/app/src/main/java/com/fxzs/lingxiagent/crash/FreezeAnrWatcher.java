package com.fxzs.lingxiagent.crash;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

/**
 * 监控主线程卡顿/疑似ANR：
 * - 在主线程上设置心跳，若长时间未响应，则记录一次“ANR_SUSPECT”事件
 * - 注意：这不是系统ANR捕获，只是辅助定位“闪退/黑屏/卡住”现象
 */
public final class FreezeAnrWatcher {

    // 由 CrashReportConfig 提供阈值

    private final Context appContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile long lastBeatUptime = System.currentTimeMillis();

    public FreezeAnrWatcher(Context context) {
        this.appContext = context.getApplicationContext();
    }

    private final Runnable beat = new Runnable() {
        @Override public void run() {
            lastBeatUptime = System.currentTimeMillis();
            mainHandler.postDelayed(this, CrashReportConfig.getCheckIntervalMs());
        }
    };

    public void start() {
        mainHandler.post(beat);
        // 后台线程周期检查主线程是否长时间未心跳
        new Thread(() -> {
            while (true) {
                try {
                    long now = System.currentTimeMillis();
                    long delta = now - lastBeatUptime;
                    if (delta > CrashReportConfig.getAnrThresholdMs()) {
                        String msg = "Main thread unresponsive for " + delta + "ms";
                        // 抓取主线程当前堆栈
                        String stack = getMainThreadStack();
                        CrashLogger.logEvent(appContext, "ANR_SUSPECT", msg, stack);
                        // 重置一次，避免短时间内重复记录
                        lastBeatUptime = now;
                    }
                    Thread.sleep(CrashReportConfig.getCheckIntervalMs());
                } catch (Throwable ignore) {}
            }
        }, "anr-checker").start();
    }

    private static String getMainThreadStack() {
        StringBuilder sb = new StringBuilder();
        Thread main = Looper.getMainLooper().getThread();
        if (main != null) {
            for (StackTraceElement el : main.getStackTrace()) {
                sb.append("    at ")
                  .append(el.getClassName()).append('.')
                  .append(el.getMethodName())
                  .append('(').append(el.getFileName()).append(':').append(el.getLineNumber()).append(')')
                  .append('\n');
            }
        }
        return sb.toString();
    }
}

