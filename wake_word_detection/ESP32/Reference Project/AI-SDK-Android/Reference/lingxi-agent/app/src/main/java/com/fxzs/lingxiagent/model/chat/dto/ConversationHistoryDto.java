package com.fxzs.lingxiagent.model.chat.dto;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * 对话历史记录DTO
 */
public class ConversationHistoryDto implements Serializable {
    
    @SerializedName("id")
    private Long id;
    
    @SerializedName("userId")
    private Long userId;
    
    @SerializedName("title")
    private String title;
    
    @SerializedName("pinned")
    private Boolean pinned;
    
    @SerializedName("roleId")
    private Long roleId;
    
    @SerializedName("modelId")
    private Long modelId;
    
    @SerializedName("model")
    private String model;
    
    @SerializedName("modelName")
    private String modelName;
    
    @SerializedName("systemMessage")
    private String systemMessage;
    
    @SerializedName("temperature")
    private Double temperature;
    
    @SerializedName("maxTokens")
    private Integer maxTokens;
    
    @SerializedName("maxContexts")
    private Integer maxContexts;
    
    @SerializedName("createTime")
    private Long createTime;
    
    @SerializedName("iconUrl")
    private String iconUrl;
    
    @SerializedName("roleName")
    private String roleName;
    
    @SerializedName("messageCount")
    private Integer messageCount;
    
    @SerializedName("lastMessage")
    private LastMessageDto lastMessage;
    
    @SerializedName("botDetail")
    private Object botDetail;
    
    @SerializedName("modelType")
    private Integer modelType;
    
    @SerializedName("transMap")
    private Object transMap;
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public Boolean getPinned() {
        return pinned;
    }
    
    public void setPinned(Boolean pinned) {
        this.pinned = pinned;
    }
    
    public Long getRoleId() {
        return roleId;
    }
    
    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }
    
    public Long getModelId() {
        return modelId;
    }
    
    public void setModelId(Long modelId) {
        this.modelId = modelId;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public String getModelName() {
        return modelName;
    }
    
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
    
    public String getSystemMessage() {
        return systemMessage;
    }
    
    public void setSystemMessage(String systemMessage) {
        this.systemMessage = systemMessage;
    }
    
    public Double getTemperature() {
        return temperature;
    }
    
    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }
    
    public Integer getMaxTokens() {
        return maxTokens;
    }
    
    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }
    
    public Integer getMaxContexts() {
        return maxContexts;
    }
    
    public void setMaxContexts(Integer maxContexts) {
        this.maxContexts = maxContexts;
    }
    
    public Long getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }
    
    public String getIconUrl() {
        return iconUrl;
    }
    
    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }
    
    public String getRoleName() {
        return roleName;
    }
    
    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
    
    public Integer getMessageCount() {
        return messageCount;
    }
    
    public void setMessageCount(Integer messageCount) {
        this.messageCount = messageCount;
    }
    
    public LastMessageDto getLastMessage() {
        return lastMessage;
    }
    
    public void setLastMessage(LastMessageDto lastMessage) {
        this.lastMessage = lastMessage;
    }
    
    public Object getBotDetail() {
        return botDetail;
    }
    
    public void setBotDetail(Object botDetail) {
        this.botDetail = botDetail;
    }
    
    public Integer getModelType() {
        return modelType;
    }
    
    public void setModelType(Integer modelType) {
        this.modelType = modelType;
    }
    
    public Object getTransMap() {
        return transMap;
    }
    
    public void setTransMap(Object transMap) {
        this.transMap = transMap;
    }
    
    /**
     * 最后一条消息DTO
     */
    public static class LastMessageDto implements Serializable {
        
        @SerializedName("createTime")
        private Long createTime;
        
        @SerializedName("updateTime")
        private Long updateTime;
        
        @SerializedName("creator")
        private String creator;
        
        @SerializedName("updater")
        private String updater;
        
        @SerializedName("deleted")
        private Boolean deleted;
        
        @SerializedName("id")
        private Long id;
        
        @SerializedName("conversationId")
        private Long conversationId;
        
        @SerializedName("replyId")
        private Long replyId;
        
        @SerializedName("type")
        private String type;
        
        @SerializedName("userId")
        private Long userId;
        
        @SerializedName("roleId")
        private Long roleId;
        
        @SerializedName("segmentIds")
        private Object segmentIds;
        
        @SerializedName("model")
        private String model;
        
        @SerializedName("modelId")
        private Long modelId;
        
        @SerializedName("content")
        private String content;
        
        @SerializedName("useContext")
        private Boolean useContext;
        
        @SerializedName("formatBody")
        private String formatBody;
        
        @SerializedName("images")
        private Object images;
        
        @SerializedName("fileContent")
        private Object fileContent;
        
        @SerializedName("fileListJson")
        private Object fileListJson;
        
        // Getters and Setters
        public Long getCreateTime() {
            return createTime;
        }
        
        public void setCreateTime(Long createTime) {
            this.createTime = createTime;
        }
        
        public Long getUpdateTime() {
            return updateTime;
        }
        
        public void setUpdateTime(Long updateTime) {
            this.updateTime = updateTime;
        }
        
        public String getCreator() {
            return creator;
        }
        
        public void setCreator(String creator) {
            this.creator = creator;
        }
        
        public String getUpdater() {
            return updater;
        }
        
        public void setUpdater(String updater) {
            this.updater = updater;
        }
        
        public Boolean getDeleted() {
            return deleted;
        }
        
        public void setDeleted(Boolean deleted) {
            this.deleted = deleted;
        }
        
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public Long getConversationId() {
            return conversationId;
        }
        
        public void setConversationId(Long conversationId) {
            this.conversationId = conversationId;
        }
        
        public Long getReplyId() {
            return replyId;
        }
        
        public void setReplyId(Long replyId) {
            this.replyId = replyId;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public Long getUserId() {
            return userId;
        }
        
        public void setUserId(Long userId) {
            this.userId = userId;
        }
        
        public Long getRoleId() {
            return roleId;
        }
        
        public void setRoleId(Long roleId) {
            this.roleId = roleId;
        }
        
        public Object getSegmentIds() {
            return segmentIds;
        }
        
        public void setSegmentIds(Object segmentIds) {
            this.segmentIds = segmentIds;
        }
        
        public String getModel() {
            return model;
        }
        
        public void setModel(String model) {
            this.model = model;
        }
        
        public Long getModelId() {
            return modelId;
        }
        
        public void setModelId(Long modelId) {
            this.modelId = modelId;
        }
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
        
        public Boolean getUseContext() {
            return useContext;
        }
        
        public void setUseContext(Boolean useContext) {
            this.useContext = useContext;
        }
        
        public String getFormatBody() {
            return formatBody;
        }
        
        public void setFormatBody(String formatBody) {
            this.formatBody = formatBody;
        }
        
        public Object getImages() {
            return images;
        }
        
        public void setImages(Object images) {
            this.images = images;
        }
        
        public Object getFileContent() {
            return fileContent;
        }
        
        public void setFileContent(Object fileContent) {
            this.fileContent = fileContent;
        }
        
        public Object getFileListJson() {
            return fileListJson;
        }
        
        public void setFileListJson(Object fileListJson) {
            this.fileListJson = fileListJson;
        }
    }
} 