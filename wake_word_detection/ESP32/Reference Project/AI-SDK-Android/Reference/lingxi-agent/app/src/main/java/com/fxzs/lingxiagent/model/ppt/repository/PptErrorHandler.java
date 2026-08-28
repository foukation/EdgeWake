package com.fxzs.lingxiagent.model.ppt.repository;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import retrofit2.HttpException;

/**
 * PPT相关错误处理工具类
 */
public class PptErrorHandler {
    
    /**
     * 错误类型枚举
     */
    public enum ErrorType {
        NETWORK_ERROR,      // 网络错误
        SERVER_ERROR,       // 服务器错误
        TIMEOUT_ERROR,      // 超时错误
        PARSE_ERROR,        // 解析错误
        BUSINESS_ERROR,     // 业务逻辑错误
        UNKNOWN_ERROR       // 未知错误
    }
    
    /**
     * 错误信息包装类
     */
    public static class ErrorInfo {
        private final ErrorType type;
        private final String message;
        private final int code;
        private final boolean retryable;
        
        public ErrorInfo(ErrorType type, String message, int code, boolean retryable) {
            this.type = type;
            this.message = message;
            this.code = code;
            this.retryable = retryable;
        }
        
        public ErrorType getType() { return type; }
        public String getMessage() { return message; }
        public int getCode() { return code; }
        public boolean isRetryable() { return retryable; }
    }
    
    /**
     * 处理网络请求异常
     */
    public static ErrorInfo handleError(Throwable throwable) {
        if (throwable instanceof HttpException) {
            HttpException httpException = (HttpException) throwable;
            int code = httpException.code();
            String message = getHttpErrorMessage(code);
            boolean retryable = isRetryableHttpError(code);
            return new ErrorInfo(ErrorType.SERVER_ERROR, message, code, retryable);
            
        } else if (throwable instanceof SocketTimeoutException) {
            return new ErrorInfo(ErrorType.TIMEOUT_ERROR, "请求超时，请检查网络连接", -1, true);
            
        } else if (throwable instanceof UnknownHostException) {
            return new ErrorInfo(ErrorType.NETWORK_ERROR, "网络连接失败，请检查网络设置", -1, true);
            
        } else if (throwable instanceof IOException) {
            return new ErrorInfo(ErrorType.NETWORK_ERROR, "网络连接异常：" + throwable.getMessage(), -1, true);
            
        } else {
            return new ErrorInfo(ErrorType.UNKNOWN_ERROR, "未知错误：" + throwable.getMessage(), -1, false);
        }
    }
    
    /**
     * 获取HTTP错误消息
     */
    private static String getHttpErrorMessage(int code) {
        switch (code) {
            case 400:
                return "请求参数错误";
            case 401:
                return "身份验证失败，请重新登录";
            case 403:
                return "访问被拒绝，权限不足";
            case 404:
                return "请求的资源不存在";
            case 408:
                return "请求超时";
            case 429:
                return "请求过于频繁，请稍后再试";
            case 500:
                return "服务器内部错误";
            case 502:
                return "网关错误";
            case 503:
                return "服务暂时不可用";
            case 504:
                return "网关超时";
            default:
                return "服务器错误 (HTTP " + code + ")";
        }
    }
    
    /**
     * 判断HTTP错误是否可重试
     */
    private static boolean isRetryableHttpError(int code) {
        return code == 408 || code == 429 || code >= 500;
    }
    
    /**
     * 检查网络连接状态
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = 
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }
    
    /**
     * 获取用户友好的错误提示
     */
    public static String getUserFriendlyMessage(ErrorInfo errorInfo) {
        switch (errorInfo.getType()) {
            case NETWORK_ERROR:
                return "网络连接异常，请检查网络设置后重试";
            case TIMEOUT_ERROR:
                return "请求超时，请稍后重试";
            case SERVER_ERROR:
                if (errorInfo.getCode() == 401) {
                    return "登录已过期，请重新登录";
                } else if (errorInfo.getCode() >= 500) {
                    return "服务器繁忙，请稍后重试";
                } else {
                    return errorInfo.getMessage();
                }
            case PARSE_ERROR:
                return "数据解析失败，请稍后重试";
            case BUSINESS_ERROR:
                return errorInfo.getMessage();
            default:
                return "操作失败，请稍后重试";
        }
    }
    
    /**
     * 重试策略配置
     */
    public static class RetryConfig {
        private final int maxRetries;
        private final long initialDelayMs;
        private final long maxDelayMs;
        private final double backoffMultiplier;
        
        public RetryConfig(int maxRetries, long initialDelayMs, long maxDelayMs, double backoffMultiplier) {
            this.maxRetries = maxRetries;
            this.initialDelayMs = initialDelayMs;
            this.maxDelayMs = maxDelayMs;
            this.backoffMultiplier = backoffMultiplier;
        }
        
        public static RetryConfig getDefault() {
            return new RetryConfig(3, 1000, 10000, 2.0);
        }
        
        public static RetryConfig getPollingConfig() {
            return new RetryConfig(60, 5000, 5000, 1.0);
        }
        
        public int getMaxRetries() { return maxRetries; }
        public long getInitialDelayMs() { return initialDelayMs; }
        public long getMaxDelayMs() { return maxDelayMs; }
        public double getBackoffMultiplier() { return backoffMultiplier; }
        
        /**
         * 计算下次重试的延迟时间
         */
        public long getDelayForRetry(int retryCount) {
            long delay = (long) (initialDelayMs * Math.pow(backoffMultiplier, retryCount));
            return Math.min(delay, maxDelayMs);
        }
    }
}