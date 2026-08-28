package com.fxzs.lingxiagent.model.ppt.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 大纲生成响应DTO
 */
public class OutlineResponse {
    
    @SerializedName("ppt_id")
    private String pptId;
    
    @SerializedName("title")
    private String title;
    
    @SerializedName("outline")
    private List<OutlineItem> outline;
    
    @SerializedName("total_slides")
    private int totalSlides;
    
    @SerializedName("estimated_duration")
    private int estimatedDuration; // 预计演示时长（分钟）
    
    @SerializedName("status")
    private String status;
    
    @SerializedName("created_at")
    private String createdAt;
    
    public OutlineResponse() {
    }
    
    // Getters and Setters
    public String getPptId() {
        return pptId;
    }
    
    public void setPptId(String pptId) {
        this.pptId = pptId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public List<OutlineItem> getOutline() {
        return outline;
    }
    
    public void setOutline(List<OutlineItem> outline) {
        this.outline = outline;
    }
    
    public int getTotalSlides() {
        return totalSlides;
    }
    
    public void setTotalSlides(int totalSlides) {
        this.totalSlides = totalSlides;
    }
    
    public int getEstimatedDuration() {
        return estimatedDuration;
    }
    
    public void setEstimatedDuration(int estimatedDuration) {
        this.estimatedDuration = estimatedDuration;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
    /**
     * 检查大纲是否有效
     */
    public boolean isValid() {
        return outline != null && !outline.isEmpty() && pptId != null;
    }
}