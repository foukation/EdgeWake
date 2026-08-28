package com.fxzs.lingxiagent.model.common;

/**
 * API异常类
 */
public class ApiException extends RuntimeException {
    
    private int code;
    private String displayMessage;
    
    public ApiException(int code, String message) {
        super(message);
        this.code = code;
        this.displayMessage = message;
    }
    
    public ApiException(String message) {
        super(message);
        this.code = -1;
        this.displayMessage = message;
    }
    
    public ApiException(Throwable cause) {
        super(cause);
        this.code = -1;
        this.displayMessage = "网络请求失败";
    }
    
    public int getCode() {
        return code;
    }
    
    public String getDisplayMessage() {
        return displayMessage != null ? displayMessage : getMessage();
    }
    
    @Override
    public String toString() {
        return "ApiException{" +
                "code=" + code +
                ", message='" + getMessage() + '\'' +
                '}';
    }
}