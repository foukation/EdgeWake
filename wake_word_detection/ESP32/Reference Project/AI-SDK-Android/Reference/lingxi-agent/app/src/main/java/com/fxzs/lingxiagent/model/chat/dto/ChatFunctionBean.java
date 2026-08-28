package com.fxzs.lingxiagent.model.chat.dto;

import com.fxzs.lingxiagent.network.ZNet.bean.getCatDetailListBean;

public class ChatFunctionBean {
    int id;
    int icon;
    String name;
    getCatDetailListBean getCatDetailListBean;

    public ChatFunctionBean(int id, int icon, String name) {
        this.id = id;
        this.icon = icon;
        this.name = name;
    }

    public ChatFunctionBean(int id, int icon, String name, getCatDetailListBean getCatDetailListBean) {
        this.id = id;
        this.icon = icon;
        this.name = name;
        this.getCatDetailListBean = getCatDetailListBean;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public getCatDetailListBean getCatDetailListBean() {
        return getCatDetailListBean;
    }

    public void setCatDetailListBean(getCatDetailListBean getCatDetailListBean) {
        this.getCatDetailListBean = getCatDetailListBean;
    }
}
