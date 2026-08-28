package com.fxzs.lingxiagent.model.drawing.dto;

import java.io.Serializable;
import java.util.List;

/**
 * 绘画会话DTO
 */
public class DrawingSessionDto implements Serializable {
    
    private Long id;
    private String name;              // 会话名称
    private String firstPrompt;       // 首次提示词
    private String lastImageUrl;      // 最后一张图片URL
    private Integer imageCount;       // 图片数量
    private String createTime;        // 创建时间
    private String updateTime;        // 更新时间
    private List<DrawingImageDto> images; // 会话中的图片列表
    private List<DrawingImageDto> aiImageList; // API返回的图片列表字段
    private DrawingImageDto latestImage; // 历史中用

    private String picUrl;
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getFirstPrompt() {
        return firstPrompt;
    }
    
    public void setFirstPrompt(String firstPrompt) {
        this.firstPrompt = firstPrompt;
    }
    
    public String getLastImageUrl() {
        return lastImageUrl;
    }
    
    public void setLastImageUrl(String lastImageUrl) {
        this.lastImageUrl = lastImageUrl;
    }
    
    public Integer getImageCount() {
        return imageCount;
    }
    
    public void setImageCount(Integer imageCount) {
        this.imageCount = imageCount;
    }
    
    public String getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }
    
    public String getUpdateTime() {
        return updateTime;
    }
    
    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }
    
    public List<DrawingImageDto> getImages() {
        return images;
    }
    
    public void setImages(List<DrawingImageDto> images) {
        this.images = images;
    }
    
    public List<DrawingImageDto> getAiImageList() {
        return aiImageList;
    }
    
    public void setAiImageList(List<DrawingImageDto> aiImageList) {
        this.aiImageList = aiImageList;
    }

    public String getPicUrl() {
        return picUrl;
    }

    public void setPicUrl(String picUrl) {
        this.picUrl = picUrl;
    }

    public DrawingImageDto getLatestImage() {
        return latestImage;
    }

    public void setLatestImage(DrawingImageDto latestImage) {
        this.latestImage = latestImage;
    }
}