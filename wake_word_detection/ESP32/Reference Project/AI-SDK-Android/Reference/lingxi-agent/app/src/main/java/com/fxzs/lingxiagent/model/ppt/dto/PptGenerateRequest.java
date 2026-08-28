package com.fxzs.lingxiagent.model.ppt.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

/**
 * PPT生成请求DTO
 */
public class PptGenerateRequest {
    
    @SerializedName("ppt_id")
    private String pptId;
    
    @SerializedName("template_id")
    private String templateId;
    
    @SerializedName("outline")
    private List<OutlineItem> outline;
    
    @SerializedName("color_scheme")
    private String colorScheme;
    
    @SerializedName("style")
    private String style;
    
    @SerializedName("options")
    private Map<String, Object> options;
    
    @SerializedName("include_charts")
    private boolean includeCharts;
    
    @SerializedName("include_images")
    private boolean includeImages;
    
    @SerializedName("language")
    private String language;
    
    public PptGenerateRequest() {
        this.includeCharts = true;
        this.includeImages = true;
        this.language = "zh-CN";
    }
    
    public PptGenerateRequest(String pptId, String templateId, List<OutlineItem> outline) {
        this();
        this.pptId = pptId;
        this.templateId = templateId;
        this.outline = outline;
    }
    
    // Getters and Setters
    public String getPptId() {
        return pptId;
    }
    
    public void setPptId(String pptId) {
        this.pptId = pptId;
    }
    
    public String getTemplateId() {
        return templateId;
    }
    
    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }
    
    public List<OutlineItem> getOutline() {
        return outline;
    }
    
    public void setOutline(List<OutlineItem> outline) {
        this.outline = outline;
    }
    
    public String getColorScheme() {
        return colorScheme;
    }
    
    public void setColorScheme(String colorScheme) {
        this.colorScheme = colorScheme;
    }
    
    public String getStyle() {
        return style;
    }
    
    public void setStyle(String style) {
        this.style = style;
    }
    
    public Map<String, Object> getOptions() {
        return options;
    }
    
    public void setOptions(Map<String, Object> options) {
        this.options = options;
    }
    
    public boolean isIncludeCharts() {
        return includeCharts;
    }
    
    public void setIncludeCharts(boolean includeCharts) {
        this.includeCharts = includeCharts;
    }
    
    public boolean isIncludeImages() {
        return includeImages;
    }
    
    public void setIncludeImages(boolean includeImages) {
        this.includeImages = includeImages;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
}