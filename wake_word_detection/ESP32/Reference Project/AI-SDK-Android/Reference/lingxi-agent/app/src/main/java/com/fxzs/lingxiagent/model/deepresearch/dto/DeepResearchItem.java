package com.fxzs.lingxiagent.model.deepresearch.dto;

public class DeepResearchItem {
    private String think;
    private String thinkContent;

    private int animStatus;// 0: 未播放, 1: 已播放
    private int Status; // 0: 进行中, 1: 已完成 2:报告生成中 3:报告已完成
    private WebSearch web_search;

    private String query;
    private boolean isCreateFile;

    public String getThink() {
        return think;
    }

    public void setThink(String think) {
        this.think = think;
    }

    public String getThinkContent() {
        return thinkContent;
    }

    public void setThinkContent(String thinkContent) {
        this.thinkContent = thinkContent;
    }



    public WebSearch getWeb_search() {
        return web_search;
    }

    public void setWeb_search(WebSearch web_search) {
        this.web_search = web_search;
    }

    public int getStatus() {
        return Status;
    }

    public void setStatus(int status) {
        Status = status;
    }

    public int getAnimStatus() {
        return animStatus;
    }

    public void setAnimStatus(int animStatus) {
        this.animStatus = animStatus;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public boolean isCreateFile() {
        return isCreateFile;
    }

    public void setCreateFile(boolean createFile) {
        isCreateFile = createFile;
    }

    @Override
    public String toString() {
        return "DeepResearchItem{" +
                "think='" + think + '\'' +
                ", thinkContent='" + thinkContent + '\'' +
                ", animStatus=" + animStatus +
                ", Status=" + Status +
                ", web_search=" + web_search +
                ", query='" + query + '\'' +
                ", isCreateFile=" + isCreateFile +
                '}';
    }
}
