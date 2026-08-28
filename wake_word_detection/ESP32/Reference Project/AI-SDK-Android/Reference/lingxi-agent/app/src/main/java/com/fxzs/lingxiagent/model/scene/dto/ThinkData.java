package com.fxzs.lingxiagent.model.scene.dto;

public class ThinkData {
    private String text;

    public ThinkData(String text) {
        this.text = text;
    }

    // Getters
    public String getThinkText() { return text; }

    // Setters
    public void setThinkText(String text) { this.text = text; }
}
