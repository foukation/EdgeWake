package com.fxzs.lingxiagent.util.media;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.core.content.FileProvider;

import java.io.File;

import timber.log.Timber;

/**
 * Intent创建和处理的辅助工具类
 * 处理文件打开Intent的创建和安全启动
 */
public class IntentHelper {
    
    private static final String TAG = "IntentHelper";
    private static final String FILE_PROVIDER_AUTHORITY = "com.fxzs.lingxiagent.fileprovider";
    
    /**
     * 创建查看文件的Intent
     * @param filePath 文件路径
     * @param mimeType MIME类型
     * @return Intent对象，如果创建失败返回null
     */
    public static Intent createViewIntent(String filePath, String mimeType) {
        if (filePath == null || filePath.isEmpty()) {
            Timber.tag(TAG).e( "File path is null or empty");
            return null;
        }
        
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                Timber.tag(TAG).e( "File does not exist: " + filePath);
                return null;
            }
            
            Uri fileUri = getFileUri(file);
            return createViewIntent(fileUri, mimeType);
            
        } catch (Exception e) {
            Timber.tag(TAG).e( "Failed to create view intent for file: " + filePath, e);
            return null;
        }
    }
    
    /**
     * 创建查看文件的Intent
     * @param fileUri 文件URI
     * @param mimeType MIME类型
     * @return Intent对象，如果创建失败返回null
     */
    public static Intent createViewIntent(Uri fileUri, String mimeType) {
        if (fileUri == null) {
            Timber.tag(TAG).e( "File URI is null");
            return null;
        }
        
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            return intent;
            
        } catch (Exception e) {
            Timber.tag(TAG).e( "Failed to create view intent for URI: " + fileUri, e);
            return null;
        }
    }
    
    /**
     * 获取文件的URI，处理Android 7.0+的FileProvider
     * @param file 文件对象
     * @return 文件URI
     */
    private static Uri getFileUri(File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Android 7.0+ 使用FileProvider
            return FileProvider.getUriForFile(
                getApplicationContext(), 
                FILE_PROVIDER_AUTHORITY, 
                file
            );
        } else {
            // Android 7.0以下直接使用file://
            return Uri.fromFile(file);
        }
    }
    
    /**
     * 检查系统是否有应用可以处理该Intent
     * @param context 上下文
     * @param intent Intent对象
     * @return 如果有应用可以处理返回true
     */
    public static boolean canHandleIntent(Context context, Intent intent) {
        if (context == null || intent == null) {
            return false;
        }
        
        try {
            return intent.resolveActivity(context.getPackageManager()) != null;
        } catch (Exception e) {
            Timber.tag(TAG).e( "Failed to check if intent can be handled", e);
            return false;
        }
    }
    
    /**
     * 安全启动Activity，处理异常情况
     * @param context 上下文
     * @param intent Intent对象
     * @param errorMessage 失败时显示的错误消息
     * @return 如果启动成功返回true
     */
    public static boolean startActivitySafely(Context context, Intent intent, String errorMessage) {
        Timber.tag(TAG).d( "startActivitySafely called with intent: " + intent);
        
        if (context == null || intent == null) {
            Timber.tag(TAG).e( "Context or intent is null");
            return false;
        }
        
        try {
            boolean canHandle = canHandleIntent(context, intent);
            Timber.tag(TAG).d( "Can handle intent: " + canHandle);
            
            if (canHandle) {
                Timber.tag(TAG).d( "Starting activity with intent: " + intent);
                context.startActivity(intent);
                Timber.tag(TAG).d( "Activity started successfully");
                return true;
            } else {
                Timber.tag(TAG).w("No app can handle this intent: " + intent);
                if (errorMessage != null && !errorMessage.isEmpty()) {
                    showToast(context, errorMessage);
                }
                return false;
            }
        } catch (ActivityNotFoundException e) {
            Timber.tag(TAG).e( "Activity not found for intent: " + intent, e);
            if (errorMessage != null && !errorMessage.isEmpty()) {
                showToast(context, errorMessage);
            }
            return false;
        } catch (Exception e) {
            Timber.tag(TAG).e( "Failed to start activity", e);
            if (errorMessage != null && !errorMessage.isEmpty()) {
                showToast(context, errorMessage);
            }
            return false;
        }
    }
    
    /**
     * 创建选择应用的Intent
     * @param intent 原始Intent
     * @param title 选择器标题
     * @return 选择器Intent
     */
    public static Intent createChooserIntent(Intent intent, String title) {
        if (intent == null) {
            return null;
        }
        
        try {
            return Intent.createChooser(intent, title);
        } catch (Exception e) {
            Timber.tag(TAG).e( "Failed to create chooser intent", e);
            return intent; // 返回原始Intent作为备选
        }
    }
    
    /**
     * 显示Toast消息
     * @param context 上下文
     * @param message 消息内容
     */
    private static void showToast(Context context, String message) {
        if (context != null && message != null) {
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 获取应用上下文
     * 这是一个临时实现，实际项目中应该通过依赖注入或其他方式获取
     * @return 应用上下文
     */
    private static Context getApplicationContext() {
        // 这个方法不应该被调用，因为我们总是使用带Context参数的方法
        Timber.tag(TAG).e( "getApplicationContext() called - this should not happen");
        return null;
    }
    
    /**
     * 创建查看文件的Intent（带Context参数）
     * @param context 上下文
     * @param filePath 文件路径
     * @param mimeType MIME类型
     * @return Intent对象，如果创建失败返回null
     */
    public static Intent createViewIntent(Context context, String filePath, String mimeType) {
        Timber.tag(TAG).d( "createViewIntent called with filePath: " + filePath + ", mimeType: " + mimeType);
        
        if (context == null || filePath == null || filePath.isEmpty()) {
            Timber.tag(TAG).e( "Context is null or file path is null/empty");
            return null;
        }
        
        try {
            File file = new File(filePath);
            Timber.tag(TAG).d( "File exists: " + file.exists() + ", canRead: " + file.canRead() + 
                  ", length: " + file.length());
            
            if (!file.exists()) {
                Timber.tag(TAG).e( "File does not exist: " + filePath);
                return null;
            }
            
            Uri fileUri = getFileUri(context, file);
            Timber.tag(TAG).d( "Generated file URI: " + fileUri);
            
            return createViewIntent(fileUri, mimeType);
            
        } catch (Exception e) {
            Timber.tag(TAG).e( "Failed to create view intent for file: " + filePath, e);
            return null;
        }
    }
    
    /**
     * 获取文件的URI，处理Android 7.0+的FileProvider（带Context参数）
     * @param context 上下文
     * @param file 文件对象
     * @return 文件URI
     */
    private static Uri getFileUri(Context context, File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Android 7.0+ 使用FileProvider
            return FileProvider.getUriForFile(
                context, 
                FILE_PROVIDER_AUTHORITY, 
                file
            );
        } else {
            // Android 7.0以下直接使用file://
            return Uri.fromFile(file);
        }
    }
}