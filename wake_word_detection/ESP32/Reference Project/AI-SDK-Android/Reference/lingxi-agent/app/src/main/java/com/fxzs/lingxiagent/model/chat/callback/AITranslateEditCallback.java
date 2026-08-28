package com.fxzs.lingxiagent.model.chat.callback;


public interface AITranslateEditCallback {
    void send(String content,String prompt);
    void close();
    void voice();
    void keyboard();
    void pressDown();

    void pressUp(boolean isInArea);

    default void voiceMove(boolean status){}

    default void send(String content,String prompt,String fromLang,String toLang){};
}
