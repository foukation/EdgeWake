package com.fxzs.lingxiagent.model.ppt.dto;

import com.google.gson.annotations.SerializedName;

/**
 * PPT会话创建请求DTO
 */
public class PptSessionCreateRequest {
    
    @SerializedName("title")
    private String title;
    
    public PptSessionCreateRequest() {
    }
    
    public PptSessionCreateRequest(String title) {
        this.title = title;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    @Override
    public String toString() {
        return "PptSessionCreateRequest{" +
                "title='" + title + '\'' +
                '}';
    }
}
