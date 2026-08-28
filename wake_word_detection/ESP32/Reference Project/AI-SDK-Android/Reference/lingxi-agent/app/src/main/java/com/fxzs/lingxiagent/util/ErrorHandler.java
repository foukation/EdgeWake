package com.fxzs.lingxiagent.util;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.Toast;

import com.fxzs.lingxiagent.model.ppt.repository.PptErrorHandler;

/**
 * 统一错误处理工具类
 * 提供用户友好的错误提示和处理机制
 */
public class ErrorHandler {
    
    /**
     * 错误处理回调接口
     */
    public interface ErrorCallback {
        void onRetry();
        void onCancel();
    }
    
    /**
     * 显示错误对话框
     */
    public static void showErrorDialog(Context context, Throwable error, ErrorCallback callback) {
        PptErrorHandler.ErrorInfo errorInfo = PptErrorHandler.handleError(error);
        showErrorDialog(context, errorInfo, callback);
    }
    
    /**
     * 显示错误对话框
     */
    public static void showErrorDialog(Context context, PptErrorHandler.ErrorInfo errorInfo, ErrorCallback callback) {
        String title = getErrorTitle(errorInfo.getType());
        String message = PptErrorHandler.getUserFriendlyMessage(errorInfo);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false);
        
        if (errorInfo.isRetryable() && callback != null) {
            builder.setPositiveButton("重试", (dialog, which) -> {
                dialog.dismiss();
                callback.onRetry();
            });
            
            builder.setNegativeButton("取消", (dialog, which) -> {
                dialog.dismiss();
                if (callback != null) {
                    callback.onCancel();
                }
            });
        } else {
            builder.setPositiveButton("确定", (dialog, which) -> {
                dialog.dismiss();
                if (callback != null) {
                    callback.onCancel();
                }
            });
        }
        
        builder.show();
    }
    
    /**
     * 显示简单的错误Toast
     */
    public static void showErrorToast(Context context, Throwable error) {
        PptErrorHandler.ErrorInfo errorInfo = PptErrorHandler.handleError(error);
        String message = PptErrorHandler.getUserFriendlyMessage(errorInfo);
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
    
    /**
     * 显示简单的错误Toast
     */
    public static void showErrorToast(Context context, String message) {
        Toast.makeText(context, message+111111, Toast.LENGTH_LONG).show();
    }
    
    /**
     * 显示成功Toast
     */
    public static void showSuccessToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 显示信息Toast
     */
    public static void showInfoToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 显示重试对话框
     */
    public static void showRetryDialog(Context context, String message, Runnable retryAction) {
        new AlertDialog.Builder(context)
            .setTitle("操作失败")
            .setMessage(message)
            .setPositiveButton("重试", (dialog, which) -> {
                dialog.dismiss();
                if (retryAction != null) {
                    retryAction.run();
                }
            })
            .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
            .show();
    }
    
    /**
     * 显示网络错误对话框
     */
    public static void showNetworkErrorDialog(Context context, ErrorCallback callback) {
        new AlertDialog.Builder(context)
            .setTitle("网络连接失败")
            .setMessage("请检查网络连接后重试")
            .setPositiveButton("重试", (dialog, which) -> {
                dialog.dismiss();
                if (callback != null) {
                    callback.onRetry();
                }
            })
            .setNegativeButton("取消", (dialog, which) -> {
                dialog.dismiss();
                if (callback != null) {
                    callback.onCancel();
                }
            })
            .show();
    }
    
    /**
     * 显示确认对话框
     */
    public static void showConfirmDialog(Context context, String title, String message, 
                                       Runnable confirmAction, Runnable cancelAction) {
        new AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("确定", (dialog, which) -> {
                dialog.dismiss();
                if (confirmAction != null) {
                    confirmAction.run();
                }
            })
            .setNegativeButton("取消", (dialog, which) -> {
                dialog.dismiss();
                if (cancelAction != null) {
                    cancelAction.run();
                }
            })
            .show();
    }
    
    /**
     * 获取错误标题
     */
    private static String getErrorTitle(PptErrorHandler.ErrorType errorType) {
        switch (errorType) {
            case NETWORK_ERROR:
                return "网络错误";
            case TIMEOUT_ERROR:
                return "请求超时";
            case SERVER_ERROR:
                return "服务器错误";
            case PARSE_ERROR:
                return "数据错误";
            case BUSINESS_ERROR:
                return "操作失败";
            default:
                return "错误";
        }
    }
    
    /**
     * 处理API错误的通用方法
     */
    public static void handleApiError(Context context, Throwable error, ErrorCallback callback) {
        if (!PptErrorHandler.isNetworkAvailable(context)) {
            showNetworkErrorDialog(context, callback);
        } else {
            showErrorDialog(context, error, callback);
        }
    }
}