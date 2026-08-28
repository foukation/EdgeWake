package com.fxzs.lingxiagent.model.ppt.dto;

import com.google.gson.annotations.SerializedName;

/**
 * 封面列表请求DTO
 */
public class CoverListRequest {
    
    @SerializedName("color")
    private String color;
    
    @SerializedName("style")
    private String style;
    
    @SerializedName("pageNum")
    private int pageNum = 1;
    
    @SerializedName("pageSize")
    private int pageSize = 20;
    
    public CoverListRequest() {
    }
    
    public CoverListRequest(String color, String style, int pageNum, int pageSize) {
        this.color = color;
        this.style = style;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }
    
    // Getters and Setters
    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
    }
    
    public String getStyle() {
        return style;
    }
    
    public void setStyle(String style) {
        this.style = style;
    }
    
    public int getPageNum() {
        return pageNum;
    }
    
    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }
    
    public int getPageSize() {
        return pageSize;
    }
    
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
    
    @Override
    public String toString() {
        return "CoverListRequest{" +
                "color='" + color + '\'' +
                ", style='" + style + '\'' +
                ", pageNum=" + pageNum +
                ", pageSize=" + pageSize +
                '}';
    }
}