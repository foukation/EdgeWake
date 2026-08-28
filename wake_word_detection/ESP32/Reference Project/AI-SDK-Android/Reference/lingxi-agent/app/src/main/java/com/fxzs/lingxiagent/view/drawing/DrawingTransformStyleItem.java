package com.fxzs.lingxiagent.view.drawing;

import java.io.Serializable;

/**
 * 风格转绘专用 style item，避免与文生图链路的 DrawingStyleAdapter/数据模型冲突。
 * iconUrl:"https://picture1save.oss-cn-shenzhen.aliyuncs.com/%E7%B9%81%E6%98%9F%E6%99%BA%E7%AE%97/%E5%87%A0%E7%9B%AE%E7%A7%91%E6%8A%80/Frame%20766.png"
 * id: 16
 * name: "蒸汽朋克"
 * prompt:  "生成蒸汽朋克风格，"
 * sort:0
 */
public class DrawingTransformStyleItem implements Serializable {

    private int id;
    private String name;
    private String iconUrl;
    private String prompt;
    private int sort;

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public int getSort() { return sort; }
    public void setSort(int sort) { this.sort = sort; }
}

