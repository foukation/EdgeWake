package com.fxzs.lingxiagent.view.chat;

import androidx.recyclerview.widget.RecyclerView;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import timber.log.Timber;

/**
 * ViewHolder 池管理器
 * 优化 RecyclerView 的 ViewHolder 缓存和复用机制
 */
public class ViewHolderPoolManager {
    
    private static final String TAG = "ViewHolderPoolManager";
    
    /**
     * 全局共享的 RecyclerView 池
     * 多个 ChatAdapter 实例可以共享同一个池，提高内存效率
     */
    private static volatile RecyclerView.RecycledViewPool sharedViewPool;
    
    /**
     * ViewHolder 类型的缓存大小配置
     */
    private static final ConcurrentHashMap<Integer, Integer> viewTypeCacheSize = new ConcurrentHashMap<>();
    
    /**
     * 统计各种类型 ViewHolder 的使用次数
     */
    private static final ConcurrentHashMap<Integer, AtomicInteger> viewTypeUsageCount = new ConcurrentHashMap<>();
    
    /**
     * 默认缓存大小配置
     */
    static {
        // 为常用的消息类型设置更大的缓存
        viewTypeCacheSize.put(ChatAdapter.TYPE_USER, 10);  // 用户消息较多
        viewTypeCacheSize.put(ChatAdapter.TYPE_AI, 15);    // AI消息最多
        viewTypeCacheSize.put(ChatAdapter.TYPE_AI_DRAWING, 5);  // 绘画消息中等
        viewTypeCacheSize.put(ChatAdapter.TYPE_USER_FILE, 8);   // 文件消息较多
        viewTypeCacheSize.put(ChatAdapter.TYPE_USER_FILE_IMAGE, 8);  // 图片消息较多
        
        // 头部消息类型缓存较小（通常只有一个）
        viewTypeCacheSize.put(ChatAdapter.TYPE_USER_HEAD_AGENT, 2);
        viewTypeCacheSize.put(ChatAdapter.TYPE_USER_HEAD_MEETING, 2);
        viewTypeCacheSize.put(ChatAdapter.TYPE_USER_HEAD_HOME, 2);
        
        // 卡片消息类型中等缓存
        viewTypeCacheSize.put(ChatAdapter.TYPE_ASSISTANT_CARD, 5);
        viewTypeCacheSize.put(ChatAdapter.TYPE_ASSISTANT_FOOD_CARD, 3);
        viewTypeCacheSize.put(ChatAdapter.TYPE_ASSISTANT_HOTEL_CARD, 3);
        viewTypeCacheSize.put(ChatAdapter.TYPE_ASSISTANT_PLANE_CARD, 3);
        viewTypeCacheSize.put(ChatAdapter.TYPE_ASSISTANT_PLAN_CARD, 3);
        viewTypeCacheSize.put(ChatAdapter.TYPE_ASSISTANT_TRAIN_CARD, 3);
        viewTypeCacheSize.put(ChatAdapter.TYPE_ASSISTANT_ORDER_CARD, 3);
        viewTypeCacheSize.put(ChatAdapter.TYPE_ASSISTANT_H5_CARD, 5);
        
        // 权限相关缓存较小
        viewTypeCacheSize.put(ChatAdapter.TYPE_ASSISTANT_FLOAT_PERM_CARD, 2);
        viewTypeCacheSize.put(ChatAdapter.TYPE_ASSISTANT_ACC_PERM_CARD, 2);
        
        // 其他消息类型默认缓存
        viewTypeCacheSize.put(ChatAdapter.TYPE_ASSISTANT_IMG, 5);
    }
    
    /**
     * 获取全局共享的 RecyclerView 池
     */
    public static RecyclerView.RecycledViewPool getSharedViewPool() {
        if (sharedViewPool == null) {
            synchronized (ViewHolderPoolManager.class) {
                if (sharedViewPool == null) {
                    sharedViewPool = new RecyclerView.RecycledViewPool();
                    configureViewPool(sharedViewPool);
                    Timber.tag(TAG).d( "Created shared RecycledViewPool with optimized cache sizes");
                }
            }
        }
        return sharedViewPool;
    }
    
    /**
     * 配置 ViewPool 的缓存大小
     */
    private static void configureViewPool(RecyclerView.RecycledViewPool pool) {
        for (ConcurrentHashMap.Entry<Integer, Integer> entry : viewTypeCacheSize.entrySet()) {
            int viewType = entry.getKey();
            int cacheSize = entry.getValue();
            pool.setMaxRecycledViews(viewType, cacheSize);
            Timber.tag(TAG).d( "Set cache size for viewType " + viewType + " to " + cacheSize);
        }
    }
    
