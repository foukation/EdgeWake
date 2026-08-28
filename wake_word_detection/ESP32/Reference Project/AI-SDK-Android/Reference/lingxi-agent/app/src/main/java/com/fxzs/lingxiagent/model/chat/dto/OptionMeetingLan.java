package com.fxzs.lingxiagent.model.chat.dto;
//本地用的
public class OptionMeetingLan {
    int resId;
    String title;
    String subTitle;
    String key; // API返回的key值，如"16k_zh_large"

    boolean isSelect;

    public OptionMeetingLan(String title, String subTitle, boolean isSelect) {
        this.title = title;
        this.subTitle = subTitle;
        this.isSelect = isSelect;
    }

    public OptionMeetingLan(String key, String title, String subTitle, boolean isSelect) {
        this.key = key;
        this.title = title;
        this.subTitle = subTitle;
        this.isSelect = isSelect;
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

    public String getSubTitle() {
        return subTitle;
    }

    public void setSubTitle(String subTitle) {
        this.subTitle = subTitle;
    }

    public boolean isSelect() {
        return isSelect;
    }

    public void setSelect(boolean select) {
        isSelect = select;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
