package com.fxzs.lingxiagent.view.chat;

import android.app.ActivityManager;
import android.content.Context;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import timber.log.Timber;

/**
 * 内存优化器 - 用于ChatAdapter的内存使用优化
 * 
 * 主要功能：
 * - 内存使用监控和预警
 * - 自动内存清理和垃圾回收
 * - ViewHolder内存池管理
 * - 弱引用管理，防止内存泄漏
 * 
 * 设计原则：
 * - 使用弱引用避免强引用链导致的内存泄漏
 * - 基于内存压力动态调整缓存策略
 * - 提供手动和自动清理机制
 * 
 * @author ChatAdapter Refactoring Team
 * @since 2024
 */
public class MemoryOptimizer {
    
    private static final String TAG = "MemoryOptimizer";
    
    // 内存阈值配置（以MB为单位）
    private static final long LOW_MEMORY_THRESHOLD = 50 * 1024 * 1024; // 50MB
    private static final long CRITICAL_MEMORY_THRESHOLD = 20 * 1024 * 1024; // 20MB
    
    // 单例实例
    private static volatile MemoryOptimizer instance;
    private static final Object lock = new Object();
    
    // 弱引用管理 - 避免强引用导致的内存泄漏
    private final ConcurrentLinkedQueue<WeakReference<ChatAdapter>> adaptersQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<String, WeakReference<Object>> weakReferenceCache = new ConcurrentHashMap<>();
    
    // 内存监控相关
    private ActivityManager activityManager;
    private Context applicationContext;
    
    private MemoryOptimizer() {
        // 私有构造函数，确保单例
    }
    
