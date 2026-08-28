package com.fxzs.lingxiagent.model.drawing.dto;

import java.io.Serializable;

/**
 * AI绘画示例DTO
 */
public class DrawingSampleDto implements Serializable {
    
    private Long id;
    private String name;        // 示例名称
    private String prompt;      // 提示词
    private String sampleUrl;   // API返回的示例图片URL字段
    private String imageUrl;    // 兼容旧字段
    private Long catId;         // API返回的分类ID字段
    private Long categoryId;    // 兼容旧字段
    private String categoryName; // 分类名称
    private Integer sort;       // 排序
    private Integer width;      // 图片宽度
    private Integer height;     // 图片高度
    private Long styleId;       // 风格ID
    private String styleName;   // 风格名称
    private String imageResource; // 本地图片资源名称
    private String category;     // 分类标识
    private String style;        // 风格（同styleName）
    private String ratio;        //

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
    
    public String getImageUrl() {
        // 优先返回sampleUrl（API字段），如果为空则返回imageUrl
        return sampleUrl != null ? sampleUrl : imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public String getSampleUrl() {
        return sampleUrl;
    }
    
    public void setSampleUrl(String sampleUrl) {
        this.sampleUrl = sampleUrl;
    }
    
    public Long getCategoryId() {
        // 优先返回catId（API字段），如果为空则返回categoryId
        return catId != null ? catId : categoryId;
    }
    
    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }
    
    public Long getCatId() {
        return catId;
    }
    
    public void setCatId(Long catId) {
        this.catId = catId;
    }
    
    public String getCategoryName() {
        return categoryName;
    }
    
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
    
    // 兼容API返回的catName字段
    public String getCatName() {
        return categoryName;
    }
    
    public void setCatName(String catName) {
        this.categoryName = catName;
    }
    
    public Integer getSort() {
        return sort;
    }
    
    public void setSort(Integer sort) {
        this.sort = sort;
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
    
    public Long getStyleId() {
        return styleId;
    }
    
    public void setStyleId(Long styleId) {
        this.styleId = styleId;
    }
    
    public String getStyleName() {
        return styleName;
    }
    
    public void setStyleName(String styleName) {
        this.styleName = styleName;
    }
    
    public String getImageResource() {
        return imageResource;
    }
    
    public void setImageResource(String imageResource) {
        this.imageResource = imageResource;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getStyle() {
        return style;
    }
    
    public void setStyle(String style) {
        this.style = style;
    }

    public String getRatio() {
        return ratio;
    }

    public void setRatio(String ratio) {
        this.ratio = ratio;
    }
}