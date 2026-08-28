package com.fxzs.lingxiagent.util.media;

import android.text.TextUtils;

/**
 * 文件类型检测器
 * 根据文件名和MIME类型判断文件类型
 */
public class FileTypeDetector {
    
    /**
     * 文件类型枚举
     */
    public enum FileType {
        IMAGE,      // 图片文件
        DOCUMENT,   // 文档文件
        UNKNOWN     // 未知类型
    }
    
    /**
     * 检测文件类型
     * @param fileName 文件名
     * @param mimeType MIME类型（可选）
     * @return 文件类型
     */
    public static FileType detectFileType(String fileName, String mimeType) {
        // 首先尝试通过文件名检测
        FileType typeFromFileName = detectFileTypeByFileName(fileName);
        if (typeFromFileName != FileType.UNKNOWN) {
            return typeFromFileName;
        }
        
        // 如果文件名检测失败，尝试通过MIME类型检测
        if (!TextUtils.isEmpty(mimeType)) {
            return detectFileTypeByMimeType(mimeType);
        }
        
        return FileType.UNKNOWN;
    }
    
    /**
     * 根据文件名检测文件类型
     * @param fileName 文件名
     * @return 文件类型
     */
    private static FileType detectFileTypeByFileName(String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            return FileType.UNKNOWN;
        }
        
        String extension = getFileExtension(fileName);
        if (TextUtils.isEmpty(extension)) {
            return FileType.UNKNOWN;
        }
        
        if (isImageFileByExtension(extension)) {
            return FileType.IMAGE;
        } else if (isDocumentFileByExtension(extension)) {
            return FileType.DOCUMENT;
        }
        
        return FileType.UNKNOWN;
    }
    
    /**
     * 根据MIME类型检测文件类型
     * @param mimeType MIME类型
     * @return 文件类型
     */
    private static FileType detectFileTypeByMimeType(String mimeType) {
        if (TextUtils.isEmpty(mimeType)) {
            return FileType.UNKNOWN;
        }
        
        String lowerMimeType = mimeType.toLowerCase();
        
        if (lowerMimeType.startsWith("image/")) {
            return FileType.IMAGE;
        } else if (lowerMimeType.startsWith("application/") || 
                   lowerMimeType.startsWith("text/")) {
            return FileType.DOCUMENT;
        }
        
        return FileType.UNKNOWN;
    }
    
    /**
     * 获取文件扩展名
     * @param fileName 文件名
     * @return 文件扩展名（不包含点号），如果没有扩展名返回空字符串
     */
    private static String getFileExtension(String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            return "";
        }
        
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return "";
        }
        
        return fileName.substring(lastDotIndex + 1);
    }
    
    /**
     * 根据文件扩展名获取MIME类型
     * @param fileName 文件名
     * @return MIME类型
     */
    public static String getMimeType(String fileName) {
        String extension = getFileExtension(fileName);
        return MediaPreviewConfig.getMimeType(extension);
    }
    
    /**
     * 检查是否为图片文件
     * @param fileName 文件名
     * @return 如果是图片文件返回true
     */
    public static boolean isImageFile(String fileName) {
        String extension = getFileExtension(fileName);
        return isImageFileByExtension(extension);
    }
    
    /**
     * 检查是否为图片文件（通过扩展名）
     * @param extension 文件扩展名（不包含点号）
     * @return 如果是图片文件返回true
     */
    private static boolean isImageFileByExtension(String extension) {
        return MediaPreviewConfig.isSupportedImageExtension(extension);
    }
    
    /**
     * 检查是否为文档文件
     * @param fileName 文件名
     * @return 如果是文档文件返回true
     */
    public static boolean isDocumentFile(String fileName) {
        String extension = getFileExtension(fileName);
        return isDocumentFileByExtension(extension);
    }
    
    /**
     * 检查是否为文档文件（通过扩展名）
     * @param extension 文件扩展名（不包含点号）
     * @return 如果是文档文件返回true
     */
    private static boolean isDocumentFileByExtension(String extension) {
        return MediaPreviewConfig.isSupportedDocumentExtension(extension);
    }
}