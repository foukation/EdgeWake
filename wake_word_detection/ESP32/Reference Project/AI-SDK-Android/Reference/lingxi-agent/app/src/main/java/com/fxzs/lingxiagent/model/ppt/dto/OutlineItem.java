package com.fxzs.lingxiagent.model.ppt.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * PPT大纲项数据模型
 */
public class OutlineItem {
    
    @SerializedName("id")
    private String id;
    
    @SerializedName("title")
    private String title;
    
    @SerializedName("content")
    private String content;
    
    @SerializedName("order")
    private int order;
    
    @SerializedName("level")
    private int level; // 层级：1为主标题，2为子标题
    
    @SerializedName("parent_id")
    private String parentId;
    
    @SerializedName("sub_items")
    private List<OutlineItem> subItems;
    
    // UI状态字段（不参与序列化）
    private transient boolean expanded = true;
    private transient boolean editing = false;
    private transient boolean selected = false;
    
    public OutlineItem() {
        this.id = generateId();
    }

    public OutlineItem(String title, String content) {
        this.id = generateId();
        this.title = title;
        this.content = content;
        this.level = 1;
        this.expanded = true;
    }

    public OutlineItem(String title, String content, int level) {
        this.id = generateId();
        this.title = title;
        this.content = content;
        this.level = level;
        this.expanded = true;
    }

    /**
     * 生成唯一ID
     */
    private String generateId() {
        return "outline_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
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
    
    public int getOrder() {
        return order;
    }
    
    public void setOrder(int order) {
        this.order = order;
    }
    
    public int getLevel() {
        return level;
    }
    
    public void setLevel(int level) {
        this.level = level;
    }
    
    public String getParentId() {
        return parentId;
    }
    
    public void setParentId(String parentId) {
        this.parentId = parentId;
    }
    
    public List<OutlineItem> getSubItems() {
        return subItems;
    }
    
    public void setSubItems(List<OutlineItem> subItems) {
        this.subItems = subItems;
    }
    
    // UI状态相关方法
    public boolean isExpanded() {
        return expanded;
    }
    
    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }
    
    public boolean isEditing() {
        return editing;
    }
    
    public void setEditing(boolean editing) {
        this.editing = editing;
    }
    
    public boolean isSelected() {
        return selected;
    }
    
    public void setSelected(boolean selected) {
        this.selected = selected;
    }
    
    /**
     * 检查是否有子项
     */
    public boolean hasSubItems() {
        return subItems != null && !subItems.isEmpty();
    }
    
    /**
     * 检查是否为主标题
     */
    public boolean isMainTitle() {
        return level == 1;
    }
    
    /**
     * 检查是否为子标题
     */
    public boolean isSubTitle() {
        return level > 1;
    }
    
    /**
     * 检查内容是否为空
     */
    public boolean hasContent() {
        return content != null && !content.trim().isEmpty();
    }
    
    /**
     * 切换展开状态
     */
    public void toggleExpanded() {
        this.expanded = !this.expanded;
    }
    
    /**
     * 切换编辑状态
     */
    public void toggleEditing() {
        this.editing = !this.editing;
    }
}