package com.fxzs.lingxiagent.model.ppt.dto;

/**
 * PPT生成结果包装类
 */
public class PptGenerationResult {
    
    private boolean success;
    private String pptId;
    private String taskId;
    private String message;
    private String downloadUrl;
    private String previewUrl;
    private Throwable error;
    
    private PptGenerationResult(boolean success) {
        this.success = success;
    }
    
    /**
     * 创建成功结果
     */
    public static PptGenerationResult success(String pptId, String downloadUrl) {
        PptGenerationResult result = new PptGenerationResult(true);
        result.pptId = pptId;
        result.downloadUrl = downloadUrl;
        return result;
    }
    
    /**
     * 创建成功结果（带任务ID）
     */
    public static PptGenerationResult success(String pptId, String taskId, String downloadUrl) {
        PptGenerationResult result = new PptGenerationResult(true);
        result.pptId = pptId;
        result.taskId = taskId;
        result.downloadUrl = downloadUrl;
        return result;
    }
    
    /**
     * 创建失败结果
     */
    public static PptGenerationResult failure(String message) {
        PptGenerationResult result = new PptGenerationResult(false);
        result.message = message;
        return result;
    }
    
    /**
     * 创建失败结果（带异常）
     */
    public static PptGenerationResult failure(String message, Throwable error) {
        PptGenerationResult result = new PptGenerationResult(false);
        result.message = message;
        result.error = error;
        return result;
    }
    
    /**
     * 创建进行中结果
     */
    public static PptGenerationResult inProgress(String taskId, String message) {
        PptGenerationResult result = new PptGenerationResult(false);
        result.taskId = taskId;
        result.message = message;
        return result;
    }
    
    // Getters
    public boolean isSuccess() {
        return success;
    }
    
    public String getPptId() {
        return pptId;
    }
    
    public String getTaskId() {
        return taskId;
    }
    
    public String getMessage() {
        return message;
    }
    
    public String getDownloadUrl() {
        return downloadUrl;
    }
    
    public String getPreviewUrl() {
        return previewUrl;
    }
    
    public Throwable getError() {
        return error;
    }
    
    public void setPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
    }
    
    /**
     * 检查是否有下载链接
     */
    public boolean hasDownloadUrl() {
        return downloadUrl != null && !downloadUrl.isEmpty();
    }
    
    /**
     * 检查是否有预览链接
     */
    public boolean hasPreviewUrl() {
        return previewUrl != null && !previewUrl.isEmpty();
    }
}