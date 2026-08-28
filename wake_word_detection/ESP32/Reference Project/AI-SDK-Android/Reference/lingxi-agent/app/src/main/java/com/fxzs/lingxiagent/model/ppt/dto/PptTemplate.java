package com.fxzs.lingxiagent.model.ppt.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

/**
 * PPT模板数据模型
 */
public class PptTemplate {
    
    @SerializedName("id")
    private String id;
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("description")
    private String description;
    
    @SerializedName("category")
    private String category;
    
    @SerializedName("thumbnail_url")
    private String thumbnailUrl;
    
    @SerializedName("preview_images")
    private List<String> previewImages;
    
    @SerializedName("color_schemes")
    private Map<String, String> colorSchemes;
    
    @SerializedName("styles")
    private List<String> styles;
    
    @SerializedName("is_premium")
    private boolean isPremium;
    
    @SerializedName("tags")
    private List<String> tags;
    
    @SerializedName("usage_count")
    private int usageCount;
    
    @SerializedName("rating")
    private float rating;
    
    @SerializedName("created_at")
    private String createdAt;
    
    // UI状态字段（不参与序列化）
    private transient boolean selected = false;
    
    public PptTemplate() {
    }
    
    public PptTemplate(String id, String name, String thumbnailUrl) {
        this.id = id;
        this.name = name;
        this.thumbnailUrl = thumbnailUrl;
        this.isPremium = false;
        this.rating = 0.0f;
        this.usageCount = 0;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getThumbnailUrl() {
        return thumbnailUrl;
    }
    
    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }
    
    public List<String> getPreviewImages() {
        return previewImages;
    }
    
    public void setPreviewImages(List<String> previewImages) {
        this.previewImages = previewImages;
    }
    
    public Map<String, String> getColorSchemes() {
        return colorSchemes;
    }
    
    public void setColorSchemes(Map<String, String> colorSchemes) {
        this.colorSchemes = colorSchemes;
    }
    
    public List<String> getStyles() {
        return styles;
    }
    
    public void setStyles(List<String> styles) {
        this.styles = styles;
    }
    
    public boolean isPremium() {
        return isPremium;
    }
    
    public void setPremium(boolean premium) {
        isPremium = premium;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    public int getUsageCount() {
        return usageCount;
    }
    
    public void setUsageCount(int usageCount) {
        this.usageCount = usageCount;
    }
    
    public float getRating() {
        return rating;
    }
    
    public void setRating(float rating) {
        this.rating = rating;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
    // UI状态相关方法
    public boolean isSelected() {
        return selected;
    }
    
    public void setSelected(boolean selected) {
        this.selected = selected;
    }
    
    /**
     * 检查是否有预览图片
     */
    public boolean hasPreviewImages() {
        return previewImages != null && !previewImages.isEmpty();
    }
    
    /**
     * 检查是否有颜色方案
     */
    public boolean hasColorSchemes() {
        return colorSchemes != null && !colorSchemes.isEmpty();
    }
    
    /**
     * 检查是否有标签
     */
    public boolean hasTags() {
        return tags != null && !tags.isEmpty();
    }
    
    /**
     * 获取主要颜色（第一个颜色方案的主色）
     */
    public String getPrimaryColor() {
        if (hasColorSchemes()) {
            return colorSchemes.values().iterator().next();
        }
        return "#007AFF"; // 默认蓝色
    }
    
    /**
     * 检查是否匹配搜索关键词
     */
    public boolean matchesKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return true;
        }
        
        String lowerKeyword = keyword.toLowerCase();
        
        // 检查名称
        if (name != null && name.toLowerCase().contains(lowerKeyword)) {
            return true;
        }
        
        // 检查描述
        if (description != null && description.toLowerCase().contains(lowerKeyword)) {
            return true;
        }
        
        // 检查分类
        if (category != null && category.toLowerCase().contains(lowerKeyword)) {
            return true;
        }
        
        // 检查标签
        if (hasTags()) {
            for (String tag : tags) {
                if (tag.toLowerCase().contains(lowerKeyword)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    // 为了兼容VMPptTemplateSelection中的使用，添加这些方法
    private String color;
    private String style;
    
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

    /**
     * 获取封面URL，用于PPT任务提交
     * 使用thumbnailUrl作为coverUrl
     */
    public String getCoverUrl() {
        return thumbnailUrl;
    }
}