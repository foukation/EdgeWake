package com.fxzs.lingxiagent.lingxi.translate.model;

/**
 * 对话结果数据模型
 */
public class DialogResult {
    
    private String originalText;      // 原文
    private String translatedText;    // 译文
    private long timestamp;           // 时间戳
    private boolean isFromLanguageA;  // 是否来自语言A（true: A->B, false: B->A）
    private boolean isCompleted;      // 是否完成翻译
    
    public DialogResult() {
        this.timestamp = System.currentTimeMillis();
        this.isCompleted = false;
    }
    
    public DialogResult(String originalText, String translatedText, boolean isFromLanguageA) {
        this.originalText = originalText;
        this.translatedText = translatedText;
        this.isFromLanguageA = isFromLanguageA;
        this.timestamp = System.currentTimeMillis();
        this.isCompleted = true;
    }

    public String getOriginalText() {
        return originalText;
    }

    public void setOriginalText(String originalText) {
        this.originalText = originalText;
    }

    public String getTranslatedText() {
        return translatedText;
    }

    public void setTranslatedText(String translatedText) {
        this.translatedText = translatedText;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isFromLanguageA() {
        return isFromLanguageA;
    }

    public void setFromLanguageA(boolean fromLanguageA) {
        isFromLanguageA = fromLanguageA;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }
}