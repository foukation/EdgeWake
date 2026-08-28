package com.fxzs.lingxiagent.util;

import android.content.Context;

import timber.log.Timber;

/**
 * 崩溃安全辅助类
 * 提供安全的初始化和错误处理方法
 */
public class CrashSafetyHelper {
    
    private static final String TAG = "CrashSafetyHelper";
    
    /**
     * 安全执行操作
     */
    public static void safeExecute(String operationName, Runnable operation) {
        try {
            operation.run();
        } catch (Exception e) {
            Timber.tag(TAG).e ("安全执行失败: %s%s",  operationName, e);
        }
    }
    
    /**
     * 安全执行操作并返回结果
     */
    public static <T> T safeExecute(String operationName, SafeSupplier<T> supplier, T defaultValue) {
        try {
            return supplier.get();
        } catch (Exception e) {
            Timber.tag(TAG).e( "安全执行失败: " + operationName+ e);
            return defaultValue;
        }
    }
    
    /**
     * 安全供应商接口
     */
    public interface SafeSupplier<T> {
        T get() throws Exception;
    }
    
    /**
     * 检查对象是否为空
     */
    public static boolean isNull(Object obj) {
        return obj == null;
    }
    
    /**
     * 检查字符串是否为空
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    /**
     * 安全获取字符串
     */
    public static String safeGetString(String str, String defaultValue) {
        return isEmpty(str) ? defaultValue : str;
    }
    
    /**
     * 安全转换为整数
     */
    public static int safeParseInt(String str, int defaultValue) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            Timber.tag(TAG).w( "解析整数失败: " + str, e);
            return defaultValue;
        }
    }
    
    /**
     * 安全转换为长整数
     */
    public static long safeParseLong(String str, long defaultValue) {
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            Timber.tag(TAG).w( "解析长整数失败: " + str, e);
            return defaultValue;
        }
    }
    
    /**
     * 安全转换为浮点数
     */
    public static float safeParseFloat(String str, float defaultValue) {
        try {
            return Float.parseFloat(str);
        } catch (NumberFormatException e) {
            Timber.tag(TAG).w( "解析浮点数失败: " + str, e);
            return defaultValue;
        }
    }
    
    /**
     * 安全转换为双精度浮点数
     */
    public static double safeParseDouble(String str, double defaultValue) {
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            Timber.tag(TAG).w( "解析双精度浮点数失败: " + str, e);
            return defaultValue;
        }
    }
    
    /**
     * 安全转换为布尔值
     */
    public static boolean safeParseBoolean(String str, boolean defaultValue) {
        try {
            return Boolean.parseBoolean(str);
        } catch (Exception e) {
            Timber.tag(TAG).w( "解析布尔值失败: " + str, e);
            return defaultValue;
        }
    }
    
    /**
     * 记录错误信息
     */
    public static void logError(String tag, String message, Throwable throwable) {
        Timber.tag(TAG).e(message);
    }
    
    /**
     * 记录警告信息
     */
    public static void logWarning(String tag, String message, Throwable throwable) {
        Timber.tag(TAG).w( message, throwable);
    }
    
    /**
     * 记录调试信息
     */
    public static void logDebug(String tag, String message) {
        Timber.tag(TAG).d( message);
    }
    
    /**
     * 检查Context是否有效
     */
    public static boolean isContextValid(Context context) {
        return context != null && !isActivityDestroyed(context);
    }
    
    /**
     * 检查Activity是否已销毁
     */
    private static boolean isActivityDestroyed(Context context) {
        if (context instanceof android.app.Activity) {
            android.app.Activity activity = (android.app.Activity) context;
            return activity.isDestroyed() || activity.isFinishing();
        }
        return false;
    }
    
    /**
     * 安全关闭资源
     */
    public static void safeClose(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                Timber.tag(TAG).w( "关闭资源失败%s", e);
            }
        }
    }
    
    /**
     * 安全取消任务
     */
    public static void safeCancel(java.util.concurrent.Future<?> future) {
        if (future != null && !future.isCancelled()) {
            try {
                future.cancel(true);
            } catch (Exception e) {
                Timber.tag(TAG).w( "取消任务失败"+e);
            }
        }
    }
    
    /**
     * 安全释放资源
     */
    public static void safeDispose(io.reactivex.disposables.Disposable disposable) {
        if (disposable != null && !disposable.isDisposed()) {
            try {
                disposable.dispose();
            } catch (Exception e) {
                Timber.tag(TAG).w( "释放资源失败"+ e);
            }
        }
    }
}
