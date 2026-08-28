package com.fxzs.lingxiagent.util;

import android.app.ActivityManager;
import android.content.Context;

import timber.log.Timber;

/**
 * 内存管理器
 * 用于监控和管理应用内存使用
 */
public class MemoryManager {
    
    private static final String TAG = "MemoryManager";
    private static MemoryManager instance;
    
    private MemoryManager() {
    }
    
    /**
     * 获取单例实例
     */
    public static synchronized MemoryManager getInstance() {
        if (instance == null) {
            instance = new MemoryManager();
        }
        return instance;
    }
    
    /**
     * 检查内存状态
     */
    public void checkMemoryStatus(Context context) {
        try {
            if (context != null) {
                ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                if (activityManager != null) {
                    ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
                    activityManager.getMemoryInfo(memoryInfo);
                    
                    long availableMemory = memoryInfo.availMem;
                    long totalMemory = memoryInfo.totalMem;
                    boolean lowMemory = memoryInfo.lowMemory;
                    
                    Timber.tag(TAG).d( "可用内存: " + (availableMemory / 1024 / 1024) + "MB");
                    Timber.tag(TAG).d( "总内存: " + (totalMemory / 1024 / 1024) + "MB");
                    Timber.tag(TAG).d( "内存不足: " + lowMemory);
                    
                    if (lowMemory) {
                        Timber.tag(TAG).w("系统内存不足，建议清理缓存");
                        // 可以在这里触发内存清理操作
                        performMemoryCleanup(context);
                    }
                }
            }
        } catch (Exception e) {
            Timber.tag(TAG).w("检查内存状态失败"+ e);
        }
    }
    
    /**
     * 获取内存信息
     */
    public MemoryInfo getMemoryInfo(Context context) {
        try {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            return new MemoryInfo(usedMemory, maxMemory, totalMemory, freeMemory);
        } catch (Exception e) {
            Timber.tag(TAG).w( "获取内存信息失败"+e);
            return new MemoryInfo(0, 0, 0, 0);
        }
    }
    
    /**
     * 执行内存清理
     */
    private void performMemoryCleanup(Context context) {
        try {
            // 建议垃圾回收
            System.gc();
            
            // 清理图片缓存
            PerformanceOptimizer.clearImageMemoryCache(context);
            
            Timber.tag(TAG).d( "内存清理完成");
        } catch (Exception e) {
            Timber.tag(TAG).w("内存清理失败"+e);
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
        
        @Override
        public String toString() {
            return String.format("MemoryInfo{used=%dMB, max=%dMB, total=%dMB, free=%dMB}",
                usedMemory / 1024 / 1024,
                maxMemory / 1024 / 1024,
                totalMemory / 1024 / 1024,
                freeMemory / 1024 / 1024);
        }
    }
}
