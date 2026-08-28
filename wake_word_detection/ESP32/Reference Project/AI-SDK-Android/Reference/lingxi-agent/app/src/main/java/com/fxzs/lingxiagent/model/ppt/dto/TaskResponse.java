package com.fxzs.lingxiagent.model.ppt.dto;

import com.google.gson.annotations.SerializedName;

/**
 * 任务响应DTO
 */
public class TaskResponse {
    
    @SerializedName("task_id")
    private String taskId;
    
    @SerializedName("ppt_id")
    private String pptId;
    
    @SerializedName("status")
    private String status;
    
    @SerializedName("message")
    private String message;
    
    @SerializedName("created_at")
    private String createdAt;
    
    @SerializedName("estimated_time")
    private int estimatedTime; // 预计完成时间（秒）
    
    public TaskResponse() {
    }
    
    public TaskResponse(String taskId, String pptId, String status) {
        this.taskId = taskId;
        this.pptId = pptId;
        this.status = status;
    }
    
    // Getters and Setters
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    public String getPptId() {
        return pptId;
    }
    
    public void setPptId(String pptId) {
        this.pptId = pptId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
    public int getEstimatedTime() {
        return estimatedTime;
    }
    
    public void setEstimatedTime(int estimatedTime) {
        this.estimatedTime = estimatedTime;
    }
    
    /**
     * 检查任务是否成功创建
     */
    public boolean isSuccess() {
        return taskId != null && !taskId.isEmpty();
    }
}