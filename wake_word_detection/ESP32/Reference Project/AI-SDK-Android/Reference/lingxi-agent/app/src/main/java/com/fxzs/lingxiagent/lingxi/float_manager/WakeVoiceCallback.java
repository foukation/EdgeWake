package com.fxzs.lingxiagent.lingxi.float_manager;


public interface WakeVoiceCallback {
    void onSuccessMsg(String content);
    void onErrorMsg(String error,String content,int code);
}