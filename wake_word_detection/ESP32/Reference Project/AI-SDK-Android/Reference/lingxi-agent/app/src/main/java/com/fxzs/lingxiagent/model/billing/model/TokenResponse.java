package com.fxzs.lingxiagent.model.billing.model;
public class TokenResponse {

    private String token;
    private long expiresTime;
    private String tokenType;

    public String getToken() {
        return token;
    }

    public long getExpiresTime() {
        return expiresTime;
    }

    public String getTokenType() {
        return tokenType;
    }
}
