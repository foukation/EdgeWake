package com.fxzs.lingxiagent.model.chat.callback;


import com.fxzs.lingxiagent.model.chat.dto.ChatFileBean;
import com.fxzs.lingxiagent.model.chat.dto.OptionModel;

import java.util.List;

public interface SuperEditCallback {
    void send(String content, OptionModel selectOptionModel);
    void sendWithFile(String content, OptionModel selectOptionModel, List<ChatFileBean> fileList,boolean isFile);
    void voice();
    void keyboard();
    void pressDown();
    void pressUp(boolean isInArea);

    default void voiceMove(boolean status){}
    default void modeChange(OptionModel model,int size){}
}