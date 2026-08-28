package com.fxzs.lingxiagent.network.ZNet.bean;

import java.io.Serializable;

public class SampleListBean implements Serializable {

   /* {
        "id": 1,
            "name": null,
            "prompt": "图片风格为(人像摄影)，低头沉思的女性，身穿少数名族服饰，丁达尔效应，人文摄影，(比例1:1)",
            "sampleUrl": "https://picture1save.oss-cn-shenzhen.aliyuncs.com/%E5%85%83%E6%99%AFai%E5%8A%A9%E6%89%8B/ai%E7%BB%98%E7%94%BB/%E7%B2%BE%E9%80%89/%E5%9B%BE%E7%89%87%E7%94%9F%E6%88%90%E9%9C%80%E6%B1%82%280%29.png",
            "catId": 1,
            "sort": null,
            "width": 768,
            "height": 768
    }*/

    private int id;
    private String name;
    private String prompt;
    private String sampleUrl;
    private int catId;
    private Integer sort;
    private int width;
    private int height;

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

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getSampleUrl() {
        return sampleUrl;
    }

    public void setSampleUrl(String sampleUrl) {
        this.sampleUrl = sampleUrl;
    }

    public int getCatId() {
        return catId;
    }

    public void setCatId(int catId) {
        this.catId = catId;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }
}