    /**
     * 优化 RecyclerView 的缓存设置
     */
    public static void optimizeRecyclerView(RecyclerView recyclerView) {
        if (recyclerView == null) return;
        
        // 设置共享池
        recyclerView.setRecycledViewPool(getSharedViewPool());
        
        // 优化 ViewHolder 缓存设置
        recyclerView.setItemViewCacheSize(20); // 增加缓存大小，默认是2
        
        // 禁用嵌套滚动以提高性能
        recyclerView.setNestedScrollingEnabled(false);
        
        // 设置固定大小提高性能（如果高度固定）
        recyclerView.setHasFixedSize(false);
        
        // 禁用动画以减少卡顿（特别是流式更新时）
        recyclerView.setItemAnimator(null);
        
        Timber.tag(TAG).d( "Optimized RecyclerView cache settings");
    }
    
    /**
     * 记录 ViewHolder 使用情况（用于动态优化）
     */
    public static void recordViewHolderUsage(int viewType) {
        if (com.fxzs.lingxiagent.BuildConfig.DEBUG) {
            viewTypeUsageCount.computeIfAbsent(viewType, k -> new AtomicInteger(0)).incrementAndGet();
        }
    }
    
    /**
     * 打印 ViewHolder 使用统计（调试用）
     */
    public static void printUsageStats() {
        if (!com.fxzs.lingxiagent.BuildConfig.DEBUG) return;
        
        Timber.tag(TAG).d( "=== ViewHolder Usage Statistics ===");
        for (ConcurrentHashMap.Entry<Integer, AtomicInteger> entry : viewTypeUsageCount.entrySet()) {
            int viewType = entry.getKey();
            int usage = entry.getValue().get();
            int cacheSize = viewTypeCacheSize.getOrDefault(viewType, 2);
            Timber.tag(TAG).d( "ViewType " + viewType + ": used " + usage + " times, cache size " + cacheSize);
        }
        Timber.tag(TAG).d( "=== End Statistics ===");
    }
    
    /**
     * 动态调整缓存大小（基于使用统计）
     */
    public static void optimizeCacheSizes() {
        if (!com.fxzs.lingxiagent.BuildConfig.DEBUG) return;
        
        RecyclerView.RecycledViewPool pool = getSharedViewPool();
        
        for (ConcurrentHashMap.Entry<Integer, AtomicInteger> entry : viewTypeUsageCount.entrySet()) {
            int viewType = entry.getKey();
            int usage = entry.getValue().get();
            int currentCacheSize = viewTypeCacheSize.getOrDefault(viewType, 2);
            
            // 根据使用频率动态调整缓存大小
            int optimalCacheSize = calculateOptimalCacheSize(usage, currentCacheSize);
            
            if (optimalCacheSize != currentCacheSize) {
                viewTypeCacheSize.put(viewType, optimalCacheSize);
                pool.setMaxRecycledViews(viewType, optimalCacheSize);
                Timber.tag(TAG).d( "Adjusted cache size for viewType " + viewType + 
                          " from " + currentCacheSize + " to " + optimalCacheSize);
            }
        }
    }
    
    /**
     * 计算最优缓存大小
     */
    private static int calculateOptimalCacheSize(int usage, int currentSize) {
        // 简单的启发式算法
        if (usage > 50) {
            return Math.min(currentSize + 2, 20); // 使用频繁，增加缓存
        } else if (usage < 5) {
            return Math.max(currentSize - 1, 1); // 使用很少，减少缓存
        }
        return currentSize; // 保持当前大小
    }
    
    /**
     * 清理缓存（内存紧张时调用）
     */
    public static void clearCache() {
        if (sharedViewPool != null) {
            sharedViewPool.clear();
            Timber.tag(TAG).d( "Cleared shared ViewPool cache");
        }
        viewTypeUsageCount.clear();
    }
    
    /**
     * 获取当前缓存统计信息
     */
    public static String getCacheInfo() {
        StringBuilder info = new StringBuilder("ViewHolder Cache Info:\n");
        for (ConcurrentHashMap.Entry<Integer, Integer> entry : viewTypeCacheSize.entrySet()) {
            int viewType = entry.getKey();
            int cacheSize = entry.getValue();
            int usage = viewTypeUsageCount.getOrDefault(viewType, new AtomicInteger(0)).get();
            info.append("Type ").append(viewType).append(": cache=").append(cacheSize)
                .append(", used=").append(usage).append("\n");
        }
        return info.toString();
    }
}