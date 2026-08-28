package com.fxzs.lingxiagent.model.meeting.dto;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class MeetingDto implements Serializable {
    @SerializedName("id")
    private String id;

    @SerializedName("title")
    private String title;

    @SerializedName("content")
    private String content;

    @SerializedName("summary")
    private String summary;

    @SerializedName("createTime")
    private String createTime;

    @SerializedName("updateTime")
    private String updateTime;

    @SerializedName("duration")
    private String duration;

    @SerializedName("language")
    private String language;

    @SerializedName("conversionId")
    private String conversionId;

    @SerializedName("status")
    private Integer status; // 0: processing, 1: completed, 2: failed

    @SerializedName("audioUrl")
    private String audioUrl;

    @SerializedName("type")
    private Integer type; // 0: real-time, 1: uploaded audio
    
    @SerializedName("fileUrl")
    private String fileUrl; // 文件地址
    
    @SerializedName("name")
    private String name; // 会议名称/录音文件名称
    
    @SerializedName("abstractText")
    private String abstractText; // 摘要文本
    
    @SerializedName("abstractChapterText")
    private String abstractChapterText; // 按章节摘要
    
    @SerializedName("abstractDetailText")
    private String abstractDetailText; // 详细摘要
    
    @SerializedName("abstractOptimizeText")
    private String abstractOptimizeText; // 优化摘要（按主题）
    
    @SerializedName("userId")
    private Integer userId; // 用户ID

    @SerializedName("topic")
    private String topic; // 会议话题

    // 本地录音文件路径（不序列化到JSON）
    private transient String audioFilePath;

    // Getters and setters
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
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

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getConversionId() {
        return conversionId;
    }

    public void setConversionId(String conversionId) {
        this.conversionId = conversionId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }
    
    public String getFileUrl() {
        return fileUrl;
    }
    
    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getAbstractText() {
        return abstractText;
    }
    
    public void setAbstractText(String abstractText) {
        this.abstractText = abstractText;
    }
    
    public String getAbstractChapterText() {
        return abstractChapterText;
    }
    
    public void setAbstractChapterText(String abstractChapterText) {
        this.abstractChapterText = abstractChapterText;
    }
    
    public String getAbstractDetailText() {
        return abstractDetailText;
    }
    
    public void setAbstractDetailText(String abstractDetailText) {
        this.abstractDetailText = abstractDetailText;
    }
    
    public String getAbstractOptimizeText() {
        return abstractOptimizeText;
    }
    
    public void setAbstractOptimizeText(String abstractOptimizeText) {
        this.abstractOptimizeText = abstractOptimizeText;
    }
    
    public String getMeetingText() {
        return content;
    }

    public Integer getUserId() {
        return userId;
    }
    
    public void setUserId(Integer userId) {
        this.userId = userId;
    }
    
    public String getAudioFilePath() {
        return audioFilePath;
    }
    
    public void setAudioFilePath(String audioFilePath) {
        this.audioFilePath = audioFilePath;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }
}