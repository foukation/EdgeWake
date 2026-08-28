package com.fxzs.lingxiagent.model.drawing.api;

import java.io.Serializable;

/**
 * 更新会话请求
 */
public class UpdateSessionRequest implements Serializable {
    
    private String name;           // 会话名称
    private String lastImageUrl;   // 最后一张图片URL
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getLastImageUrl() {
        return lastImageUrl;
    }
    
    public void setLastImageUrl(String lastImageUrl) {
        this.lastImageUrl = lastImageUrl;
    }
}