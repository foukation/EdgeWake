package com.fxzs.lingxiagent.model.meeting.dto;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * 录音识别任务响应DTO
 */
public class SoundRecordTaskResponseDto implements Serializable {
    
    @SerializedName("header")
    private Object header;
    
    @SerializedName("skipSign")
    private Boolean skipSign;
    
    @SerializedName("data")
    private TaskData data;
    
    @SerializedName("requestId")
    private String requestId;
    
    @SerializedName("stream")
    private Boolean stream;
    
    /**
     * 嵌套的任务数据结构
     */
    public static class TaskData implements Serializable {
        @SerializedName("header")
        private Object header;
        
        @SerializedName("skipSign")
        private Boolean skipSign;
        
        @SerializedName("statusStr")
        private String statusStr; // "doing", "completed", "failed"
        
        @SerializedName("status")
        private Integer status; // 数字状态：1=处理中, 2=完成, 3=失败
        
        @SerializedName("resultDetail")
        private Object resultDetail;
        
        @SerializedName("audioDuration")
        private Double audioDuration;
        
        @SerializedName("result")
        private String result; // 识别结果文本
        
        @SerializedName("errorMsg")
        private String errorMsg;
        
        @SerializedName("taskId")
        private Long taskId;
        
        @SerializedName("stream")
        private Boolean stream;
        
        // Getters and Setters
        public Object getHeader() { return header; }
        public void setHeader(Object header) { this.header = header; }
        
        public Boolean getSkipSign() { return skipSign; }
        public void setSkipSign(Boolean skipSign) { this.skipSign = skipSign; }
        
        public String getStatusStr() { return statusStr; }
        public void setStatusStr(String statusStr) { this.statusStr = statusStr; }
        
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
        
        public Object getResultDetail() { return resultDetail; }
        public void setResultDetail(Object resultDetail) { this.resultDetail = resultDetail; }
        
        public Double getAudioDuration() { return audioDuration; }
        public void setAudioDuration(Double audioDuration) { this.audioDuration = audioDuration; }
        
        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }
        
        public String getErrorMsg() { return errorMsg; }
        public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }
        
        public Long getTaskId() { return taskId; }
        public void setTaskId(Long taskId) { this.taskId = taskId; }
        
        public Boolean getStream() { return stream; }
        public void setStream(Boolean stream) { this.stream = stream; }
    }
    
    // 保持原有的字段用于向后兼容
    @SerializedName("taskId")
    private Integer taskId;
    
    @SerializedName("status")
    private String status; // processing, completed, failed
    
    @SerializedName("result")
    private String result; // 识别结果文本
    
    @SerializedName("message")
    private String message;
    
    @SerializedName("progress")
    private Integer progress; // 处理进度百分比
    
    @SerializedName("meetingId")
    private Integer meetingId;
    
    public SoundRecordTaskResponseDto() {
    }
    
    // 新增的主要字段的getter和setter
    public Object getHeader() {
        return header;
    }
    
    public void setHeader(Object header) {
        this.header = header;
    }
    
    public Boolean getSkipSign() {
        return skipSign;
    }
    
    public void setSkipSign(Boolean skipSign) {
        this.skipSign = skipSign;
    }
    
    public TaskData getData() {
        return data;
    }
    
    public void setData(TaskData data) {
        this.data = data;
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public Boolean getStream() {
        return stream;
    }
    
    public void setStream(Boolean stream) {
        this.stream = stream;
    }
    
    // 兼容性方法：从嵌套结构中获取数据
    public Integer getTaskId() {
        if (data != null && data.getTaskId() != null) {
            return data.getTaskId().intValue();
        }
        return taskId;
    }
    
    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
    }
    
    public String getStatus() {
        if (data != null && data.getStatusStr() != null) {
            // 将API返回的状态字符串映射为标准状态
            switch (data.getStatusStr()) {
                case "doing":
                    return "processing";
                case "success":
                    return "completed";
                case "failed":
                    return "failed";
                default:
                    return data.getStatusStr();
            }
        }
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getResult() {
        if (data != null && data.getResult() != null) {
            return data.getResult();
        }
        return result;
    }
    
    public void setResult(String result) {
        this.result = result;
    }
    
    public String getMessage() {
        if (data != null && data.getErrorMsg() != null) {
            return data.getErrorMsg();
        }
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Integer getProgress() {
        // 根据状态推断进度
        if (data != null) {
            if (data.getStatus() != null) {
                switch (data.getStatus()) {
                    case 1: // 处理中
                        return 50;
                    case 2: // 完成
                        return 100;
                    case 3: // 失败
                        return 0;
                }
            }
        }
        return progress;
    }
    
    public void setProgress(Integer progress) {
        this.progress = progress;
    }
    
    public Integer getMeetingId() {
        return meetingId;
    }
    
    public void setMeetingId(Integer meetingId) {
        this.meetingId = meetingId;
    }
    
    @Override
    public String toString() {
        return "SoundRecordTaskResponseDto{" +
                "header=" + header +
                ", skipSign=" + skipSign +
                ", data=" + data +
                ", requestId='" + requestId + '\'' +
                ", stream=" + stream +
                ", taskId=" + getTaskId() +
                ", status='" + getStatus() + '\'' +
                ", result='" + (getResult() != null ? getResult().substring(0, Math.min(getResult().length(), 100)) + "..." : "null") + '\'' +
                ", message='" + getMessage() + '\'' +
                ", progress=" + getProgress() +
                ", meetingId=" + meetingId +
                '}';
    }
}