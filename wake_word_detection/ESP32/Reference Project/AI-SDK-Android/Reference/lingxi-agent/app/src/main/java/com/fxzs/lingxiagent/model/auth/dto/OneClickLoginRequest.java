package com.fxzs.lingxiagent.model.auth.dto;

/**
 * 一键登录请求参数
 */
public class OneClickLoginRequest {
    
    private String token;
    
    public OneClickLoginRequest() {
        // 默认构造函数，用于反序列化
    }
    
    public OneClickLoginRequest(String token) {
        this.token = token;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
}