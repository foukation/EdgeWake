package com.fxzs.lingxiagent.model.drawing.dto;

import java.io.Serializable;

/**
 * 绘画风格DTO - Volc API
 */
public class DrawingStyleDto implements Serializable ,Comparable<DrawingStyleDto>{
    
    private Long id;
    private String name;           // 风格名称
    private String prompt;         // 风格提示词
    private String iconUrl;        // 图标URL
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
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

    @Override
    public int compareTo(DrawingStyleDto o) {
        return (int) (this.id - o.id);
    }
}