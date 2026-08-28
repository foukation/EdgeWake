package com.fxzs.lingxiagent.model.chat.callback;

public interface RequestCallback<T> {
    void callback(T data);
}
