package com.fxzs.lingxiagent.model.api;

public class ApiResponse {
    private String status;
    private String data;
    private String message;
    
    public ApiResponse() {
    }
    
    public ApiResponse(String status, String data, String message) {
        this.status = status;
        this.data = data;
        this.message = message;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getData() {
        return data;
    }
    
    public void setData(String data) {
        this.data = data;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}