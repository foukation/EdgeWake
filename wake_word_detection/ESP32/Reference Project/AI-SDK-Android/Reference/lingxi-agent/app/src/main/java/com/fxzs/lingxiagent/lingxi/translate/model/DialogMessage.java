package com.fxzs.lingxiagent.lingxi.translate.model;

/**
 * 对话模式消息项
 * 每条消息包含显示文本、说话人信息和消息类型
 */
public class DialogMessage {
    public static final int TYPE_ORIGINAL = 1;  // 原文（自己说的）
    public static final int TYPE_TRANSLATION = 2;  // 译文（对方说的）
    
    private String seId;
    private String text;
    private int messageType;  // TYPE_ORIGINAL 或 TYPE_TRANSLATION
    private String speakerName;  // 说话人名称
    private boolean isRecognizing;  // 是否正在识别中
    private long timestamp;
    
    public DialogMessage(String seId, String text, int messageType, String speakerName, boolean isRecognizing) {
        this.seId = seId;
        this.text = text;
        this.messageType = messageType;
        this.speakerName = speakerName;
        this.isRecognizing = isRecognizing;
        this.timestamp = System.currentTimeMillis();
    }
    
    // Getters
    public String getSeId() { return seId; }
    public String getText() { return text; }
    public int getMessageType() { return messageType; }
    public String getSpeakerName() { return speakerName; }
    public boolean isRecognizing() { return isRecognizing; }
    public long getTimestamp() { return timestamp; }
    
    // Setters
    public void setSeId(String seId) { this.seId = seId; }
    public void setText(String text) { this.text = text; }
    public void setRecognizing(boolean recognizing) { this.isRecognizing = recognizing; }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DialogMessage that = (DialogMessage) obj;
        return seId != null ? seId.equals(that.seId) : that.seId == null;
    }
    
    @Override
    public int hashCode() {
        return seId != null ? seId.hashCode() : 0;
    }
}