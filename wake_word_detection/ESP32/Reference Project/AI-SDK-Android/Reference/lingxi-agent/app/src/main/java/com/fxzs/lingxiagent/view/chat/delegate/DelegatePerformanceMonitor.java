package com.fxzs.lingxiagent.view.chat.delegate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import timber.log.Timber;

/**
 * 委托性能监控器
 * 用于监控和记录委托模式的性能指标
 */
public class DelegatePerformanceMonitor {
    
    private static final String TAG = "DelegatePerformanceMonitor";
    
    private static final boolean ENABLE_MONITORING = com.fxzs.lingxiagent.BuildConfig.DEBUG;
    
    /**
     * 统计每种委托类型的使用次数
     */
    private static final Map<String, AtomicLong> delegateUsageCount = new ConcurrentHashMap<>();
    
    /**
     * 统计每种委托类型的绑定时间
     */
    private static final Map<String, AtomicLong> delegateBindingTime = new ConcurrentHashMap<>();
    
    /**
     * 记录委托绑定开始时间
     */
    private static final ThreadLocal<Long> bindingStartTime = new ThreadLocal<>();
    
    /**
     * 记录委托开始绑定ViewHolder
     * 
     * @param delegateClass 委托类
     */
    public static void recordBindingStart(Class<? extends ViewTypeDelegate> delegateClass) {
        if (!ENABLE_MONITORING) return;
        
        bindingStartTime.set(System.nanoTime());
        
        // 增加使用次数计数
        String className = delegateClass.getSimpleName();
        delegateUsageCount.computeIfAbsent(className, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    /**
     * 记录委托完成绑定ViewHolder
     * 
     * @param delegateClass 委托类
     */
    public static void recordBindingEnd(Class<? extends ViewTypeDelegate> delegateClass) {
        if (!ENABLE_MONITORING) return;
        
        Long startTime = bindingStartTime.get();
        if (startTime != null) {
            long duration = System.nanoTime() - startTime;
            String className = delegateClass.getSimpleName();
            delegateBindingTime.computeIfAbsent(className, k -> new AtomicLong(0)).addAndGet(duration);
            bindingStartTime.remove();
        }
    }
    
    /**
     * 记录委托使用情况（用于简单计数）
     * 
     * @param delegateClass 委托类
     */
    public static void recordDelegateUsage(Class<? extends ViewTypeDelegate> delegateClass) {
        if (!ENABLE_MONITORING) return;
        
        String className = delegateClass.getSimpleName();
        delegateUsageCount.computeIfAbsent(className, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    /**
     * 打印性能统计信息
     */
    public static void printPerformanceStats() {
        if (!ENABLE_MONITORING) return;
        
        Timber.tag(TAG).d( "=== Delegate Performance Statistics ===");
        
        // 打印使用次数统计
        Timber.tag(TAG).d( "Usage Count:");
        for (Map.Entry<String, AtomicLong> entry : delegateUsageCount.entrySet()) {
            Timber.tag(TAG).d( "  " + entry.getKey() + ": " + entry.getValue().get() + " times");
        }
        
        // 打印平均绑定时间
        Timber.tag(TAG).d( "Average Binding Time:");
        for (Map.Entry<String, AtomicLong> entry : delegateBindingTime.entrySet()) {
            String className = entry.getKey();
            long totalTime = entry.getValue().get();
            long usageCount = delegateUsageCount.getOrDefault(className, new AtomicLong(0)).get();
            
            if (usageCount > 0) {
                long avgTimeNanos = totalTime / usageCount;
                double avgTimeMicros = avgTimeNanos / 1000.0;
                Timber.tag(TAG).d( "  " + className + ": " + String.format("%.2f", avgTimeMicros) + " μs");
            }
        }
        
        Timber.tag(TAG).d( "=== End Performance Statistics ===");
    }
    
    /**
     * 重置性能统计数据
     */
    public static void resetStats() {
        if (!ENABLE_MONITORING) return;
        
        delegateUsageCount.clear();
        delegateBindingTime.clear();
        Timber.tag(TAG).d( "Performance stats reset");
    }
    
    /**
     * 获取总的委托使用次数
     */
    public static long getTotalUsageCount() {
        if (!ENABLE_MONITORING) return 0;
        
        return delegateUsageCount.values().stream()
                .mapToLong(AtomicLong::get)
                .sum();
    }
    
    /**
     * 获取特定委托的使用次数
     */
    public static long getDelegateUsageCount(Class<? extends ViewTypeDelegate> delegateClass) {
        if (!ENABLE_MONITORING) return 0;
        
        String className = delegateClass.getSimpleName();
        AtomicLong count = delegateUsageCount.get(className);
        return count != null ? count.get() : 0;
    }
    
    /**
     * 检查是否启用了监控
     */
    public static boolean isMonitoringEnabled() {
        return ENABLE_MONITORING;
    }
}