package com.fxzs.lingxiagent.util;

import android.app.ProgressDialog;
import android.content.Context;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.StringRes;

/**
 * 加载状态管理器
 * 统一管理各种加载状态的显示和隐藏
 */
public class LoadingManager {
    
    private ProgressDialog progressDialog;
    private Context context;
    
    public LoadingManager(Context context) {
        this.context = context;
    }
    
    /**
     * 显示加载对话框
     */
    public void showLoadingDialog(String message) {
        hideLoadingDialog(); // 先隐藏之前的对话框
        
        progressDialog = new ProgressDialog(context);
        progressDialog.setMessage(message);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }
    
    /**
     * 显示加载对话框
     */
    public void showLoadingDialog(@StringRes int messageResId) {
        showLoadingDialog(context.getString(messageResId));
    }
    
    /**
     * 隐藏加载对话框
     */
    public void hideLoadingDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }
    
    /**
     * 更新加载对话框消息
     */
    public void updateLoadingMessage(String message) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.setMessage(message);
        }
    }
    
    /**
     * 显示进度条
     */
    public static void showProgressBar(ProgressBar progressBar) {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * 隐藏进度条
     */
    public static void hideProgressBar(ProgressBar progressBar) {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }
    
    /**
     * 设置进度条进度
     */
    public static void setProgress(ProgressBar progressBar, int progress) {
        if (progressBar != null) {
            progressBar.setProgress(progress);
        }
    }
    
    /**
     * 显示加载文本
     */
    public static void showLoadingText(TextView textView, String message) {
        if (textView != null) {
            textView.setText(message);
            textView.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * 隐藏加载文本
     */
    public static void hideLoadingText(TextView textView) {
        if (textView != null) {
            textView.setVisibility(View.GONE);
        }
    }
    
    /**
     * 显示加载覆盖层
     */
    public static void showLoadingOverlay(View overlay) {
        if (overlay != null) {
            overlay.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * 隐藏加载覆盖层
     */
    public static void hideLoadingOverlay(View overlay) {
        if (overlay != null) {
            overlay.setVisibility(View.GONE);
        }
    }
    
    /**
     * 设置视图启用状态
     */
    public static void setViewEnabled(View view, boolean enabled) {
        if (view != null) {
            view.setEnabled(enabled);
            view.setAlpha(enabled ? 1.0f : 0.5f);
        }
    }
    
    /**
     * 批量设置视图启用状态
     */
    public static void setViewsEnabled(boolean enabled, View... views) {
        for (View view : views) {
            setViewEnabled(view, enabled);
        }
    }
    
    /**
     * 销毁资源
     */
    public void destroy() {
        hideLoadingDialog();
        context = null;
    }
}