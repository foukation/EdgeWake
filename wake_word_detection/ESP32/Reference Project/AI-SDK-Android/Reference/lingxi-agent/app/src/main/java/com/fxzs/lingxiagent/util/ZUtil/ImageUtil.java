package com.fxzs.lingxiagent.util.ZUtil;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.widget.ImageView;

import androidx.annotation.RequiresApi;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.fxzs.lingxiagent.R;
import com.fxzs.lingxiagent.util.NetworkUtils;
import com.fxzs.lingxiagent.util.ZUtils;

import timber.log.Timber;

public class ImageUtil {

    public static String TAG = "ImageUtil";

    public static void load(Context context, int resID, ImageView imageView) {
        Glide.with(context) // 使用 Activity 或 Fragment 的 Context
                .load(resID)
                .into(imageView);
    }

    public static void loadRadius(Context context, int resID, ImageView imageView) {
        RequestOptions options = new RequestOptions()
                .transform(new CenterCrop(), new RoundedCorners(16));
        Glide.with(context) // 使用 Activity 或 Fragment 的 Context
                .load(resID)
                .apply(options)
                .into(imageView);
    }

    public static void net(Context context, String imageUrl, ImageView imageView) {
        if (!NetworkUtils.isNetworkAvailable(context)) {
            imageView.setImageResource(R.drawable.default_error_img);
            return;
        }
        Glide.with(context) // 使用 Activity 或 Fragment 的 Context
                .load(imageUrl)
                .error(R.drawable.default_error_img)
                .into(imageView);
    }

    public static void loadUriRadius(Context context, Uri imageUrl, ImageView imageView) {
        RequestOptions options = new RequestOptions()
                .transform(new CenterCrop(), new RoundedCorners(16));
        Glide.with(context) // 使用 Activity 或 Fragment 的 Context
                .load(imageUrl)
                .apply(options)
                .into(imageView);
    }

    public static void netRadius(Context context, String imageUrl, ImageView imageView) {
//        Timber.tag(TAG).e("imageUrl = "+imageUrl);

        RequestOptions options = new RequestOptions()
                .transform(new CenterCrop(), new RoundedCorners(16));

//        GlideUrl path = new GlideUrl(imageUrl, new LazyHeaders.Builder()
//                .addHeader("device-type", "android")
////                .addHeader("Cookie", setCookie!!)
//        .build());
        if (!NetworkUtils.isNetworkAvailable(context)) {
            imageView.setImageResource(R.drawable.default_error_img);
            return;
        }
        Glide.with(context) // 使用 Activity 或 Fragment 的 Context
                .load(imageUrl)
                .apply(options)
                .error(R.drawable.default_error_img)
                .into(imageView);
    }
    public static void netRadiusXY(Context context, String imageUrl, ImageView imageView,int targetWidth,int targetHeight) {
//        Timber.tag(TAG).e("imageUrl = "+imageUrl);

        RequestOptions options = new RequestOptions()
                .transform(new CenterCrop(), new RoundedCorners(16));

//        GlideUrl path = new GlideUrl(imageUrl, new LazyHeaders.Builder()
//                .addHeader("device-type", "android")
////                .addHeader("Cookie", setCookie!!)
//        .build());
        if (!NetworkUtils.isNetworkAvailable(context)) {
            imageView.setImageResource(R.drawable.default_error_img);
            return;
        }
        Glide.with(context) // 使用 Activity 或 Fragment 的 Context
                .load(imageUrl)
                .override(targetWidth, targetHeight)
                .apply(options)
                .error(R.drawable.default_error_img)
                .into(imageView);

        // 加载图片
//        Glide.with(context)
//                .load(data.getImageUrl())
//                .override(targetWidth, targetHeight)
//                .centerCrop()
////                                .placeholder(R.drawable.placeholder_image) // 添加占位图
//                .into(iv_content);
    }

    public static void netCircle(Context context, String imageUrl, ImageView imageView) {
        Timber.tag(TAG).e( "imageUrl = " + imageUrl);

        RequestOptions options = new RequestOptions()
                .transform(new CenterCrop(), new CircleCrop());

//        GlideUrl path = new GlideUrl(imageUrl, new LazyHeaders.Builder()
//                .addHeader("device-type", "android")
////                .addHeader("Cookie", setCookie!!)
//        .build());
        if (!NetworkUtils.isNetworkAvailable(context)) {
            imageView.setImageResource(R.drawable.default_error_img);
            return;
        }
        Glide.with(context) // 使用 Activity 或 Fragment 的 Context
                .load(imageUrl)
                .apply(options)
                .error(R.drawable.default_error_img)
                .into(imageView);
    }

    public static void loadGif(Context context, int resId, ImageView imageView) {
        Glide.with(context)
                .asGif()
                .load(resId)
                .into(imageView);
    }

