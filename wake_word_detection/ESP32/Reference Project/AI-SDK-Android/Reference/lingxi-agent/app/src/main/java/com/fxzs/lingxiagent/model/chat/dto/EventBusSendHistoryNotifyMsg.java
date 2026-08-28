package com.fxzs.lingxiagent.model.chat.dto;

public class EventBusSendHistoryNotifyMsg {

    String message;

    public EventBusSendHistoryNotifyMsg(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
