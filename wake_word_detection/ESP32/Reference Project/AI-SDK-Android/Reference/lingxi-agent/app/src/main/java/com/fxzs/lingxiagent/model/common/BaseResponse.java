package com.fxzs.lingxiagent.model.common;

import com.google.gson.annotations.SerializedName;

/**
 * 通用API响应包装类
 * @param <T> 响应数据类型
 */
public class BaseResponse<T> {
    
    @SerializedName("code")
    private int code;
    
    @SerializedName("msg")
    private String message;
    
    @SerializedName("data")
    private T data;
    
    @SerializedName("success")
    private boolean success;
    
    public BaseResponse() {
    }
    
    public BaseResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.success = code == 0 || code == 200;
    }
    
    public int getCode() {
        return code;
    }
    
    public void setCode(int code) {
        this.code = code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public String getMsg() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public T getData() {
        return data;
    }
    
    public void setData(T data) {
        this.data = data;
    }
    
    public boolean isSuccess() {
        return success || code == 0 || code == 200;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
}