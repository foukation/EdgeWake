package com.fxzs.lingxiagent.model.user.dto;

public class AppVersionResponse {
    private int updateMode;// 0：实时升级(提醒用户升级)，1：强制升级(必须升级APP，否则无法使用)
    private int versionCode;// 新版本号
    private String versionName;// 新版本名称
    private String updateContent;// 更新内容说明
    private Boolean updatePrivacy;// 是否涉及更新隐私协议。true：涉及隐私协议更新，false：不涉及隐私协议更新
    private String downloadUrl;// 安装包下载地址
    private String learnMoreUrl;// 了解更多
    private String sha256;// 安装包sha256值（大写）
    private long size;// 升级包大小，字节
    private long updateTime;// 升级utc时间戳(10位)，精确到秒。为空表示不指定时间

    // 默认构造函数
    public AppVersionResponse() {
    }

    public int getUpdateMode() {
        return updateMode;
    }

    public String getUpdateContent() {
        return updateContent;
    }

    public void setUpdateContent(String updateContent) {
        this.updateContent = updateContent;
    }

    public void setUpdateMode(int updateMode) {
        this.updateMode = updateMode;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public Boolean getUpdatePrivacy() {
        return updatePrivacy;
    }

    public void setUpdatePrivacy(Boolean updatePrivacy) {
        this.updatePrivacy = updatePrivacy;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getLearnMoreUrl() {
        return learnMoreUrl;
    }

    public void setLearnMoreUrl(String learnMoreUrl) {
        this.learnMoreUrl = learnMoreUrl;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }
}