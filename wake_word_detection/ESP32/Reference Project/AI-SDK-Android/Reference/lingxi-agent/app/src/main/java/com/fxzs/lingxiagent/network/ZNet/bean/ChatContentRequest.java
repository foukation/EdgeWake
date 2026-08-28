package com.fxzs.lingxiagent.network.ZNet.bean;

import java.util.List;

public class ChatContentRequest {
    private String title;
    private List<ChatContent> history;

    public ChatContentRequest(String title, List<ChatContent> history) {
        this.title = title;
        this.history = history;
    }

    // Getters and setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<ChatContent> getHistory() {
        return history;
    }

    public void setHistory(List<ChatContent> history) {
        this.history = history;
    }
}