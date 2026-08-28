package com.fxzs.lingxiagent.model.ppt.dto;

/**
 * PPT会话创建响应DTO
 * 根据实际API响应格式，data字段直接返回sessionId数字
 */
public class PptSessionCreateResponse {

    // 实际API返回的是数字类型的sessionId
    private String sessionId;

    // 为了兼容，保留这些字段但不参与序列化
    private transient String title;
    private transient Long createTime;

    public PptSessionCreateResponse() {
    }

    // 用于直接从数字创建响应对象
    public PptSessionCreateResponse(String sessionId) {
        this.sessionId = sessionId;
        this.createTime = System.currentTimeMillis();
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "PptSessionCreateResponse{" +
                "sessionId='" + sessionId + '\'' +
                ", title='" + title + '\'' +
                ", createTime=" + createTime +
                '}';
    }
}
