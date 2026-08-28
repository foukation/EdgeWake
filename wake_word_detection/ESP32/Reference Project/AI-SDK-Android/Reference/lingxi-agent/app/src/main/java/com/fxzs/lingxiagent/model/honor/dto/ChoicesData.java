package com.fxzs.lingxiagent.model.honor.dto;

public class ChoicesData {
    private MessageData message;
    private String finishReason;

    public ChoicesData(MessageData message, String finishReason) {
        this.message = message;
        this.finishReason = finishReason;
    }

    // Getters
    public MessageData getMessage() { return message; }
    public String getFinishReason() { return finishReason; }

    // Setters
    public void setMessage(MessageData message) { this.message = message; }
    public void setFinishReason(String finishReason) { this.finishReason = finishReason; }
}
