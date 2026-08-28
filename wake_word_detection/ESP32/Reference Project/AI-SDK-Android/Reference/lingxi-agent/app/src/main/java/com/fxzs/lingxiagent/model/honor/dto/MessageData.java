package com.fxzs.lingxiagent.model.honor.dto;

public class MessageData {
    private String contentType;
    private HybridContentData hybridContent;

    public MessageData(String contentType, HybridContentData hybridContent) {
        this.contentType = contentType;
        this.hybridContent = hybridContent;
    }

    // Getters
    public String getContentType() { return contentType; }
    public HybridContentData getHybridContent() { return hybridContent; }

    // Setters
    public void setContentType(String contentType) { this.contentType = contentType; }
    public void setHybridContent(HybridContentData hybridContent) { this.hybridContent = hybridContent; }
}

