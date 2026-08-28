package com.fxzs.lingxiagent.network.ZNet.bean;

import java.io.Serializable;

public class getCatDetailListBean implements Serializable {
    /*{
        "id": 26,
            "modelId": 41,
            "modelName": "生活感悟大师",
            "preInput": "您好，欢迎来到心灵驿站，我是生活感悟大师，有何生活谜题，我愿为您拆解。",
            "tips": "面对选择总是犹豫不决怎么办？\n感觉生活迷茫，如何找寻方向？\n亲情关系冷淡，怎么改善？",
            "name": "生活感悟大师",
            "botId": "xopkqXM3ukkd",
            "menuCatId": 5,
            "icon": "https://picture1save.oss-cn-shenzhen.aliyuncs.com/%E9%80%9A%E9%80%9A%E5%8A%A9%E6%89%8BAgent%E5%A4%B4%E5%83%8F/%E7%94%9F%E6%B4%BB%E6%84%9F%E6%82%9F%E5%A4%A7%E5%B8%88.png",
            "useNewPage": 0,
            "menuId": 3,
            "description": "生活感悟大师，洞悉生活万象，以智语解困惑，引您穿越迷茫，拥抱生活阳光。"
    }*/

    private long id;
    private long modelId;
    private String modelName;
    private String preInput;
    private String tips;
    private String name;
    private String botId;
    private int menuCatId;
    private String icon;
    private int useNewPage;
    private int menuId;
    private String description;

    private String recommendQuestions;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getModelId() {
        return modelId;
    }

    public void setModelId(long modelId) {
        this.modelId = modelId;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getPreInput() {
        return preInput;
    }

    public void setPreInput(String preInput) {
        this.preInput = preInput;
    }

    public String getTips() {
        return tips;
    }

    public void setTips(String tips) {
        this.tips = tips;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBotId() {
        return botId;
    }

    public void setBotId(String botId) {
        this.botId = botId;
    }

    public int getMenuCatId() {
        return menuCatId;
    }

    public void setMenuCatId(int menuCatId) {
        this.menuCatId = menuCatId;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public int getUseNewPage() {
        return useNewPage;
    }

    public void setUseNewPage(int useNewPage) {
        this.useNewPage = useNewPage;
    }

    public int getMenuId() {
        return menuId;
    }

    public void setMenuId(int menuId) {
        this.menuId = menuId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRecommendQuestions() {
        return recommendQuestions;
    }

    public void setRecommendQuestions(String recommendQuestions) {
        this.recommendQuestions = recommendQuestions;
    }
}
