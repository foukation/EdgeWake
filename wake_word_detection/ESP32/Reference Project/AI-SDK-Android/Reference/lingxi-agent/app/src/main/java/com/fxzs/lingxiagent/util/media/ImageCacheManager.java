package com.fxzs.lingxiagent.util.media;

import android.content.Context;
import android.content.SharedPreferences;

import com.fxzs.lingxiagent.util.ZUtil.ImageUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import timber.log.Timber;

/**
 * 图片缓存管理器
 * 负责管理图片缓存策略、预加载和缓存清理
 */
public class ImageCacheManager {
    
    private static final String TAG = "ImageCacheManager";
    private static final String PREFS_NAME = "image_cache_prefs";
    private static final String KEY_CACHE_SIZE_LIMIT = "cache_size_limit";
    private static final String KEY_PRELOAD_ENABLED = "preload_enabled";
    private static final String KEY_LAST_CACHE_CLEAN = "last_cache_clean";
    
    // 默认缓存大小限制：100MB
    private static final long DEFAULT_CACHE_SIZE_LIMIT = 100 * 1024 * 1024;
    // 缓存清理间隔：7天
    private static final long CACHE_CLEAN_INTERVAL = 7 * 24 * 60 * 60 * 1000;
    
    private Context context;
    private SharedPreferences preferences;
    private ExecutorService executorService;
    
    public ImageCacheManager(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.executorService = Executors.newSingleThreadExecutor();
        
        // 检查是否需要清理缓存
        checkAndCleanCacheIfNeeded();
    }
    
