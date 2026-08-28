package com.fxzs.lingxiagent.model.chat.dto;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

/**
 * 指定对话的消息分页结果 DTO
 */
public class ConversationDetailPageDto implements Serializable {

    @SerializedName("list")
    private List<ConversationDetailDto> list;

    @SerializedName("total")
    private Integer total;

    public List<ConversationDetailDto> getList() {
        return list;
    }

    public void setList(List<ConversationDetailDto> list) {
        this.list = list;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }
}


