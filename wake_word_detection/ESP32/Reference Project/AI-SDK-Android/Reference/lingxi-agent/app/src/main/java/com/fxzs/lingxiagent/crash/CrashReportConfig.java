package com.fxzs.lingxiagent.crash;

import android.content.Context;

import com.fxzs.lingxiagent.model.common.Constants;

/**
 * 崩溃上报配置
 */
public final class CrashReportConfig {

    /**
     * 崩溃上报接口路径（拼接 Constants.BASE_URL 使用）
     * 示例：BASE_URL + API_PATH -> https://domain/app-api/system/error-report/crash-log
     */
    private static String API_PATH = "app-api/system/error-report/crash-log";

    // ===================== 监控与阈值配置（默认开启轻量监控） =====================

    /** 是否启用主线程卡顿/疑似ANR监控（FreezeAnrWatcher） */
    private static boolean ENABLE_ANR_WATCH = true;

    /** 主线程心跳检测间隔（毫秒）。值越小越敏感，开销也略增。默认 2000ms */
    private static long CHECK_INTERVAL_MS = 2000L;

    /** 判定疑似ANR的卡顿阈值（毫秒）。超过该时长未响应则记录事件。默认 5000ms */
    private static long ANR_THRESHOLD_MS = 5000L;

    /** 是否启用UI异常监控（短时间内频繁重建/闪屏） */
    private static boolean ENABLE_UI_ANOMALY_MONITOR = true;

    /** 统计UI重建的时间窗口（毫秒）。默认 3000ms */
    private static long UI_RECREATE_WINDOW_MS = 3000L;

    /** 在时间窗口内，超过多少次 onCreate 视为异常。默认 3 次 */
    private static int UI_RECREATE_THRESHOLD = 3;

    /**
     * 事件最小落盘间隔（毫秒）。
     * 同一 eventType 在该间隔内重复触发，将被抑制（避免频繁落盘和上报）。默认 10000ms
     */
    private static long MIN_EVENT_REPORT_INTERVAL_MS = 10_000L;

    /**
     * 本地最多保留的崩溃/事件文件数。超过后按最旧优先删除。默认 100 条
     */
    private static int MAX_RETAINED_FILES = 100;

    private CrashReportConfig() {}

    /** 初始化占位：可接入远端配置/本地开关 */
    public static void init(Context context) {
        // TODO: 从远端/本地配置动态覆盖以上参数（如需要）
    }

    /** 上报完整地址 */
    public static String getEndpointUrl() {
        return Constants.BASE_URL + API_PATH;
    }

    /** 设置接口路径（不含 BASE_URL） */
    public static void setApiPath(String apiPath) {
        if (apiPath != null && !apiPath.isEmpty()) {
            API_PATH = apiPath;
        }
    }

    // ===================== Getter / Setter =====================

    public static boolean isEnableAnrWatch() { return ENABLE_ANR_WATCH; }
    public static void setEnableAnrWatch(boolean enable) { ENABLE_ANR_WATCH = enable; }

    public static long getCheckIntervalMs() { return CHECK_INTERVAL_MS; }
    public static void setCheckIntervalMs(long v) { if (v > 0) CHECK_INTERVAL_MS = v; }

    public static long getAnrThresholdMs() { return ANR_THRESHOLD_MS; }
    public static void setAnrThresholdMs(long v) { if (v >= 500) ANR_THRESHOLD_MS = v; }

    public static boolean isEnableUiAnomalyMonitor() { return ENABLE_UI_ANOMALY_MONITOR; }
    public static void setEnableUiAnomalyMonitor(boolean enable) { ENABLE_UI_ANOMALY_MONITOR = enable; }

    public static long getUiRecreateWindowMs() { return UI_RECREATE_WINDOW_MS; }
    public static void setUiRecreateWindowMs(long v) { if (v >= 500) UI_RECREATE_WINDOW_MS = v; }

    public static int getUiRecreateThreshold() { return UI_RECREATE_THRESHOLD; }
    public static void setUiRecreateThreshold(int v) { if (v >= 1) UI_RECREATE_THRESHOLD = v; }

    public static long getMinEventReportIntervalMs() { return MIN_EVENT_REPORT_INTERVAL_MS; }
    public static void setMinEventReportIntervalMs(long v) { if (v >= 0) MIN_EVENT_REPORT_INTERVAL_MS = v; }

    public static int getMaxRetainedFiles() { return MAX_RETAINED_FILES; }
    public static void setMaxRetainedFiles(int v) { if (v >= 10) MAX_RETAINED_FILES = v; }
}

