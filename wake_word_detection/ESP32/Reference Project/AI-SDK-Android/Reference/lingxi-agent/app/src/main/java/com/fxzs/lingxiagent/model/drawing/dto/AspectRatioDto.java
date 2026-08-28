package com.fxzs.lingxiagent.model.drawing.dto;

import java.io.Serializable;

/**
 * 宽高比DTO
 */
public class AspectRatioDto implements Serializable {
    
    private String ratio;          // 比例值，如 "16:9"
    private String displayName;    // 显示名称
    private Integer width;         // 实际宽度
    private Integer height;        // 实际高度
    private Boolean isDefault;     // 是否默认
    
    public AspectRatioDto() {}
    
    public AspectRatioDto(String ratio, String displayName, Integer width, Integer height, Boolean isDefault) {
        this.ratio = ratio;
        this.displayName = displayName;
        this.width = width;
        this.height = height;
        this.isDefault = isDefault;
    }
    
    // Getters and Setters
    public String getRatio() {
        return ratio;
    }
    
    public void setRatio(String ratio) {
        this.ratio = ratio;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
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
    
    public Boolean getIsDefault() {
        return isDefault;
    }
    
    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }
}