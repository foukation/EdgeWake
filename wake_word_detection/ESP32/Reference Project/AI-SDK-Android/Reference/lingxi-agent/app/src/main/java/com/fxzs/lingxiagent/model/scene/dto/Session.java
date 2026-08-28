package com.fxzs.lingxiagent.model.scene.dto;

public class Session {
    private String attributes;
    private String sessionId;

    public Session(String sessionId) {
        this.sessionId = sessionId;
    }

    // Getters
    public String getSessionId() { return sessionId; }

    // Setters
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getAttributes() { return attributes; }

    // Setters
    public void setAttributes(String attributes) { this.attributes = attributes; }
}
