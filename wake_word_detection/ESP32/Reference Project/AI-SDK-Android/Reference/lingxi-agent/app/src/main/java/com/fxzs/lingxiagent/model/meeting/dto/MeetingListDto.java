package com.fxzs.lingxiagent.model.meeting.dto;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class MeetingListDto implements Serializable {
    @SerializedName("total")
    private int total;

    @SerializedName("page")
    private int page;

    @SerializedName("size")
    private int size;

    @SerializedName("list")
    private List<MeetingDto> list;

    // Getters and setters
    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public List<MeetingDto> getList() {
        return list;
    }

    public void setList(List<MeetingDto> list) {
        this.list = list;
    }
}