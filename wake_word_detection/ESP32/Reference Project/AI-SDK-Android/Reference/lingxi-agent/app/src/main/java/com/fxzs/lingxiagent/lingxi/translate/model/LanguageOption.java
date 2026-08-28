package com.fxzs.lingxiagent.lingxi.translate.model;

/**
 * 语言选项数据模型
 */
public class LanguageOption {
    
    private String name;          // 语言名称，如"中文"
    private String code;          // 语言代码，如"zh"
    private boolean selected;     // 是否选中

    public LanguageOption() {
    }

    public LanguageOption(String name, String code) {
        this.name = name;
        this.code = code;
        this.selected = false;
    }

    public LanguageOption(String name, String code, boolean selected) {
        this.name = name;
        this.code = code;
        this.selected = selected;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    // 为了兼容，保留description属性的getter
    public String getDescription() {
        return code;
    }
}