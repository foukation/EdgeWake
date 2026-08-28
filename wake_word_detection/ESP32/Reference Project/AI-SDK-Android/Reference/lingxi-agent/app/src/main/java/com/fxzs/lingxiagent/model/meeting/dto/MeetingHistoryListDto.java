package com.fxzs.lingxiagent.model.meeting.dto;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

/**
 * 会议历史记录分页结果DTO
 */
public class MeetingHistoryListDto implements Serializable {
    
    @SerializedName("list")
    private List<MeetingHistoryDto> list;
    
    @SerializedName("total")
    private Integer total;
    
    // Getters and Setters
    public List<MeetingHistoryDto> getList() {
        return list;
    }
    
    public void setList(List<MeetingHistoryDto> list) {
        this.list = list;
    }
    
    public Integer getTotal() {
        return total;
    }
    
    public void setTotal(Integer total) {
        this.total = total;
    }
}
