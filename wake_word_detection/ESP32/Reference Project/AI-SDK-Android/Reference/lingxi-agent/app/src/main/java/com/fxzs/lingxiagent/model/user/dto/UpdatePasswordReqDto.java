package com.fxzs.lingxiagent.model.user.dto;

public class UpdatePasswordReqDto {
    private String oldPassword;
    private String password;
    private String code;
    
    // Getters and Setters

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
}