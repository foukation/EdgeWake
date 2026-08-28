package com.fxzs.lingxiagent.model.meeting.dto;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class MeetingSummaryRequestDto implements Serializable {
    @SerializedName("botKey")
    private String botKey;
    
    @SerializedName("content")
    private String content;
    
    @SerializedName("meetingId")
    private Integer meetingId;
    
    public MeetingSummaryRequestDto() {}
    
    public MeetingSummaryRequestDto(String botKey, String content, Integer meetingId) {
        this.botKey = botKey;
        this.content = content;
        this.meetingId = meetingId;
    }
    
    public String getBotKey() {
        return botKey;
    }
    
    public void setBotKey(String botKey) {
        this.botKey = botKey;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public Integer getMeetingId() {
        return meetingId;
    }
    
    public void setMeetingId(Integer meetingId) {
        this.meetingId = meetingId;
    }
}