package com.fxzs.lingxiagent.util;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;

import com.fxzs.lingxiagent.model.common.Constants;

import timber.log.Timber;

/**
 * 设备性能配置工具类
 * 根据设备内存自动调整性能参数，优化应用性能
 */
public class DevicePerformanceConfig {
    private static final String TAG = "DevicePerformanceConfig";
    
    /**
     * 设备性能等级
     */
    public enum PerformanceLevel {
        LOW_END,      // 低端设备（< 3GB RAM）
        MID_RANGE,    // 中端设备（3-5GB RAM）
        HIGH_END      // 高端设备（> 5GB RAM）
    }
    
    private static PerformanceLevel performanceLevel = null;
    private static long totalMemoryMB = 0;
    
    /**
     * 初始化设备性能配置
     * 建议在 Application.onCreate() 中调用
     * 
     * @param application 应用上下文
     */
    public static void init(Application application) {
        detectDevicePerformance(application);
    }
    
    /**
     * 检测设备性能等级
     * 
     * @param application 应用上下文
     */
    private static void detectDevicePerformance(Application application) {
        try {
            ActivityManager activityManager = 
                (ActivityManager) application.getSystemService(Context.ACTIVITY_SERVICE);
            
            if (activityManager != null) {
                ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
                activityManager.getMemoryInfo(memInfo);
                
                // 获取设备总内存（MB）
                totalMemoryMB = memInfo.totalMem / (1024 * 1024);
                
                Timber.tag(TAG).d("设备总内存: %d MB", totalMemoryMB);
                
                // 根据内存大小判断性能等级
                if (totalMemoryMB < 3000) {
                    performanceLevel = PerformanceLevel.LOW_END;
                    Timber.tag(TAG).d("检测到低端设备（< 3GB RAM）");
                } else if (totalMemoryMB < 5000) {
                    performanceLevel = PerformanceLevel.MID_RANGE;
                    Timber.tag(TAG).d("检测到中端设备（3-5GB RAM）");
                } else {
                    performanceLevel = PerformanceLevel.HIGH_END;
                    Timber.tag(TAG).d("检测到高端设备（> 5GB RAM）");
                }
            }
        } catch (Exception e) {
            Timber.tag(TAG).e(e, "获取设备内存失败");
            // 默认使用中端设备配置
            performanceLevel = PerformanceLevel.MID_RANGE;
        }
        
        if (performanceLevel == null) {
            performanceLevel = PerformanceLevel.MID_RANGE;
            Timber.tag(TAG).d("使用默认配置：中端设备");
        }
        
        logPerformanceConfig();
    }
    
    /**
     * 获取设备性能等级
     * 
     * @return 性能等级
     */
    public static PerformanceLevel getPerformanceLevel() {
        if (performanceLevel == null) {
            return PerformanceLevel.MID_RANGE;
        }
        return performanceLevel;
    }
    
    /**
     * 获取设备总内存（MB）
     * 
     * @return 总内存大小
     */
    public static long getTotalMemoryMB() {
        return totalMemoryMB;
    }
    
    /**
     * 获取最大消息数量限制
     * 根据设备性能自动调整
     * 
     * @return 最大消息数量
     */
    public static int getMaxMessages() {
        switch (getPerformanceLevel()) {
            case LOW_END:
                return Constants.MAX_MESSAGES_LOW_END;
            case MID_RANGE:
                return Constants.MAX_MESSAGES_MID_RANGE;
            case HIGH_END:
                return Constants.MAX_MESSAGES_HIGH_END;
            default:
                return Constants.MAX_MESSAGES_MID_RANGE;
        }
    }
    
    /**
     * 获取 RecyclerView 缓存池大小
     * 根据设备性能自动调整
     * 
     * @return 缓存池大小
     */
    public static int getRecyclerCacheSize() {
        switch (getPerformanceLevel()) {
            case LOW_END:
                return Constants.RECYCLER_CACHE_SIZE_LOW_END;
            case MID_RANGE:
                return Constants.RECYCLER_CACHE_SIZE_MID_RANGE;
            case HIGH_END:
                return Constants.RECYCLER_CACHE_SIZE_HIGH_END;
            default:
                return Constants.RECYCLER_CACHE_SIZE_MID_RANGE;
        }
    }
    
    /**
     * 获取 RecyclerView ViewHolder 缓存数量
     * 根据设备性能自动调整
     * 
     * @return ViewHolder 缓存数量
     */
    public static int getRecyclerViewCacheSize() {
        switch (getPerformanceLevel()) {
            case LOW_END:
                return Constants.RECYCLER_VIEW_CACHE_SIZE_LOW_END;
            case MID_RANGE:
                return Constants.RECYCLER_VIEW_CACHE_SIZE_MID_RANGE;
            case HIGH_END:
                return Constants.RECYCLER_VIEW_CACHE_SIZE_HIGH_END;
            default:
                return Constants.RECYCLER_VIEW_CACHE_SIZE_MID_RANGE;
        }
    }
    
    /**
     * 是否为低端设备
     * 
     * @return true 如果是低端设备
     */
    public static boolean isLowEndDevice() {
        return getPerformanceLevel() == PerformanceLevel.LOW_END;
    }
    
    /**
     * 是否为高端设备
     * 
     * @return true 如果是高端设备
     */
    public static boolean isHighEndDevice() {
        return getPerformanceLevel() == PerformanceLevel.HIGH_END;
    }
    
    /**
     * 打印性能配置信息
     */
    private static void logPerformanceConfig() {
        Timber.tag(TAG).d("========== 设备性能配置 ==========");
        Timber.tag(TAG).d("性能等级: %s", getPerformanceLevel());
        Timber.tag(TAG).d("设备内存: %d MB", totalMemoryMB);
        Timber.tag(TAG).d("最大消息数: %d", getMaxMessages());
        Timber.tag(TAG).d("RecyclerView 缓存池: %d", getRecyclerCacheSize());
        Timber.tag(TAG).d("RecyclerView ViewHolder 缓存: %d", getRecyclerViewCacheSize());
        Timber.tag(TAG).d("==================================");
    }
    
    /**
     * 获取性能配置摘要（用于调试）
     * 
     * @return 配置摘要字符串
     */
    public static String getConfigSummary() {
        return String.format(
            "设备: %s | 内存: %dMB | 消息限制: %d | RecyclerView缓存: %d/%d",
            getPerformanceLevel(),
            totalMemoryMB,
            getMaxMessages(),
            getRecyclerCacheSize(),
            getRecyclerViewCacheSize()
        );
    }
}
