package com.fxzs.lingxiagent.model.chat.callback;

public interface DialogEditCallback {
    void callback(String result);

    default void onCancel(String result) {

    }
}
