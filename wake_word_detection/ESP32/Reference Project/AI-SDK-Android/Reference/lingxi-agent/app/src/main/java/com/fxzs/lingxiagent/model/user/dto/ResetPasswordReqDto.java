package com.fxzs.lingxiagent.model.user.dto;

import com.fxzs.lingxiagent.model.network.SignVerifier;
import com.fxzs.lingxiagent.util.AesUtil;

public class ResetPasswordReqDto {
    private String mobile;
    private String password;
    private String code;

    public ResetPasswordReqDto(String mobile, String code, String password) {
        this.mobile = AesUtil.encrypt(mobile, SignVerifier.getClientId());
        this.code = AesUtil.encrypt(code, SignVerifier.getClientId());
        this.password = AesUtil.encrypt(password, SignVerifier.getClientId());
    }
    
    // Getters and Setters
    public String getMobile() {
        return mobile;
    }
    
    public void setMobile(String mobile) {
        this.mobile = mobile;
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