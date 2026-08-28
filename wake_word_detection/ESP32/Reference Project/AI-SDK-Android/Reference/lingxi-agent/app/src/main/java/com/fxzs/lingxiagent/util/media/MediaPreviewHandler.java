package com.fxzs.lingxiagent.util.media;

import android.content.Context;

import com.fxzs.lingxiagent.model.chat.dto.ChatFileBean;
import com.fxzs.lingxiagent.view.media.ImagePreviewActivity;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * 媒体预览处理器
 * 统一处理媒体文件的预览和打开逻辑
 */
public class MediaPreviewHandler {
    
    private static final String TAG = "MediaPreviewHandler";
    
    private Context context;
    private FileTypeDetector fileTypeDetector;
    private DocumentOpener documentOpener;
    private ImageCacheManager cacheManager;
    
    public MediaPreviewHandler(Context context) {
        this.context = context;
        this.documentOpener = new DocumentOpener(context);
        this.cacheManager = new ImageCacheManager(context);
        
        // 优化缓存设置
        cacheManager.optimizeCacheSettings();
    }
    
    /**
     * 处理文件点击事件
     * @param fileBean 文件信息
     * @return 如果成功处理返回true
     */
    public boolean handleFileClick(ChatFileBean fileBean) {
        Timber.tag(TAG).d( "handleFileClick called");
        
        if (fileBean == null) {
            Timber.tag(TAG).e( "ChatFileBean is null");
            return false;
        }
        
        String fileName = fileBean.getName();
        String filePath = fileBean.getEffectivePath();
        
        Timber.tag(TAG).d( "Handling file click: " + fileName + ", path: " + filePath);
        
        // 检测文件类型
        FileTypeDetector.FileType fileType = FileTypeDetector.detectFileType(fileName, null);
        
        switch (fileType) {
            case IMAGE:
                return openImagePreview(fileBean);
            case DOCUMENT:
                return openDocument(fileBean);
            case UNKNOWN:
            default:
                Timber.tag(TAG).w( "Unknown file type for: " + fileName);
                // 对于未知类型，尝试作为文档打开
                return openDocument(fileBean);
        }
    }
    
    /**
     * 打开图片预览
     * @param fileBean 文件信息
     * @return 如果成功启动预览返回true
     */
    private boolean openImagePreview(ChatFileBean fileBean) {
        try {
            String imageUrl = fileBean.getEffectivePath();
            String imageName = fileBean.getDisplayName();
            
            Timber.tag(TAG).d( "Opening image preview: " + imageName);
            
            // 检查缓存大小并在必要时清理
            cacheManager.checkCacheSizeAndCleanIfNeeded();
            
            ImagePreviewActivity.start(context, imageUrl, imageName);
            return true;
            
        } catch (Exception e) {
            Timber.tag(TAG).e( "Failed to open image preview", e);
            ErrorHandler.handleImageLoadError(context, fileBean.getEffectivePath(), e);
            return false;
        }
    }
    
    /**
     * 打开文档
     * @param fileBean 文件信息
     * @return 如果成功启动打开操作返回true
     */
    private boolean openDocument(ChatFileBean fileBean) {
        try {
            Timber.tag(TAG).d( "Opening document: " + fileBean.getDisplayName());
            
            return documentOpener.openWithThirdParty(fileBean);
            
        } catch (Exception e) {
            Timber.tag(TAG).e( "Failed to open document", e);
            ErrorHandler.handleDocumentOpenError(context, fileBean.getDisplayName(), e);
            return false;
        }
    }
    
