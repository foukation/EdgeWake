package com.fxzs.lingxiagent.network.ZNet.bean;

public class ChatContent {
    private String role;
    private String content;
    private String cardType;
    private String extraContent;

    public ChatContent(String role, String content, String cardType, String extraContent) {
        this.role = role;
        this.content = content;
        this.cardType = cardType;
        this.extraContent = extraContent;
    }

    // Getters and setters
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCardType() {
        return cardType;
    }

    public void setCardType(String cardType) {
        this.cardType = cardType;
    }
}