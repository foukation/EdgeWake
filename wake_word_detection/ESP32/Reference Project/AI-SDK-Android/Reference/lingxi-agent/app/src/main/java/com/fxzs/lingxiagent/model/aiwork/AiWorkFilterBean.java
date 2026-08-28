package com.fxzs.lingxiagent.model.aiwork;

public class AiWorkFilterBean {

    String name;
    int type;

    boolean isSelect = false;

    public AiWorkFilterBean(String name) {
        this.name = name;
    }

    public AiWorkFilterBean(String name,int type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public boolean isSelect() {
        return isSelect;
    }

    public void setSelect(boolean select) {
        isSelect = select;
    }

}
