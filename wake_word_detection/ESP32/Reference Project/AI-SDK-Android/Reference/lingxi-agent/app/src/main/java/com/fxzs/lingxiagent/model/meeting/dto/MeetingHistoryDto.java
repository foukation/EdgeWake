package com.fxzs.lingxiagent.model.meeting.dto;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * 会议历史记录DTO
 */
public class MeetingHistoryDto implements Serializable {
    
    @SerializedName("id")
    private Long id;
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("content")
    private String content;
    
    @SerializedName("fileUrl")
    private String fileUrl;
    
    @SerializedName("type")
    private Integer type;
    
    @SerializedName("abstractText")
    private String abstractText;
    
    @SerializedName("meetingText")
    private String meetingText;
    
    @SerializedName("topic")
    private String topic;
    
    @SerializedName("conversionId")
    private String conversionId;
    
    @SerializedName("userId")
    private Long userId;
    
    @SerializedName("taskId")
    private Long taskId;
    
    @SerializedName("top")
    private Integer top;
    
    @SerializedName("meetingTranscription")
    private String meetingTranscription;
    
    @SerializedName("abstractChapterText")
    private String abstractChapterText;
    
    @SerializedName("abstractDetailText")
    private String abstractDetailText;
    
    @SerializedName("abstractOptimizeText")
    private String abstractOptimizeText;
    
    @SerializedName("createTime")
    private long createTime;
    
    @SerializedName("updateTime")
    private String updateTime;
    
    @SerializedName("r1")
    private String r1;
    
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
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getFileUrl() {
        return fileUrl;
    }
    
    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }
    
    public Integer getType() {
        return type;
    }
    
    public void setType(Integer type) {
        this.type = type;
    }
    
    public String getAbstractText() {
        return abstractText;
    }
    
    public void setAbstractText(String abstractText) {
        this.abstractText = abstractText;
    }
    
    public String getMeetingText() {
        return meetingText;
    }
    
    public void setMeetingText(String meetingText) {
        this.meetingText = meetingText;
    }
    
    public String getTopic() {
        return topic;
    }
    
    public void setTopic(String topic) {
        this.topic = topic;
    }
    
    public String getConversionId() {
        return conversionId;
    }
    
    public void setConversionId(String conversionId) {
        this.conversionId = conversionId;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Long getTaskId() {
        return taskId;
    }
    
    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }
    
    public Integer getTop() {
        return top;
    }
    
    public void setTop(Integer top) {
        this.top = top;
    }
    
    public String getMeetingTranscription() {
        return meetingTranscription;
    }
    
    public void setMeetingTranscription(String meetingTranscription) {
        this.meetingTranscription = meetingTranscription;
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
    
    public long getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }
    
    public String getUpdateTime() {
        return updateTime;
    }
    
    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }
    
    public String getR1() {
        return r1;
    }
    
    public void setR1(String r1) {
        this.r1 = r1;
    }
}
