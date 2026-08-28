package com.fxzs.lingxiagent.model.history;

public class Options{
    public Object seed;
    public int width;
    public int height;
    public String prompt;
    public Object options;
    public int styleId;
    public Object imagUrls;
    public int ddimSteps;
    public int sessionId;
    public Object reqScheduleConf;

    public Object getSeed() {
        return seed;
    }

    public void setSeed(Object seed) {
        this.seed = seed;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public Object getOptions() {
        return options;
    }

    public void setOptions(Object options) {
        this.options = options;
    }

    public int getStyleId() {
        return styleId;
    }

    public void setStyleId(int styleId) {
        this.styleId = styleId;
    }

    public Object getImagUrls() {
        return imagUrls;
    }

    public void setImagUrls(Object imagUrls) {
        this.imagUrls = imagUrls;
    }

    public int getDdimSteps() {
        return ddimSteps;
    }

    public void setDdimSteps(int ddimSteps) {
        this.ddimSteps = ddimSteps;
    }

    public int getSessionId() {
        return sessionId;
    }

    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }

    public Object getReqScheduleConf() {
        return reqScheduleConf;
    }

    public void setReqScheduleConf(Object reqScheduleConf) {
        this.reqScheduleConf = reqScheduleConf;
    }
}
