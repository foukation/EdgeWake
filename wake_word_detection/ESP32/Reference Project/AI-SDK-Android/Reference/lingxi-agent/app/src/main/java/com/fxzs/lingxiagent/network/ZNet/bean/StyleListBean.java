package com.fxzs.lingxiagent.network.ZNet.bean;

public class StyleListBean {
//    {
//        "id": 1,
//            "name": "写实",
//            "prompt": "写实，街头摄影，深景深，低对比度，半身像，四分之三侧脸，",
//            "iconUrl": "https://picture1save.oss-cn-shenzhen.aliyuncs.com/%E5%85%83%E6%99%AFai%E5%8A%A9%E6%89%8B/%E5%9B%BE%E5%83%8F%E9%A3%8E%E6%A0%BC/%E5%86%99%E5%AE%9E.png"
//    }
    private int id;
    private String name;
    private String prompt;
    private String iconUrl;

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

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }
}
