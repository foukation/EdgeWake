package com.fxzs.lingxiagent.model.chat.dto;

public class ConversationDetailDto {
    public int id;
    public int conversationId;
    public int replyId;
    public String type;
    public int userId;
    public int roleId;
    public String model;
    public int modelId;
    public String content;
    public boolean useContext;
    public String createTime;
    public String roleName;
    public String formatBody;
    public String images;
    public String fileListJson;
    // 新增：思考内容与卡片文本
    public String thinkText;
    public String cardText;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getConversationId() {
        return conversationId;
    }

    public void setConversationId(int conversationId) {
        this.conversationId = conversationId;
    }

    public int getReplyId() {
        return replyId;
    }

    public void setReplyId(int replyId) {
        this.replyId = replyId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getRoleId() {
        return roleId;
    }

    public void setRoleId(int roleId) {
        this.roleId = roleId;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getModelId() {
        return modelId;
    }

    public void setModelId(int modelId) {
        this.modelId = modelId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isUseContext() {
        return useContext;
    }

    public void setUseContext(boolean useContext) {
        this.useContext = useContext;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getFormatBody() {
        return formatBody;
    }

    public void setFormatBody(String formatBody) {
        this.formatBody = formatBody;
    }

    public String getImages() {
        return images;
    }

    public void setImages(String images) {
        this.images = images;
    }

    public String getFileListJson() {
        return fileListJson;
    }

    public void setFileListJson(String fileListJson) {
        this.fileListJson = fileListJson;
    }

    public String getThinkText() {
        return thinkText;
    }

    public void setThinkText(String thinkText) {
        this.thinkText = thinkText;
    }

    public String getCardText() {
        return cardText;
    }

    public void setCardText(String cardText) {
        this.cardText = cardText;
    }
}
