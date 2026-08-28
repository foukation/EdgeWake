package com.fxzs.lingxiagent.util;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

/**
 * 性能监控器
 * 用于监控应用性能指标
 */
public class PerformanceMonitor {
    
    private static final String TAG = "PerformanceMonitor";
    private static PerformanceMonitor instance;
    
    private final Map<String, Long> operationStartTimes = new HashMap<>();
    private final Context context;
    
    private PerformanceMonitor(Context context) {
        this.context = context.getApplicationContext();
    }
    
    /**
     * 获取单例实例
     */
    public static synchronized PerformanceMonitor getInstance(Context context) {
        if (instance == null) {
            instance = new PerformanceMonitor(context);
        }
        return instance;
    }
    
    /**
     * 开始操作计时
     */
    public void startOperation(String operationName) {
        try {
            operationStartTimes.put(operationName, System.currentTimeMillis());
            Timber.tag(TAG).d( "开始操作: " + operationName);
        } catch (Exception e) {
            Timber.tag(TAG).e( "开始操作失败: " + operationName, e);
        }
    }
    
    /**
     * 结束操作计时
     */
    public long endOperation(String operationName) {
        try {
            Long startTime = operationStartTimes.remove(operationName);
            if (startTime != null) {
                long duration = System.currentTimeMillis() - startTime;
                Timber.tag(TAG).d( "操作完成: " + operationName + ", 耗时: " + duration + "ms");
                return duration;
            } else {
                Timber.tag(TAG).w( "未找到操作开始时间: " + operationName);
                return 0;
            }
        } catch (Exception e) {
            Timber.tag(TAG).w( "结束操作失败: " + operationName, e);
            return 0;
        }
    }
    
    /**
     * 记录页面加载时间
     */
    public void recordPageLoadTime(String pageName, long loadTime) {
        try {
            Timber.tag(TAG).d( "页面加载时间: " + pageName + ", 时间: " + loadTime + "ms");
        } catch (Exception e) {
            Timber.tag(TAG).w( "记录页面加载时间失败: " + pageName, e);
        }
    }
    
    /**
     * 获取内存使用情况
     */
    public MemoryInfo getMemoryInfo() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            return new MemoryInfo(usedMemory, maxMemory, totalMemory, freeMemory);
        } catch (Exception e) {
            Timber.tag(TAG).w( "获取内存信息失败"+ e);
            return new MemoryInfo(0, 0, 0, 0);
        }
    }
    
    /**
     * 内存信息类
     */
    public static class MemoryInfo {
        private final long usedMemory;
        private final long maxMemory;
        private final long totalMemory;
        private final long freeMemory;
        
        public MemoryInfo(long usedMemory, long maxMemory, long totalMemory, long freeMemory) {
            this.usedMemory = usedMemory;
            this.maxMemory = maxMemory;
            this.totalMemory = totalMemory;
            this.freeMemory = freeMemory;
        }
        
        public long getUsedMemory() { return usedMemory; }
        public long getMaxMemory() { return maxMemory; }
        public long getTotalMemory() { return totalMemory; }
        public long getFreeMemory() { return freeMemory; }
    }
}
