package com.fxzs.lingxiagent.model.chat.api;

import com.fxzs.lingxiagent.model.chat.dto.Credentials;

import java.util.Date;

public class getStsConfig {
    public Credentials credentials;
    public String requestId;
    public Date expiration;
    public String objectKey;
    public int startTime;
    public int expiredTime;

    public Credentials getCredentials() {
        return credentials;
    }

    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Date getExpiration() {
        return expiration;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setExpiration(Date expiration) {
        this.expiration = expiration;
    }

    public int getStartTime() {
        return startTime;
    }

    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }

    public int getExpiredTime() {
        return expiredTime;
    }

    public void setExpiredTime(int expiredTime) {
        this.expiredTime = expiredTime;
    }
}
