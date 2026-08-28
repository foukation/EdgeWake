package com.fxzs.lingxiagent.model.intention.dto;

public class ClientActionsMulRequest {
    public String text;
    public String SessionID;

    public ClientActionsMulRequest(String text, String sessionID) {
        this.text = text;
        this.SessionID = sessionID;
    }
}

