package com.fxzs.lingxiagent.model.history;

public class AllRecordImage {

    private int id;
    private String name;
    private int userId;
    private long createTime;
    private long updateTime;
    private LatestImage latestImage;
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

    public void setUserId(int userId) {
        this.userId = userId;
    }
    public int getUserId() {
        return userId;
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

    public void setLatestImage(LatestImage latestImage) {
        this.latestImage = latestImage;
    }
    public LatestImage getLatestImage() {
        return latestImage;
    }

}