    /**
     * 获取MemoryOptimizer单例实例
     */
    public static MemoryOptimizer getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new MemoryOptimizer();
                    Timber.tag(TAG).d( "MemoryOptimizer instance created");
                }
            }
        }
        return instance;
    }
    
    /**
     * 初始化内存优化器
     * 
     * @param context 应用上下文
     */
    public void initialize(Context context) {
        if (context != null) {
            this.applicationContext = context.getApplicationContext();
            this.activityManager = (ActivityManager) applicationContext.getSystemService(Context.ACTIVITY_SERVICE);
            Timber.tag(TAG).d( "MemoryOptimizer initialized");
        }
    }
    
    /**
     * 注册ChatAdapter实例以便内存管理
     * 
     * @param adapter ChatAdapter实例
     */
    public void registerAdapter(ChatAdapter adapter) {
        if (adapter != null) {
            // 清理已失效的弱引用
            cleanupDeadReferences();
            
            // 添加新的弱引用
            adaptersQueue.offer(new WeakReference<>(adapter));
            Timber.tag(TAG).d( "Registered ChatAdapter for memory management");
        }
    }
    
    /**
     * 获取当前可用内存（字节）
     */
    public long getAvailableMemory() {
        if (activityManager != null) {
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memoryInfo);
            return memoryInfo.availMem;
        }
        return 0;
    }
    
    /**
     * 检查是否处于内存不足状态
     */
    public boolean isLowMemory() {
        return getAvailableMemory() < LOW_MEMORY_THRESHOLD;
    }
    
    /**
     * 检查是否处于内存严重不足状态
     */
    public boolean isCriticalMemory() {
        return getAvailableMemory() < CRITICAL_MEMORY_THRESHOLD;
    }
    
    /**
     * 获取内存使用信息字符串
     */
    public String getMemoryInfo() {
        if (activityManager != null) {
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memoryInfo);
            
            long availableMB = memoryInfo.availMem / (1024 * 1024);
            long totalMB = memoryInfo.totalMem / (1024 * 1024);
            long usedMB = totalMB - availableMB;
            
            return String.format("内存使用: %dMB / %dMB (%.1f%%)", 
                    usedMB, totalMB, (float) usedMB / totalMB * 100);
        }
        return "内存信息不可用";
    }
    
    /**
     * 执行内存优化
     * 
     * 基于当前内存状态执行不同级别的优化：
     * - 正常状态：清理无效引用
     * - 内存不足：执行轻度清理
     * - 内存严重不足：执行深度清理
     */
    public void optimizeMemory() {
        long availableMemory = getAvailableMemory();
        
        if (availableMemory < CRITICAL_MEMORY_THRESHOLD) {
            // 内存严重不足：执行深度清理
            Timber.tag(TAG).w( "Critical memory situation, performing deep cleanup");
            performDeepCleanup();
        } else if (availableMemory < LOW_MEMORY_THRESHOLD) {
            // 内存不足：执行轻度清理
            Timber.tag(TAG).i( "Low memory detected, performing light cleanup");
            performLightCleanup();
        } else {
            // 正常状态：常规清理
            Timber.tag(TAG).d( "Normal memory state, performing routine cleanup");
            performRoutineCleanup();
        }
    }
    
    /**
     * 常规清理 - 清理无效引用和过期缓存
     */
    private void performRoutineCleanup() {
        cleanupDeadReferences();
        cleanupWeakReferenceCache();
        Timber.tag(TAG).d( "Routine cleanup completed");
    }
    
    /**
     * 轻度清理 - 清理缓存并建议GC
     */
    private void performLightCleanup() {
        performRoutineCleanup();
        
        // 清理所有已注册的ChatAdapter缓存
        for (WeakReference<ChatAdapter> ref : adaptersQueue) {
            ChatAdapter adapter = ref.get();
            if (adapter != null) {
                // 清理ViewHolder缓存
                adapter.clearViewHolderCache();
            }
        }
        
        // 建议执行垃圾回收
        System.gc();
        Timber.tag(TAG).i( "Light cleanup completed");
    }
    
    /**
     * 深度清理 - 清理所有可能的缓存和资源
     */
    private void performDeepCleanup() {
        performLightCleanup();
        
        // 清理所有已注册的ChatAdapter的深度缓存
        for (WeakReference<ChatAdapter> ref : adaptersQueue) {
            ChatAdapter adapter = ref.get();
            if (adapter != null) {
                // 清理ViewHolderPoolManager缓存
                adapter.clearViewHolderCache();
                
                // 取消所有Markdown渲染
                adapter.cancelAllMarkdownRendering();
                
                // 优化ViewHolder缓存大小
                adapter.optimizeViewHolderCacheSizes();
            }
        }
        
        // 强制垃圾回收
        Runtime.getRuntime().gc();
        Timber.tag(TAG).w( "Deep cleanup completed");
    }
    
    /**
     * 清理失效的弱引用
     */
    private void cleanupDeadReferences() {
        adaptersQueue.removeIf(ref -> ref.get() == null);
        
        // 清理弱引用缓存中的失效引用
        weakReferenceCache.entrySet().removeIf(entry -> entry.getValue().get() == null);
    }
    
    /**
     * 清理弱引用缓存
     */
    private void cleanupWeakReferenceCache() {
        if (weakReferenceCache.size() > 100) { // 当缓存过多时清理
            weakReferenceCache.clear();
            Timber.tag(TAG).d( "Weak reference cache cleared due to size limit");
        }
    }
    
    /**
     * 添加对象到弱引用缓存
     * 
     * @param key 缓存键
     * @param object 要缓存的对象
     */
    public void addToWeakCache(String key, Object object) {
        if (key != null && object != null) {
            weakReferenceCache.put(key, new WeakReference<>(object));
        }
    }
    
    /**
     * 从弱引用缓存获取对象
     * 
     * @param key 缓存键
     * @return 缓存的对象，如果不存在或已被回收则返回null
     */
    public Object getFromWeakCache(String key) {
        WeakReference<Object> ref = weakReferenceCache.get(key);
        return ref != null ? ref.get() : null;
    }
    
    /**
     * 内存状态监听器接口
     */
    public interface MemoryStateListener {
        /**
         * 内存不足时回调
         */
        void onLowMemory();
        
        /**
         * 内存严重不足时回调
         */
        void onCriticalMemory();
    }
    
    // 内存状态监听器 - 使用弱引用避免内存泄漏
    private final ConcurrentLinkedQueue<WeakReference<MemoryStateListener>> listeners = new ConcurrentLinkedQueue<>();
    
    /**
     * 添加内存状态监听器
     * 
     * @param listener 监听器
     */
    public void addMemoryStateListener(MemoryStateListener listener) {
        if (listener != null) {
            listeners.offer(new WeakReference<>(listener));
        }
    }
    
    /**
     * 通知内存状态变化
     */
    private void notifyMemoryStateChange() {
        // 清理失效的监听器引用
        listeners.removeIf(ref -> ref.get() == null);
        
        if (isCriticalMemory()) {
            for (WeakReference<MemoryStateListener> ref : listeners) {
                MemoryStateListener listener = ref.get();
                if (listener != null) {
                    listener.onCriticalMemory();
                }
            }
        } else if (isLowMemory()) {
            for (WeakReference<MemoryStateListener> ref : listeners) {
                MemoryStateListener listener = ref.get();
                if (listener != null) {
                    listener.onLowMemory();
                }
            }
        }
    }
    
    /**
     * 获取内存优化统计信息
     */
    public String getOptimizationStats() {
        StringBuilder stats = new StringBuilder();
        stats.append("=== Memory Optimization Stats ===\n");
        stats.append(getMemoryInfo()).append("\n");
        stats.append("Registered Adapters: ").append(adaptersQueue.size()).append("\n");
        stats.append("Weak Cache Size: ").append(weakReferenceCache.size()).append("\n");
        stats.append("Memory State: ");
        
        if (isCriticalMemory()) {
            stats.append("CRITICAL");
        } else if (isLowMemory()) {
            stats.append("LOW");
        } else {
            stats.append("NORMAL");
        }
        
        stats.append("\n=== End Stats ===");
        return stats.toString();
    }
}