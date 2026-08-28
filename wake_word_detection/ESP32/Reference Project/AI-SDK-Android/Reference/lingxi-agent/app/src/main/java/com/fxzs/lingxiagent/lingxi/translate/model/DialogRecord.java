package com.fxzs.lingxiagent.lingxi.translate.model;

public class DialogRecord {
    private String sourceText;
    private String targetText;
    private String startTime; // yyyy-MM-dd HH:mm:ss
    private String endTime;   // yyyy-MM-dd HH:mm:ss
    private int translationId;
    private int speakerId; // A=1, B=2
    private String source; // language code
    private String target; // language code

    public DialogRecord() {}

    public String getSourceText() { return sourceText; }
    public void setSourceText(String sourceText) { this.sourceText = sourceText; }

    public String getTargetText() { return targetText; }
    public void setTargetText(String targetText) { this.targetText = targetText; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public int getTranslationId() { return translationId; }
    public void setTranslationId(int translationId) { this.translationId = translationId; }

    public int getSpeakerId() { return speakerId; }
    public void setSpeakerId(int speakerId) { this.speakerId = speakerId; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
}

