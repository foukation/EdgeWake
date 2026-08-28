package com.fxzs.lingxiagent.util;

import android.content.Context;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import timber.log.Timber;

/**
 * 性能优化器
 * 提供各种性能优化功能
 */
public class PerformanceOptimizer {
    
    private static final String TAG = "PerformanceOptimizer";
    
    /**
     * 优化RecyclerView性能
     */
    public static void optimizeRecyclerView(RecyclerView recyclerView) {
        try {
            if (recyclerView != null) {
                // 设置固定大小以提高性能
                recyclerView.setHasFixedSize(true);
                
                // 设置项目动画器为null以减少动画开销
                recyclerView.setItemAnimator(null);
                
                // 设置嵌套滚动为false以提高性能
                recyclerView.setNestedScrollingEnabled(false);
                
                Timber.tag(TAG).d( "RecyclerView性能优化完成");
            }
        } catch (Exception e) {
            Timber.tag(TAG).w( "RecyclerView性能优化失败"+ e);
        }
    }
    
    /**
     * 加载幻灯片图片
     */
    public static void loadSlideImage(ImageView imageView, String imageUrl) {
        try {
            if (imageView != null && imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(imageView.getContext())
                    .load(imageUrl)
                    .centerCrop()
                    .into(imageView);
                
                Timber.tag(TAG).d( "幻灯片图片加载: " + imageUrl);
            }
        } catch (Exception e) {
            Timber.tag(TAG).w( "幻灯片图片加载失败: " + imageUrl, e);
        }
    }
    
    /**
     * 清理图片内存缓存
     */
    public static void clearImageMemoryCache(Context context) {
        try {
            if (context != null) {
                Glide.get(context).clearMemory();
                Timber.tag(TAG).d( "图片内存缓存已清理");
            }
        } catch (Exception e) {
            Timber.tag(TAG).w( "清理图片内存缓存失败", e);
        }
    }
    
    /**
     * 预加载图片
     */
    public static void preloadImage(Context context, String imageUrl) {
        try {
            if (context != null && imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(context)
                    .load(imageUrl)
                    .preload();
                
                Timber.tag(TAG).d( "图片预加载: " + imageUrl);
            }
        } catch (Exception e) {
            Timber.tag(TAG).w( "图片预加载失败: " + imageUrl, e);
        }
    }
    
    /**
     * 优化内存使用
     */
    public static void optimizeMemoryUsage(Context context) {
        try {
            // 建议垃圾回收
            System.gc();
            
            // 清理图片缓存
            clearImageMemoryCache(context);
            
            Timber.tag(TAG).d( "内存使用优化完成");
        } catch (Exception e) {
            Timber.tag(TAG).w( "内存使用优化失败", e);
        }
    }
}
