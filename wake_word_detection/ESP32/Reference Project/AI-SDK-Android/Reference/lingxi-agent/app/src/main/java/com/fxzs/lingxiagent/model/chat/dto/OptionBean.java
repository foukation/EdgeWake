package com.fxzs.lingxiagent.model.chat.dto;
//本地用的
public class OptionBean {
    int resId;
    String title;
    String subTitle;

    boolean isSelect;

    public OptionBean(int resId, String title) {
        this.resId = resId;
        this.title = title;
    }

    public int getResId() {
        return resId;
    }

    public void setResId(int resId) {
        this.resId = resId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
