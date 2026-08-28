package com.fxzs.lingxiagent.util.media;

import java.util.HashMap;
import java.util.Map;

/**
 * 媒体预览配置类
 * 定义支持的文件类型、扩展名和MIME类型映射
 */
public class MediaPreviewConfig {
    
    /**
     * 支持的图片文件扩展名
     */
    public static final String[] SUPPORTED_IMAGE_EXTENSIONS = {
        "jpg", "jpeg", "png", "gif", "webp", "bmp"
    };
    
    /**
     * 支持的文档文件扩展名
     */
    public static final String[] SUPPORTED_DOCUMENT_EXTENSIONS = {
        "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt"
    };
    
    /**
     * 文件扩展名到MIME类型的映射
     */
    public static final Map<String, String> MIME_TYPE_MAP = new HashMap<>();
    
    static {
        // 图片MIME类型
        MIME_TYPE_MAP.put("jpg", "image/jpeg");
        MIME_TYPE_MAP.put("jpeg", "image/jpeg");
        MIME_TYPE_MAP.put("png", "image/png");
        MIME_TYPE_MAP.put("gif", "image/gif");
        MIME_TYPE_MAP.put("webp", "image/webp");
        MIME_TYPE_MAP.put("bmp", "image/bmp");
        
        // 文档MIME类型
        MIME_TYPE_MAP.put("pdf", "application/pdf");
        MIME_TYPE_MAP.put("doc", "application/msword");
        MIME_TYPE_MAP.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        MIME_TYPE_MAP.put("ppt", "application/vnd.ms-powerpoint");
        MIME_TYPE_MAP.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        MIME_TYPE_MAP.put("xls", "application/vnd.ms-excel");
        MIME_TYPE_MAP.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        MIME_TYPE_MAP.put("txt", "text/plain");
    }
    
    /**
     * 获取文件扩展名对应的MIME类型
     * @param extension 文件扩展名（不包含点号）
     * @return MIME类型，如果未找到则返回"application/octet-stream"
     */
    public static String getMimeType(String extension) {
        if (extension == null || extension.isEmpty()) {
            return "application/octet-stream";
        }
        
        String mimeType = MIME_TYPE_MAP.get(extension.toLowerCase());
        return mimeType != null ? mimeType : "application/octet-stream";
    }
    
    /**
     * 检查是否为支持的图片文件扩展名
     * @param extension 文件扩展名（不包含点号）
     * @return 如果是支持的图片扩展名返回true
     */
    public static boolean isSupportedImageExtension(String extension) {
        if (extension == null || extension.isEmpty()) {
            return false;
        }
        
        String lowerExtension = extension.toLowerCase();
        for (String supportedExt : SUPPORTED_IMAGE_EXTENSIONS) {
            if (supportedExt.equals(lowerExtension)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查是否为支持的文档文件扩展名
     * @param extension 文件扩展名（不包含点号）
     * @return 如果是支持的文档扩展名返回true
     */
    public static boolean isSupportedDocumentExtension(String extension) {
        if (extension == null || extension.isEmpty()) {
            return false;
        }
        
        String lowerExtension = extension.toLowerCase();
        for (String supportedExt : SUPPORTED_DOCUMENT_EXTENSIONS) {
            if (supportedExt.equals(lowerExtension)) {
                return true;
            }
        }
        return false;
    }
}