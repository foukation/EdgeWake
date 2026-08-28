package com.fxzs.lingxiagent.model.chat.dto;

import java.util.List;

public class AiWritingTypeBean {

    String name;
    String SubName;

    boolean isSelect = false;

    List<AiWritingTypeBean> listModel;


    public AiWritingTypeBean(String name) {
        this.name = name;
    }

    public AiWritingTypeBean(String name, List<AiWritingTypeBean> listModel) {
        this.name = name;
        this.listModel = listModel;
    }

    public AiWritingTypeBean(String name, String subName, boolean isSelect) {
        this.name = name;
        SubName = subName;
        this.isSelect = isSelect;
    }

    public AiWritingTypeBean(String name, String subName, boolean isSelect, List<AiWritingTypeBean> listModel) {
        this.name = name;
        SubName = subName;
        this.isSelect = isSelect;
        this.listModel = listModel;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSubName() {
        return SubName;
    }

    public void setSubName(String subName) {
        SubName = subName;
    }

    public boolean isSelect() {
        return isSelect;
    }

    public void setSelect(boolean select) {
        isSelect = select;
    }

    public List<AiWritingTypeBean> getListModel() {
        return listModel;
    }

    public void setListModel(List<AiWritingTypeBean> listModel) {
        this.listModel = listModel;
    }
}
