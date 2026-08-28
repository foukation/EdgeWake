package com.fxzs.lingxiagent.model.ppt.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 大纲生成请求DTO
 * 基于联通AI PPT接口文档
 */
public class OutlineRequest {
    
    @SerializedName("theme")
    private String theme; // PPT主题
    
    @SerializedName("fileAnalyseResults")
    private List<String> fileAnalyseResults; // 文档解析结果url链接数组
    
    public OutlineRequest() {
    }
    
    public OutlineRequest(String theme) {
        this.theme = theme;
    }
    
    public OutlineRequest(String theme, List<String> fileAnalyseResults) {
        this.theme = theme;
        this.fileAnalyseResults = fileAnalyseResults;
    }
    
    // Getters and Setters
    public String getTheme() {
        return theme;
    }
    
    public void setTheme(String theme) {
        this.theme = theme;
    }
    
    public List<String> getFileAnalyseResults() {
        return fileAnalyseResults;
    }
    
    public void setFileAnalyseResults(List<String> fileAnalyseResults) {
        this.fileAnalyseResults = fileAnalyseResults;
    }
    
    /**
     * 检查是否有文档解析结果
     */
    public boolean hasFileAnalyseResults() {
        return fileAnalyseResults != null && !fileAnalyseResults.isEmpty();
    }
}