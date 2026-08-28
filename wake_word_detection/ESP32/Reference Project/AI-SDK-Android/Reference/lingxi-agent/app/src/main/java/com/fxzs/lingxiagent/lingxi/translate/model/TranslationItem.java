package com.fxzs.lingxiagent.lingxi.translate.model;

/**
 * 翻译结果数据项
 */
public class TranslationItem {
    private String seId;
    private int seVer;
    private String sourceText;
    private String targetText;
    private int startTime;
    private int endTime;
    private boolean isEnd;
    private String audio;
    
    public TranslationItem(String seId, int seVer, String sourceText, String targetText, 
                          int startTime, int endTime, boolean isEnd, String audio) {
        this.seId = seId;
        this.seVer = seVer;
        this.sourceText = sourceText;
        this.targetText = targetText;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isEnd = isEnd;
        this.audio = audio;
    }
    
    // Getters
    public String getSeId() { return seId; }
    public int getSeVer() { return seVer; }
    public String getSourceText() { return sourceText; }
    public String getTargetText() { return targetText; }
    public int getStartTime() { return startTime; }
    public int getEndTime() { return endTime; }
    public boolean isEnd() { return isEnd; }
    public String getAudio() { return audio; }
    
    // Setters
    public void setSeId(String seId) { this.seId = seId; }
    public void setSeVer(int seVer) { this.seVer = seVer; }
    public void setSourceText(String sourceText) { this.sourceText = sourceText; }
    public void setTargetText(String targetText) { this.targetText = targetText; }
    public void setStartTime(int startTime) { this.startTime = startTime; }
    public void setEndTime(int endTime) { this.endTime = endTime; }
    public void setIsEnd(boolean isEnd) { this.isEnd = isEnd; }
    public void setAudio(String audio) { this.audio = audio; }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TranslationItem that = (TranslationItem) obj;
        return seId != null ? seId.equals(that.seId) : that.seId == null;
    }
    
    @Override
    public int hashCode() {
        return seId != null ? seId.hashCode() : 0;
    }
    
    @Override
    public String toString() {
        return "TranslationItem{" +
                "seId='" + seId + '\'' +
                ", sourceText='" + sourceText + '\'' +
                ", targetText='" + targetText + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", isEnd=" + isEnd +
                '}';
    }
}