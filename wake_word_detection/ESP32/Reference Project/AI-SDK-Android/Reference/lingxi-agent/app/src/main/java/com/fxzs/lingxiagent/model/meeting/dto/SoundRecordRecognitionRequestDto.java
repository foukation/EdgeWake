package com.fxzs.lingxiagent.model.meeting.dto;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * 录音识别任务提交请求DTO
 */
public class SoundRecordRecognitionRequestDto implements Serializable {
    
    @SerializedName("fileUrl")
    private String fileUrl;
    
    @SerializedName("engineModelType")
    private String engineModelType = "16k_zh_large";
    
    @SerializedName("meetingId")
    private Integer meetingId;
    
    public SoundRecordRecognitionRequestDto() {
    }
    
    public SoundRecordRecognitionRequestDto(String fileUrl, Integer meetingId) {
        this.fileUrl = fileUrl;
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
    
    public Integer getMeetingId() {
        return meetingId;
    }
    
    public void setMeetingId(Integer meetingId) {
        this.meetingId = meetingId;
    }
    
    @Override
    public String toString() {
        return "SoundRecordRecognitionRequestDto{" +
                "fileUrl='" + fileUrl + '\'' +
                ", engineModelType='" + engineModelType + '\'' +
                ", meetingId=" + meetingId +
                '}';
    }
}