    /**
     * 预加载图片列表
     * @param imageUrls 图片URL列表
     */
    public void preloadImages(String... imageUrls) {
        if (!isPreloadEnabled() || imageUrls == null || imageUrls.length == 0) {
            return;
        }
        
        executorService.execute(() -> {
            for (String imageUrl : imageUrls) {
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    try {
                        Timber.tag(TAG).d( "Preloading image: " + imageUrl);
                        ImageUtil.preloadImage(context, imageUrl);
                        
                        // 添加小延迟避免过度占用资源
                        Thread.sleep(100);
                    } catch (Exception e) {
                        Timber.tag(TAG).w( "Failed to preload image: " + imageUrl, e);
                    }
                }
            }
        });
    }
    
    /**
     * 智能预加载：根据用户行为预测需要预加载的图片
     * @param currentImageUrl 当前查看的图片
     * @param relatedImageUrls 相关图片列表
     */
    public void smartPreload(String currentImageUrl, String[] relatedImageUrls) {
        if (!isPreloadEnabled() || relatedImageUrls == null) {
            return;
        }
        
        executorService.execute(() -> {
            // 预加载相关图片（通常是聊天中的下一张图片）
            int preloadCount = Math.min(3, relatedImageUrls.length); // 最多预加载3张
            
            for (int i = 0; i < preloadCount; i++) {
                String imageUrl = relatedImageUrls[i];
                if (imageUrl != null && !imageUrl.equals(currentImageUrl)) {
                    try {
                        Timber.tag(TAG).d( "Smart preloading image: " + imageUrl);
                        ImageUtil.preloadImage(context, imageUrl);
                        Thread.sleep(200); // 稍长的延迟
                    } catch (Exception e) {
                        Timber.tag(TAG).w( "Failed to smart preload image: " + imageUrl, e);
                    }
                }
            }
        });
    }
    
    /**
     * 获取缓存大小
     * @param callback 回调接口
     */
    public void getCacheSize(ImageUtil.CacheSizeCallback callback) {
        ImageUtil.getCacheSize(context, callback);
    }
    
    /**
     * 清理缓存
     * @param callback 清理完成回调
     */
    public void clearCache(ClearCacheCallback callback) {
        executorService.execute(() -> {
            try {
                Timber.tag(TAG).d( "Starting cache cleanup");
                ImageUtil.clearImageCache(context);
                
                // 更新最后清理时间
                preferences.edit()
                        .putLong(KEY_LAST_CACHE_CLEAN, System.currentTimeMillis())
                        .apply();
                
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        callback.onCacheCleared(true);
                    });
                }
                
                Timber.tag(TAG).d( "Cache cleanup completed");
            } catch (Exception e) {
                Timber.tag(TAG).e( "Failed to clear cache", e);
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        callback.onCacheCleared(false);
                    });
                }
            }
        });
    }
    
    /**
     * 检查缓存大小并在必要时清理
     */
    public void checkCacheSizeAndCleanIfNeeded() {
        getCacheSize(sizeInBytes -> {
            long sizeLimit = getCacheSizeLimit();
            
            Timber.tag(TAG).d( "Current cache size: " + ImageUtil.formatCacheSize(sizeInBytes) + 
                      ", limit: " + ImageUtil.formatCacheSize(sizeLimit));
            
            if (sizeInBytes > sizeLimit) {
                Timber.tag(TAG).i( "Cache size exceeded limit, cleaning cache");
                clearCache(success -> {
                    if (success) {
                        Timber.tag(TAG).i( "Cache cleaned due to size limit");
                    } else {
                        Timber.tag(TAG).w( "Failed to clean cache");
                    }
                });
            }
        });
    }
    
    /**
     * 检查是否需要定期清理缓存
     */
    private void checkAndCleanCacheIfNeeded() {
        long lastClean = preferences.getLong(KEY_LAST_CACHE_CLEAN, 0);
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - lastClean > CACHE_CLEAN_INTERVAL) {
            Timber.tag(TAG).i( "Performing scheduled cache cleanup");
            clearCache(success -> {
                if (success) {
                    Timber.tag(TAG).i( "Scheduled cache cleanup completed");
                }
            });
        }
    }
    
    /**
     * 获取缓存大小限制
     * @return 缓存大小限制（字节）
     */
    public long getCacheSizeLimit() {
        return preferences.getLong(KEY_CACHE_SIZE_LIMIT, DEFAULT_CACHE_SIZE_LIMIT);
    }
    
    /**
     * 设置缓存大小限制
     * @param sizeInBytes 缓存大小限制（字节）
     */
    public void setCacheSizeLimit(long sizeInBytes) {
        preferences.edit()
                .putLong(KEY_CACHE_SIZE_LIMIT, sizeInBytes)
                .apply();
        
        Timber.tag(TAG).d( "Cache size limit set to: " + ImageUtil.formatCacheSize(sizeInBytes));
    }
    
    /**
     * 是否启用预加载
     * @return true如果启用预加载
     */
    public boolean isPreloadEnabled() {
        return preferences.getBoolean(KEY_PRELOAD_ENABLED, true);
    }
    
    /**
     * 设置是否启用预加载
     * @param enabled 是否启用
     */
    public void setPreloadEnabled(boolean enabled) {
        preferences.edit()
                .putBoolean(KEY_PRELOAD_ENABLED, enabled)
                .apply();
        
        Timber.tag(TAG).d( "Preload " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * 获取缓存统计信息
     * @param callback 回调接口
     */
    public void getCacheStats(CacheStatsCallback callback) {
        if (callback == null) {
            return;
        }
        
        getCacheSize(sizeInBytes -> {
            CacheStats stats = new CacheStats();
            stats.currentSize = sizeInBytes;
            stats.sizeLimit = getCacheSizeLimit();
            stats.preloadEnabled = isPreloadEnabled();
            stats.lastCleanTime = preferences.getLong(KEY_LAST_CACHE_CLEAN, 0);
            stats.usagePercentage = (stats.sizeLimit > 0) ? 
                    (int) ((stats.currentSize * 100) / stats.sizeLimit) : 0;
            
            callback.onCacheStatsReady(stats);
        });
    }
    
    /**
     * 优化缓存设置（根据设备性能自动调整）
     */
    public void optimizeCacheSettings() {
        executorService.execute(() -> {
            try {
                // 获取设备内存信息
                android.app.ActivityManager activityManager = 
                        (android.app.ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                android.app.ActivityManager.MemoryInfo memoryInfo = 
                        new android.app.ActivityManager.MemoryInfo();
                activityManager.getMemoryInfo(memoryInfo);
                
                // 根据可用内存调整缓存大小
                long availableMemory = memoryInfo.availMem;
                long totalMemory = memoryInfo.totalMem;
                
                long recommendedCacheSize;
                if (totalMemory > 4L * 1024 * 1024 * 1024) { // 4GB以上
                    recommendedCacheSize = 200 * 1024 * 1024; // 200MB
                } else if (totalMemory > 2L * 1024 * 1024 * 1024) { // 2-4GB
                    recommendedCacheSize = 100 * 1024 * 1024; // 100MB
                } else { // 2GB以下
                    recommendedCacheSize = 50 * 1024 * 1024; // 50MB
                }
                
                // 如果当前设置是默认值，则更新为推荐值
                if (getCacheSizeLimit() == DEFAULT_CACHE_SIZE_LIMIT) {
                    setCacheSizeLimit(recommendedCacheSize);
                    Timber.tag(TAG).i( "Cache size optimized to: " + ImageUtil.formatCacheSize(recommendedCacheSize));
                }
                
                // 低内存设备禁用预加载
                if (availableMemory < 500 * 1024 * 1024) { // 可用内存小于500MB
                    setPreloadEnabled(false);
                    Timber.tag(TAG).i( "Preload disabled due to low memory");
                }
                
            } catch (Exception e) {
                Timber.tag(TAG).e( "Failed to optimize cache settings", e);
            }
        });
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
    
    /**
     * 缓存清理回调接口
     */
    public interface ClearCacheCallback {
        void onCacheCleared(boolean success);
    }
    
    /**
     * 缓存统计回调接口
     */
    public interface CacheStatsCallback {
        void onCacheStatsReady(CacheStats stats);
    }
    
    /**
     * 缓存统计信息
     */
    public static class CacheStats {
        public long currentSize;
        public long sizeLimit;
        public boolean preloadEnabled;
        public long lastCleanTime;
        public int usagePercentage;
        
        public String getCurrentSizeFormatted() {
            return ImageUtil.formatCacheSize(currentSize);
        }
        
        public String getSizeLimitFormatted() {
            return ImageUtil.formatCacheSize(sizeLimit);
        }
        
        public boolean isNearLimit() {
            return usagePercentage > 80;
        }
        
        public boolean isOverLimit() {
            return currentSize > sizeLimit;
        }
    }
}