package com.fxzs.lingxiagent.model.scene.dto;

import java.util.List;

public class SceneResponse {
    private String errorCode;
    private String errorMessage;
    private List<ChoicesData> choices;
    private Session session;

    public SceneResponse(String errorCode, String errorMessage,
                         List<ChoicesData> choices, Session session) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.choices = choices;
        this.session = session;
    }

    // Getters
    public String getErrorCode() { return errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public List<ChoicesData> getChoices() { return choices; }
    public Session getSession() { return session; }

    // Setters
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public void setChoices(List<ChoicesData> choices) { this.choices = choices; }
    public void setSession(Session session) { this.session = session; }
}
