package com.fxzs.lingxiagent.model.chat.callback;


import com.fxzs.lingxiagent.model.chat.dto.ChatMessage;

public interface MsgActionCallback {
    void refresh(String content);
    void refreshTranslation(String content,String fromLanguage,String toLanguage);
    void msgClick();
    void continueDrawing(ChatMessage message);
    void regenerateDrawing(ChatMessage message);
    void downloadDrawing(ChatMessage message);
    void viewDrawing(ChatMessage message);
    //默认头部点击事件
    default void sendMsg(String message){};
}
