package com.fxzs.lingxiagent.model.chat.callback;


public interface AIMeetingEditCallback {
    void send(String content);
    void close();
    void voice();
    void keyboard();
    void pressDown();
    void pressUp(boolean isInArea);

    default void voiceMove(boolean status){};
}
