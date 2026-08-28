package com.fxzs.lingxiagent.model.scene.dto;

public class DeltaContentBean {

    private String stepId;
    private String text;
    //type=2时, data为H5结构
    private String url;
    private int height;
    private int width;
    private String intercept;
    //type=3时, data为模板结构
    private String templateId;
    private String content;
    //type=4时, data为系统级交互结构
    private String namespace;

    public DeltaContentBean(String stepId, String text) {
        this.stepId = stepId;
        this.text = text;
    }

    // Getters
    public String getContentText() { return text; }

    // Setters
    public void setContentText(String data) { this.text = text; }

    public String getStepId() {
        return stepId;
    }

    public void setStepId(String stepId) {
        this.stepId = stepId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public String getIntercept() {
        return intercept;
    }

    public void setIntercept(String intercept) {
        this.intercept = intercept;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }
}
