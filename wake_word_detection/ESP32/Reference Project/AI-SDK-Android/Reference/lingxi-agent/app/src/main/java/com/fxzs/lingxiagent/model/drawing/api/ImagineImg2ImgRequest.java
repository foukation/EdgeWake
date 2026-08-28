package com.fxzs.lingxiagent.model.drawing.api;

import java.io.Serializable;
import java.util.Map;

/**
 * imagine-img2img 请求体
 */
public class ImagineImg2ImgRequest implements Serializable {

    private String prompt;
    private String referenceImageUrl;
    private String size;
    private Integer styleId;
    private String stylePrompt;
    private String realPrompt;
    private String sessionId;
    private Boolean watermark;
    private String sequentialImageGeneration;
    private Map<String, Object> options;

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getReferenceImageUrl() {
        return referenceImageUrl;
    }

    public void setReferenceImageUrl(String referenceImageUrl) {
        this.referenceImageUrl = referenceImageUrl;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public Integer getStyleId() {
        return styleId;
    }

    public void setStyleId(Integer styleId) {
        this.styleId = styleId;
    }

    public String getStylePrompt() {
        return stylePrompt;
    }

    public void setStylePrompt(String stylePrompt) {
        this.stylePrompt = stylePrompt;
    }

    public String getRealPrompt() {
        return realPrompt;
    }

    public void setRealPrompt(String realPrompt) {
        this.realPrompt = realPrompt;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Boolean getWatermark() {
        return watermark;
    }

    public void setWatermark(Boolean watermark) {
        this.watermark = watermark;
    }

    public String getSequentialImageGeneration() {
        return sequentialImageGeneration;
    }

    public void setSequentialImageGeneration(String sequentialImageGeneration) {
        this.sequentialImageGeneration = sequentialImageGeneration;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public void setOptions(Map<String, Object> options) {
        this.options = options;
    }
}

