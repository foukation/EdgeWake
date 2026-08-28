package com.fxzs.lingxiagent.lingxi.translate.model;

import java.util.List;

/**
 * 翻译结果数据模型
 */
public class TranslateDetailBean {

  List<TranslateResult> messageList;
  int type;

    public List<TranslateResult> getMessageList() {
        return messageList;
    }

    public void setMessageList(List<TranslateResult> messageList) {
        this.messageList = messageList;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}