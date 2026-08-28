package com.fxzs.lingxiagent.model.ppt.dto;

import com.google.gson.annotations.SerializedName;

/**
 * PPT幻灯片数据模型
 */
public class PptSlide {
    
    /**
     * 幻灯片类型枚举
     */
    public enum SlideType {
        @SerializedName("cover")
        COVER("cover", "封面页"),
        
        @SerializedName("section")
        SECTION("section", "章节页"),
        
        @SerializedName("content")
        CONTENT("content", "内容页"),
        
        @SerializedName("chart")
        CHART("chart", "图表页"),
        
        @SerializedName("image")
        IMAGE("image", "图片页"),
        
        @SerializedName("ending")
        ENDING("ending", "结束页");
        
        private final String value;
        private final String description;
        
        SlideType(String value, String description) {
            this.value = value;
            this.description = description;
        }
        
        public String getValue() {
            return value;
        }
        
        public String getDescription() {
            return description;
        }
        
        public static SlideType fromValue(String value) {
            for (SlideType type : SlideType.values()) {
                if (type.value.equals(value)) {
                    return type;
                }
            }
            return CONTENT; // 默认返回内容页
        }
    }
    
    @SerializedName("id")
    private String id;
    
    @SerializedName("title")
    private String title;
    
    @SerializedName("content")
    private String content;
    
    @SerializedName("type")
    private SlideType type;
    
    @SerializedName("order")
    private int order;
    
    @SerializedName("image_url")
    private String imageUrl;
    
    @SerializedName("thumbnail_url")
    private String thumbnailUrl;
    
    @SerializedName("has_chart")
    private boolean hasChart;
    
    @SerializedName("chart_data")
    private ChartData chartData;
    
    @SerializedName("background_color")
    private String backgroundColor;
    
    @SerializedName("text_color")
    private String textColor;
    
    public PptSlide() {
    }
    
    public PptSlide(String title, String content, SlideType type) {
        this.title = title;
        this.content = content;
        this.type = type;
        this.hasChart = false;
    }
    
    public PptSlide(String title, String content, SlideType type, boolean hasChart) {
        this.title = title;
        this.content = content;
        this.type = type;
        this.hasChart = hasChart;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public SlideType getType() {
        return type;
    }
    
    public void setType(SlideType type) {
        this.type = type;
    }
    
    public int getOrder() {
        return order;
    }
    
    public void setOrder(int order) {
        this.order = order;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public String getThumbnailUrl() {
        return thumbnailUrl;
    }
    
    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }
    
    public boolean hasChart() {
        return hasChart;
    }
    
    public void setHasChart(boolean hasChart) {
        this.hasChart = hasChart;
    }
    
    public ChartData getChartData() {
        return chartData;
    }
    
    public void setChartData(ChartData chartData) {
        this.chartData = chartData;
        this.hasChart = chartData != null;
    }
    
    public String getBackgroundColor() {
        return backgroundColor;
    }
    
    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }
    
    public String getTextColor() {
        return textColor;
    }
    
    public void setTextColor(String textColor) {
        this.textColor = textColor;
    }
    
    /**
     * 检查是否有内容
     */
    public boolean hasContent() {
        return content != null && !content.trim().isEmpty();
    }
    
    /**
     * 检查是否有图片
     */
    public boolean hasImage() {
        return imageUrl != null && !imageUrl.trim().isEmpty();
    }
    
    /**
     * 检查是否为封面页
     */
    public boolean isCoverSlide() {
        return type == SlideType.COVER;
    }
    
    /**
     * 检查是否为内容页
     */
    public boolean isContentSlide() {
        return type == SlideType.CONTENT;
    }
    
    /**
     * 检查是否为章节页
     */
    public boolean isSectionSlide() {
        return type == SlideType.SECTION;
    }
}