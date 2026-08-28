package com.fxzs.lingxiagent.model.chat.dto;

public class EventBusShareCancelModel {
    boolean isCancel;
    String shareType;

    public EventBusShareCancelModel(boolean isCancel, String shareType) {
        this.isCancel = isCancel;
        this.shareType = shareType;
    }

    public boolean isCancel() {
        return isCancel;
    }
    public String getShareType() {
        return shareType;
    }
}
