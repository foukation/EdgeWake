package com.fxzs.lingxiagent.model.auth.dto;

import com.google.gson.annotations.SerializedName;

/**
 * 登录响应数据
 */
public class LoginResponse {
    
    @SerializedName("accessToken")
    private String accessToken;
    
    @SerializedName("refreshToken")
    private String refreshToken;
    
    @SerializedName("expiresTime")
    private Long expiresTime;
    
    @SerializedName("userId")
    private Long userId;

    @SerializedName("remainFailCount")
    private int remainFailCount;

    @SerializedName("user")
    private UserDto user;
    
    // 用于错误处理
    private transient String message;
    private transient int code;
    
    public LoginResponse() {
    }
    
    public String getAccessToken() {
        return accessToken;
    }
    
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    
    public String getRefreshToken() {
        return refreshToken;
    }
    
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    
    public Long getExpiresTime() {
        return expiresTime;
    }
    
    public void setExpiresTime(Long expiresTime) {
        this.expiresTime = expiresTime;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public UserDto getUser() {
        return user;
    }
    
    public void setUser(UserDto user) {
        this.user = user;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
    
    public boolean isSuccess() {
        return accessToken != null && !accessToken.isEmpty();
    }
    
    public String getToken() {
        return accessToken;
    }

    public int getRemainFailCount() {
        return remainFailCount;
    }

    public void setRemainFailCount(int remainFailCount) {
        this.remainFailCount = remainFailCount;
    }

    public UserInfo getUserInfo() {
        if (user != null) {
            UserInfo userInfo = new UserInfo();
            userInfo.setPhone(user.getMobile());
            userInfo.setUserId(String.valueOf(userId));
            return userInfo;
        }
        return null;
    }
    
    public static class UserInfo {
        private String phone;
        private String userId;
        
        public String getPhone() {
            return phone;
        }
        
        public void setPhone(String phone) {
            this.phone = phone;
        }
        
        public String getUserId() {
            return userId;
        }
        
        public void setUserId(String userId) {
            this.userId = userId;
        }
    }
}