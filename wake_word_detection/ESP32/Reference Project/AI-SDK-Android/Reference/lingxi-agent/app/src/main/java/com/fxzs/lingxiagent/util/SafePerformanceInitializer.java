package com.fxzs.lingxiagent.util;

import android.content.Context;

import timber.log.Timber;

/**
 * 安全性能初始化器
 * 提供安全的性能监控初始化和操作方法
 */
public class SafePerformanceInitializer {
    
    private static final String TAG = "SafePerformanceInitializer";
    
    /**
     * 安全初始化性能监控器
     */
    public static PerformanceMonitor safeInitPerformanceMonitor(Context context) {
        try {
            return PerformanceMonitor.getInstance(context);
        } catch (Exception e) {
            Timber.tag(TAG).w( "性能监控器初始化失败"+ e);
            return null;
        }
    }
    
    /**
     * 安全结束操作
     */
    public static void safeEndOperation(PerformanceMonitor monitor, String operationName) {
        try {
            if (monitor != null) {
                monitor.endOperation(operationName);
            }
        } catch (Exception e) {
            Timber.tag(TAG).w("结束操作失败: " + operationName + e);
        }
    }
    
    /**
     * 安全开始操作
     */
    public static void safeStartOperation(PerformanceMonitor monitor, String operationName) {
        try {
            if (monitor != null) {
                monitor.startOperation(operationName);
            }
        } catch (Exception e) {
            Timber.tag(TAG).w( "开始操作失败: " + operationName + e);
        }
    }
}
