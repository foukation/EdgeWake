package com.fxzs.lingxiagent.model.chat.callback;


public interface SSECallback {
    void receive(String content);
    void end();
}
