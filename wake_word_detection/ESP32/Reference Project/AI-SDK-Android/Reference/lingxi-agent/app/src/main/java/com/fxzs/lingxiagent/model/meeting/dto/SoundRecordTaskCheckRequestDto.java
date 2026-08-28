package com.fxzs.lingxiagent.model.meeting.dto;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * 录音识别任务结果查询请求DTO
 */
public class SoundRecordTaskCheckRequestDto implements Serializable {
    
    @SerializedName("fileUrl")
    private String fileUrl;
    
    @SerializedName("engineModelType")
    private String engineModelType;
    
    @SerializedName("channelNum")
    private Integer channelNum;
    
    @SerializedName("resTextFormat")
    private Integer resTextFormat;
    
    @SerializedName("sourceType")
    private Integer sourceType;
    
    @SerializedName("taskId")
    private Long taskId;
    
    @SerializedName("meetingId")
    private Integer meetingId;
    
    public SoundRecordTaskCheckRequestDto() {
    }
    
    public SoundRecordTaskCheckRequestDto(Long taskId, Integer meetingId) {
        this.taskId = taskId;
        this.meetingId = meetingId;
    }
    
    public String getFileUrl() {
        return fileUrl;
    }
    
    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }
    
    public String getEngineModelType() {
        return engineModelType;
    }
    
    public void setEngineModelType(String engineModelType) {
        this.engineModelType = engineModelType;
    }
    
    public Integer getChannelNum() {
        return channelNum;
    }
    
    public void setChannelNum(Integer channelNum) {
        this.channelNum = channelNum;
    }
    
    public Integer getResTextFormat() {
        return resTextFormat;
    }
    
    public void setResTextFormat(Integer resTextFormat) {
        this.resTextFormat = resTextFormat;
    }
    
    public Integer getSourceType() {
        return sourceType;
    }
    
    public void setSourceType(Integer sourceType) {
        this.sourceType = sourceType;
    }
    
    public Long getTaskId() {
        return taskId;
    }
    
    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }
    
    public Integer getMeetingId() {
        return meetingId;
    }
    
    public void setMeetingId(Integer meetingId) {
        this.meetingId = meetingId;
    }
    
    @Override
    public String toString() {
        return "SoundRecordTaskCheckRequestDto{" +
                "fileUrl='" + fileUrl + '\'' +
                ", engineModelType='" + engineModelType + '\'' +
                ", channelNum=" + channelNum +
                ", resTextFormat=" + resTextFormat +
                ", sourceType=" + sourceType +
                ", taskId=" + taskId +
                ", meetingId=" + meetingId +
                '}';
    }
}