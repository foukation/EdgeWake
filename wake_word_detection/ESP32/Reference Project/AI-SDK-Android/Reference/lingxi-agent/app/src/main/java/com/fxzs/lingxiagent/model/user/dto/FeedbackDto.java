package com.fxzs.lingxiagent.model.user.dto;

import java.util.List;

public class FeedbackDto {
    private Long id;
    private String title;
    private String content;
    private String contactInfo;
    private String contact; // API返回的contact字段
    private List<String> imageUrls;
    private List<String> images; // API可能返回images字段
    private Long createTime; // API返回的创建时间戳
    private Long updateTime; // API返回的更新时间戳
    private Long replyTime; // API返回的回复时间戳
    private Integer status; // 0: 待处理, 1: 已处理, 2: 已回复
    private String reply; // 官方回复内容

    // 兼容字段，用于显示格式化后的时间
    private String createTimeStr;
    private String replyTimeStr;
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getContactInfo() {
        return contactInfo;
    }
    
    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }
    
    public List<String> getImageUrls() {
        return imageUrls;
    }
    
    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }
    
    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
    }
    
    public String getReply() {
        return reply;
    }
    
    public void setReply(String reply) {
        this.reply = reply;
    }
    
    public Long getReplyTime() {
        return replyTime;
    }

    public void setReplyTime(Long replyTime) {
        this.replyTime = replyTime;
    }
    
    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public Long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Long updateTime) {
        this.updateTime = updateTime;
    }

    public String getCreateTimeStr() {
        return createTimeStr;
    }

    public void setCreateTimeStr(String createTimeStr) {
        this.createTimeStr = createTimeStr;
    }

    public String getReplyTimeStr() {
        return replyTimeStr;
    }

    public void setReplyTimeStr(String replyTimeStr) {
        this.replyTimeStr = replyTimeStr;
    }
}