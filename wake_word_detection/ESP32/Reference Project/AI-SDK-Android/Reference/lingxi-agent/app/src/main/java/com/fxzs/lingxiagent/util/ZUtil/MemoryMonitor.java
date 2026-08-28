package com.fxzs.lingxiagent.util.ZUtil;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;

import timber.log.Timber;

/**
 * 内存监控工具类
 * 用于追踪内存使用情况，帮助定位内存相关的崩溃
 */
public class MemoryMonitor {
    private static final String TAG = "MemoryMonitor";
    
    /**
     * 打印当前内存使用情况
     */
    public static void logMemoryUsage(Context context, String location) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        
        // 获取应用的内存信息
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / 1024 / 1024; // MB
        long totalMemory = runtime.totalMemory() / 1024 / 1024; // MB
        long freeMemory = runtime.freeMemory() / 1024 / 1024; // MB
        long usedMemory = totalMemory - freeMemory;
        
        // 获取Native内存信息
        long nativeHeapSize = Debug.getNativeHeapSize() / 1024 / 1024; // MB
        long nativeHeapAllocated = Debug.getNativeHeapAllocatedSize() / 1024 / 1024; // MB
        long nativeHeapFree = Debug.getNativeHeapFreeSize() / 1024 / 1024; // MB
        
        Timber.tag(TAG).d( "=== Memory Usage at " + location + " ===");
        Timber.tag(TAG).d( "Max Memory: " + maxMemory + " MB");
        Timber.tag(TAG).d( "Total Memory: " + totalMemory + " MB");
        Timber.tag(TAG).d( "Used Memory: " + usedMemory + " MB");
        Timber.tag(TAG).d( "Free Memory: " + freeMemory + " MB");
        Timber.tag(TAG).d( "Memory Usage: " + (usedMemory * 100 / maxMemory) + "%");
        Timber.tag(TAG).d( "Native Heap Size: " + nativeHeapSize + " MB");
        Timber.tag(TAG).d( "Native Heap Allocated: " + nativeHeapAllocated + " MB");
        Timber.tag(TAG).d( "Native Heap Free: " + nativeHeapFree + " MB");
        Timber.tag(TAG).d( "System Low Memory: " + memoryInfo.lowMemory);
        Timber.tag(TAG).d( "Available System Memory: " + (memoryInfo.availMem / 1024 / 1024) + " MB");
        Timber.tag(TAG).d( "=====================================");
        
        // 如果内存使用超过80%，发出警告
        if (usedMemory * 100 / maxMemory > 80) {
            Timber.tag(TAG).w( "WARNING: Memory usage is high! Consider freeing resources.");
        }
        
        // 如果系统内存不足，发出错误
        if (memoryInfo.lowMemory) {
            Timber.tag(TAG).e( "ERROR: System is in low memory state!");
        }
    }
    
    /**
     * 强制垃圾回收并记录
     */
    public static void forceGC(String reason) {
        Timber.tag(TAG).d( "Forcing garbage collection: " + reason);
        System.gc();
        System.runFinalization();
        System.gc();
    }
}