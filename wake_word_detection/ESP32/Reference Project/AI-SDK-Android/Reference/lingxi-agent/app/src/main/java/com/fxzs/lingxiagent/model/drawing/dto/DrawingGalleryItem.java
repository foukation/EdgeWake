package com.fxzs.lingxiagent.model.drawing.dto;

/**
 * 绘画画廊项目数据类
 */
public class DrawingGalleryItem {
    private String imageUrl;
    private String prompt;
    private String style;
    private String styleId;
    private String actionText;
    private int width;
    private int height;
    private String ratio;

    public DrawingGalleryItem() {
    }
    
    public DrawingGalleryItem(String imageUrl, String prompt, String style, String actionText) {
        this.imageUrl = imageUrl;
        this.prompt = prompt;
        this.style = style;
        this.actionText = actionText;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public String getPrompt() {
        return prompt;
    }
    
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
    
    public String getStyle() {
        return style;
    }
    
    public void setStyle(String style) {
        this.style = style;
    }
    
    public String getActionText() {
        return actionText;
    }
    
    public void setActionText(String actionText) {
        this.actionText = actionText;
    }

    public String getStyleId() {
        return styleId;
    }

    public void setStyleId(String styleId) {
        this.styleId = styleId;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getRatio() {
        return ratio;
    }

    public void setRatio(String ratio) {
        this.ratio = ratio;
    }
}