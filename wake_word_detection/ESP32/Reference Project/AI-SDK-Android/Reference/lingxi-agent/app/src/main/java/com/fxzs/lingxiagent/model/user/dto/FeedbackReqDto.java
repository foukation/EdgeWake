package com.fxzs.lingxiagent.model.user.dto;

import android.os.Build;

public class FeedbackReqDto {
    private String title;       // 反馈标题
    private int type = 0;       // 反馈类型：0：功能问题、1：用户体验问题、2：其他问题，默认为0
    private String content;     // 反馈内容
    private String contact;     // 联系方式（可选），脱敏
    private String imageUrls;   // 图片链接，多个用逗号分隔（可选）
    private String logUrl;      // 手机日志url地址
    private String appVersion;  // 应用版本
    private String os;          // 系统：iOS/android
    private String osVersion;   // 系统版本
    private String brand;       // 手机品牌
    private String model;       // 手机型号

    public FeedbackReqDto() {
        os = "android";
        osVersion = Build.DISPLAY;
        brand = Build.BRAND;
        model = Build.MODEL;
    }
    // Getters and Setters
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public int getType() {
        return type;
    }
    
    public void setType(int type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getContact() {
        return contact;
    }
    
    public void setContact(String contact) {
        this.contact = contact;
    }
    
    public String getImageUrls() {
        return imageUrls;
    }
    
    public void setImageUrls(String imageUrls) {
        this.imageUrls = imageUrls;
    }

    public String getLogUrl() {
        return logUrl;
    }

    public void setLogUrl(String logUrl) {
        this.logUrl = logUrl;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}