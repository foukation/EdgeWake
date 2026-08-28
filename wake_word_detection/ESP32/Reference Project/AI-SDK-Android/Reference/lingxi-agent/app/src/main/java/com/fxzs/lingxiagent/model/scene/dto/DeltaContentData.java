package com.fxzs.lingxiagent.model.scene.dto;

public class DeltaContentData {

    public DeltaContentBean data;
    public String textValue;
    public String content;
    public String templateId;
    public String url;

    public String getUrl() {
        return url;
    }

    public DeltaContentData() {
    }

    public String getContent() {
        return content;
    }

    public String getTemplateId() {
        return templateId;
    }

    public DeltaContentBean getDeltaContentBean() { return data; }

    public void setDeltaContentBean(DeltaContentBean data) { this.data = data; }

    public String getTextValue() {
        return textValue;
    }
}
