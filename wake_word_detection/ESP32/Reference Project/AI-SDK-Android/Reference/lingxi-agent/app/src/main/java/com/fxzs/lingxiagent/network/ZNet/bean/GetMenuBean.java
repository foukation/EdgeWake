package com.fxzs.lingxiagent.network.ZNet.bean;

public class GetMenuBean {
//    {"id":4,"name":"灵犀智能体","menuId":2,"menuName":"AI对话页面"}
    int id;
    String name;
    int menuId;
    String menuName;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMenuId() {
        return menuId;
    }

    public void setMenuId(int menuId) {
        this.menuId = menuId;
    }

    public String getMenuName() {
        return menuName;
    }

    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }
}
