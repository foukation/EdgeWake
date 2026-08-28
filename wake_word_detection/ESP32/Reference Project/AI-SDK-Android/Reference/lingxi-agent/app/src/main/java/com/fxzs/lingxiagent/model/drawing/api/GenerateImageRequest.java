package com.fxzs.lingxiagent.model.drawing.api;

import java.io.Serializable;

/**
 * 生成图片请求 - Volc API
 */
public class GenerateImageRequest implements Serializable {
    
    private String prompt;         // 提示词 (0-1200字符)
    private Integer height;        // 图片高度
    private Integer width;         // 图片宽度
    private Object options;        // 额外绘制参数（可选）
    private String reqScheduleConf; // 模型配置（可选）
    private Integer seed;          // 随机种子（可选）
    private Long sessionId;        // 会话ID（可选）
    private Integer ddimSteps;     // 生成图像步数（可选）
    private String[] imagUrls;     // 图片文件URL（可选）
    private Long styleId;          // 风格ID（可选）
    
    // Getters and Setters
    public String getPrompt() {
        return prompt;
    }
    
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
    
    public Object getOptions() {
        return options;
    }
    
    public void setOptions(Object options) {
        this.options = options;
    }
    
    public String getReqScheduleConf() {
        return reqScheduleConf;
    }
    
    public void setReqScheduleConf(String reqScheduleConf) {
        this.reqScheduleConf = reqScheduleConf;
    }
    
    public Integer getSeed() {
        return seed;
    }
    
    public void setSeed(Integer seed) {
        this.seed = seed;
    }
    
    public Integer getDdimSteps() {
        return ddimSteps;
    }
    
    public void setDdimSteps(Integer ddimSteps) {
        this.ddimSteps = ddimSteps;
    }
    
    public String[] getImagUrls() {
        return imagUrls;
    }
    
    public void setImagUrls(String[] imagUrls) {
        this.imagUrls = imagUrls;
    }
    
    public Long getStyleId() {
        return styleId;
    }
    
    public void setStyleId(Long styleId) {
        this.styleId = styleId;
    }
    
    public Long getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }
    
    public Integer getWidth() {
        return width;
    }
    
    public void setWidth(Integer width) {
        this.width = width;
    }
    
    public Integer getHeight() {
        return height;
    }
    
    public void setHeight(Integer height) {
        this.height = height;
    }
}