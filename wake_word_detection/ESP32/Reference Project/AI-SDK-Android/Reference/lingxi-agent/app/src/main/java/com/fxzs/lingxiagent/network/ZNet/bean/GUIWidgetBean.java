package com.fxzs.lingxiagent.network.ZNet.bean;

import java.util.List;

public class GUIWidgetBean {

    /**
     * categoryKey : office
     * categoryTitle : 电脑办公
     * displayText : 电脑办公常用操作
     * actionCommands : ["打开文档","打开表格","打开浏览器"]
     */

    private String categoryKey;
    private String categoryTitle;
    private String displayText;
    private List<String> actionCommands;

    public String getCategoryKey() {
        return categoryKey;
    }

    public void setCategoryKey(String categoryKey) {
        this.categoryKey = categoryKey;
    }

    public String getCategoryTitle() {
        return categoryTitle;
    }

    public void setCategoryTitle(String categoryTitle) {
        this.categoryTitle = categoryTitle;
    }

    public String getDisplayText() {
        return displayText;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    public List<String> getActionCommands() {
        return actionCommands;
    }

    public void setActionCommands(List<String> actionCommands) {
        this.actionCommands = actionCommands;
    }
}
