package com.fxzs.lingxiagent.util.media;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.net.UnknownHostException;

import timber.log.Timber;

/**
 * 错误处理类
 * 统一处理媒体预览相关的错误情况
 */
public class ErrorHandler {
    
    private static final String TAG = "ErrorHandler";
    
    /**
     * 处理图片加载错误
     * @param context 上下文
     * @param imageUrl 图片URL
     * @param error 错误信息
     */
    public static void handleImageLoadError(Context context, String imageUrl, Throwable error) {
        Timber.tag(TAG).e( "Image load failed: " + imageUrl, error);
        
        String message;
        if (error instanceof UnknownHostException) {
            message = "网络连接失败，请检查网络设置";
        } else if (error instanceof FileNotFoundException) {
            message = "图片文件不存在";
        } else {
            message = "图片加载失败，请重试";
        }
        
        showToast(context, message);
    }
    
    /**
     * 处理文档打开错误
     * @param context 上下文
     * @param fileName 文件名
     * @param error 错误信息
     */
    public static void handleDocumentOpenError(Context context, String fileName, Exception error) {
        Timber.tag(TAG).e( "Document open failed: " + fileName, error);
        
        if (error instanceof ActivityNotFoundException) {
            showInstallAppDialog(context, fileName);
        } else if (error instanceof SecurityException) {
            showPermissionDialog(context);
        } else {
            String message = "无法打开文档：" + (fileName != null ? fileName : "未知文件");
            showToast(context, message);
        }
    }
    
    /**
     * 显示安装应用的对话框
     * @param context 上下文
     * @param fileName 文件名
     */
    public static void showInstallAppDialog(Context context, String fileName) {
        if (context == null) {
            return;
        }
        
        String fileExtension = getFileExtension(fileName);
        String appSuggestion = getAppSuggestion(fileExtension);
        
        new AlertDialog.Builder(context)
                .setTitle("需要安装应用")
                .setMessage("没有找到可以打开此文件的应用。\n\n" + appSuggestion)
                .setPositiveButton("去应用商店", (dialog, which) -> {
                    openAppStore(context, fileExtension);
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    /**
     * 显示权限对话框
     * @param context 上下文
     */
    public static void showPermissionDialog(Context context) {
        if (context == null) {
            return;
        }
        
        new AlertDialog.Builder(context)
                .setTitle("需要文件访问权限")
                .setMessage("应用需要文件访问权限才能打开文档。请在设置中授予权限。")
                .setPositiveButton("去设置", (dialog, which) -> {
                    openAppSettings(context);
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    /**
     * 打开应用商店
     * @param context 上下文
     * @param fileExtension 文件扩展名
     */
    private static void openAppStore(Context context, String fileExtension) {
        try {
            String packageName = getRecommendedAppPackage(fileExtension);
            if (packageName != null) {
                // 尝试打开具体应用的商店页面
                Intent intent = new Intent(Intent.ACTION_VIEW, 
                        Uri.parse("market://details?id=" + packageName));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } else {
                // 打开通用应用商店搜索
                String searchQuery = getAppSearchQuery(fileExtension);
                Intent intent = new Intent(Intent.ACTION_VIEW, 
                        Uri.parse("market://search?q=" + searchQuery));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        } catch (ActivityNotFoundException e) {
            // 如果没有应用商店，打开网页版
            try {
                String packageName = getRecommendedAppPackage(fileExtension);
                String url = packageName != null 
                        ? "https://play.google.com/store/apps/details?id=" + packageName
                        : "https://play.google.com/store/search?q=" + getAppSearchQuery(fileExtension);
                
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception ex) {
                Timber.tag(TAG).e( "Failed to open app store", ex);
                showToast(context, "无法打开应用商店");
            }
        } catch (Exception e) {
            Timber.tag(TAG).e( "Failed to open app store", e);
            showToast(context, "无法打开应用商店");
        }
    }
    
    /**
     * 打开应用设置页面
     * @param context 上下文
     */
    private static void openAppSettings(Context context) {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Timber.tag(TAG).e( "Failed to open app settings", e);
            showToast(context, "无法打开设置页面");
        }
    }
    
    /**
     * 获取推荐应用的包名
     * @param fileExtension 文件扩展名
     * @return 应用包名，如果没有推荐应用返回null
     */
    private static String getRecommendedAppPackage(String fileExtension) {
        if (fileExtension == null) {
            return null;
        }
        
        switch (fileExtension.toLowerCase()) {
            case "pdf":
                return "com.adobe.reader"; // Adobe Acrobat Reader
            case "doc":
            case "docx":
            case "ppt":
            case "pptx":
            case "xls":
            case "xlsx":
                return "cn.wps.moffice_eng"; // WPS Office
            default:
                return null;
        }
    }
    
    /**
     * 获取应用搜索查询词
     * @param fileExtension 文件扩展名
     * @return 搜索查询词
     */
    private static String getAppSearchQuery(String fileExtension) {
        if (fileExtension == null) {
            return "file viewer";
        }
        
        switch (fileExtension.toLowerCase()) {
            case "pdf":
                return "pdf reader";
            case "doc":
            case "docx":
                return "word document viewer";
            case "ppt":
            case "pptx":
                return "powerpoint viewer";
            case "xls":
            case "xlsx":
                return "excel viewer";
            default:
                return "file viewer";
        }
    }
    
    /**
     * 根据文件扩展名获取应用安装建议
     * @param fileExtension 文件扩展名
     * @return 应用建议
     */
    private static String getAppSuggestion(String fileExtension) {
        if (fileExtension == null || fileExtension.isEmpty()) {
            return "建议安装支持此文件类型的应用";
        }
        
        switch (fileExtension.toLowerCase()) {
            case "pdf":
                return "建议安装PDF阅读器，如Adobe Acrobat Reader";
            case "doc":
            case "docx":
                return "建议安装Microsoft Word或WPS Office";
            case "ppt":
            case "pptx":
                return "建议安装Microsoft PowerPoint或WPS Office";
            case "xls":
            case "xlsx":
                return "建议安装Microsoft Excel或WPS Office";
            case "txt":
                return "建议安装文本编辑器";
            default:
                return "建议安装支持此文件类型的应用";
        }
    }
    
    /**
     * 获取文件扩展名
     * @param fileName 文件名
     * @return 文件扩展名
     */
    private static String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return "";
        }
        
        return fileName.substring(lastDotIndex + 1);
    }
    
    /**
     * 显示Toast消息
     * @param context 上下文
     * @param message 消息内容
     */
    private static void showToast(Context context, String message) {
        if (context != null && message != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 处理网络相关错误
     * @param context 上下文
     * @param error 错误信息
     */
    public static void handleNetworkError(Context context, Throwable error) {
        Timber.tag(TAG).e( "Network error occurred", error);
        
        String message;
        if (error instanceof UnknownHostException) {
            message = "网络连接失败，请检查网络设置";
        } else {
            message = "网络错误，请稍后重试";
        }
        
        showToast(context, message);
    }
    
    /**
     * 处理文件不存在错误
     * @param context 上下文
     * @param fileName 文件名
     */
    public static void handleFileNotFoundError(Context context, String fileName) {
        String displayName = fileName != null ? fileName : "文件";
        String message = displayName + " 不存在或已被删除";
        
        Timber.tag(TAG).w("File not found: " + fileName);
        showToast(context, message);
    }
    
    /**
     * 处理权限错误
     * @param context 上下文
     * @param error 错误信息
     */
    public static void handlePermissionError(Context context, SecurityException error) {
        Timber.tag(TAG).e( "Permission error occurred", error);
        showPermissionDialog(context);
    }
}