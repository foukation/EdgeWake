package com.fxzs.lingxiagent.model.chat.dto;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

/**
 * 对话历史记录分页结果DTO
 */
public class ConversationHistoryListDto implements Serializable {
    
    @SerializedName("list")
    private List<ConversationHistoryDto> list;
    
    @SerializedName("total")
    private Integer total;
    
    // Getters and Setters
    public List<ConversationHistoryDto> getList() {
        return list;
    }
    
    public void setList(List<ConversationHistoryDto> list) {
        this.list = list;
    }
    
    public Integer getTotal() {
        return total;
    }
    
    public void setTotal(Integer total) {
        this.total = total;
    }
} 