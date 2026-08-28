package com.fxzs.lingxiagent.model.intention.repository;

public interface TokenCallback {
    void onSuccess();
    void onError(String errMsg);
}