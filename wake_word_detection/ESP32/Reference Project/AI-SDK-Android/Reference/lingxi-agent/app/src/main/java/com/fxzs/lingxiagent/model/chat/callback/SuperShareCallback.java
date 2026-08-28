package com.fxzs.lingxiagent.model.chat.callback;


import com.fxzs.lingxiagent.model.chat.dto.ChatMessage;

import java.util.List;

public interface SuperShareCallback {
    List<ChatMessage> getSelectMessages();
    List<Integer> getSelectPositions();

    void closeBottomLayout();
    void onShareLongPic();
}