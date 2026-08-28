package com.fxzs.lingxiagent.util.ZUtil;

import android.content.Context;
import android.graphics.Typeface;
import android.util.LruCache;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import timber.log.Timber;

/**
 * 字体缓存管理器
 * 用于优化字体加载性能，避免重复从assets加载字体文件
 */
public class FontCacheManager {
    private static final String TAG = "FontCacheManager";
    private static volatile FontCacheManager instance;

    // 使用LruCache提供更好的缓存策略
    private final LruCache<String, Typeface> fontCache;

    // 最大缓存数量，避免内存泄漏
    private static final int MAX_CACHE_SIZE = 10;

    // 后台线程池，用于异步加载字体
    private final ExecutorService executorService;

    private FontCacheManager() {
        // 初始化LRU缓存
        fontCache = new LruCache<String, Typeface>(MAX_CACHE_SIZE) {
            @Override
            protected void entryRemoved(boolean evicted, String key, Typeface oldValue, Typeface newValue) {
                if (evicted) {
                    Timber.tag(TAG).d( "Font evicted from cache: " + key);
                }
            }
        };

        // 初始化线程池
        executorService = Executors.newSingleThreadExecutor();
    }
    
    /**
     * 获取单例实例
     */
    public static FontCacheManager getInstance() {
        if (instance == null) {
            synchronized (FontCacheManager.class) {
                if (instance == null) {
                    instance = new FontCacheManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * 获取字体，如果缓存中不存在则从assets加载并缓存
     * @param context 上下文
     * @param fontPath 字体文件路径（相对于assets目录）
     * @return Typeface对象，如果加载失败返回默认字体
     */
    public Typeface getTypeface(Context context, String fontPath) {
        if (context == null || fontPath == null || fontPath.isEmpty()) {
            Timber.tag(TAG).w( "Invalid parameters for getTypeface");
            return Typeface.DEFAULT;
        }

        // 先从缓存中获取
        synchronized (fontCache) {
            Typeface cachedTypeface = fontCache.get(fontPath);
            if (cachedTypeface != null) {
                Timber.tag(TAG).d( "Font loaded from cache: " + fontPath);
                return cachedTypeface;
            }
        }

        // 缓存中不存在，从assets加载
        try {
            Timber.tag(TAG).d( "Loading font from assets: " + fontPath);
            Typeface typeface = Typeface.createFromAsset(context.getAssets(), fontPath);

            // 添加到缓存（LruCache会自动处理大小限制）
            synchronized (fontCache) {
                fontCache.put(fontPath, typeface);
            }
            Timber.tag(TAG).d( "Font cached successfully: " + fontPath);

            return typeface;
        } catch (Exception e) {
            Timber.tag(TAG).e( "Failed to load font from assets: " + fontPath, e);
            return Typeface.DEFAULT;
        }
    }
    
    /**
     * 预加载字体到缓存
     * @param context 上下文
     * @param fontPath 字体文件路径
     */
    public void preloadFont(Context context, String fontPath) {
        // 使用线程池在后台预加载字体
        executorService.execute(() -> {
            try {
                // 预加载前检查内存状态
                MemoryMonitor.logMemoryUsage(context, "Font Preload Start");

                getTypeface(context, fontPath);
                Timber.tag(TAG).d( "Font preloaded: " + fontPath);

                // 预加载后再次检查内存状态
                MemoryMonitor.logMemoryUsage(context, "Font Preload Complete");
            } catch (Exception e) {
                Timber.tag(TAG).e( "Failed to preload font: " + fontPath, e);
            }
        });
    }
    
    /**
     * 清理所有缓存
     */
    public void clearCache() {
        synchronized (fontCache) {
            fontCache.evictAll();
        }
        Timber.tag(TAG).d( "Font cache cleared");
    }

    /**
     * 获取缓存大小
     */
    public int getCacheSize() {
        synchronized (fontCache) {
            return fontCache.size();
        }
    }

    /**
     * 检查字体是否已缓存
     */
    public boolean isFontCached(String fontPath) {
        synchronized (fontCache) {
            return fontCache.get(fontPath) != null;
        }
    }

    /**
     * 移除特定字体的缓存
     */
    public void removeFont(String fontPath) {
        synchronized (fontCache) {
            Typeface removed = fontCache.remove(fontPath);
            if (removed != null) {
                Timber.tag(TAG).d( "Font removed from cache: " + fontPath);
            }
        }
    }

    /**
     * 释放资源
     */
    public void destroy() {
        clearCache();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            Timber.tag(TAG).d( "FontCacheManager destroyed");
        }
    }
}
