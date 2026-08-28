package com.fxzs.lingxiagent.model.drawing.api;

import java.util.List;

/**
 * 分页结果
 */
public class PageResult<T> {
    private List<T> records;
    private List<T> list;  // 可能API返回的是list字段
    private List<T> data;  // 或者是data字段
    private Long total;
    private Long pages;
    private Long current;
    private Long size;
    
    public List<T> getRecords() {
        // 优先返回records，如果为空则尝试list，再尝试data
        if (records != null) return records;
        if (list != null) return list;
        if (data != null) return data;
        return null;
    }
    
    public void setRecords(List<T> records) {
        this.records = records;
    }
    
    public List<T> getList() {
        return list;
    }
    
    public void setList(List<T> list) {
        this.list = list;
    }
    
    public List<T> getData() {
        return data;
    }
    
    public void setData(List<T> data) {
        this.data = data;
    }
    
    public Long getTotal() {
        return total;
    }
    
    public void setTotal(Long total) {
        this.total = total;
    }
    
    public Long getPages() {
        return pages;
    }
    
    public void setPages(Long pages) {
        this.pages = pages;
    }
    
    public Long getCurrent() {
        return current;
    }
    
    public void setCurrent(Long current) {
        this.current = current;
    }
    
    public Long getSize() {
        return size;
    }
    
    public void setSize(Long size) {
        this.size = size;
    }
}