    public static void loadGifXY(Context context, int resId, ImageView imageView,int targetWidth,int targetHeight) {
        RequestOptions options = new RequestOptions()
                .transform(new RoundedCorners(16))
                .override(targetWidth, targetHeight);
        Glide.with(context)
                .asGif()
                .apply(options)
                .load(resId)
                .into(imageView);
    }

    public static void downloadImageByUrl(Context context, String url) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "downloaded_image.jpg");
        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        long downloadId = manager.enqueue(request);
            registerDownloadReceiver(context, downloadId);
    }

    private static void registerDownloadReceiver(Context context, long downloadId) {
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);

                if (id == downloadId) {
                    ctx.unregisterReceiver(this);
                    ZUtils.showToast("图片下载成功");
                }
            }
        };

        context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }
    
    /**
     * 加载回调接口
     */
    public interface LoadCallback {
        void onLoadSuccess();
        void onLoadFailed();
    }
    
    /**
     * 加载图片并提供回调
     * @param context 上下文
     * @param imageUrl 图片URL
     * @param imageView ImageView
     * @param callback 加载回调
     */
    public static void loadImageWithCallback(Context context, String imageUrl, ImageView imageView, LoadCallback callback) {
        if (!NetworkUtils.isNetworkAvailable(context)) {
            imageView.setImageResource(R.drawable.default_error_img);
            if (callback != null) {
                callback.onLoadFailed();
            }
            return;
        }
        Glide.with(context)
                .load(imageUrl)
                .error(R.drawable.default_error_img)
                .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                    @Override
                    public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException e, Object model, 
                                              com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, 
                                              boolean isFirstResource) {
                        if (callback != null) {
                            callback.onLoadFailed();
                        }
                        return false;
                    }
                    
                    @Override
                    public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, 
                                                 com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, 
                                                 com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                        if (callback != null) {
                            callback.onLoadSuccess();
                        }
                        return false;
                    }
                })
                .into(imageView);
    }
    
    /**
     * 预加载图片
     * @param context 上下文
     * @param imageUrl 图片URL
     */
    public static void preloadImage(Context context, String imageUrl) {
        try {
            Glide.with(context)
                    .load(imageUrl)
                    .preload();
        } catch (Exception e) {
            Timber.tag(TAG).e( "Failed to preload image: " + imageUrl, e);
        }
    }
    
    /**
     * 带重试机制的图片加载
     * @param context 上下文
     * @param imageUrl 图片URL
     * @param imageView ImageView
     * @param callback 加载回调
     * @param maxRetries 最大重试次数
     */
    public static void loadImageWithRetry(Context context, String imageUrl, ImageView imageView, 
                                        LoadCallback callback, int maxRetries) {
        loadImageWithRetryInternal(context, imageUrl, imageView, callback, maxRetries, 0);
    }
    
    /**
     * 内部重试方法
     */
    private static void loadImageWithRetryInternal(Context context, String imageUrl, ImageView imageView, 
                                                 LoadCallback callback, int maxRetries, int currentRetry) {
        if (!NetworkUtils.isNetworkAvailable(context)) {
            imageView.setImageResource(R.drawable.default_error_img);
            if (callback != null) {
                callback.onLoadFailed();
            }
            return;
        }
        Glide.with(context)
                .load(imageUrl)
                .error(R.drawable.default_error_img)
                .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                    @Override
                    public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException e, Object model, 
                                              com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, 
                                              boolean isFirstResource) {
                        Timber.tag(TAG).w("Image load failed, retry " + (currentRetry + 1) + "/" + maxRetries + ": " + imageUrl, e);
                        
                        if (currentRetry < maxRetries) {
                            // 延迟重试
                            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                                loadImageWithRetryInternal(context, imageUrl, imageView, callback, maxRetries, currentRetry + 1);
                            }, 1000 * (currentRetry + 1)); // 递增延迟
                        } else {
                            if (callback != null) {
                                callback.onLoadFailed();
                            }
                        }
                        return true; // 阻止Glide显示错误图片
                    }
                    
                    @Override
                    public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, 
                                                 com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, 
                                                 com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                        Timber.tag(TAG).d( "Image loaded successfully: " + imageUrl + " (retry: " + currentRetry + ")");
                        if (callback != null) {
                            callback.onLoadSuccess();
                        }
                        return false;
                    }
                })
                .into(imageView);
    }
    
    /**
     * 优化的网络图片加载，包含缓存策略和内存优化
     * @param context 上下文
     * @param imageUrl 图片URL
     * @param imageView ImageView
     * @param callback 加载回调
     */
    public static void loadNetworkImageOptimized(Context context, String imageUrl, ImageView imageView, LoadCallback callback) {
        RequestOptions options = new RequestOptions()
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL) // 缓存原图和转换后的图
                .skipMemoryCache(false) // 启用内存缓存
                .dontAnimate() // 禁用动画以提高性能
                .timeout(10000); // 10秒超时
        
        if (!NetworkUtils.isNetworkAvailable(context)) {
            imageView.setImageResource(R.drawable.default_error_img);
            if (callback != null) {
                callback.onLoadFailed();
            }
            return;
        }
        Glide.with(context)
                .load(imageUrl)
                .apply(options)
                .error(R.drawable.default_error_img)
                .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                    @Override
                    public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException e, Object model, 
                                              com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, 
                                              boolean isFirstResource) {
                        Timber.tag(TAG).e( "Optimized image load failed: " + imageUrl, e);
                        if (callback != null) {
                            callback.onLoadFailed();
                        }
                        return false;
                    }
                    
                    @Override
                    public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, 
                                                 com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, 
                                                 com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                        Timber.tag(TAG).d( "Optimized image loaded: " + imageUrl + " from " + dataSource);
                        if (callback != null) {
                            callback.onLoadSuccess();
                        }
                        return false;
                    }
                })
                .into(imageView);
    }
    
    /**
     * 大图片优化加载，防止OOM
     * @param context 上下文
     * @param imageUrl 图片URL
     * @param imageView ImageView
     * @param maxWidth 最大宽度
     * @param maxHeight 最大高度
     * @param callback 加载回调
     */
    public static void loadLargeImageOptimized(Context context, String imageUrl, ImageView imageView, 
                                             int maxWidth, int maxHeight, LoadCallback callback) {
        RequestOptions options = new RequestOptions()
                .override(maxWidth, maxHeight) // 限制图片尺寸
                .downsample(com.bumptech.glide.load.resource.bitmap.DownsampleStrategy.AT_MOST) // 降采样策略
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                .skipMemoryCache(false)
                .format(com.bumptech.glide.load.DecodeFormat.PREFER_RGB_565) // 使用RGB_565格式节省内存
                .timeout(15000); // 大图片需要更长的超时时间
        
        if (!NetworkUtils.isNetworkAvailable(context)) {
            imageView.setImageResource(R.drawable.default_error_img);
            if (callback != null) {
                callback.onLoadFailed();
            }
            return;
        }
        Glide.with(context)
                .load(imageUrl)
                .apply(options)
                .error(R.drawable.default_error_img)
                .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                    @Override
                    public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException e, Object model, 
                                              com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, 
                                              boolean isFirstResource) {
                        Timber.tag(TAG).e( "Large image load failed: " + imageUrl, e);
                        if (callback != null) {
                            callback.onLoadFailed();
                        }
                        return false;
                    }
                    
                    @Override
                    public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, 
                                                 com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, 
                                                 com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                        Timber.tag(TAG).d( "Large image loaded successfully: " + imageUrl);
                        if (callback != null) {
                            callback.onLoadSuccess();
                        }
                        return false;
                    }
                })
                .into(imageView);
    }
    
    /**
     * 清除图片缓存
     * @param context 上下文
     */
    public static void clearImageCache(Context context) {
        try {
            // 清除内存缓存（主线程）
            Glide.get(context).clearMemory();
            
            // 清除磁盘缓存（后台线程）
            new Thread(() -> {
                try {
                    Glide.get(context).clearDiskCache();
                    Timber.tag(TAG).d( "Image cache cleared successfully");
                } catch (Exception e) {
                    Timber.tag(TAG).e( "Failed to clear disk cache", e);
                }
            }).start();
        } catch (Exception e) {
            Timber.tag(TAG).e( "Failed to clear image cache", e);
        }
    }
    
    /**
     * 获取缓存大小（异步）
     * @param context 上下文
     * @param callback 回调接口
     */
    public static void getCacheSize(Context context, CacheSizeCallback callback) {
        new Thread(() -> {
            try {
                java.io.File cacheDir = Glide.getPhotoCacheDir(context);
                long size = 0;
                if (cacheDir != null && cacheDir.exists()) {
                    size = calculateDirectorySize(cacheDir);
                }
                
                final long finalSize = size;
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    if (callback != null) {
                        callback.onCacheSizeCalculated(finalSize);
                    }
                });
            } catch (Exception e) {
                Timber.tag(TAG).e( "Failed to calculate cache size", e);
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    if (callback != null) {
                        callback.onCacheSizeCalculated(0);
                    }
                });
            }
        }).start();
    }
    
    /**
     * 计算目录大小
     */
    private static long calculateDirectorySize(java.io.File directory) {
        long size = 0;
        if (directory.isDirectory()) {
            java.io.File[] files = directory.listFiles();
            if (files != null) {
                for (java.io.File file : files) {
                    if (file.isDirectory()) {
                        size += calculateDirectorySize(file);
                    } else {
                        size += file.length();
                    }
                }
            }
        } else {
            size = directory.length();
        }
        return size;
    }
    
    /**
     * 缓存大小回调接口
     */
    public interface CacheSizeCallback {
        void onCacheSizeCalculated(long sizeInBytes);
    }
    
    /**
     * 格式化缓存大小显示
     * @param bytes 字节数
     * @return 格式化的大小字符串
     */
    public static String formatCacheSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
}
