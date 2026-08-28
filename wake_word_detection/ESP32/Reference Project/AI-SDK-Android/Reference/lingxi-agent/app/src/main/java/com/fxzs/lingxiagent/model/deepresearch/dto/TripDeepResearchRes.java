package com.fxzs.lingxiagent.model.deepresearch.dto;

public class TripDeepResearchRes {
    private String errorCode;
    private String errorMessage;
    private String think;
    private WebSearch web_search;
    private String report;

    public TripDeepResearchRes(String think, WebSearch web_search, String report) {
        this.think = think;
        this.web_search = web_search;
        this.report = report;
    }
    public String getThink() { return think; }
    public void setThink(String think) { this.think = think; }
    public WebSearch getWeb_search() { return web_search; }
    public void setWeb_search(WebSearch web_search) { this.web_search = web_search; }
    public String getReport() { return report; }
    public void setReport(String report) { this.report = report; }
    // Getters
    public String getErrorCode() { return errorCode; }
    public String getErrorMessage() { return errorMessage; }

    // Setters
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
