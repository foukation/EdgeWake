package com.fxzs.lingxiagent.model.drawing.dto;

import java.io.Serializable;

/**
 * 绘画图片DTO
 */
public class DrawingImageDto implements Serializable {
    
    private Long id;
    private String imageUrl;       // 图片URL
    private String sampleUrl;      // 示例图片URL（API可能返回此字段）
    private String picUrl;         // 图片URL（Volc API返回此字段）
    private String thumbnailUrl;   // 缩略图URL
    private String prompt;         // 提示词
    private String style;          // 风格
    private String aspectRatio;    // 宽高比
    private Integer width;         // 宽度
    private Integer height;        // 高度
    private String taskId;         // 任务ID
    private Integer status;        // 状态：0-生成中 1-成功 2-失败
    private String errorMsg;       // 错误信息
    private Long sessionId;        // 会话ID
    private String createTime;     // 创建时间
    private String updateTime;     // 更新时间

    private long styleId;

    Options options;
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getImageUrl() {
        // 优先返回imageUrl，如果为空则返回picUrl，最后返回sampleUrl
        if (imageUrl != null && !imageUrl.isEmpty()) {
            return imageUrl;
        } else if (picUrl != null && !picUrl.isEmpty()) {
            return picUrl;
        } else {
            return sampleUrl;
        }
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
    
    public String getPicUrl() {
        return picUrl;
    }
    
    public void setPicUrl(String picUrl) {
        this.picUrl = picUrl;
    }
    
    public String getThumbnailUrl() {
        return thumbnailUrl;
    }
    
    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
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
    
    public String getAspectRatio() {
        return aspectRatio;
    }
    
    public void setAspectRatio(String aspectRatio) {
        this.aspectRatio = aspectRatio;
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
    
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
    }
    
    public String getErrorMsg() {
        return errorMsg;
    }
    
    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }
    
    public Long getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }
    
    public String getUpdateTime() {
        return updateTime;
    }
    
    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public long getStyleId() {
        return styleId;
    }

    public void setStyleId(long styleId) {
        this.styleId = styleId;
    }


    public Options getOptions() {
        return options;
    }

    public void setOptions(Options options) {
        this.options = options;
    }

    public class Options implements Serializable{
        public String size;
        public String model;
        public int styleId;
        public boolean watermark;
        public String realPrompt;
        public String stylePrompt;
        public String referenceImageUrl;
        public String sequentialImageGeneration;

        public String getSize() {
            return size;
        }

        public void setSize(String size) {
            this.size = size;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public int getStyleId() {
            return styleId;
        }

        public void setStyleId(int styleId) {
            this.styleId = styleId;
        }

        public boolean isWatermark() {
            return watermark;
        }

        public void setWatermark(boolean watermark) {
            this.watermark = watermark;
        }

        public String getRealPrompt() {
            return realPrompt;
        }

        public void setRealPrompt(String realPrompt) {
            this.realPrompt = realPrompt;
        }

        public String getStylePrompt() {
            return stylePrompt;
        }

        public void setStylePrompt(String stylePrompt) {
            this.stylePrompt = stylePrompt;
        }

        public String getReferenceImageUrl() {
            return referenceImageUrl;
        }

        public void setReferenceImageUrl(String referenceImageUrl) {
            this.referenceImageUrl = referenceImageUrl;
        }

        public String getSequentialImageGeneration() {
            return sequentialImageGeneration;
        }

        public void setSequentialImageGeneration(String sequentialImageGeneration) {
            this.sequentialImageGeneration = sequentialImageGeneration;
        }
    }
}