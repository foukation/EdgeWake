package com.fxzs.lingxiagent.model.scene.dto;

public class RecommendPrompt {
    private String text;

    public RecommendPrompt(String text) {
        this.text = text;
    }

    // Getters
    public String getPromptText() { return text; }

    // Setters
    public void setPromptText(String text) { this.text = text; }
}

