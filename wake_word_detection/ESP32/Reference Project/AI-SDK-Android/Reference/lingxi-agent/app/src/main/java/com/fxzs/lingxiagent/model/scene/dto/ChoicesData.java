package com.fxzs.lingxiagent.model.scene.dto;

public class ChoicesData {
    private MessageData message;//非流式必填
    private MessageData delta;//流式必填

    public ChoicesData(MessageData message, MessageData delta) {
        this.message = message;
        this.delta = delta;
    }

    // Getters
    public MessageData getMessage() { return message; }
    public MessageData getMessageData() { return delta; }

    // Setters
    public void setMessage(MessageData message) { this.message = message; }
    public void setMessageData(MessageData delta) { this.delta = delta; }
}
