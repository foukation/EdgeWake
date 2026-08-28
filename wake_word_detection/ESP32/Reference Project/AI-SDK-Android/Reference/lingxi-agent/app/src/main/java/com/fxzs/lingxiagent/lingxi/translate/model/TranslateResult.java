package com.fxzs.lingxiagent.lingxi.translate.model;

/**
 * 翻译结果数据模型
 */
public class TranslateResult {
    
    private String originalText;      // 原文
    private String source;      // 原文lan
    private String sourceText;      // 原文lan
    private String translatedText;    // 译文
    private String target;    // 译文lan
    private String targetText;    // 译文lan
    private long timestamp;           // 时间戳
    private boolean isCompleted;      // 是否完成翻译
    
    public TranslateResult() {
        this.timestamp = System.currentTimeMillis();
        this.isCompleted = false;
    }
    
    public TranslateResult(String originalText, String translatedText) {
        this.originalText = originalText;
        this.translatedText = translatedText;
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

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getSourceText() {
        return sourceText;
    }

    public void setSourceText(String sourceText) {
        this.sourceText = sourceText;
    }

    public String getTargetText() {
        return targetText;
    }

    public void setTargetText(String targetText) {
        this.targetText = targetText;
    }
}