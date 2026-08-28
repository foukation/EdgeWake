package com.fxzs.lingxiagent.model.drawing.api;

import java.io.Serializable;

/**
 * 创建图片会话请求
 */
public class CreateImageSessionRequest implements Serializable {

    private String name; // 会话名称（使用prompt作为名称）
    private Integer type;

    public CreateImageSessionRequest() {
    }

    public CreateImageSessionRequest(String name) {
        this.name = name;
    }

    public CreateImageSessionRequest(String name, Integer type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }
}