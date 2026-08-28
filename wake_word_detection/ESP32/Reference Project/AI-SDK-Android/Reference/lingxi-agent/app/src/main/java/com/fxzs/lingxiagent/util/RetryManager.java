package com.fxzs.lingxiagent.util;

import android.os.Handler;
import android.os.Looper;

import com.fxzs.lingxiagent.model.ppt.repository.PptErrorHandler;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 重试机制管理器
 * 提供智能重试策略和执行机制
 */
public class RetryManager {
    
    /**
     * 重试任务接口
     */
    public interface RetryTask {
        void execute();
    }
    
    /**
     * 重试回调接口
     */
    public interface RetryCallback {
        void onRetryStarted(int retryCount);
        void onRetryFailed(int retryCount, Throwable error);
        void onMaxRetriesReached(Throwable lastError);
        void onRetrySuccess(int retryCount);
    }
    
    private final Handler handler;
    private final AtomicInteger currentRetryCount;
    private PptErrorHandler.RetryConfig retryConfig;
    private RetryTask retryTask;
    private RetryCallback retryCallback;
    private Throwable lastError;
    private boolean isRetrying;
    
    public RetryManager() {
        this.handler = new Handler(Looper.getMainLooper());
        this.currentRetryCount = new AtomicInteger(0);
        this.retryConfig = PptErrorHandler.RetryConfig.getDefault();
        this.isRetrying = false;
    }
    
    public RetryManager(PptErrorHandler.RetryConfig config) {
        this();
        this.retryConfig = config;
    }
    
    /**
     * 开始重试任务
     */
    public void startRetry(RetryTask task, RetryCallback callback) {
        this.retryTask = task;
        this.retryCallback = callback;
        this.currentRetryCount.set(0);
        this.isRetrying = true;
        
        executeRetry();
    }
    
    /**
     * 执行重试
     */
    private void executeRetry() {
        if (!isRetrying) {
            return;
        }
        
        int retryCount = currentRetryCount.get();
        
        if (retryCount >= retryConfig.getMaxRetries()) {
            // 达到最大重试次数
            isRetrying = false;
            if (retryCallback != null) {
                retryCallback.onMaxRetriesReached(lastError);
            }
            return;
        }
        
        if (retryCallback != null) {
            retryCallback.onRetryStarted(retryCount);
        }
        
        try {
            if (retryTask != null) {
                retryTask.execute();
            }
            
            // 如果执行成功，停止重试
            onRetrySuccess();
            
        } catch (Exception e) {
            onRetryFailed(e);
        }
    }
    
    /**
     * 重试成功
     */
    public void onRetrySuccess() {
        isRetrying = false;
        if (retryCallback != null) {
            retryCallback.onRetrySuccess(currentRetryCount.get());
        }
    }
    
    /**
     * 重试失败
     */
    public void onRetryFailed(Throwable error) {
        this.lastError = error;
        int retryCount = currentRetryCount.incrementAndGet();
        
        if (retryCallback != null) {
            retryCallback.onRetryFailed(retryCount, error);
        }
        
        // 检查是否应该继续重试
        PptErrorHandler.ErrorInfo errorInfo = PptErrorHandler.handleError(error);
        if (!errorInfo.isRetryable()) {
            // 不可重试的错误，直接停止
            isRetrying = false;
            if (retryCallback != null) {
                retryCallback.onMaxRetriesReached(error);
            }
            return;
        }
        
        if (retryCount < retryConfig.getMaxRetries()) {
            // 计算延迟时间并安排下次重试
            long delay = retryConfig.getDelayForRetry(retryCount - 1);
            handler.postDelayed(this::executeRetry, delay);
        } else {
            // 达到最大重试次数
            isRetrying = false;
            if (retryCallback != null) {
                retryCallback.onMaxRetriesReached(error);
            }
        }
    }
    
    /**
     * 停止重试
     */
    public void stopRetry() {
        isRetrying = false;
        handler.removeCallbacksAndMessages(null);
    }
    
    /**
     * 重置重试状态
     */
    public void reset() {
        stopRetry();
        currentRetryCount.set(0);
        lastError = null;
    }
    
    /**
     * 是否正在重试
     */
    public boolean isRetrying() {
        return isRetrying;
    }
    
    /**
     * 获取当前重试次数
     */
    public int getCurrentRetryCount() {
        return currentRetryCount.get();
    }
    
    /**
     * 获取最后一次错误
     */
    public Throwable getLastError() {
        return lastError;
    }
    
    /**
     * 设置重试配置
     */
    public void setRetryConfig(PptErrorHandler.RetryConfig config) {
        this.retryConfig = config;
    }
    
    /**
     * 创建简单的重试管理器
     */
    public static RetryManager createSimple(int maxRetries, long delayMs) {
        PptErrorHandler.RetryConfig config = new PptErrorHandler.RetryConfig(
            maxRetries, delayMs, delayMs, 1.0
        );
        return new RetryManager(config);
    }
    
    /**
     * 创建指数退避重试管理器
     */
    public static RetryManager createExponentialBackoff(int maxRetries, long initialDelayMs) {
        PptErrorHandler.RetryConfig config = new PptErrorHandler.RetryConfig(
            maxRetries, initialDelayMs, 30000, 2.0
        );
        return new RetryManager(config);
    }
    
    /**
     * 销毁资源
     */
    public void destroy() {
        stopRetry();
        retryTask = null;
        retryCallback = null;
        lastError = null;
    }
}