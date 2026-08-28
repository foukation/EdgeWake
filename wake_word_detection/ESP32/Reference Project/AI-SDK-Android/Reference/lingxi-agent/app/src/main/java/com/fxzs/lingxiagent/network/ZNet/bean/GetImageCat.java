package com.fxzs.lingxiagent.network.ZNet.bean;

public class GetImageCat {
//    {
//        "createTime": 1733674534000,
//            "updateTime": 1733674542000,
//            "creator": "1",
//            "updater": null,
//            "deleted": false,
//            "id": 2,
//            "name": "人像摄影",
//            "sort": null
//    },
    private long createTime;
    private long updateTime;
    private String creator;
    private String updater;
    private boolean deleted;
    private int id;
    private String name;
    private int sort;

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getUpdater() {
        return updater;
    }

    public void setUpdater(String updater) {
        this.updater = updater;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSort() {
        return sort;
    }

    public void setSort(int sort) {
        this.sort = sort;
    }
}
