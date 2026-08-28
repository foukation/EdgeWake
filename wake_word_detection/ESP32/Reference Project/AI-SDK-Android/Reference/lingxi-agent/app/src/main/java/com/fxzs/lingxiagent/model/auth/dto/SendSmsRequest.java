package com.fxzs.lingxiagent.model.auth.dto;

import com.fxzs.lingxiagent.model.network.SignVerifier;
import com.fxzs.lingxiagent.util.AesUtil;
import com.google.gson.annotations.SerializedName;

/**
 * 发送验证码请求参数
 */
public class SendSmsRequest {
    
    @SerializedName("mobile")
    private String mobile;
    
    @SerializedName("scene")
    private Integer scene; // 场景：1-登录，2-注册，3-重置密码，4-更换手机号
    
    public SendSmsRequest() {
    }
    
    public SendSmsRequest(String mobile, Integer scene) {
        this.mobile = AesUtil.encrypt(mobile, SignVerifier.getClientId());
        this.scene = scene;
    }
    
    public String getMobile() {
        return mobile;
    }
    
    public void setMobile(String mobile) {
        this.mobile = mobile;
    }
    
    public Integer getScene() {
        return scene;
    }
    
    public void setScene(Integer scene) {
        this.scene = scene;
    }
}