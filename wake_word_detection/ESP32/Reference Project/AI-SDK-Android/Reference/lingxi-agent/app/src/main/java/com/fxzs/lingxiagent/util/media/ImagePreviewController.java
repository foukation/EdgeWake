package com.fxzs.lingxiagent.util.media;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ProgressBar;

import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;

import timber.log.Timber;

/**
 * 图片预览控制器
 * 处理图片加载、缩放、分享等逻辑
 */
public class ImagePreviewController {
    
    private static final String TAG = "ImagePreviewController";
    
    private PhotoView photoView;
    private ProgressBar loadingIndicator;
    private Context context;
    
    /**
     * 加载回调接口
     */
    public interface LoadCallback {
        void onLoadSuccess();
        void onLoadFailed(String errorMessage);
    }
    
    public ImagePreviewController(PhotoView photoView, ProgressBar loadingIndicator) {
        this.photoView = photoView;
        this.loadingIndicator = loadingIndicator;
        this.context = photoView.getContext();
        
        setupZoomControls();
    }
    
    /**
     * 加载图片
     * @param imageUrl 图片URL
     * @param callback 加载回调
     */
    public void loadImage(String imageUrl, LoadCallback callback) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            if (callback != null) {
                callback.onLoadFailed("图片地址为空");
            }
            return;
        }
        
        Timber.tag(TAG).d( "Loading image: " + imageUrl);
        
        // 显示加载状态
        showLoadingState();
        
        // 使用优化的图片加载方法
        loadImageWithOptimization(imageUrl, callback);
    }
    
    /**
     * 使用优化策略加载图片
     * @param imageUrl 图片URL
     * @param callback 加载回调
     */
    private void loadImageWithOptimization(String imageUrl, LoadCallback callback) {
        // 检查是否为大图片URL（通过URL特征或文件大小判断）
        if (isLargeImageUrl(imageUrl)) {
            Timber.tag(TAG).d( "Detected large image, using optimized loading: " + imageUrl);
            loadLargeImageInternal(imageUrl, callback);
            return;
        }
        
        // 首先尝试优化加载
        com.fxzs.lingxiagent.util.ZUtil.ImageUtil.loadNetworkImageOptimized(
            context, 
            imageUrl, 
            photoView, 
            new com.fxzs.lingxiagent.util.ZUtil.ImageUtil.LoadCallback() {
                @Override
                public void onLoadSuccess() {
                    Timber.tag(TAG).d( "Image loaded successfully with optimization: " + imageUrl);
                    hideLoadingState();
                    if (callback != null) {
                        callback.onLoadSuccess();
                    }
                }
                
                @Override
                public void onLoadFailed() {
                    Timber.tag(TAG).w("Optimized load failed, trying with retry: " + imageUrl);
                    // 如果优化加载失败，尝试带重试的加载
                    loadImageWithRetry(imageUrl, callback, 3);
                }
            }
        );
    }
    
    /**
     * 判断是否为大图片URL
     * @param imageUrl 图片URL
     * @return true如果可能是大图片
     */
    private boolean isLargeImageUrl(String imageUrl) {
        if (imageUrl == null) {
            return false;
        }
        
        // 通过URL特征判断（可以根据实际情况调整）
        String lowerUrl = imageUrl.toLowerCase();
        
        // 检查是否包含高分辨率标识
        if (lowerUrl.contains("hd") || lowerUrl.contains("high") || 
            lowerUrl.contains("large") || lowerUrl.contains("original") ||
            lowerUrl.contains("full")) {
            return true;
        }
        
        // 检查文件扩展名（某些格式通常较大）
        if (lowerUrl.endsWith(".tiff") || lowerUrl.endsWith(".tif") ||
            lowerUrl.endsWith(".bmp") || lowerUrl.endsWith(".raw")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 内部大图片加载方法
     * @param imageUrl 图片URL
     * @param callback 加载回调
     */
    private void loadLargeImageInternal(String imageUrl, LoadCallback callback) {
        // 获取屏幕尺寸作为最大尺寸限制
        android.util.DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int maxWidth = metrics.widthPixels;
        int maxHeight = metrics.heightPixels;
        
        com.fxzs.lingxiagent.util.ZUtil.ImageUtil.loadLargeImageOptimized(
            context,
            imageUrl,
            photoView,
            maxWidth,
            maxHeight,
            new com.fxzs.lingxiagent.util.ZUtil.ImageUtil.LoadCallback() {
                @Override
                public void onLoadSuccess() {
                    Timber.tag(TAG).d( "Large image loaded successfully: " + imageUrl);
                    hideLoadingState();
                    if (callback != null) {
                        callback.onLoadSuccess();
                    }
                }
                
                @Override
                public void onLoadFailed() {
                    Timber.tag(TAG).w( "Large image load failed, trying with retry: " + imageUrl);
                    // 如果大图片加载失败，尝试普通重试加载
                    loadImageWithRetry(imageUrl, callback, 2); // 大图片重试次数少一些
                }
            }
        );
    }
    
    /**
     * 带重试机制的图片加载
     * @param imageUrl 图片URL
     * @param callback 加载回调
     * @param maxRetries 最大重试次数
     */
    private void loadImageWithRetry(String imageUrl, LoadCallback callback, int maxRetries) {
        com.fxzs.lingxiagent.util.ZUtil.ImageUtil.loadImageWithRetry(
            context,
            imageUrl,
            photoView,
            new com.fxzs.lingxiagent.util.ZUtil.ImageUtil.LoadCallback() {
                @Override
                public void onLoadSuccess() {
                    Timber.tag(TAG).d( "Image loaded successfully with retry: " + imageUrl);
                    hideLoadingState();
                    if (callback != null) {
                        callback.onLoadSuccess();
                    }
                }
                
                @Override
                public void onLoadFailed() {
                    Timber.tag(TAG).e( "Image load failed after retries: " + imageUrl);
                    hideLoadingState();
                    if (callback != null) {
                        callback.onLoadFailed("图片加载失败，请检查网络连接");
                    }
                }
            },
            maxRetries
        );
    }
    
    /**
     * 加载大图片（防止OOM）
     * @param imageUrl 图片URL
     * @param callback 加载回调
     */
    public void loadLargeImage(String imageUrl, LoadCallback callback) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            if (callback != null) {
                callback.onLoadFailed("图片地址为空");
            }
            return;
        }
        
        Timber.tag(TAG).d( "Loading large image: " + imageUrl);
        showLoadingState();
        
        // 获取屏幕尺寸作为最大尺寸限制
        android.util.DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int maxWidth = metrics.widthPixels;
        int maxHeight = metrics.heightPixels;
        
        com.fxzs.lingxiagent.util.ZUtil.ImageUtil.loadLargeImageOptimized(
            context,
            imageUrl,
            photoView,
            maxWidth,
            maxHeight,
            new com.fxzs.lingxiagent.util.ZUtil.ImageUtil.LoadCallback() {
                @Override
                public void onLoadSuccess() {
                    Timber.tag(TAG).d( "Large image loaded successfully: " + imageUrl);
                    hideLoadingState();
                    if (callback != null) {
                        callback.onLoadSuccess();
                    }
                }
                
                @Override
                public void onLoadFailed() {
                    Timber.tag(TAG).e( "Large image load failed: " + imageUrl);
                    hideLoadingState();
                    if (callback != null) {
                        callback.onLoadFailed("大图片加载失败");
                    }
                }
            }
        );
    }
    
    /**
     * 设置缩放控制
     */
    public void setupZoomControls() {
        if (photoView == null) {
            return;
        }
        
        // 设置最小和最大缩放比例
        photoView.setMinimumScale(0.5f);
        photoView.setMaximumScale(5.0f);
        photoView.setMediumScale(2.0f);
        
        // 启用缩放
        photoView.setZoomable(true);
        
        // 设置缩放类型
        photoView.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        
        // 允许父视图拦截触摸事件（用于滑动关闭等手势）
        photoView.setAllowParentInterceptOnEdge(true);
    }
    
    /**
     * 分享图片
     * @param imageUrl 图片URL
     * @param imageName 图片名称
     */
    public void shareImage(String imageUrl, String imageName) {
        if (context == null) {
            Timber.tag(TAG).e( "Context is null, cannot share image");
            return;
        }
        
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            
            String shareText = "分享图片";
            if (imageName != null && !imageName.isEmpty()) {
                shareText = "分享图片：" + imageName;
            }
            
            if (imageUrl != null && !imageUrl.isEmpty()) {
                shareText += "\n" + imageUrl;
            }
            
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, imageName != null ? imageName : "图片分享");
            
            Intent chooserIntent = Intent.createChooser(shareIntent, "分享图片");
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            context.startActivity(chooserIntent);
            
        } catch (Exception e) {
            Timber.tag(TAG).e( "Failed to share image", e);
            // TODO: 显示分享失败的提示
        }
    }
    
    /**
     * 预加载图片
     * @param imageUrl 图片URL
     */
    public void preloadImage(String imageUrl) {
        if (context == null || imageUrl == null || imageUrl.isEmpty()) {
            return;
        }
        
        try {
            Glide.with(context)
                    .load(imageUrl)
                    .preload();
        } catch (Exception e) {
            Timber.tag(TAG).e( "Failed to preload image: " + imageUrl, e);
        }
    }
    
    /**
     * 显示加载状态
     */
    public void showLoadingState() {
        if (loadingIndicator != null) {
            loadingIndicator.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * 隐藏加载状态
     */
    public void hideLoadingState() {
        if (loadingIndicator != null) {
            loadingIndicator.setVisibility(View.GONE);
        }
    }
    
    /**
     * 显示错误状态
     */
    public void showErrorState() {
        hideLoadingState();
        // 错误状态的显示由Activity处理
    }
    
    /**
     * 重置PhotoView状态
     */
    public void resetPhotoView() {
        if (photoView != null) {
            photoView.setScale(1.0f, true);
        }
    }
    
    /**
     * 获取当前缩放比例
     * @return 当前缩放比例
     */
    public float getCurrentScale() {
        return photoView != null ? photoView.getScale() : 1.0f;
    }
    
    /**
     * 设置缩放比例
     * @param scale 缩放比例
     * @param animate 是否使用动画
     */
    public void setScale(float scale, boolean animate) {
        if (photoView != null) {
            photoView.setScale(scale, animate);
        }
    }
    
    /**
     * 检查内存状态并优化
     */
    public void checkMemoryAndOptimize() {
        try {
            android.app.ActivityManager activityManager = 
                    (android.app.ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            android.app.ActivityManager.MemoryInfo memoryInfo = 
                    new android.app.ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memoryInfo);
            
            // 如果可用内存低于阈值，清理Glide内存缓存
            long availableMemory = memoryInfo.availMem;
            long threshold = memoryInfo.threshold;
            
            if (availableMemory < threshold * 1.5) {
                Timber.tag(TAG).w("Low memory detected, clearing Glide memory cache");
                Glide.get(context).clearMemory();
            }
            
        } catch (Exception e) {
            Timber.tag(TAG).e( "Failed to check memory status", e);
        }
    }
    
    /**
     * 暂停图片加载（用于Activity暂停时）
     */
    public void pauseImageLoading() {
        try {
            if (context != null) {
                Glide.with(context).pauseRequests();
                Timber.tag(TAG).d( "Image loading paused");
            }
        } catch (Exception e) {
            Timber.tag(TAG).e( "Failed to pause image loading", e);
        }
    }
    
    /**
     * 恢复图片加载（用于Activity恢复时）
     */
    public void resumeImageLoading() {
        try {
            if (context != null) {
                Glide.with(context).resumeRequests();
                Timber.tag(TAG).d( "Image loading resumed");
            }
        } catch (Exception e) {
            Timber.tag(TAG).e( "Failed to resume image loading", e);
        }
    }
    
    /**
     * 获取内存使用情况
     * @return 内存使用信息字符串
     */
    public String getMemoryUsageInfo() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();
            
            return String.format("Memory: Used=%s, Free=%s, Total=%s, Max=%s",
                    formatBytes(usedMemory),
                    formatBytes(freeMemory),
                    formatBytes(totalMemory),
                    formatBytes(maxMemory));
        } catch (Exception e) {
            Timber.tag(TAG).e( "Failed to get memory usage info", e);
            return "Memory info unavailable";
        }
    }
    
    /**
     * 格式化字节数显示
     * @param bytes 字节数
     * @return 格式化的字符串
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + "B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1fKB", bytes / 1024.0);
        } else {
            return String.format("%.1fMB", bytes / (1024.0 * 1024.0));
        }
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        if (context != null && photoView != null) {
            try {
                Glide.with(context).clear(photoView);
            } catch (Exception e) {
                Timber.tag(TAG).e( "Failed to clear Glide target", e);
            }
        }
        
        photoView = null;
        loadingIndicator = null;
        context = null;
    }
}