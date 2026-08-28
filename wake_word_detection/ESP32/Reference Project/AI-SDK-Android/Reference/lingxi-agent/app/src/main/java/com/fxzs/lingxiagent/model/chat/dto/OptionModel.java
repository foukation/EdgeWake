package com.fxzs.lingxiagent.model.chat.dto;

import java.io.Serializable;

public class OptionModel implements Serializable {
//    {"id":127,"keyId":null,"name":"deepSeek-r1","model":"bot-20250214144025-krvtm","platform":null,"sort":null,"status":null,"temperature":null,"maxTokens":null,"maxContexts":null,"createTime":null}



    private long id;
    private String keyId;
    private String name;
    private String model;
    private String platform;
    private String sort;
    private String status;
    private String temperature;
    private String maxTokens;
    private String maxContexts;
    private String createTime;
    public void setId(long id) {
        this.id = id;
    }
    public long getId() {
        return id;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }
    public String getKeyId() {
        return keyId;
    }

    public void setName(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }

    public void setModel(String model) {
        this.model = model;
    }
    public String getModel() {
        return model;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }
    public String getPlatform() {
        return platform;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }
    public String getSort() {
        return sort;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    public String getStatus() {
        return status;
    }

    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }
    public String getTemperature() {
        return temperature;
    }

    public void setMaxTokens(String maxTokens) {
        this.maxTokens = maxTokens;
    }
    public String getMaxTokens() {
        return maxTokens;
    }

    public void setMaxContexts(String maxContexts) {
        this.maxContexts = maxContexts;
    }
    public String getMaxContexts() {
        return maxContexts;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }
    public String getCreateTime() {
        return createTime;
    }
}
