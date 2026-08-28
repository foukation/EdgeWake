package com.fxzs.lingxiagent.model.ppt.dto;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

/**
 * PPT任务提交请求DTO
 * 根据接口文档：https://mouse-api-prod.shanghaijimu.com/app-api/lt/ai/xf/ppt/ppt/task/commit
 */
public class PptTaskCommitRequest {

    @SerializedName("coverId")
    private String coverId;

    @SerializedName("coverUrl")
    private String coverUrl;

    @SerializedName("sessionId")
    private String sessionId;

    @SerializedName("customData")
    private Map<String, Object> customData;

    // 根据接口文档，这些字段应该在顶层，而不是在customData中
    @SerializedName("title")
    private String title;

    @SerializedName("subTitle")
    private String subTitle;

    @SerializedName("author")
    private String author;

    @SerializedName("catalogs")
    private java.util.List<Map<String, Object>> catalogs;
    
    public PptTaskCommitRequest() {
    }

    public PptTaskCommitRequest(String coverId, Map<String, Object> customData) {
        this.coverId = coverId;
        this.customData = customData;

        // 从customData中提取字段到顶层
        if (customData != null) {
            this.title = (String) customData.get("title");
            this.subTitle = (String) customData.get("subTitle");
            this.author = (String) customData.get("author");
            this.catalogs = (java.util.List<Map<String, Object>>) customData.get("catalogs");
        }
    }

    // Getters and Setters
    public String getCoverId() {
        return coverId;
    }

    public void setCoverId(String coverId) {
        this.coverId = coverId;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Map<String, Object> getCustomData() {
        return customData;
    }

    public void setCustomData(Map<String, Object> customData) {
        this.customData = customData;

        // 从customData中提取字段到顶层
        if (customData != null) {
            this.title = (String) customData.get("title");
            this.subTitle = (String) customData.get("subTitle");
            this.author = (String) customData.get("author");
            this.catalogs = (java.util.List<Map<String, Object>>) customData.get("catalogs");
        }
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubTitle() {
        return subTitle;
    }

    public void setSubTitle(String subTitle) {
        this.subTitle = subTitle;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public java.util.List<Map<String, Object>> getCatalogs() {
        return catalogs;
    }

    public void setCatalogs(java.util.List<Map<String, Object>> catalogs) {
        this.catalogs = catalogs;
    }
    
    @Override
    public String toString() {
        return "PptTaskCommitRequest{" +
                "coverId='" + coverId + '\'' +
                ", coverUrl='" + coverUrl + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", title='" + title + '\'' +
                ", subTitle='" + subTitle + '\'' +
                ", author='" + author + '\'' +
                ", catalogs=" + catalogs +
                ", customData=" + customData +
                '}';
    }
}