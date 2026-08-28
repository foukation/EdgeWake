package com.fxzs.lingxiagent.model.billing.model;

public class BaseResponse<T> {

    private int code;
    private String msg;

    private Boolean success;
    private T data;

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    public T getData() {
        return data;
    }

    public Boolean getSuccess() {
        return success;
    }
}