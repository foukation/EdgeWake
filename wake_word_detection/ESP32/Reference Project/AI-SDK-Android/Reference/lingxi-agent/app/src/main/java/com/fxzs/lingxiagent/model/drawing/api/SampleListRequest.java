package com.fxzs.lingxiagent.model.drawing.api;

import java.io.Serializable;

/**
 * 示例列表请求参数
 */
public class SampleListRequest implements Serializable {
    
    private Long catId;         // 分类ID
    private Integer pageNo = 1;     // 页码
    private Integer pageSize = 10;  // 每页数量
    
    // Getters and Setters
    public Long getCatId() {
        return catId;
    }
    
    public void setCatId(Long catId) {
        this.catId = catId;
    }
    
    public Integer getPageNo() {
        return pageNo;
    }
    
    public void setPageNo(Integer pageNo) {
        this.pageNo = pageNo;
    }
    
    public Integer getPageSize() {
        return pageSize;
    }
    
    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
    
    @Override
    public String toString() {
        return "SampleListRequest{" +
                "catId=" + catId +
                ", pageNo=" + pageNo +
                ", pageSize=" + pageSize +
                '}';
    }
}