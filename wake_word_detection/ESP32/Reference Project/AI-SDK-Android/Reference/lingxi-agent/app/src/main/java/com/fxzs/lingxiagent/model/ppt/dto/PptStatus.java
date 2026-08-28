package com.fxzs.lingxiagent.model.ppt.dto;

import com.google.gson.annotations.SerializedName;

/**
 * PPT状态枚举
 */
public enum PptStatus {
    
    @SerializedName("outline_generating")
    OUTLINE_GENERATING("outline_generating", "大纲生成中"),
    
    @SerializedName("outline_ready")
    OUTLINE_READY("outline_ready", "大纲已完成"),
    
    @SerializedName("ppt_generating")
    PPT_GENERATING("ppt_generating", "PPT生成中"),
    
    @SerializedName("ppt_ready")
    PPT_READY("ppt_ready", "PPT已完成"),
    
    @SerializedName("failed")
    FAILED("failed", "生成失败");
    
    private final String value;
    private final String description;
    
    PptStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }
    
    public String getValue() {
        return value;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据字符串值获取对应的枚举
     */
    public static PptStatus fromValue(String value) {
        for (PptStatus status : PptStatus.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        return FAILED; // 默认返回失败状态
    }
    
    /**
     * 检查是否为生成中状态
     */
    public boolean isGenerating() {
        return this == OUTLINE_GENERATING || this == PPT_GENERATING;
    }
    
    /**
     * 检查是否为完成状态
     */
    public boolean isCompleted() {
        return this == OUTLINE_READY || this == PPT_READY;
    }
    
    /**
     * 检查是否为失败状态
     */
    public boolean isFailed() {
        return this == FAILED;
    }
}