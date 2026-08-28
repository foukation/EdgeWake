package com.fxzs.lingxiagent.model.intention.repository;

public interface ApiCallback<T> {
    void onSuccess(T result);
    void onError(String errMsg);
}