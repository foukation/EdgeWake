package com.fxzs.lingxiagent.model.ppt.dto;

import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.List;

/**
 * PPT项目数据模型
 */
public class PptProject {
    
    @SerializedName("id")
    private String id;
    
    @SerializedName("title")
    private String title;
    
    @SerializedName("topic")
    private String topic;
    
    @SerializedName("status")
    private PptStatus status;
    
    @SerializedName("template_id")
    private String templateId;
    
    @SerializedName("outline")
    private List<OutlineItem> outline;
    
    @SerializedName("slides")
    private List<PptSlide> slides;
    
    @SerializedName("created_at")
    private Date createdAt;
    
    @SerializedName("updated_at")
    private Date updatedAt;
    
    @SerializedName("download_url")
    private String downloadUrl;
    
    @SerializedName("preview_url")
    private String previewUrl;
    
    public PptProject() {
    }
    
    public PptProject(String id, String title, String topic) {
        this.id = id;
        this.title = title;
        this.topic = topic;
        this.status = PptStatus.OUTLINE_GENERATING;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getTopic() {
        return topic;
    }
    
    public void setTopic(String topic) {
        this.topic = topic;
    }
    
    public PptStatus getStatus() {
        return status;
    }
    
    public void setStatus(PptStatus status) {
        this.status = status;
    }
    
    public String getTemplateId() {
        return templateId;
    }
    
    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }
    
    public List<OutlineItem> getOutline() {
        return outline;
    }
    
    public void setOutline(List<OutlineItem> outline) {
        this.outline = outline;
    }
    
    public List<PptSlide> getSlides() {
        return slides;
    }
    
    public void setSlides(List<PptSlide> slides) {
        this.slides = slides;
    }
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
    public Date getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public String getDownloadUrl() {
        return downloadUrl;
    }
    
    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }
    
    public String getPreviewUrl() {
        return previewUrl;
    }
    
    public void setPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
    }
    
    /**
     * 获取PPT文件URL（优先返回下载链接）
     */
    public String getPptUrl() {
        return downloadUrl != null ? downloadUrl : previewUrl;
    }

    /**
     * 设置PPT文件URL（设置为下载链接）
     */
    public void setPptUrl(String pptUrl) {
        this.downloadUrl = pptUrl;
    }
    
    /**
     * 检查PPT是否已完成生成
     */
    public boolean isCompleted() {
        return status == PptStatus.PPT_READY;
    }
    
    /**
     * 检查是否正在生成中
     */
    public boolean isGenerating() {
        return status == PptStatus.OUTLINE_GENERATING || status == PptStatus.PPT_GENERATING;
    }
    
    /**
     * 检查是否生成失败
     */
    public boolean isFailed() {
        return status == PptStatus.FAILED;
    }
}