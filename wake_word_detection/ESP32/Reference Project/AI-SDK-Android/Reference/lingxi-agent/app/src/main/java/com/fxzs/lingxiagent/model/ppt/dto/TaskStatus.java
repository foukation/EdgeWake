package com.fxzs.lingxiagent.model.ppt.dto;

import com.google.gson.annotations.SerializedName;

/**
 * 任务状态DTO
 */
public class TaskStatus {
    
    @SerializedName("task_id")
    private String taskId;
    
    @SerializedName("ppt_id")
    private String pptId;
    
    @SerializedName("status")
    private String status;
    
    @SerializedName("progress")
    private int progress; // 进度百分比 0-100
    
    @SerializedName("current_step")
    private String currentStep;
    
    @SerializedName("result")
    private String result; // 完成时的结果URL或数据
    
    @SerializedName("error_message")
    private String errorMessage;
    
    @SerializedName("updated_at")
    private String updatedAt;
    
    @SerializedName("estimated_remaining_time")
    private int estimatedRemainingTime; // 预计剩余时间（秒）
    
    // PPT特定字段
    @SerializedName("ppt_status")
    private String pptStatus;
    
    @SerializedName("ppt_url")
    private String pptUrl;
    
    @SerializedName("err_msg")
    private String errMsg;
    
    @SerializedName("total_pages")
    private int totalPages;
    
    @SerializedName("done_pages")
    private int donePages;
    
    public TaskStatus() {
    }
    
    public TaskStatus(String taskId, String status, int progress) {
        this.taskId = taskId;
        this.status = status;
        this.progress = progress;
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
    
    public int getProgress() {
        return progress;
    }
    
    public void setProgress(int progress) {
        this.progress = progress;
    }
    
    public String getCurrentStep() {
        return currentStep;
    }
    
    public void setCurrentStep(String currentStep) {
        this.currentStep = currentStep;
    }
    
    public String getResult() {
        return result;
    }
    
    public void setResult(String result) {
        this.result = result;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public int getEstimatedRemainingTime() {
        return estimatedRemainingTime;
    }
    
    public void setEstimatedRemainingTime(int estimatedRemainingTime) {
        this.estimatedRemainingTime = estimatedRemainingTime;
    }
    
    /**
     * 检查任务是否正在进行中
     */
    public boolean isInProgress() {
        return "running".equals(status) || "pending".equals(status);
    }
    
    /**
     * 检查任务是否已完成
     */
    public boolean isCompleted() {
        return "completed".equals(status) || "success".equals(status);
    }
    
    /**
     * 检查任务是否失败
     */
    public boolean isFailed() {
        return "failed".equals(status) || "error".equals(status);
    }
    
    // PPT特定方法
    public String getPptStatus() {
        return pptStatus;
    }
    
    public void setPptStatus(String pptStatus) {
        this.pptStatus = pptStatus;
    }
    
    public String getPptUrl() {
        return pptUrl;
    }
    
    public void setPptUrl(String pptUrl) {
        this.pptUrl = pptUrl;
    }
    
    public String getErrMsg() {
        return errMsg;
    }
    
    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
    }
    
    public int getTotalPages() {
        return totalPages;
    }
    
    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
    
    public int getDonePages() {
        return donePages;
    }
    
    public void setDonePages(int donePages) {
        this.donePages = donePages;
    }
    
    /**
     * 获取进度描述
     */
    public String getProgressDescription() {
        if (currentStep != null && !currentStep.isEmpty()) {
            return currentStep + " (" + progress + "%)";
        }
        return progress + "%";
    }
}