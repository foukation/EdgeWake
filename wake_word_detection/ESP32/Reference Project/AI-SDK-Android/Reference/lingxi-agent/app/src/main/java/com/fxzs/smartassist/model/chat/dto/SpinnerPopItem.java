package com.fxzs.smartassist.model.chat.dto;

public class SpinnerPopItem {
    private String name;
    private String checkItem;

    public SpinnerPopItem(String name) {
        this.name = name;
    }

    public SpinnerPopItem(String name, String checkItem) {
        this.name = name;
        this.checkItem = checkItem;
    }

    public String getName() { return name; }
    public String getCheckItem() { return checkItem; }
}
