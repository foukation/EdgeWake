package com.fxzs.lingxiagent.model.chat.callback;


public interface NexusVoiceCallback {
    void onSuccessMsg(String content);
    void onErrorMsg(String error);
}