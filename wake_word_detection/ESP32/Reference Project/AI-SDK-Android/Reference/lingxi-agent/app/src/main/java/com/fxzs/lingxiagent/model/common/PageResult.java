package com.fxzs.lingxiagent.model.common;

import java.io.Serializable;
import java.util.List;

public class PageResult<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private List<T> records;
    private Long total;
    private Long pages;
    private Long current;
    private Long size;
    
    // 默认构造函数
    public PageResult() {
    }
    
    // Getters and Setters
    public List<T> getRecords() {
        return records;
    }
    
    public void setRecords(List<T> records) {
        this.records = records;
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