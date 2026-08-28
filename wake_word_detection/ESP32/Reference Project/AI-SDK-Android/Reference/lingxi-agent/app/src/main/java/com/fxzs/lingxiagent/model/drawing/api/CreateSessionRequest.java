package com.fxzs.lingxiagent.model.drawing.api;

import java.io.Serializable;

/**
 * 创建会话请求
 */
public class CreateSessionRequest implements Serializable {
    
    private String name;        // 会话名称
    private String firstPrompt; // 首次提示词
    
    public CreateSessionRequest() {}
    
    public CreateSessionRequest(String name, String firstPrompt) {
        this.name = name;
        this.firstPrompt = firstPrompt;
    }
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getFirstPrompt() {
        return firstPrompt;
    }
    
    public void setFirstPrompt(String firstPrompt) {
        this.firstPrompt = firstPrompt;
    }
}