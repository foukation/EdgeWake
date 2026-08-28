package com.fxzs.lingxiagent.model.meeting.dto;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * 录音识别任务提交响应DTO
 * 处理嵌套的服务器响应结构
 */
public class SoundRecordTaskSubmitResponseDto implements Serializable {
    
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
    
    public static class TaskData implements Serializable {
        @SerializedName("header")
        private Object header;
        
        @SerializedName("skipSign")
        private Boolean skipSign;
        
        @SerializedName("taskId")
        private Long taskId;  // 使用Long因为服务器返回的是长整型
        
        @SerializedName("stream")
        private Boolean stream;
        
        public Long getTaskId() {
            return taskId;
        }
        
        public void setTaskId(Long taskId) {
            this.taskId = taskId;
        }
        
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
        
        public Boolean getStream() {
            return stream;
        }
        
        public void setStream(Boolean stream) {
            this.stream = stream;
        }
        
        @Override
        public String toString() {
            return "TaskData{" +
                    "header=" + header +
                    ", skipSign=" + skipSign +
                    ", taskId=" + taskId +
                    ", stream=" + stream +
                    '}';
        }
    }
    
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
    
    @Override
    public String toString() {
        return "SoundRecordTaskSubmitResponseDto{" +
                "header=" + header +
                ", skipSign=" + skipSign +
                ", data=" + data +
                ", requestId='" + requestId + '\'' +
                ", stream=" + stream +
                '}';
    }
}