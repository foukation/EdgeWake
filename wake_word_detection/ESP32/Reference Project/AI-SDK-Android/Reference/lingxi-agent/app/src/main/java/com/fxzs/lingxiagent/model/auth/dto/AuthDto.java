package com.fxzs.smartassist.model.auth.dto;

import com.google.gson.annotations.SerializedName;

/**
 * 用户信息数据传输对象
 */
public class AuthDto {
    
    @SerializedName("id")
    private Long id;
    
    @SerializedName("mobile")
    private String mobile;
    
    @SerializedName("nickname")
    private String nickname;
    
    @SerializedName("avatar")
    private String avatar;
    
    @SerializedName("username")
    private String username;
    
    @SerializedName("email")
    private String email;
    
    @SerializedName("status")
    private Integer status;
    
    @SerializedName("createTime")
    private String createTime;
    
    public AuthDto() {
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getMobile() {
        return mobile;
    }
    
    public void setMobile(String mobile) {
        this.mobile = mobile;
    }
    
    public String getNickname() {
        return nickname;
    }
    
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
    
    public String getAvatar() {
        return avatar;
    }
    
    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
    }
    
    public String getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }
}