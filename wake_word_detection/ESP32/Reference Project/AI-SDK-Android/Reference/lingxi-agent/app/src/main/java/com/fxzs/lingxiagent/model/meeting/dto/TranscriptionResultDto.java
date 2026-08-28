package com.fxzs.lingxiagent.model.meeting.dto;

import java.io.Serializable;

/**
 * 转写结果DTO
 */
public class TranscriptionResultDto implements Serializable {
    
    private String taskId;
    private String status; // processing, completed, failed
    private String language;
    private String transcriptionText;
    private Long duration; // 音频时长（秒）
    private String fileUrl;
    private String createdAt;
    private String completedAt;
    
    public TranscriptionResultDto() {
    }
    
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
    
    public String getTranscriptionText() {
        return transcriptionText;
    }
    
    public void setTranscriptionText(String transcriptionText) {
        this.transcriptionText = transcriptionText;
    }
    
    public Long getDuration() {
        return duration;
    }
    
    public void setDuration(Long duration) {
        this.duration = duration;
    }
    
    public String getFileUrl() {
        return fileUrl;
    }
    
    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(String completedAt) {
        this.completedAt = completedAt;
    }
}