    /**
     * 预加载图片（可选功能）
     * @param fileBean 文件信息
     */
    public void preloadImageIfNeeded(ChatFileBean fileBean) {
        if (fileBean == null) {
            return;
        }
        
        String fileName = fileBean.getName();
        if (FileTypeDetector.isImageFile(fileName)) {
            String imageUrl = fileBean.getEffectivePath();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                // 使用缓存管理器预加载图片
                cacheManager.preloadImages(imageUrl);
            }
        }
    }
    
    /**
     * 批量预加载图片
     * @param fileBeans 文件信息列表
     */
    public void preloadImages(List<ChatFileBean> fileBeans) {
        if (fileBeans == null || fileBeans.isEmpty()) {
            return;
        }
        
        List<String> imageUrls = new ArrayList<>();
        for (ChatFileBean fileBean : fileBeans) {
            if (fileBean != null && FileTypeDetector.isImageFile(fileBean.getName())) {
                String imageUrl = fileBean.getEffectivePath();
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    imageUrls.add(imageUrl);
                }
            }
        }
        
        if (!imageUrls.isEmpty()) {
            cacheManager.preloadImages(imageUrls.toArray(new String[0]));
        }
    }
    
    /**
     * 智能预加载相关图片
     * @param currentFileBean 当前文件
     * @param relatedFileBeans 相关文件列表
     */
    public void smartPreloadRelatedImages(ChatFileBean currentFileBean, List<ChatFileBean> relatedFileBeans) {
        if (currentFileBean == null || relatedFileBeans == null) {
            return;
        }
        
        String currentImageUrl = null;
        if (FileTypeDetector.isImageFile(currentFileBean.getName())) {
            currentImageUrl = currentFileBean.getEffectivePath();
        }
        
        List<String> relatedImageUrls = new ArrayList<>();
        for (ChatFileBean fileBean : relatedFileBeans) {
            if (fileBean != null && FileTypeDetector.isImageFile(fileBean.getName())) {
                String imageUrl = fileBean.getEffectivePath();
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    relatedImageUrls.add(imageUrl);
                }
            }
        }
        
        if (!relatedImageUrls.isEmpty()) {
            cacheManager.smartPreload(currentImageUrl, relatedImageUrls.toArray(new String[0]));
        }
    }
    
    /**
     * 检查文件是否可以预览
     * @param fileBean 文件信息
     * @return 如果可以预览返回true
     */
    public boolean canPreview(ChatFileBean fileBean) {
        if (fileBean == null || fileBean.getName() == null) {
            return false;
        }
        
        FileTypeDetector.FileType fileType = FileTypeDetector.detectFileType(fileBean.getName(), null);
        return fileType != FileTypeDetector.FileType.UNKNOWN;
    }
    
    /**
     * 获取文件类型描述
     * @param fileBean 文件信息
     * @return 文件类型描述
     */
    public String getFileTypeDescription(ChatFileBean fileBean) {
        if (fileBean == null || fileBean.getName() == null) {
            return "未知文件";
        }
        
        FileTypeDetector.FileType fileType = FileTypeDetector.detectFileType(fileBean.getName(), null);
        switch (fileType) {
            case IMAGE:
                return "图片文件";
            case DOCUMENT:
                return "文档文件";
            case UNKNOWN:
            default:
                return "未知文件";
        }
    }
    
    /**
     * 获取缓存管理器
     * @return 缓存管理器实例
     */
    public ImageCacheManager getCacheManager() {
        return cacheManager;
    }
    
    /**
     * 获取缓存统计信息
     * @param callback 回调接口
     */
    public void getCacheStats(ImageCacheManager.CacheStatsCallback callback) {
        if (cacheManager != null) {
            cacheManager.getCacheStats(callback);
        }
    }
    
    /**
     * 清理图片缓存
     * @param callback 清理完成回调
     */
    public void clearImageCache(ImageCacheManager.ClearCacheCallback callback) {
        if (cacheManager != null) {
            cacheManager.clearCache(callback);
        }
    }
    
    /**
     * 设置缓存大小限制
     * @param sizeInMB 缓存大小限制（MB）
     */
    public void setCacheSizeLimit(int sizeInMB) {
        if (cacheManager != null) {
            long sizeInBytes = sizeInMB * 1024L * 1024L;
            cacheManager.setCacheSizeLimit(sizeInBytes);
        }
    }
    
    /**
     * 设置是否启用预加载
     * @param enabled 是否启用
     */
    public void setPreloadEnabled(boolean enabled) {
        if (cacheManager != null) {
            cacheManager.setPreloadEnabled(enabled);
        }
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        if (cacheManager != null) {
            cacheManager.cleanup();
        }
    }
}