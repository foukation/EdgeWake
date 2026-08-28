package com.fxzs.lingxiagent.util.media;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.fxzs.lingxiagent.model.chat.dto.ChatFileBean;

import timber.log.Timber;

/**
 * 文档打开器
 * 使用WebView预览文档文件
 */
public class DocumentOpener {
    
    private static final String TAG = "DocumentOpener";
    
    private Context context;
    
    public DocumentOpener(Context context) {
        this.context = context;
    }
    
    /**
     * 使用WebView预览文档
     * @param fileBean 文件信息
     * @return 如果成功启动预览返回true
     */
    public boolean openWithThirdParty(ChatFileBean fileBean) {
        if (fileBean == null) {
            Timber.tag(TAG).e( "ChatFileBean is null");
            return false;
        }
        
        String filePath = getEffectiveFilePath(fileBean);
        if (TextUtils.isEmpty(filePath)) {
            Timber.tag(TAG).e( "File path is empty");
            handleOpenFailure(fileBean.getDisplayName(), "文件路径为空");
            return false;
        }
        
        Timber.tag(TAG).d( "Opening document with WebView: " + filePath);
        return openWithWebView(filePath, fileBean.getDisplayName());
    }
    
    /**
     * 获取有效的文件路径
     * @param fileBean 文件信息
     * @return 文件路径
     */
    private String getEffectiveFilePath(ChatFileBean fileBean) {
        if (!TextUtils.isEmpty(fileBean.getPath())) {
            return fileBean.getPath();
        } else if (!TextUtils.isEmpty(fileBean.getFileUriString())) {
            return fileBean.getFileUriString();
        }
        return "";
    }
    
    /**
     * 使用WebView打开文档预览
     * @param filePath 文件路径或URL
     * @param fileName 文件名
     * @return 如果成功启动WebView返回true
     */
    private boolean openWithWebView(String filePath, String fileName) {
        try {
            String previewUrl = preparePreviewUrl(filePath);
            Timber.tag(TAG).d( "Opening document in WebView: " + previewUrl);
            
            // 使用WebViewActivity打开文档预览
            Intent webViewIntent = new Intent(context, com.fxzs.lingxiagent.view.common.WebViewActivity.class);
            webViewIntent.putExtra("extra_url", previewUrl);
            webViewIntent.putExtra("extra_title", fileName != null ? fileName : "文档预览");
            webViewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            context.startActivity(webViewIntent);
            Timber.tag(TAG).d( "Successfully opened document in WebView");
            return true;
            
        } catch (Exception e) {
            Timber.tag(TAG).e( "Failed to open document in WebView", e);
            handleOpenFailure(fileName, "WebView预览失败：" + e.getMessage());
            return false;
        }
    }
    
    /**
     * 准备预览URL
     * @param filePath 文件路径或URL
     * @return 预览URL
     */
    private String preparePreviewUrl(String filePath) {
        if (filePath.startsWith("http://") || filePath.startsWith("https://")) {
            // 网络文件：添加预览参数
            return filePath + "?ci-process=doc-preview&dstType=html";
        } else {
            // 本地文件：转换为file:// URL
            return "file://" + filePath;
        }
    }
    
    /**
     * 处理打开失败的情况
     * @param fileName 文件名
     * @param errorMessage 错误消息
     */
    public void handleOpenFailure(String fileName, String errorMessage) {
        String displayName = !TextUtils.isEmpty(fileName) ? fileName : "未知文件";
        Timber.tag(TAG).w( "Failed to open document: " + displayName + ", reason: " + errorMessage);
        
        // 使用ErrorHandler统一处理错误
        if (errorMessage != null && errorMessage.contains("没有找到可以打开此文件的应用")) {
            ErrorHandler.showInstallAppDialog(context, fileName);
        } else if (errorMessage != null && errorMessage.contains("权限")) {
            ErrorHandler.showPermissionDialog(context);
        } else {
            // 显示Toast提示用户
            showToast("无法打开文件 " + displayName + "：" + errorMessage);
        }
    }
    
    /**
     * 显示Toast消息
     * @param message 消息内容
     */
    private void showToast(String message) {
        if (context != null && message != null) {
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show();
        }
    }

}