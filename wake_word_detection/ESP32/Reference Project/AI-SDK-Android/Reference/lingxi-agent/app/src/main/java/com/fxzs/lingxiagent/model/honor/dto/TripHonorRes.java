package com.fxzs.lingxiagent.model.honor.dto;

public class TripHonorRes {
    private String errorCode;
    private String errorMessage;
    private ChoicesData choices;
    private String sessionId;

    public TripHonorRes(String errorCode, String errorMessage,
                        ChoicesData choices, String sessionId) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.choices = choices;
        this.sessionId = sessionId;
    }

    // Getters
    public String getErrorCode() { return errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public ChoicesData getChoices() { return choices; }
    public String getSessionId() { return sessionId; }

    // Setters
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public void setChoices(ChoicesData choices) { this.choices = choices; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
}
