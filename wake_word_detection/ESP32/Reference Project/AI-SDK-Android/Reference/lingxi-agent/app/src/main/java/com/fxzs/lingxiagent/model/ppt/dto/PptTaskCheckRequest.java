package com.fxzs.lingxiagent.model.ppt.dto;

import com.google.gson.annotations.SerializedName;

/**
 * PPT任务状态查询请求DTO
 */
public class PptTaskCheckRequest {
    
    @SerializedName("taskId")
    private String taskId;
    
    public PptTaskCheckRequest() {
    }
    
    public PptTaskCheckRequest(String taskId) {
        this.taskId = taskId;
    }
    
    // Getters and Setters
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    @Override
    public String toString() {
        return "PptTaskCheckRequest{" +
                "taskId='" + taskId + '\'' +
                '}';
    }
}