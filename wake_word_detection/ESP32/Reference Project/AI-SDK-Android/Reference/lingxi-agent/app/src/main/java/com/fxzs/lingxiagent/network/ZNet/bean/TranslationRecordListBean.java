package com.fxzs.lingxiagent.network.ZNet.bean;

import java.util.List;

public class TranslationRecordListBean {

    private int id;
    private String name;
    private String source;
    private String target;
    private String sourceText;
    private String targetText;
    private String top;
    private int userId;
    private String timeDuration;
    private int type;
    private long createTime;
    private long updateTime;
    private String meetingId;
    private List<TranslationRecordListBean> messageList;
    public void setId(int id) {
        this.id = id;
    }
    public int getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }

    public void setSource(String source) {
        this.source = source;
    }
    public String getSource() {
        return source;
    }

    public void setTarget(String target) {
        this.target = target;
    }
    public String getTarget() {
        return target;
    }

    public void setSourceText(String sourceText) {
        this.sourceText = sourceText;
    }
    public String getSourceText() {
        return sourceText;
    }

    public void setTargetText(String targetText) {
        this.targetText = targetText;
    }
    public String getTargetText() {
        return targetText;
    }

    public void setTop(String top) {
        this.top = top;
    }
    public String getTop() {
        return top;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }
    public int getUserId() {
        return userId;
    }

    public void setTimeDuration(String timeDuration) {
        this.timeDuration = timeDuration;
    }
    public String getTimeDuration() {
        return timeDuration;
    }

    public void setType(int type) {
        this.type = type;
    }
    public int getType() {
        return type;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }
    public long getCreateTime() {
        return createTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }
    public long getUpdateTime() {
        return updateTime;
    }

    public void setMeetingId(String meetingId) {
        this.meetingId = meetingId;
    }
    public String getMeetingId() {
        return meetingId;
    }

    public List<TranslationRecordListBean> getMessageList() {
        return messageList;
    }

    public void setMessageList(List<TranslationRecordListBean> messageList) {
        this.messageList = messageList;
    }
}