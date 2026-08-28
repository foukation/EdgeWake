package com.fxzs.lingxiagent.model.user.dto;

public class UpdateMobileReqDto {
    private String oldMobile;
    private String mobile;
    private String code;
    
    // Getters and Setters
    public String getOldMobile() {
        return oldMobile;
    }

    public void setOldMobile(String oldMobile) {
        this.oldMobile = oldMobile;
    }

    public String getMobile() {
        return mobile;
    }
    
    public void setMobile(String mobile) {
        this.mobile = mobile;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
}